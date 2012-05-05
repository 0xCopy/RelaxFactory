package ro.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import ro.model.RoSession;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static one.xio.HttpMethod.UTF8;
import static ro.server.GeoIpIndexRecord.reclen;
import static ro.server.GeoIpService.IPMASK;
import static ro.server.GeoIpService.bufAbstraction;

/**
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
public class KernelImpl {
  public static final RoSessionLocator RO_SESSION_LOCATOR = new RoSessionLocator();
  public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 3);
  public static final ThreadLocal<ByteBuffer> ThreadLocalHeaders = new ThreadLocal<ByteBuffer>();
  public static ThreadLocal<InetAddress> ThreadLocalInetAddress = new ThreadLocal<InetAddress>();
  public static final ThreadLocal<Map<String, String>> ThreadLocalSetCookies = new ThreadLocal<Map<String, String>>();
  private static final String MYSESSIONSTRING = KernelImpl.class.getCanonicalName();
  public static final String YYYY_MM_DD_T_HH_MM_SS_SSSZ = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
  public static final Gson GSON = new GsonBuilder()
//    .registerTypeAdapter(Id.class, new IdTypeAdapter())
      .enableComplexMapKeySerialization()
//    .serializeNulls()
      .setDateFormat(YYYY_MM_DD_T_HH_MM_SS_SSSZ)
      .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
      .setPrettyPrinting()
//    .setVersion(1.0)
      .create();
  public static final Charset ISO88591 = Charset.forName("ISO-8859-1");
  public static final String MYGEOIPSTRING = "mygeoipstring";
  public static final String COOKIE = "Cookie";

  //  private static ByteBuffer indexBuf;
  private static int blockCount;
  //  private static ByteBuffer locBuf;
  public static InetAddress LOOPBACK = null;
  public static final BlockingDeque couchDq = new LinkedBlockingDeque(5);static {
    final Runnable task = new Runnable() {
      @Override
      public void run() {
        while (true) {

          SocketChannel channel = null;

          System.err.println("opening " + new InetSocketAddress(LOOPBACK, 5984).toString());
          try {
            channel = SocketChannel.open();
            channel.configureBlocking(false);
            channel.connect(new InetSocketAddress(LOOPBACK, 5984));
            couchDq.putLast(channel);
          } catch (Exception e) {
            throw new Error("couch connector down - failllling fast!");
          }

        }
      }
    };
    //aggressively push two threads into 3 entries
    EXECUTOR_SERVICE.submit(task);
  }

  private static int rbs;
  private static int sbs;

  static {
    try {
      try {
        KernelImpl.LOOPBACK = (InetAddress) InetAddress.class.getMethod("getLoopBackAddress").invoke(null);
      } catch (NoSuchMethodException e) {
        KernelImpl.LOOPBACK = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        System.err.println("java 6 LOOPBACK detected");
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }


  static public String getSessionCookieId() throws Exception {
    String id = null;
    RoSession roSession = null;
    try {
      ThreadLocalSessionHeaders invoke = new ThreadLocalSessionHeaders().invoke();
      ByteBuffer headerBuffer = invoke.getHb();
      Map<String, int[]> headerIndex = invoke.getHeaders();
//      String s = UTF8.decode((ByteBuffer) hb.rewind()).toString().trim();
//      System.err.println("gsci:" + s);

      if (headerIndex.containsKey(COOKIE)) {
        int[] optionalStartStopMarkers = headerIndex.get(COOKIE);
        id = getCookieAsString(MYSESSIONSTRING, headerBuffer, optionalStartStopMarkers);
      }

      /* if (null != id)
   roSession = RO_SESSION_LOCATOR.find(RoSession.class, id);*/
    } catch (Throwable e) {
      System.err.println("cookie failure on " + id);
    }
    if (null == id) {
      roSession = RO_SESSION_LOCATOR.create(RoSession.class);
      id = roSession.getId();

      Map<String, String> stringMap = ThreadLocalSetCookies.get();
      if (null == stringMap) {
        Map<String, String> value = new TreeMap<String, String>();
        value.put(MYSESSIONSTRING, id);
        ThreadLocalSetCookies.set(value);
      }
      Date expire = new Date(TimeUnit.DAYS.toMillis(14) + System.currentTimeMillis());
      String cookietext = MessageFormat.format("{0} ; path=/ ; expires={1} ; HttpOnly", id, expire.toGMTString());
      ThreadLocalSetCookies.get().put(MYSESSIONSTRING, cookietext);
      final InetAddress inet4Address = ThreadLocalInetAddress.get();
      if (null != inet4Address) {

        int i = lookupInetAddress(inet4Address, GeoIpService.indexMMBuf, bufAbstraction);
        ByteBuffer b = (ByteBuffer) GeoIpService.locationMMBuf.duplicate().clear().position(i);
        while (b.hasRemaining() && '\n' != b.get()) ;
        rtrimByteBuffer(b).position(i);

        //maxmind is iso not utf
        CharBuffer cityString = ISO88591.decode(b);//attempt a utf8 switchout here...
        final String geoip = MessageFormat.format("{0} ; path=/ ; expires={1}", UTF8.decode(ByteBuffer.wrap(cityString.toString().getBytes(UTF8))), expire.toGMTString());
        ThreadLocalSetCookies.get().put(MYGEOIPSTRING, geoip);
        EXECUTOR_SERVICE.schedule(new Runnable() {
          @Override
          public void run() {
            try {

              SessionToolImpl.setSessionProperty(InetAddress.class.getCanonicalName(), inet4Address.getCanonicalHostName());
              SessionToolImpl.setSessionProperty("geoip", geoip);

            } catch (Throwable ignored) {
            }
          }
        }, 250, MILLISECONDS);
      }
    }
    return id;
  }

  private static ByteBuffer rtrimByteBuffer(ByteBuffer b) {
    while (Character.isWhitespace(b.get(b.position() - 1))) b.position(b.position() - 1);
    b.flip();
    return b;
  }

  static public RoSession getCurrentSession() throws Exception {
    String id = null;
    RoSession roSession = null;
    id = getSessionCookieId();
    if (null != id)
      roSession = RO_SESSION_LOCATOR.find(RoSession.class, id);
    return roSession;
  }

  public static String getCookieAsString(String cookieKey, ByteBuffer headerBuffer, int... optionalStartStopMarkers) {

    if (0 < optionalStartStopMarkers.length) {
      if (1 < optionalStartStopMarkers.length) {
        headerBuffer.limit(optionalStartStopMarkers[1]);
      }
      headerBuffer.position(optionalStartStopMarkers[0]);
    }
    String coo =
        UTF8.decode(headerBuffer).toString().trim();

    String[] split = coo.split(";");
    String val = null;
    for (String s : split) {
      String[] chunk = s.split("=");
      String cname = chunk[0];
      if (cookieKey.equals(cname.trim())) {
        val = chunk[1].trim();
        break;
      }
    }
    return val;
  }


  /**
   * offset to locationcsv buffer [n] to EOL
   *
   * @param inet4Address
   * @param indexRecords
   * @param bufAbstraction
   * @return
   */
  public static int lookupInetAddress(InetAddress inet4Address, final ByteBuffer indexRecords, List<Long> bufAbstraction) {

    int newPosition;
    int abs;
    int ret;

    byte[] address = inet4Address.getAddress();
    long compare = 0;
    for (int i = 0; i < address.length; i++) {
      compare |= (address[i] & 0xff) << 8 * (address.length - 1 - i);
    }
    int a = Collections.binarySearch(bufAbstraction, IPMASK & compare);
    int b = bufAbstraction.size() - 1;
    abs = min(abs(a), b);

    newPosition = reclen * abs + 4;
    indexRecords.position(newPosition);
    ret = indexRecords.getInt();


    return ret;
  }

  public static SocketChannel createCouchConnection() {

      while (true) {

        try {
          SocketChannel take = (SocketChannel) couchDq.take();
          if (take.isOpen()) {
            return take;
          }
        } catch (InterruptedException e) {
          e.printStackTrace();  //todo: verify for a purpose
        }
      }

  }


  public static void moveCaretToDoubleEol(ByteBuffer buffer) {
    int distance;
    int eol = buffer.position();

    do {
      int prev = eol;
      while (buffer.hasRemaining() && '\n' != buffer.get()) ;
      eol = buffer.position();
      distance = abs(eol - prev);
      if (2 == distance && '\r' == buffer.get(eol - 2)) break;
    } while (buffer.hasRemaining() && 1 < distance);
  }

  static String deepToString(Object... d) {
    return Arrays.deepToString(d);
  }

  public static void recycleChannel(SocketChannel channel) throws IOException {
    final boolean offer = couchDq.offer(channel);
    if (!offer) channel.close();
  }

  public static int getReceiveBufferSize() {
    if (0 == rbs)
      try {
        rbs = createCouchConnection().socket().getReceiveBufferSize();
      } catch (IOException e) {
        e.printStackTrace();  //todo: verify for a purpose
      }

    return rbs;
  }

  public static int getSendBufferSize() {
    if (0 == sbs)
      try {
        sbs = createCouchConnection().socket().getSendBufferSize();
      } catch (IOException e) {


      }
    return sbs;
  }

  static class ThreadLocalSessionHeaders {

    private ByteBuffer hb;

    private Map<String, int[]> headers;

    public ByteBuffer getHb() {
      return hb;
    }

    public Map<String, int[]> getHeaders() {
      return headers;
    }

    public ThreadLocalSessionHeaders invoke() {
      hb = ThreadLocalHeaders.get();
      headers = HttpHeaders.getHeaders((ByteBuffer) hb.rewind());
      return this;
    }

  }

  //test
  public static void main(String... args) throws Exception {


    GeoIpService.startGeoIpService("geoip");


    EXECUTOR_SERVICE.submit(new Callable<Object>() {


      public Object call() throws IOException {
        String id;
        {
          RoSessionLocator roSessionLocator = new RoSessionLocator();
          RoSession roSession = roSessionLocator.create(RoSession.class);
          id = roSession.getId();
          String s = GSON.toJson(roSession);
          System.err.println("created: " + s);
        }

        {
          RoSessionLocator roSessionLocator = new RoSessionLocator();
          RoSession roSession = roSessionLocator.find(RoSession.class, id);
          String s = GSON.toJson(roSession);
          System.err.println("find: " + s);

        }

        return null;
      }
    });

    startServer(args);

  }

  public static void startServer(String... args) throws IOException {
    AsioVisitor topLevel = new RfPostWrapper();
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(8080));
    serverSocketChannel.configureBlocking(false);
    HttpMethod.enqueue(serverSocketChannel, OP_ACCEPT, topLevel);
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(8888));
    serverSocketChannel.configureBlocking(false);
    HttpMethod.enqueue(serverSocketChannel, OP_ACCEPT, topLevel);
    final CouchAgent.SessionCouchAgent ro = new CouchAgent.SessionCouchAgent("ro");
    HttpMethod.enqueue(createCouchConnection(), OP_CONNECT | OP_WRITE, ro, ro.getFeedString());
    HttpMethod.init(args, topLevel);
  }
}
