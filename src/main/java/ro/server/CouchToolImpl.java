package ro.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.concurrent.SynchronousQueue;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import ro.model.RoSession;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static ro.server.CouchChangesClient.GSON;

/**
 * User: jim
 * Date: 4/20/12
 * Time: 5:04 PM
 */
public class CouchToolImpl {

  public static final RoSessionLocator RO_SESSION_LOCATOR = new RoSessionLocator();
  public static final String INSTANCE = "rosession";

  /**
   * @param id
   * @param key
   * @return
   * @throws InterruptedException
   */
  static public String getProperty(String id, final String key) throws InterruptedException {
    RoSession ret;
    LinkedHashMap linkedHashMap = null;
    try {
      linkedHashMap = fetchMapById(key);
    } catch (ClosedChannelException e) {
    } catch (IOException e) {
    }
    assert linkedHashMap != null;
    return (String) linkedHashMap.get(key);
  }

  //maximum wastefulness
  static public String setProperty(String id, String key, String value) throws IOException, InterruptedException {
    LinkedHashMap linkedHashMap = fetchMapById(id);
    linkedHashMap.put(key, value);
    CouchTx rev = sendJson(GSON.toJson(linkedHashMap), id, String.valueOf(linkedHashMap.get("_rev")));

    return rev.rev;
  }


  /**
   * @param json
   * @return new _rev
   */
  public static CouchTx sendJson(final String json, final String... idver) throws IOException {
    final SocketChannel channel = CouchChangesClient.createConnection();
    final SynchronousQueue synchronousQueue = new SynchronousQueue();
    HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, new AsioVisitor() {

      @Override
      public void onWrite(final SelectionKey selectionKey) {
        String method;
        String call;
        method = idver.length == 0 ? "POST" : "PUT";

        String identifier = "";
        for (int i = 0; i < idver.length; i++) {
          String s = idver[i];
          switch (i) {
            case 0:
              identifier += s;
              break;
            case 1:
              identifier += "?rev=" + s;
              break;
          }
        }

        call = MessageFormat.format("{0} /{4}/{1} HTTP/1.1\r\nContent-Type: application/json\r\nContent-Length: {2}\r\n\r\n{3}", method, identifier, json.length(), json, INSTANCE);
        ByteBuffer encode = UTF8.encode(call);
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        try {
          channel.write(encode);
          selectionKey.attach(new JsonResponseReader(synchronousQueue));
          selectionKey.interestOps(SelectionKey.OP_READ);

        } catch (IOException e) {
          e.printStackTrace();  //todo: verify for a purpose
        }

      }

      @Override
      public void onAccept(SelectionKey key) {

      }

      @Override
      public void onRead(SelectionKey key) {

      }

      @Override
      public void onConnect(SelectionKey key) {
        try {
          if (((SocketChannel) key.channel()).finishConnect()) {
            key.interestOps(OP_WRITE);
          }
        } catch (IOException e) {
          e.printStackTrace();  //todo: verify for a purpose
        }
      }
    });


    try {
      return GSON.fromJson(String.valueOf(synchronousQueue.take()), CouchTx.class);
    } catch (InterruptedException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return null;
  }

  public static LinkedHashMap fetchMapById(final String key) throws IOException, InterruptedException {
    String take = fetchJsonById(key);
    return GSON.fromJson(take, LinkedHashMap.class);
  }

  public static String fetchJsonById(final String key) throws IOException, InterruptedException {
    final SocketChannel channel = CouchChangesClient.createConnection();
    final SynchronousQueue synchronousQueue = new SynchronousQueue();
    HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, new AsioVisitor() {
      @Override
      public void onRead(SelectionKey key) {
      }

      @Override
      public void onConnect(SelectionKey key) {
        try {
          if (((SocketChannel) key.channel()).finishConnect()) {
            key.interestOps(OP_WRITE);
          }
        } catch (IOException e) {
          e.printStackTrace();  //todo: verify for a purpose
        }
      }

      @Override
      public void onWrite(final SelectionKey selectionKey) {
        try {
          String format = (MessageFormat.format("GET /{0}/{1} HTTP/1.1\r\n\r\n", INSTANCE, key));
          System.err.println("attempting connect: " + format.trim());
          channel.write(UTF8.encode(format));
        } catch (IOException e) {
          e.printStackTrace();  //todo: verify for a purpose
        }
        selectionKey.attach(new JsonResponseReader(synchronousQueue));
        selectionKey.interestOps(OP_READ);
      }

      @Override
      public void onAccept(SelectionKey key) {
      }
    });

    String take = (String) synchronousQueue.take();
    return take;
  }

}