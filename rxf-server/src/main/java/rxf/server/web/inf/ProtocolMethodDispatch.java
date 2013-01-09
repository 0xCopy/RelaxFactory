package rxf.server.web.inf;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;
import rxf.server.*;
import rxf.server.Rfc822HeaderState.HttpRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.channels.SelectionKey.OP_READ;
import static one.xio.HttpMethod.GET;
import static one.xio.HttpMethod.POST;
import static rxf.server.BlobAntiPatternRelic.deepToString;
import static rxf.server.CouchNamespace.NAMESPACE;
import static rxf.server.RelaxFactoryServer.App;
import static rxf.server.RelaxFactoryServer.UTF8;

/**
 * this class holds a protocol namespace to dispatch requests
 * <p/>
 * {@link  rxf.server.CouchNamespace#NAMESPACE } is a  map of http methods each containing an ordered map of regexes tested in order of
 * map insertion.
 * <p/>
 * User: jim
 * Date: 4/18/12
 * Time: 12:37 PM
 */
public class ProtocolMethodDispatch extends Impl {

  /**
   * if you *must* register a new priority ahead of existing patterns, this is how.  otherwise the LinkedHashMap stays ordered as defined.
   *
   * @param method
   * @param pattern
   * @param impl
   * @return
   */
  public static Map<Pattern, Class<? extends Impl>> precedeAllWith(HttpMethod method,
      Pattern pattern, Class<? extends Impl> impl) {
    Map<Pattern, Class<? extends Impl>> patternImplMap = NAMESPACE.get(method);
    Map<Pattern, Class<? extends Impl>> ret = new LinkedHashMap<Pattern, Class<? extends Impl>>();
    ret.put(pattern, impl);
    if (null == patternImplMap)
      patternImplMap = new LinkedHashMap<Pattern, Class<? extends Impl>>();

    for (Entry<Pattern, Class<? extends Impl>> patternImplEntry : patternImplMap.entrySet()) {
      ret.put(patternImplEntry.getKey(), patternImplEntry.getValue());
    }
    NAMESPACE.put(method, ret);
    return patternImplMap;

  }

  /**
   * the PUT protocol handlers, only static for the sake of javadocs
   */
  public static Map<Pattern, Class<? extends Impl>> POSTmap =
      new LinkedHashMap<Pattern, Class<? extends Impl>>();

  /**
   * the GET protocol handlers, only static for the sake of javadocs
   */
  public static Map<Pattern, Class<? extends Impl>> GETmap =
      new LinkedHashMap<Pattern, Class<? extends Impl>>();

  static {
    NAMESPACE.put(POST, POSTmap);
    NAMESPACE.put(GET, GETmap);

    /**
     * for gwt requestfactory done via POST.
     *
     * TODO: rf GET from query parameters
     */
    POSTmap.put(Pattern.compile("^/gwtRequest"), GwtRequestFactoryVisitor.class);

    /**
     * any url begining with /i is a proxied $req to couchdb but only permits image/* and text/*
     */

    Pattern passthroughExpr = Pattern.compile("^/i(/.*)$");
    GETmap.put(passthroughExpr, HttpProxyImpl.class/*(passthroughExpr)*/);

    /**
     * general purpose httpd static content server that recognizes .gz and other compression suffixes when convenient
     *
     * any random config mechanism with a default will suffice here to define the content root.
     *
     * widest regex last intentionally
     *
     * system proprty: RXF_SERVER_CONTENT_ROOT
     */
    GETmap.put(Pattern.compile(".*"), ContentRootImpl.class /*(COUCH_DEFAULT_FS_ROOT)*/);
  }

  public void onAccept(SelectionKey key) throws IOException {
    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
    SocketChannel accept = channel.accept();
    accept.configureBlocking(false);
    App.get().enqueue(accept, OP_READ, new Impl() {
      public void onRead(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();

        ByteBuffer cursor = ByteBuffer.allocateDirect(BlobAntiPatternRelic.getReceiveBufferSize());
        int read = channel.read(cursor);
        if (-1 == read) {
          ((SocketChannel) key.channel()).socket().close();//cancel();
          return;
        }

        HttpMethod method = null;
        HttpRequest httpRequest = null;
        try {
          //find the method to dispatch
          Rfc822HeaderState state = new Rfc822HeaderState().apply((ByteBuffer) cursor.flip());
          httpRequest = state.$req();
          if (App.get().isDEBUG_SENDJSON()) {
            System.err.println(deepToString(UTF8.decode((ByteBuffer) httpRequest.headerBuf()
                .duplicate().rewind())));
          }
          String method1 = httpRequest.method();
          method = HttpMethod.valueOf(method1);

        } catch (Exception e) {
        }

        if (null != method) {

          Set<Entry<Pattern, Class<? extends Impl>>> entries = NAMESPACE.get(method).entrySet();
          String path = httpRequest.path();
          for (Entry<Pattern, Class<? extends Impl>> visitorEntry : entries) {
            Matcher matcher = visitorEntry.getKey().matcher(path);
            if (matcher.find()) {
              if (App.get().isDEBUG_SENDJSON()) {
                System.err.println("+?+?+? using " + matcher.toString());
              }
              Class<? extends Impl> value = visitorEntry.getValue();
              Impl impl;

              impl = value.newInstance();
              Object[] a = {impl, httpRequest, cursor};
              key.attach(a);
              if (PreRead.class.isAssignableFrom(value)) {
                impl.onRead(key);
              }

              key.selector().wakeup();

              return;
            }

          }
          System.err.println(deepToString("!!!1!1!!", "404", path, "using", NAMESPACE));
        } else {
          ((SocketChannel) key.channel()).socket().close();//cancel();
        }
      }
    });

  }

}
