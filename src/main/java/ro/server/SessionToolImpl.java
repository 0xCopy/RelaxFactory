package ro.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.concurrent.SynchronousQueue;

import one.xio.HttpMethod;

import static one.xio.HttpMethod.UTF8;
import static ro.server.KernelImpl.GSON;
import static ro.server.KernelImpl.getSessionCookieId;

/**
 * User: jim
 * Date: 4/20/12
 * Time: 5:04 PM
 */
public class SessionToolImpl {

  public static final RoSessionLocator RO_SESSION_LOCATOR = new RoSessionLocator();
  public static final String INSTANCE = "rosession";

  /**
   * @param key
   * @return
   * @throws InterruptedException
   */
  static public String getSessionProperty(final String key) throws Exception {


    final String sessionCookieId = getSessionCookieId();
    String s = null;
    String canonicalName = null;
    try {
      s = (String) fetchMapById(sessionCookieId).get(key);
    } catch (Exception e) {
      canonicalName = SessionToolImpl.class.getCanonicalName();
      final ByteBuffer byteBuffer = KernelImpl.ThreadLocalHeaders.get();
      if (!UTF8.decode(byteBuffer).toString().trim().equals(canonicalName)) {

        KernelImpl.ThreadLocalHeaders.set(UTF8.encode(canonicalName));
        return getSessionProperty(key);
      }
    }
    return s;
  }

  //maximum wastefulness
  static public String setSessionProperty(String key, String value) throws Exception {
    try {
      String id = KernelImpl.getSessionCookieId();
      LinkedHashMap linkedHashMap = fetchMapById(id);
      linkedHashMap.put(key, value);
      CouchTx tx = sendJson(GSON.toJson(linkedHashMap), INSTANCE + "/" + id, String.valueOf(linkedHashMap.get("_rev")));

      return tx.rev;
    } catch (Throwable e) {

    } finally {
    }
    return null;
  }


  /**
   * @param json
   * @return new _rev
   */
  public static CouchTx sendJson(final String json, final String... idver) throws Exception {
    final SocketChannel channel = KernelImpl.createCouchConnection();
    final SynchronousQueue<String> retVal = new SynchronousQueue<String>();
    HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, new SendJsonVisitor(json, retVal, idver));


    return GSON.fromJson(String.valueOf(retVal.take()), CouchTx.class);
  }

  public static LinkedHashMap fetchMapById(final String key) throws IOException, InterruptedException {
    String take = fetchSessionJsonById(key);
    final LinkedHashMap linkedHashMap = GSON.fromJson(take, LinkedHashMap.class);
    if (linkedHashMap.size() == 2 && linkedHashMap.containsKey("responseCode"))
      throw new IOException(String.valueOf(linkedHashMap));
    return linkedHashMap;
  }

  public static String fetchSessionJsonById(final String path) throws IOException, InterruptedException {
    final SocketChannel channel = KernelImpl.createCouchConnection();
    final SynchronousQueue<String> retVal = new SynchronousQueue<String>();
    HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, new
        FetchJsonByIdVisitor(INSTANCE + '/' + path, channel, retVal));
    final String take = retVal.take();
    return take;
  }

}