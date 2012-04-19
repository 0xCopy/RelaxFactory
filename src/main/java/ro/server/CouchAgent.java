package ro.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;

import one.xio.HttpMethod;

/**
 * User: jim
 * Date: 4/15/12
 * Time: 8:24 PM
 */
public class CouchAgent {

  static public void main(String... args) throws IOException {
    int i = 0;

    String pre = "ro";
    if (i < args.length) {
      pre = args[i++];
    }
    final String prefix = pre;
    CouchChangesClient[] changeHandlers = {
        new CouchChangesClient(prefix + "screen") {
          @Override
          public java.lang.Runnable getDocUpdateHandler(final LinkedHashMap couchChange) {
            return new Runnable() {
              @Override
              public void run() {
                String id = String.valueOf(couchChange.get("id"));
                System.err.println("screen details changed for the id: " + id);
              }
            };
          }
        },
        new SessionCouchAgent(prefix),
        new CouchChangesClient(prefix + "deal"),
        new CouchChangesClient(prefix + "npo"),
        new CouchChangesClient(prefix + "purchase") {
          @Override
          public Runnable getDocUpdateHandler(final LinkedHashMap couchChange) {
            return new Runnable() {
              @Override
              public void run() {
                String id = String.valueOf(couchChange.get("id"));
                System.err.println("revenue alert: " + id);
              }
            };
          }
        },
        new CouchChangesClient(prefix + "vendor")
    };


    for (CouchChangesClient changeHandler : changeHandlers) {
      InetSocketAddress remote = new InetSocketAddress(changeHandler.hostname, (Integer) changeHandler.port);
      SocketChannel channel = SocketChannel.open();
      channel.configureBlocking(false);
      channel.connect(remote);
      String feedString = changeHandler.getFeedString();
      System.err.println("feedstring: " + feedString);
      HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, changeHandler, feedString);
    }
    HttpMethod.main();
  }

  public static class SessionCouchAgent extends CouchChangesClient {

    public SessionCouchAgent(String prefix) {
      feedname = prefix + "session";
    }

    @Override
    public Runnable getDocUpdateHandler(final LinkedHashMap couchChange) {
      return new Runnable() {
        @Override
        public void run() {
          String id = String.valueOf(couchChange.get("id"));
          System.err.println("session details changed for the id: " + id);
        }
      };
    }
  }
}
