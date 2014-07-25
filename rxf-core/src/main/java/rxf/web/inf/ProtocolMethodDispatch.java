package rxf.web.inf;

import one.xio.AsioVisitor.Impl;
import one.xio.AsyncSingletonServer.SingleThreadSingletonServer;
import rxf.core.Errors;
import rxf.core.Tx;
import rxf.shared.KeepMatcher;
import rxf.shared.PreRead;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.channels.SelectionKey.OP_READ;
import static one.xio.HttpMethod.GET;
import static one.xio.HttpMethod.POST;
import static rxf.core.CouchNamespace.NAMESPACE;

/**
 * this class holds a protocol namespace to dispatch requests
 * <p/>
 * {@link rxf.core.CouchNamespace#NAMESPACE } is a map of http methods each containing an ordered map of regexes tested
 * in order of map insertion.
 * <p/>
 * User: jim Date: 4/18/12 Time: 12:37 PM
 */
public class ProtocolMethodDispatch extends Impl {

  public static final ByteBuffer NONCE = ByteBuffer.allocateDirect(0);
  public static final byte[] HEADER_TERMINATOR = "\r\n\r\n".getBytes(StandardCharsets.UTF_8);
  private static final boolean DEBUG_SENDJSON = false;

  /**
   * the PUT protocol handlers, only static for the sake of javadocs
   */
  public static Map<Pattern, Class<? extends Impl>> POSTmap = new LinkedHashMap<>();

  /**
   * the GET protocol handlers, only static for the sake of javadocs
   */
  public static Map<Pattern, Class<? extends Impl>> GETmap = new LinkedHashMap<>();

  static {
    NAMESPACE.put(POST, POSTmap);
    NAMESPACE.put(GET, GETmap);

    /**
     * for gwt requestfactory done via POST.
     * 
     * TODO: rf GET from query parameters
     */
    // guiced POSTmap.put(Pattern.compile("^/gwtRequest"), GwtRequestFactoryVisitor.class);

    /**
     * any url begining with /i is a proxied $req to couchdb but only permits image/* and text/*
     */

    // Pattern passthroughExpr = Pattern.compile("^/i(/.*)$");
    // retired GETmap.put(passthroughExpr, HttpProxyImpl.class/*(passthroughExpr)*/);

    /**
     * general purpose httpd static content couch that recognizes .gz and other compression suffixes when convenient
     * 
     * any random config mechanism with a default will suffice here to define the content root.
     * 
     * widest regex last intentionally system proprty: {value #RXF_CONTENT_ROOT}
     */
    GETmap.put(ContentRootCacheImpl.CACHE_PATTERN, ContentRootCacheImpl.class);
    GETmap.put(ContentRootNoCacheImpl.NOCACHE_PATTERN, ContentRootNoCacheImpl.class);
    GETmap.put(Pattern.compile(".*"), ContentRootImpl.class);
  }

  public static <T> String deepToString(T... d) {
    return Arrays.deepToString(d) + wheresWaldo();
  }

  public static String wheresWaldo(int... depth) {
    return Impl.wheresWaldo(depth);
  }

  public void onAccept(SelectionKey key) throws IOException {
    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
    SocketChannel accept = channel.accept();
    accept.configureBlocking(false);
    SingleThreadSingletonServer.enqueue(accept, OP_READ);

  }

  /**
   * this makes use of the fact that we are the default/init Visitor, allowing for temporary placement of a tx object.
   * 
   * @param key
   * @throws Exception
   */
  public void onRead(SelectionKey key) throws Exception {
    Tx tx = Tx.current(acquireTx(key).readHttpHeaders(key));
    if (null != tx) {
      String path = tx.state().asRequest().path();
      for (Entry<Pattern, Class<? extends Impl>> visitorEntry : NAMESPACE.get(
          tx.state().asRequest().httpMethod()).entrySet()) {
        Matcher matcher = visitorEntry.getKey().matcher(path);
        if (matcher.matches()) {
          if (DEBUG_SENDJSON) {
            System.err.println("+?+?+? using " + matcher.toString());
          }
          Class<? extends Impl> aClass = visitorEntry.getValue();
          Impl impl = aClass.newInstance();
          boolean keepMatch = aClass.isAnnotationPresent(KeepMatcher.class);
          Object a[] =
              keepMatch ? new Object[] {impl, tx.state(), tx.payload(), matcher.toMatchResult()}
                  : new Object[] {impl, tx.state(), tx.payload()};
          OpInterest opInterest = aClass.getAnnotation(OpInterest.class);
          key.interestOps(opInterest != null ? opInterest.value() : OP_READ).attach(a);

          key.attach(a);
          if (aClass.isAnnotationPresent(PreRead.class))
            impl.onRead(key);
          key.selector().wakeup();
          return;
        }

      }
      System.err.println(deepToString("!!!1!1!!", "404", path, "using", NAMESPACE));
      Errors.$404(key, path);
    }
  }

  /**
   * if the attachment is a tx, we resume filling headers and payload by keep. if the attachment is not Tx, it is set to
   * a fresh one.
   * <p/>
   * for TOP level default visitor root only!
   * 
   * @param key selectionKey
   * @return a tx
   */
  public static Tx acquireTx(SelectionKey key) {
    Object attachment = key.attachment();
    if (attachment instanceof Object[]) {
      Object[] objects = (Object[]) attachment;
      if (objects.length == 0)
        attachment = null;
      if (objects.length == 1)
        attachment = objects[0];
    }
    Tx tx;
    if (attachment instanceof Tx) {
      tx = Tx.current((Tx) attachment);
    } else
      tx = Tx.current(new Tx());
    key.attach(tx);
    return tx;
  }
}
