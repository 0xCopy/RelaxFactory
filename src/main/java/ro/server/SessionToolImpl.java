package ro.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.concurrent.SynchronousQueue;

import one.xio.HttpMethod;
import ro.model.RoSession;

import static ro.server.KernelImpl.GSON;

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
  static public String getSessionProperty(final String key) throws InterruptedException, IOException {
    RoSession ret;
    LinkedHashMap linkedHashMap = null;

    linkedHashMap = fetchMapById(key);

    return (String) linkedHashMap.get(key);
  }

  //maximum wastefulness
  static public String setSessionProperty(String key, String value) throws IOException, InterruptedException {
    try {
      String id = KernelImpl.getSessionCookieId();
      LinkedHashMap linkedHashMap = fetchMapById(id);
      linkedHashMap.put(key, value);
      CouchTx tx = sendJson(GSON.toJson(linkedHashMap), INSTANCE + "/" + id, String.valueOf(linkedHashMap.get("_rev")));

      return tx.rev;
    } catch (Throwable e) {
      e.printStackTrace();  //todo: verify for a purpose
    } finally {
    }
    return null;
  }


  /**
   * @param json
   * @return new _rev
   */
  public static CouchTx sendJson(final String json, final String... idver) throws IOException, InterruptedException {
    final SocketChannel channel = KernelImpl.createCouchConnection();
    final SynchronousQueue synchronousQueue = new SynchronousQueue();
    HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, new SendJsonVisitor(json, synchronousQueue, idver));


    Object take = synchronousQueue.take();
    return GSON.fromJson(String.valueOf(take), CouchTx.class);
  }

  public static LinkedHashMap fetchMapById(final String key) throws IOException, InterruptedException {
    String take = fetchSessionJsonById(key);
    return GSON.fromJson(take, LinkedHashMap.class);
  }

  public static String fetchSessionJsonById(final String path) throws IOException, InterruptedException {
    final SocketChannel channel = KernelImpl.createCouchConnection();
    final SynchronousQueue synchronousQueue = new SynchronousQueue();
    HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, new
        FetchJsonByIdVisitor(INSTANCE + '/' + path, channel, synchronousQueue));

    return (String) synchronousQueue.take();
  }

}