package rxf.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;

import static java.nio.channels.SelectionKey.OP_READ;
import static one.xio.HttpMethod.GET;
import static rxf.server.CouchMetaDriver.ACCEPT;
import static rxf.server.CouchMetaDriver.CONTENT_ENCODING;
import static rxf.server.CouchMetaDriver.CONTENT_LENGTH;
import static rxf.server.CouchMetaDriver.CONTENT_TYPE;
import static rxf.server.CouchMetaDriver.ETAG;
import static rxf.server.CouchMetaDriver.TRANSFER_ENCODING;

/**
 * a POST interception wrapper for http protocol cracking
 * User: jim
 * Date: 4/18/12
 * Time: 12:37 PM
 */
public class ProtocolMethodDispatch extends Impl {
  /**
   * this is set, and hopefully helps with cookies and session details, but is possibly redundant with other means of
   * headerstate access.
   */
  public static ThreadLocal<Rfc822HeaderState> RFState = new ThreadLocal<Rfc822HeaderState>();
  public static final EnumMap<HttpMethod, LinkedHashMap<Pattern, Impl>> NAMESPACE = new EnumMap<HttpMethod, LinkedHashMap<Pattern, Impl>>(HttpMethod.class) {
    {
      put(HttpMethod.POST, new LinkedHashMap<Pattern, Impl>() {{
        put(Pattern.compile("^/gwtRequest"), new GwtRequestFactoryVisitor());
      }});

      final Pattern passthroughExpr = Pattern.compile("^/i(/.*)$");
      put(GET, new LinkedHashMap<Pattern, Impl>() {
        {
          put(passthroughExpr, new HttpProxyImpl(passthroughExpr));
          put(Pattern.compile(".*"), new ContentRootImpl(System.getProperty("rxf.server.content.root", "./")));
        }
      });
    }
  };

  @Override
  public void onAccept(SelectionKey key) throws IOException {
    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
    SocketChannel accept = channel.accept();
    accept.configureBlocking(false);
    HttpMethod.enqueue(accept, OP_READ, this);

  }

  @Override
  public void onRead(SelectionKey key) throws Exception {
    final SocketChannel channel = (SocketChannel) key.channel();

    ByteBuffer cursor = ByteBuffer.allocateDirect(BlobAntiPatternObject.getReceiveBufferSize());
    int read = channel.read(cursor);
    if (-1 == read) {
      ((SocketChannel) key.channel()).socket().close();//cancel();
      return;
    }
    //break down the incoming headers.
    final Rfc822HeaderState state;
    RFState.set(state = new Rfc822HeaderState(CONTENT_LENGTH, CONTENT_TYPE, CONTENT_ENCODING, ETAG, TRANSFER_ENCODING, ACCEPT).cookies(BlobAntiPatternObject.class.getCanonicalName(), BlobAntiPatternObject.MYGEOIPSTRING).sourceKey(key).apply((ByteBuffer) cursor.flip()));


    //find the method to dispatch
    HttpMethod method = HttpMethod.valueOf(state.methodProtocol());

    if (null == method) {
      ((SocketChannel) key.channel()).socket().close();//cancel();

      return;
    }
    //check for namespace registration
    // todo: preRead is  wierd initiailizer which needs some review.
    for (Entry<Pattern, Impl> visitorEntry : BlobAntiPatternObject.getNamespace().get(method).entrySet()) {
      Matcher matcher = visitorEntry.getKey().matcher(state.pathResCode());
      if (matcher.find()) {
        Impl impl = visitorEntry.getValue();

        Impl ob = impl.preRead(state, cursor);
        if (null != ob) {
          key.attach(ob);
//        visitorEntry.getValue().onRead(key);
          key.selector().wakeup();
        }
        return;
      }
    }
    switch (method) {
      default:
        throw new Error(BlobAntiPatternObject.arrToString("unknown method in", state));
    }
  }
}

