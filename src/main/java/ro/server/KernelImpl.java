package ro.server;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import org.xml.sax.SAXException;
import ro.model.RoSession;

import static java.lang.Math.abs;
import static java.lang.Math.min;
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

  public static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
  public static final ThreadLocal<ByteBuffer> ThreadLocalHeaders = new ThreadLocal<ByteBuffer>();
  static ThreadLocal<InetAddress> ThreadLocalInetAddress = new ThreadLocal<InetAddress>();
  public static final ThreadLocal<Map<String, String>> ThreadLocalSetCookies = new ThreadLocal<Map<String, String>>();
  private static final String MYSESSIONSTRING = KernelImpl.class.getCanonicalName();

  //  private static ByteBuffer indexBuf;
  private static int blockCount;
  //  private static ByteBuffer locBuf;
  public static InetAddress LOOPBACK = null; static {
    try {
      try {
        KernelImpl.LOOPBACK = (InetAddress) InetAddress.class.getMethod("getLoopBackAddress").invoke(null);


      } catch (NoSuchMethodException e) {
        KernelImpl.LOOPBACK = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        System.err.println("java 6 LOOPBACK detected");
      } catch (InvocationTargetException e) {
        e.printStackTrace();  //todo: verify for a purpose
      } catch (IllegalAccessException e) {
        e.printStackTrace();  //todo: verify for a purpose
      }
    } catch (UnknownHostException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }

  public static final Gson GSON = new GsonBuilder()
//    .registerTypeAdapter(Id.class, new IdTypeAdapter())
      .enableComplexMapKeySerialization()
//    .serializeNulls()
      .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
      .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
      .setPrettyPrinting()
//    .setVersion(1.0)
      .create();
  public static final Charset ISO88591 = Charset.forName("ISO-8859-1");
  public static final String MYGEOIPSTRING = "mygeoipstring";


  static public RoSession getCurrentSession() throws UnsupportedEncodingException {
    String id = null;
    RoSession roSession = null;
    try {
      id = getSessionCookieId();
      if (null != id)
        roSession = RO_SESSION_LOCATOR.find(RoSession.class, id);
    } catch (Throwable e) {
      System.err.println("cookie failure on " + id);
      e.printStackTrace();
    }
    if (null == roSession) {
      roSession = RO_SESSION_LOCATOR.create(RoSession.class);
      Map<String, String> o = ThreadLocalSetCookies.get();
      if (null == o) {
        Map<String, String> value = new TreeMap<String, String>();
        value.put(MYSESSIONSTRING, roSession.getId());
        ThreadLocalSetCookies.set(value);
      }
      String s = new Date(TimeUnit.DAYS.toMillis(14) + System.currentTimeMillis()).toGMTString();
      final String sessionCookie = MessageFormat.format("{0}; path=/ ; expires=\"{1}\" ; HttpOnly", roSession.getId(), s);
      ThreadLocalSetCookies.get().put(MYSESSIONSTRING, sessionCookie);
      final InetAddress inet4Address = ThreadLocalInetAddress.get();
      if (null != inet4Address) {
        final int i = lookupInetAddress(inet4Address, GeoIpService.indexMMBuf, bufAbstraction);
        final ByteBuffer b = (ByteBuffer) GeoIpService.locationMMBuf.duplicate().clear().position(i);
        while (b.hasRemaining() && '\n' != b.get()) ;
        b.flip().position(i);
        final CharBuffer cityString = ISO88591.decode((ByteBuffer) b);
        final String geoip = MessageFormat.format("{0}; path=/ ; expires=\"{1}\" ", URLEncoder.encode(cityString.toString().trim(), ISO88591.name()), s);
        ThreadLocalSetCookies.get().put(MYGEOIPSTRING, geoip);
      }
    }

    return roSession;
  }


  static String getSessionCookieId() {
    ThreadLocalSessionHeaders invoke = new ThreadLocalSessionHeaders().invoke();
    ByteBuffer hb = invoke.getHb();
    Map<String, int[]> headerIndex = invoke.getHeaders();
    String headerStrring = UTF8.decode((ByteBuffer) hb.rewind()).toString().trim();
    System.err.println("gsci:" + headerStrring);
    String id = null;
    if (headerIndex.containsKey("Cookie")) {
      int[] cookieses = headerIndex.get("Cookie");
      String coo = HttpMethod.UTF8.decode((ByteBuffer) hb.limit(cookieses[1]).position(cookieses[0])).toString().trim();

      String[] split = coo.split(";");
      for (String s : split) {
        String[] chunk = s.split("=");
        String cname = chunk[0];
        if (MYSESSIONSTRING.equals(cname.trim())) {
          id = chunk[1].trim();
          break;
        }
      }
    }
    return id;
  }

  //test
  public static void main(String... args) throws InterruptedException, IOException, ExecutionException, ParserConfigurationException, SAXException, XPathExpressionException {


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
  public static int lookupInetAddress(InetAddress inet4Address, final ByteBuffer indexRecords, List<Long> bufAbstraction) {

    int newPosition;
    int abs;
    int ret;

    final byte[] address = inet4Address.getAddress();
    long compare = 0;
    for (int i = 0; i < address.length; i++) {
      compare |= (address[i] & 0xff) << 8 * (address.length - 1 - i);
    }
    int a = Collections.binarySearch(bufAbstraction, IPMASK & compare);
    final int b = bufAbstraction.size() - 1;
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
    HttpMethod.enqueue(serverSocketChannel, SelectionKey.OP_ACCEPT, topLevel);
    HttpMethod.init(args, topLevel);
  }

  public static SocketChannel createCouchConnection() throws IOException {
    System.err.println("opening " + new InetSocketAddress(LOOPBACK, 5984).toString());
    final SocketChannel channel = SocketChannel.open();
    channel.configureBlocking(false);
    channel.connect(new InetSocketAddress(LOOPBACK, 5984));
    return channel;
  }

  public static void moveCaretToDoubleEol(ByteBuffer dst) {
    byte b;
    boolean eol = false;
    while (dst.hasRemaining() && (b = dst.get()) != -1) {
      if (b != '\n') {
        if (b != '\r') {
          eol = false;
        }
      } else {
        if (!eol) {
          eol = true;
        } else {
          break;
        }
      }
    }
  }

  private static class ThreadLocalSessionHeaders {

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
}

