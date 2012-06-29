package rxf.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import com.google.gson.*;
import one.xio.AsioVisitor;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;
import rxf.server.web.inf.ProtocolMethodDispatch;

import static java.lang.Math.abs;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static one.xio.HttpMethod.wheresWaldo;

/**
 * <a href='http://www.antipatterns.com/briefing/sld024.htm'> Blob Anti Pattern </a>
 * used here as a pattern to centralize the antipatterns
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
public class BlobAntiPatternObject {
  public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 3);
  private static Queue<SocketChannel> lazyQueue = new ConcurrentLinkedQueue<SocketChannel>();
  public static final ThreadLocal<Map<String, String>> ThreadLocalSetCookies = new ThreadLocal<Map<String, String>>();
  public static final String YYYY_MM_DD_T_HH_MM_SS_SSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  public static final Gson GSON = new GsonBuilder()
      .setDateFormat(YYYY_MM_DD_T_HH_MM_SS_SSSZ)
      .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
      .setPrettyPrinting()
      .create();
  public static final Charset ISO88591 = Charset.forName("ISO-8859-1");
  public static final String MYGEOIPSTRING = "mygeoipstring";

  public static final String COOKIE = "Cookie";

  private static final TimeUnit defaultCollectorTimeUnit = TimeUnit.SECONDS;

  public static boolean DEBUG_SENDJSON = System.getenv().containsKey("DEBUG_SENDJSON");
  public static final InetSocketAddress COUCHADDR;
  public static InetAddress LOOPBACK = null;

  static {
    try {
      try {
        BlobAntiPatternObject.LOOPBACK = (InetAddress) InetAddress.class.getMethod("getLoopBackAddress").invoke(null);
      } catch (NoSuchMethodException e) {
        BlobAntiPatternObject.LOOPBACK = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
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


//  public static final BlockingDeque<SocketChannel> couchDq = new LinkedBlockingDeque<SocketChannel>(5);static {
//    Runnable task = new Runnable() {
//
//      public void run() {
//        while (!HttpMethod.killswitch) {
//
//          SocketChannel channel = null;
//
//          System.err.println("opening " + new InetSocketAddress(LOOPBACK, 5984).toString());
//
//          try {
//            channel = lazyQueue.poll();
//            if (null == channel) {
//              channel = SocketChannel.open();
//              channel.configureBlocking(false);
//              channel.connect(new InetSocketAddress(LOOPBACK, 5984));
//            } else couchDq.putFirst(channel);
//            couchDq.putLast(channel);
//          } catch (Exception e) {
//            throw new Error("couch connector down - failllling fast!" + wheresWaldo(3));
//          }
//
//        }
//      }
//    };
//    //aggressively push two threads into 3 entries
//    EXECUTOR_SERVICE.submit(task);
//  }


  public static SocketChannel createCouchConnection() {
    while (!HttpMethod.killswitch) {
      try {
        SocketChannel channel = SocketChannel.open(COUCHADDR);
        channel.configureBlocking(false);
        return channel;
      } catch (IOException e) {
        System.err.println("----- very bad connection failure in " + wheresWaldo(4));
        e.printStackTrace();
      } finally {
      }
    }
    return null;
  }


  public static ByteBuffer moveCaretToDoubleEol(ByteBuffer buffer) {
    int distance;
    int eol = buffer.position();

    do {
      int prev = eol;
      while (buffer.hasRemaining() && '\n' != buffer.get()) ;
      eol = buffer.position();
      distance = abs(eol - prev);
      if (2 == distance && '\r' == buffer.get(eol - 2)) break;
    } while (buffer.hasRemaining() && 1 < distance);
    return buffer;
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
        receiveBufferSize = couchConnection.socket().getReceiveBufferSize();
        recycleChannel(couchConnection);
      } catch (IOException ignored) {
      }

    return receiveBufferSize;
  }

  public static int getSendBufferSize() {
    if (0 == sendBufferSize)
      try {
        SocketChannel couchConnection = createCouchConnection();
        sendBufferSize = couchConnection.socket().getReceiveBufferSize();
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


  public static Map<HttpMethod, Map<Pattern, Impl>> getNamespace() {
    return ProtocolMethodDispatch.NAMESPACE;
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
    GeoIpService.startGeoIpService("geoip");
    startServer(args);
  }

  public static void startServer(String... args) throws IOException {
    AsioVisitor topLevel = new ProtocolMethodDispatch();
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(8080));
    serverSocketChannel.configureBlocking(false);
    HttpMethod.enqueue(serverSocketChannel, OP_ACCEPT, topLevel);
    /*
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(8888));
    serverSocketChannel.configureBlocking(false);
    HttpMethod.enqueue(serverSocketChannel, OP_ACCEPT, topLevel);
    */
    /*
         SessionCouchAgent<Visitor> ro = new SessionCouchAgent<Visitor>(VISITOR_LOCATOR);
        HttpMethod.enqueue(createCouchConnection(), OP_CONNECT | OP_WRITE, ro, ro.getFeedString());
    */
    HttpMethod.init(args, topLevel, 1000);
  }

  public static TimeUnit getDefaultCollectorTimeUnit() {
    return DEBUG_SENDJSON ? TimeUnit.HOURS : defaultCollectorTimeUnit;
  }

  public static ByteBuffer avoidStarvation(ByteBuffer buf) {
    if (0 == buf.remaining()) buf.rewind();
    return buf;
  }

  public static String getDefaultOrgName() {
    return System.getenv("RXF_ORGNAME") == null ? System.getProperty(CouchLocator.class.getCanonicalName().toLowerCase() + ".orgname", "rxf_") : System.getenv("RXF_ORGNAME").toLowerCase().trim();
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
                                          ByteBuffer currentBuff,
                                          ByteBuffer... prev) {
    ByteBuffer tb = currentBuff;
    int prevMark = prev.length;
    int backtrack = 0;
    boolean mismatch = false;
    int bl = terminator.length;
    for (int i = bl - 1; i >= 0 && !mismatch; i--) {
      int rskip = bl - i;
      int comparisonOffset = tb.position() - rskip - backtrack;
      if (comparisonOffset < 0) {
        if (prevMark-- > 0 && prevMark < prev.length && null != (tb = prev[prevMark])) {
          mismatch = true;
        } else {
          backtrack += tb.position();
//          tb = riter.next();
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
}
