package rxf.server;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import rxf.server.web.inf.ProtocolMethodDispatch;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.wheresWaldo;
import static rxf.server.CouchNamespace.COUCH_DEFAULT_ORGNAME;

/**
 * <a href='http://www.antipatterns.com/briefing/sld024.htm'> Blob Anti Pattern </a>
 * used here as a pattern to centralize the antipatterns
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
public class BlobAntiPatternObject {
    public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors
            .newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 3);
    public static final String YYYY_MM_DD_T_HH_MM_SS_SSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final Gson GSON = new GsonBuilder().setDateFormat(
            YYYY_MM_DD_T_HH_MM_SS_SSSZ).setFieldNamingPolicy(
            FieldNamingPolicy.IDENTITY).setPrettyPrinting().create();
    public static final Charset ISO88591 = Charset.forName("ISO-8859-1");
    public static final String MYGEOIPSTRING = "mygeoipstring";

    public static final String COOKIE = "Cookie";

    private static final TimeUnit defaultCollectorTimeUnit = TimeUnit.SECONDS;

    public static boolean DEBUG_SENDJSON = System.getenv().containsKey(
            "DEBUG_SENDJSON");
    public static final InetSocketAddress COUCHADDR;
    public static InetAddress LOOPBACK = null;
    public static final byte[] HEADER_TERMINATOR = "\r\n\r\n".getBytes(UTF8);

    static {
        try {
            try {
                BlobAntiPatternObject.LOOPBACK = (InetAddress) InetAddress.class
                        .getMethod("getLoopBackAddress").invoke(null);
            } catch (NoSuchMethodException e) {
                BlobAntiPatternObject.LOOPBACK = InetAddress
                        .getByAddress(new byte[]{127, 0, 0, 1});
                System.err.println("java 6 LOOPBACK detected");
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        COUCHADDR = new InetSocketAddress(LOOPBACK, 5984);
    }

    public static SocketChannel createCouchConnection() {
        while (!HttpMethod.killswitch) {
            try {
                SocketChannel channel = SocketChannel.open(COUCHADDR);
                channel.configureBlocking(false);
                return channel;
            } catch (IOException e) {
                System.err.println("----- very bad connection failure in "
                        + wheresWaldo(4));
                e.printStackTrace();
            } finally {
            }
        }
        return null;
    }

    public static <T> String deepToString(T... d) {
        return Arrays.deepToString(d) + wheresWaldo();
    }

    public static <T> String arrToString(T... d) {
        return Arrays.deepToString(d);
    }

    public static void recycleChannel(SocketChannel channel) {
        try {
            channel.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private static int receiveBufferSize;
    private static int sendBufferSize;

    public static int getReceiveBufferSize() {
        if (0 == receiveBufferSize)
            try {
                SocketChannel couchConnection = createCouchConnection();
                receiveBufferSize = couchConnection.socket()
                        .getReceiveBufferSize();
                recycleChannel(couchConnection);
            } catch (IOException ignored) {
            }

        return receiveBufferSize;
    }

    public static int getSendBufferSize() {
        if (0 == sendBufferSize)
            try {
                SocketChannel couchConnection = createCouchConnection();
                sendBufferSize = couchConnection.socket()
                        .getReceiveBufferSize();
                recycleChannel(couchConnection);
            } catch (IOException ignored) {
            }
        return sendBufferSize;
    }

    public static String inferRevision(Map map) {
        String rev = (String) map.get("_rev");
        if (null == rev)
            rev = (String) map.get("rev");
        if (null == rev) {
            rev = (String) map.get("version");
        }
        if (null == rev)
            rev = (String) map.get("ver");
        return rev;
    }

    public static String dequote(String s) {
        String ret = s;
        if (null != s && ret.startsWith("\"") && ret.endsWith("\"")) {
            ret = ret.substring(1, ret.lastIndexOf('"'));
        }

        return ret;
    }

    //test
    public static void main(String... args) throws Exception {
        GeoIpService.startGeoIpService();
        startServer(args);
    }

    public static void startServer(String... args) throws IOException {
        AsioVisitor topLevel;
        ServerSocketChannel serverSocketChannel;
        final String port;
        InetAddress hostname;
        {
            String json = "{}";
            for (String arg : args) {
                json += arg;
            }
            final Properties properties = BlobAntiPatternObject.GSON.fromJson(
                    json, Properties.class);
            topLevel = new ProtocolMethodDispatch();
            serverSocketChannel = ServerSocketChannel.open();
            port = properties.getProperty("port", "8080");
            hostname = InetAddress.getByName(properties.getProperty("hostname",
                    "0.0.0.0"));
        }
        serverSocketChannel.socket().bind(
                new InetSocketAddress(hostname, Integer.parseInt(port)));
        serverSocketChannel.configureBlocking(false);
        HttpMethod.enqueue(serverSocketChannel, OP_ACCEPT, topLevel);
        HttpMethod.init(topLevel, args);
    }

    public static TimeUnit getDefaultCollectorTimeUnit() {
        return DEBUG_SENDJSON ? TimeUnit.HOURS : defaultCollectorTimeUnit;
    }

    public static ByteBuffer avoidStarvation(ByteBuffer buf) {
        if (0 == buf.remaining())
            buf.rewind();
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
    static public boolean suffixMatchChunks(byte[] terminator,
                                            ByteBuffer currentBuff, ByteBuffer... prev) {
        ByteBuffer tb = currentBuff;
        int prevMark = prev.length;
        int backtrack = 0;
        boolean mismatch = false;
        int bl = terminator.length;
        int rskip = 0;
        for (int i = bl - 1; i >= 0 && !mismatch; i--) {
            rskip++;
            int comparisonOffset = tb.position() - rskip;
            if (comparisonOffset < 0) {
                prevMark--;
                if (prevMark < 0) {
                    mismatch = true;
                    break;
                } else {
                    tb = prev[prevMark];
                    rskip = 0;
                    i++;
                }
            } else {
                byte aByte = terminator[i];
                byte b = tb.get(comparisonOffset);
                if (aByte != b) {
                    mismatch = true;
                }
            }
        }
        return !mismatch;
    }

    public static void setReceiveBufferSize(int receiveBufferSize) {
        BlobAntiPatternObject.receiveBufferSize = receiveBufferSize;
    }

    public static void setSendBufferSize(int sendBufferSize) {
        BlobAntiPatternObject.sendBufferSize = sendBufferSize;
    }

}
