package rxf.rpc;

import rxf.core.Config;
import rxf.core.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static rxf.core.CouchNamespace.COUCH_DEFAULT_ORGNAME;

/**
 * <a href='http://www.antipatterns.com/briefing/sld024.htm'> Blob Anti Pattern </a>
 * used here as a pattern to centralize the antipatterns
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
public class BlobAntiPatternObject {

  public static final boolean RXF_CACHED_THREADPOOL = "true".equals(Config.get("RXF_CACHED_THREADPOOL", "false"));
  public static final int CONNECTION_POOL_SIZE = Integer.parseInt(Config.get("RXF_CONNECTION_POOL_SIZE", "20"));
  public static boolean DEBUG_SENDJSON = System.getenv().containsKey("DEBUG_SENDJSON");
  public static InetAddress LOOPBACK;
  public static int receiveBufferSize;

    public static int sendBufferSize;
  public static InetSocketAddress COUCHADDR;
  public static ExecutorService EXECUTOR_SERVICE = RXF_CACHED_THREADPOOL ?
            Executors.newCachedThreadPool():
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 3);

    static {

        String rxfcouchprefix = Config.get("RXF_COUCH_PREFIX", "http://localhost:5984");
      try {
        URI uri = new URI(rxfcouchprefix);
        int port = uri.getPort();
          port= -1 != port ? port : 80;
          setCOUCHADDR(new InetSocketAddress(uri.getHost(),port));
      } catch (URISyntaxException e) {
          e.printStackTrace();
      }

    }

    private static Deque<SocketChannel> couchConnections = new LinkedList<>();

    public static SocketChannel createCouchConnection() {
        SocketChannel ret = null;
        while (!Server.killswitch) {
            SocketChannel poll = couchConnections.poll();
            if (null != poll && poll.isConnected())
            {ret=poll;break;}
            try {
                SocketChannel channel = SocketChannel.open(getCOUCHADDR());
                channel.configureBlocking(false);
                ret=channel;break;
            } catch (Exception e) {
              e.printStackTrace();
            } finally {
            }
        }
//        GSONThreadLocalImmolater.immolate();
        return ret;
    }

    public static void recycleChannel(SocketChannel channel) {
        try {
            if (CONNECTION_POOL_SIZE >= couchConnections.size() && channel.isConnected()) {
                couchConnections.addLast(channel);
            } else {
                channel.close();
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static <T> String arrToString(T... d) {
        return Arrays.deepToString(d);
    }


    public static void setReceiveBufferSize(int receiveBufferSize) {
        receiveBufferSize = receiveBufferSize;
    }

    public static int getSendBufferSize() {
        if (0 == sendBufferSize) {
            SocketChannel couchConnection = createCouchConnection();
            sendBufferSize =  4<<10;
            recycleChannel(couchConnection);
        }
        return sendBufferSize;
    }

    public static void setSendBufferSize(int sendBufferSiz) {
        sendBufferSize = sendBufferSiz;
    }

    public static String getDefaultOrgName() {
        return COUCH_DEFAULT_ORGNAME;
    }

    public static boolean isDEBUG_SENDJSON() {
        return DEBUG_SENDJSON;
    }

    public static void setDEBUG_SENDJSON(boolean DEBUG_SENDJSON) {
        BlobAntiPatternObject.DEBUG_SENDJSON = DEBUG_SENDJSON;
    }

    public static void setLOOPBACK(InetAddress LOOPBACK) {
        BlobAntiPatternObject.LOOPBACK = LOOPBACK;
    }

    public static InetSocketAddress getCOUCHADDR() {
        return COUCHADDR;
    }

    public static void setCOUCHADDR(InetSocketAddress COUCHADDR) {
        BlobAntiPatternObject.COUCHADDR = COUCHADDR;
    }

    public static ExecutorService getEXECUTOR_SERVICE() {
        return EXECUTOR_SERVICE;
    }

}
