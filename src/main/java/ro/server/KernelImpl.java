package ro.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import ro.model.RoSession;

import static java.lang.Character.isWhitespace;
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
  public static ThreadLocal<ByteBuffer> headersByteBufferThreadLocal = new ThreadLocal<ByteBuffer>();
  public static ThreadLocal<Inet4Address> inet4AddressThreadLocal = new ThreadLocal<Inet4Address>();
  public static ThreadLocal<Map<String, String>> cookieSetterMapThreadLocal = new ThreadLocal<Map<String, String>>();
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
  public static final ConcurrentLinkedQueue<SocketChannel> couchDq = new ConcurrentLinkedQueue<SocketChannel>();

  static {
    try {
      try {
        KernelImpl.LOOPBACK = (InetAddress) InetAddress.class.getMethod("getLoopBackAddress").invoke(null);
      } catch (NoSuchMethodException e) {
        KernelImpl.LOOPBACK = InetAddress.getByAddress(new byte[]{(byte) 127, (byte) 0, (byte) 0, (byte) 1});
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


  public static String getSessionCookieId() throws Exception {
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

      Map<String, String> stringMap = cookieSetterMapThreadLocal.get();
      if (null == stringMap) {
        Map<String, String> value = new TreeMap<String, String>();
        value.put(MYSESSIONSTRING, id);
        cookieSetterMapThreadLocal.set(value);
      }
      Date expire = new Date(TimeUnit.DAYS.toMillis(14L) + System.currentTimeMillis());
      String cookietext = MessageFormat.format("{0} ; path=/ ; expires={1} ; HttpOnly", id, expire.toGMTString());
      cookieSetterMapThreadLocal.get().put(MYSESSIONSTRING, cookietext);
      final Inet4Address inet4Address = inet4AddressThreadLocal.get();
      if (null != inet4Address) {

        int i = lookupInetAddress(inet4Address, GeoIpService.indexMMBuf, bufAbstraction);
        ByteBuffer b = (ByteBuffer) GeoIpService.locationMMBuf.duplicate().clear().position(i);
        while (b.hasRemaining() && (int) '\n' != (int) b.get()) {
        }
        rtrimByteBuffer(b).position(i);

        //maxmind is iso not utf
        CharBuffer cityString = ISO88591.decode(b);//attempt a utf8 switchout here...
        final String geoip = MessageFormat.format("{0} ; path=/ ; expires={1}", UTF8.decode(ByteBuffer.wrap(cityString.toString().getBytes(UTF8))), expire.toGMTString());
        cookieSetterMapThreadLocal.get().put(MYGEOIPSTRING, geoip);
        EXECUTOR_SERVICE.schedule(new Runnable() {
          @Override
          public void run() {
            try {

              SessionToolImpl.setSessionProperty(InetAddress.class.getCanonicalName(), inet4Address.getCanonicalHostName());
              SessionToolImpl.setSessionProperty("geoip", geoip);

            } catch (Throwable ignored) {
            }
          }
        }, 250L, MILLISECONDS);
      }
    }
    return id;
  }

  public static ByteBuffer rtrimByteBuffer(Buffer b) {

    ByteBuffer b1 = (ByteBuffer) b;
    while (isWhitespace((int) b1.get(b1.position() - 1))) {
      b1.position(b1.position() - 1);
    }
    b1.flip();
    return b1;
  }

  public static RoSession getCurrentSession() throws Exception {
    String id = null;
    RoSession roSession = null;
    id = getSessionCookieId();
    if (null != id) {
      roSession = RO_SESSION_LOCATOR.find(RoSession.class, id);
    }
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

  /**
   * offset to locationcsv buffer [n] to EOL
   *
   * @param inet4Address
   * @param indexRecords
   * @param bufAbstraction
   * @return
   */
  public static int lookupInetAddress(Inet4Address inet4Address, ByteBuffer indexRecords, List<Long> bufAbstraction) {

    int newPosition;
    int abs;
    int ret;

    byte[] address = inet4Address.getAddress();
    long compare = 0L;
    for (int i = 0; i < address.length; i++) {
      compare |= (long) ((address[i] & 0xff) << 8 * (address.length - 1 - i));
    }
    int a = Collections.binarySearch(bufAbstraction, IPMASK & compare);
    int b = bufAbstraction.size() - 1;
    abs = min(abs(a), b);

    newPosition = reclen * abs + 4;
    indexRecords.position(newPosition);
    ret = indexRecords.getInt();


    return ret;
  }


  public static void startServer(String... args) throws IOException {

    AsioVisitor topLevel = new RfPostWrapper();
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(8080));
    serverSocketChannel.configureBlocking(false);
    HttpMethod.enqueue(serverSocketChannel, SelectionKey.OP_ACCEPT, topLevel);
    serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(8888));
    serverSocketChannel.configureBlocking(false);
    HttpMethod.enqueue(serverSocketChannel, OP_ACCEPT, topLevel);

    HttpMethod.enqueue(createCouchConnection(), OP_CONNECT | OP_WRITE, new SessionUpdateListener());
    HttpMethod.init(args, topLevel);
  }

  /**
   * stub for a system-wide eventhandler to update or more likely invalidate old proxies
   *
   * @param jsonMap map of json from couch updates
   */
  public static void adjustProxyCache(Map jsonMap) {
    System.err.println("notification from couchdb: " + jsonMap.toString());

  }

  public static SocketChannel createCouchConnection() throws IOException {

    SocketChannel channel = null;
    while (null == channel && !couchDq.isEmpty()) {

      SocketChannel remove = couchDq.remove();
      if (remove.isConnected()) {
        channel = remove;
      }
    }
    if (null == channel) {
      System.err.println("opening " + new InetSocketAddress(LOOPBACK, 5984).toString());
      channel = SocketChannel.open();
      channel.configureBlocking(false);
      channel.connect(new InetSocketAddress(LOOPBACK, 5984));
    } else {
      System.err.println("+++ recycling " + deepToString(channel));
    }

    return channel;
  }

  public static ByteBuffer moveCaretToDoubleEol(ByteBuffer buffer) {
    int distance;
    int eol = buffer.position();

    do {
      int prev = eol;
      while (buffer.hasRemaining() && (int) '\n' != (int) buffer.get()) ;
      eol = buffer.position();
      distance = abs(eol - prev);
      if (2 == distance && (int) '\r' == (int) buffer.get(eol - 2)) {
        break;
      }
    } while (buffer.hasRemaining() && 1 < distance);
    return buffer;
  }

  static String deepToString(Object... d) {
    return Arrays.deepToString(d);
  }

  public static void recycleChannel(SocketChannel channel) throws ClosedChannelException {
    channel.register(HttpMethod.getSelector(), 0).attach(null);
    couchDq.add(channel);
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
      hb = headersByteBufferThreadLocal.get();
      headers = HttpHeaders.getHeaders((ByteBuffer) hb.rewind());
      return this;
    }
  }

}
