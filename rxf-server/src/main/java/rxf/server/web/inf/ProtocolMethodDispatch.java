package rxf.server.web.inf;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;
import rxf.server.*;

import static java.nio.channels.SelectionKey.OP_READ;
import static one.xio.HttpHeaders.Accept;
import static one.xio.HttpHeaders.Content$2dEncoding;
import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpHeaders.Content$2dType;
import static one.xio.HttpHeaders.ETag;
import static one.xio.HttpHeaders.Transfer$2dEncoding;
import static one.xio.HttpMethod.GET;
import static one.xio.HttpMethod.POST;

/**
 * this class holds a protocol namespace to dispatch requests
 * <p/>
 * {@link  #NAMESPACE } is a  map of http methods each containing an ordered map of regexes tested in order of
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
  public static Map<Pattern, Impl> precedeAllWith(HttpMethod method, Pattern pattern
      , Impl impl) {
    Map<Pattern, Impl> patternImplMap = NAMESPACE.get(method);
    LinkedHashMap<Pattern, Impl> ret = new LinkedHashMap<Pattern, Impl>();
    ret.put(pattern, impl);
    if (null == patternImplMap) patternImplMap = new LinkedHashMap<Pattern, Impl>();

    for (Entry<Pattern, Impl> patternImplEntry : patternImplMap.entrySet()) {
      ret.put(patternImplEntry.getKey(), patternImplEntry.getValue());
    }
    NAMESPACE.put(method, ret);
    return patternImplMap;

  }

  /**
   * a map of http methods each containing an ordered map of regexes tested in order of
   * map insertion.
   */
  public static final Map<HttpMethod, Map<Pattern, Impl>> NAMESPACE = new EnumMap<HttpMethod, Map<Pattern, Impl>>(HttpMethod.class);

  /**
   * the PUT protocol handlers, only static for the sake of javadocs
   */
  public static Map<Pattern, Impl> PUTmap = new LinkedHashMap<Pattern, Impl>();

  /**
   * the GET protocol handlers, only static for the sake of javadocs
   */
  public static Map<Pattern, Impl> GETmap = new LinkedHashMap<Pattern, Impl>();

  /**
   *
   */
  public static final String RXF_SERVER_CONTENT_ROOT = "rxf.server.content.root";

  static {
    NAMESPACE.put(POST, PUTmap);
    NAMESPACE.put(GET, GETmap);

    /**
     * for gwt requestfactory done via POST.
     *
     * TODO: rf GET from query parameters
     */
    PUTmap.put(Pattern.compile("^/gwtRequest"), new GwtRequestFactoryVisitor());


    /**
     * any url begining with /i is a proxied $req to couchdb but only permits image/* and text/*
     */

    Pattern passthroughExpr = Pattern.compile("^/i(/.*)$");
    HttpProxyImpl theCouchImagePassthru = new HttpProxyImpl(passthroughExpr);
    GETmap.put(passthroughExpr, theCouchImagePassthru);

    /**
     * general purpose httpd static content server that recognizes .gz and other compression suffixes when convenient
     *
     * any random config mechanism with a default will suffice here to define the content root.
     *
     * widest regex last intentionally
     *
     * system proprty: RXF_SERVER_CONTENT_ROOT
     */
    GETmap.put(Pattern.compile(".*"), new ContentRootImpl(System.getProperty(RXF_SERVER_CONTENT_ROOT, "./")));
  }


  @Override
  public void onAccept(SelectionKey key) throws IOException {
    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
    SocketChannel accept = channel.accept();
    accept.configureBlocking(false);
    HttpMethod.enqueue(accept, OP_READ, this);

  }

  @Override
  public void onRead(SelectionKey key) throws Exception {
    SocketChannel channel = (SocketChannel) key.channel();

    ByteBuffer cursor = ByteBuffer.allocateDirect(BlobAntiPatternObject.getReceiveBufferSize());
    int read = channel.read(cursor);
    if (-1 == read) {
      ((SocketChannel) key.channel()).socket().close();//cancel();
      return;
    }
    //break down the incoming addHeaderInterest.
    Rfc822HeaderState state;

    ActionBuilder.get().state().headerInterest(Content$2dLength, Content$2dType, Content$2dEncoding, ETag, Transfer$2dEncoding, Accept).cookies(BlobAntiPatternObject.class.getCanonicalName(), BlobAntiPatternObject.MYGEOIPSTRING).sourceKey(key).apply((ByteBuffer) cursor.flip());
    HttpMethod method = null;
    try {
//find the method to dispatch
      method = HttpMethod.valueOf(ActionBuilder.get().state().methodProtocol());
    } catch (Exception e) {
    }

    if (null == method) {
      ((SocketChannel) key.channel()).socket().close();//cancel();

      return;
    }
    //check for namespace registration
    // todo: preRead is  wierd initiailizer which needs some review.
    for (Entry<Pattern, Impl> visitorEntry : BlobAntiPatternObject.getNamespace().get(method).entrySet()) {
      Matcher matcher = visitorEntry.getKey().matcher(ActionBuilder.get().state().pathResCode());
      if (matcher.find()) {
        Impl impl = visitorEntry.getValue();

        Impl ob = impl.preRead(ActionBuilder.get().state(), cursor);
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
        throw new Error(BlobAntiPatternObject.arrToString("unknown method in", ActionBuilder.get().state()));
    }
  }
}

