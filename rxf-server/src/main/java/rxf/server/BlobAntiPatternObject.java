package rxf.server;

import one.xio.HttpMethod;
import rxf.server.driver.RxfBootstrap;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static rxf.server.CouchNamespace.COUCH_DEFAULT_ORGNAME;
import static rxf.server.RelaxFactoryServerImpl.wheresWaldo;

/**
 * <a href='http://www.antipatterns.com/briefing/sld024.htm'> Blob Anti Pattern </a>
 * used here as a pattern to centralize the antipatterns
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
public class BlobAntiPatternObject {

    public static final int CONNECTION_POOL_SIZE = Integer.parseInt(RxfBootstrap.getVar("RXF_CONNECTION_POOL_SIZE", "20"));
    public static boolean DEBUG_SENDJSON = System.getenv().containsKey("DEBUG_SENDJSON");
    public static InetAddress LOOPBACK;
    public static int receiveBufferSize;
    public static int sendBufferSize;

    public static InetSocketAddress COUCHADDR;
    public static ScheduledExecutorService EXECUTOR_SERVICE =
            Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 3);

    static {

        String rxfcouchprefix = RxfBootstrap.getVar("COUCH_PREFIX","http://localhost:5894");
      try {
        URI uri = new URI(rxfcouchprefix);
        int port = uri.getPort();
          port= -1 != port ? port : 80;
          setCOUCHADDR(new InetSocketAddress(uri.getHost(),port));
      } catch (URISyntaxException e) {
          e.printStackTrace();
      }

    }

    private static Deque couchConnections = new LinkedList<>();

    public static SocketChannel createCouchConnection() {
        SocketChannel ret = null;
        while (!HttpMethod.killswitch) {
            SocketChannel poll = (SocketChannel) couchConnections.poll();
            if (null != poll && poll.isConnected())
            {ret=poll;break;}
            try {
                SocketChannel channel = SocketChannel.open(getCOUCHADDR());
                channel.configureBlocking(false);
                ret=channel;break;
            } catch (Exception e) {
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

    public static <T> String deepToString(T... d) {
        return Arrays.deepToString(d) + wheresWaldo();
    }

    public static <T> String arrToString(T... d) {
        return Arrays.deepToString(d);
    }

    public static int getReceiveBufferSize() {
        switch (receiveBufferSize) {
            case 0:
                try {
                    SocketChannel couchConnection = createCouchConnection();
                    receiveBufferSize = couchConnection.socket().getReceiveBufferSize();
                    recycleChannel(couchConnection);
                } catch (IOException ignored) {
                }
                break;
        }

        return receiveBufferSize;
    }

    public static void setReceiveBufferSize(int receiveBufferSize) {
        receiveBufferSize = receiveBufferSize;
    }

    public static int getSendBufferSize() {
        if (0 == sendBufferSize) {
            try {
                SocketChannel couchConnection = createCouchConnection();
                sendBufferSize = couchConnection.socket().getReceiveBufferSize();
                recycleChannel(couchConnection);
            } catch (IOException ignored) {
            }
        }
        return sendBufferSize;
    }

    public static void setSendBufferSize(int sendBufferSiz) {
        sendBufferSize = sendBufferSiz;
    }

    public static String dequote(String s) {
        String ret = s;
        if (null != s && ret.startsWith("\"") && ret.endsWith("\"")) {
            ret = ret.substring(1, ret.lastIndexOf('"'));
        }

        return ret;
    }

    /**
     * 'do the right thing' when handed a buffer with no remaining bytes.
     *
     * @param buf
     * @return
     */
    public static ByteBuffer avoidStarvation(ByteBuffer buf) {
        if (0 == buf.remaining()) {
            buf.rewind();
        }
        return buf;
    }

    public static String getDefaultOrgName() {
        return COUCH_DEFAULT_ORGNAME;
    }

    /**
     * byte-compare of suffixes
     *
     * @param terminator  the token used to terminate presumably unbounded growth of a list of buffers
     * @param currentBuff current ByteBuffer which does not necessarily require a list to perform suffix checks.
     * @param prev        a linked list which holds previous chunks
     * @return whether the suffix composes the tail bytes of current and prev buffers.
     */
    public static boolean suffixMatchChunks(byte[] terminator, ByteBuffer currentBuff,
                                            ByteBuffer... prev) {
        ByteBuffer tb = currentBuff;
        int prevMark = prev.length;
        int bl = terminator.length;
        int rskip = 0;
        int i = bl - 1;
        while (0 <= i) {
            rskip++;
            int comparisonOffset = tb.position() - rskip;
            if (0 > comparisonOffset) {
                prevMark--;
                if (0 <= prevMark) {
                    tb = prev[prevMark];
                    rskip = 0;
                    i++;
                } else {
                    return false;

                }
            } else if (terminator[i] != tb.get(comparisonOffset)) {
                return false;
            }
            i--;
        }
        return true;
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

    public static ScheduledExecutorService getEXECUTOR_SERVICE() {
        return EXECUTOR_SERVICE;
    }

}
