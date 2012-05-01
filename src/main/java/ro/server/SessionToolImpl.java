package ro.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.concurrent.SynchronousQueue;

import one.xio.HttpMethod;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.getSelector;
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
  static public String getSessionProperty(String key) throws Exception {


    String sessionCookieId = getSessionCookieId();
    String s = null;
    String canonicalName = null;
    try {
      s = (String) fetchMapById(sessionCookieId).get(key);
    } catch (Exception e) {
      canonicalName = SessionToolImpl.class.getCanonicalName();
      ByteBuffer byteBuffer = KernelImpl.headersByteBufferThreadLocal.get();
      if (!UTF8.decode(byteBuffer).toString().trim().equals(canonicalName)) {

        KernelImpl.headersByteBufferThreadLocal.set(UTF8.encode(canonicalName));
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
  public static CouchTx sendJson(String json, String... idver) throws Exception {
    String take;
    SocketChannel channel = null;
    try {
      channel = KernelImpl.createCouchConnection();
      SynchronousQueue<String> retVal = new SynchronousQueue<String>();
      HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, new SendJsonVisitor(json, retVal, idver));
      take = retVal.take();
    } finally {
      if (null != channel) {
        channel.register(getSelector(), 0);
        KernelImpl.couchDq.add(channel);
      }
    }
    return GSON.fromJson(take, CouchTx.class);
  }

  public static LinkedHashMap fetchMapById(String key) throws IOException, InterruptedException {
    String take = fetchSessionJsonById(key);
    LinkedHashMap linkedHashMap = GSON.fromJson(take, LinkedHashMap.class);
    if (2 == linkedHashMap.size() && linkedHashMap.containsKey("responseCode"))
      throw new IOException(KernelImpl.deepToString(linkedHashMap));
    return linkedHashMap;
  }

  public static String fetchSessionJsonById(String path) throws IOException, InterruptedException {
    SocketChannel channel = KernelImpl.createCouchConnection();
    String take;
    try {
      SynchronousQueue<String> retVal = new SynchronousQueue<String>();
      HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, new FetchJsonByIdVisitor(INSTANCE + '/' + path, channel, retVal));
      take = retVal.take();
    } finally {
      KernelImpl.recycleChannel(channel);
    }
    return take;
  }

}