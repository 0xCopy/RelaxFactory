package rxf.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;

import static java.lang.Math.abs;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.getSelector;
import static one.xio.HttpMethod.wheresWaldo;

/**
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
public class BlobAntiPatternObject {
  public static final VisitorLocator VISITOR_LOCATOR = new VisitorLocator();
  public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 3);
  private static Queue<SocketChannel> lazyQueue = new ConcurrentLinkedQueue<SocketChannel>();
  public static final ThreadLocal<ByteBuffer> ThreadLocalHeaders = new ThreadLocal<ByteBuffer>();
  public static ThreadLocal<InetAddress> ThreadLocalInetAddress = new ThreadLocal<InetAddress>();
  public static final ThreadLocal<Map<String, String>> ThreadLocalSetCookies = new ThreadLocal<Map<String, String>>();
  private static final String VISITORSTRING = BlobAntiPatternObject.class.getCanonicalName();
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

  public static final BlockingDeque<SocketChannel> couchDq = new LinkedBlockingDeque<SocketChannel>(5);static {
    final Runnable task = new Runnable() {
      @Override
      public void run() {
        while (!HttpMethod.killswitch) {

          SocketChannel channel = null;

          System.err.println("opening " + new InetSocketAddress(LOOPBACK, 5984).toString());

          try {
            channel = (SocketChannel) lazyQueue.poll();
            if (channel == null) {
              channel = SocketChannel.open();
              channel.configureBlocking(false);
              channel.connect(new InetSocketAddress(LOOPBACK, 5984));
            } else couchDq.putFirst(channel);
            couchDq.putLast(channel);
          } catch (Exception e) {
            throw new Error("couch connector down - failllling fast!" + wheresWaldo(3));
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
  }


  static public String getSessionCookieId() throws Exception {
    String id = null;
    Visitor roSession = null;
    try {
      ThreadLocalSessionHeaders invoke = new ThreadLocalSessionHeaders().invoke();
      ByteBuffer headerBuffer = invoke.getHb();
      Map<String, int[]> headerIndex = invoke.getHeaders();
//      String s = UTF8.decode((ByteBuffer) hb.rewind()).toString().trim();
//      System.err.println("gsci:" + s);

      if (headerIndex.containsKey(COOKIE)) {
        int[] optionalStartStopMarkers = headerIndex.get(COOKIE);
        id = getCookieAsString(VISITORSTRING, headerBuffer, optionalStartStopMarkers);
      }

      /* if (null != id)
   roSession = RO_SESSION_LOCATOR.find(RoSession.class, id);*/
    } catch (Throwable e) {
      System.err.println("cookie failure on " + id);
    }
    if (null == id) {
      roSession = VISITOR_LOCATOR.create(Visitor.class);
      id = roSession.getId();

      Map<String, String> stringMap = ThreadLocalSetCookies.get();
      if (null == stringMap) {
        Map<String, String> value = new TreeMap<String, String>();
        value.put(VISITORSTRING, id);
        ThreadLocalSetCookies.set(value);
      }
      Date expire = new Date(TimeUnit.DAYS.toMillis(14) + System.currentTimeMillis());
      String cookietext = MessageFormat.format("{0} ; path=/ ; expires={1} ; HttpOnly", id, expire.toGMTString());
      ThreadLocalSetCookies.get().put(VISITORSTRING, cookietext);
      final InetAddress inet4Address = ThreadLocalInetAddress.get();
      if (null != inet4Address) {


        final String geoip = MessageFormat.format("{0} ; path=/ ; expires={1}", GeoIpService.mapAddressLookup(inet4Address), expire.toGMTString());
        ThreadLocalSetCookies.get().put(MYGEOIPSTRING, geoip);
        EXECUTOR_SERVICE.schedule(new Runnable() {
          @Override
          public void run() {
            try {

              setSessionProperty(InetAddress.class.getCanonicalName(), inet4Address.getCanonicalHostName());
              setSessionProperty("geoip", geoip);

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

  static public Visitor getCurrentSession() throws Exception {
    String id = null;
    Visitor roSession = null;
    id = getSessionCookieId();
    if (null != id)
      roSession = VISITOR_LOCATOR.find(Visitor.class, id);
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


  public static long sortableInetAddress(InetAddress inet4Address) {
    byte[] address = inet4Address.getAddress();
    long compare = 0;
    for (int i = 0; i < address.length; i++) {
      compare |= (address[i] & 0xff) << 8 * (address.length - 1 - i);
    }
    return compare;
  }

  public static SocketChannel createCouchConnection() {

    while (!HttpMethod.killswitch) {

      try {
        SocketChannel take = couchDq.take();
        if (take.isOpen()) {
          System.err.println("createCouch" + wheresWaldo());
          return take;
        }
      } catch (InterruptedException e) {
        e.printStackTrace();  //todo: verify for a purpose
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

  static String deepToString(Object... d) {
    return Arrays.deepToString(d) + wheresWaldo();
  }

  public static void recycleChannel(SocketChannel channel) throws IOException {
    lazyQueue.add(channel);
    System.err.println("--- recycling" + wheresWaldo());

  }

  public static int getReceiveBufferSize() {
    if (0 == rbs)
      try {
        SocketChannel couchConnection = createCouchConnection();
        rbs = couchConnection.socket().getReceiveBufferSize();
        recycleChannel(couchConnection);
      } catch (IOException ignored) {

      }

    return rbs;
  }

  public static int getSendBufferSize() {
    if (0 == sbs)
      try {
        SocketChannel couchConnection = createCouchConnection();
        sbs = couchConnection.socket().getReceiveBufferSize();
        recycleChannel(couchConnection);
      } catch (IOException ignored) {


      }
    return sbs;
  }

  public static String getRevision(Map map) {
    String rev = null;

    rev = (String) map.get("_rev");
    if (null == rev)
      rev = (String) map.get("rev");
    if (null == rev) {
      rev = (String) map.get("version");
    }
    if (null == rev)
      rev = (String) map.get("ver");
    return rev;
  }

  public static AsioVisitor fetchJsonByIdVisitor(final String path, final SocketChannel channel, final SynchronousQueue<String> returnTo) throws ClosedChannelException {
    final String format = (MessageFormat.format("GET /{0} HTTP/1.1\r\n\r\n", path.trim()));
    return executeCouchRequest(channel, returnTo, format);
  }

  public static AsioVisitor executeCouchRequest(final SocketChannel channel, final SynchronousQueue<String> returnTo, final String requestHeaders) throws ClosedChannelException {
    final AsioVisitor.Impl impl = new AsioVisitor.Impl() {
      @Override
      public void onWrite(final SelectionKey key) throws IOException {
        System.err.println("attempting connect: " + requestHeaders.trim());
        channel.write(UTF8.encode(requestHeaders));
        key.interestOps(OP_READ).attach(createJsonResponseReader(returnTo));
      }
    };
    HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, impl);
    return impl;
  }

  //maximum wastefulness
  static public String setSessionProperty(String key, String value) throws Exception {
    try {
      String id = BlobAntiPatternObject.getSessionCookieId();
      Map linkedHashMap = fetchMapById(id);
      linkedHashMap.put(key, value);
      CouchTx tx = sendJson(GSON.toJson(linkedHashMap), VisitorPropertiesAccess.INSTANCE + "/" + id, String.valueOf(linkedHashMap.get("_rev")));

      return tx.rev;
    } catch (Throwable ignored) {

    }
    return null;
  }

  /**
   * particular to security
   *
   * @param key
   * @return
   * @throws InterruptedException
   */
  static public String getSessionProperty(String key) throws Exception {


    String sessionCookieId = getSessionCookieId();
    String s = null;
    String canonicalName = null;
    try {
      s = (String) fetchMapById(sessionCookieId).get(key);
    } catch (Exception e) {
      canonicalName = VisitorPropertiesAccess.class.getCanonicalName();
      ByteBuffer byteBuffer = ThreadLocalHeaders.get();
      if (!UTF8.decode(byteBuffer).toString().trim().equals(canonicalName)) {

        ThreadLocalHeaders.set(UTF8.encode(canonicalName));
        return getSessionProperty(key);
      }
    }
    return s;
  }

  /**
   * @param json
   * @return new _rev
   */
  public static CouchTx sendJson(String json, String... idver) throws Exception {
    String take;
    SocketChannel channel = null;
    try {
      channel = createCouchConnection();
      SynchronousQueue<String> retVal = new SynchronousQueue<String>();
      HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, new SendJsonVisitor(json, retVal, idver));
      take = retVal.take();
    } finally {
      if (null != channel) {
        channel.register(getSelector(), 0);
        couchDq.add(channel);
      }
    }
    return GSON.fromJson(take, CouchTx.class);
  }

  public static LinkedHashMap fetchMapById(String key) throws IOException, InterruptedException {
    SocketChannel channel = createCouchConnection();
    String take1;
    try {
      SynchronousQueue<String> retVal = new SynchronousQueue<String>();
      HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, fetchJsonByIdVisitor(VisitorPropertiesAccess.INSTANCE + '/' + key, channel, retVal));
      take1 = retVal.take();
    } finally {
      recycleChannel(channel);
    }
    String take = take1;
    LinkedHashMap linkedHashMap = GSON.fromJson(take, LinkedHashMap.class);
    if (2 == linkedHashMap.size() && linkedHashMap.containsKey("responseCode"))
      throw new IOException(deepToString(linkedHashMap));
    return linkedHashMap;
  }

  public static AsioVisitor createJsonResponseReader(final SynchronousQueue<String> returnTo) {
    return new AsioVisitor.Impl() {
      public long total;
      public long remaining;

      @Override
      public void onRead(SelectionKey key) throws IOException, InterruptedException {
        final SocketChannel channel = (SocketChannel) key.channel();
        {
          final int receiveBufferSize = BlobAntiPatternObject.getReceiveBufferSize();
          ByteBuffer dst = ByteBuffer.allocate(receiveBufferSize);
          int read = channel.read(dst);
          if (-1 == read) {
            channel.socket().close();
            final String o = GSON.toJson(new CouchTx() {{
              error = "closed socket";
              reason = "closed socket";
            }});
            returnTo.put(o);
            throw new IOException(o);
          }

          dst.flip();

          final String rescode = BlobAntiPatternObject.parseResponseCode(dst);

          BlobAntiPatternObject.moveCaretToDoubleEol(dst);
          System.err.println("result: " + UTF8.decode((ByteBuffer) dst.duplicate().flip()));
          int[] bounds = HttpHeaders.getHeaders((ByteBuffer) dst.duplicate().flip()).get("Content-Length");
          if (null != bounds) {
            total = Long.parseLong(UTF8.decode((ByteBuffer) dst.duplicate().limit(bounds[1]).position(bounds[0])).toString().trim());
            remaining = total - dst.remaining();

            ByteBuffer payload;
            if (remaining <= 0) {
              payload = dst.slice();
              BlobAntiPatternObject.returnJsonStringOrErrorResponse(returnTo, key, rescode, payload);
            } else {
              final LinkedList<ByteBuffer> ll = new LinkedList<ByteBuffer>();
              //
              //    synchronousQueue.clear();
              ll.add(dst.slice());
              key.interestOps(SelectionKey.OP_READ).attach(new Impl() {
                @Override
                public void onRead(SelectionKey key) throws InterruptedException, IOException {
                  ByteBuffer payload = ByteBuffer.allocate(receiveBufferSize);
                  int read = channel.read(payload);
                  ll.add(payload);
                  remaining -= read;
                  if (0 == remaining) {
                    payload = ByteBuffer.allocate((int) total);
                    ListIterator<ByteBuffer> iter = ll.listIterator();
                    while (iter.hasNext()) {
                      ByteBuffer buffer = iter.next();
                      iter.remove();
                      if (buffer.position() == total)
                        payload = (ByteBuffer) buffer.flip();
                      else
                        payload.put(buffer);     //todo: rewrite this up-kernel
                    }
                    BlobAntiPatternObject.returnJsonStringOrErrorResponse(returnTo, key, rescode, payload);
                  }
                }
              });
              key.selector().wakeup();
            }
          }
        }
      }
    };
  }

  static void returnJsonStringOrErrorResponse(SynchronousQueue<String> returnTo, SelectionKey key, String rescode, ByteBuffer payload) throws InterruptedException {
    key.attach(null);
    System.err.println("payload: " + UTF8.decode((ByteBuffer) payload.duplicate().rewind()));
    if (!payload.hasRemaining())
      payload.rewind();

    String trim = UTF8.decode(payload).toString().trim();
    if (rescode.startsWith("20") && rescode.length() == 3) {
      returnTo.put(trim);
    } else {
      returnTo.put(MessageFormat.format("'{'\"responseCode\":\"{0,number,#}\",\"orig\":{1}'}'", rescode, trim));
    }
  }

  static String parseResponseCode(ByteBuffer dst) {
    ByteBuffer d2 = null;
    try {
      while (!Character.isWhitespace(dst.get())) ;
      d2 = dst.duplicate();
      while (!Character.isWhitespace(dst.get())) ;
      return UTF8.decode((ByteBuffer) d2.limit(dst.position() - 1)).toString();
    } catch (Throwable ignored) {

    }
    return null;
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
          VisitorLocator roSessionLocator = new VisitorLocator();
          Visitor roSession = roSessionLocator.create(Visitor.class);
          id = roSession.getId();
          String s = GSON.toJson(roSession);
          System.err.println("created: " + s);
        }

        {
          VisitorLocator roSessionLocator = new VisitorLocator();
          Visitor roSession = roSessionLocator.find(Visitor.class, id);
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
    final CouchAgent.SessionCouchAgent ro = new CouchAgent.SessionCouchAgent("rxf");
    HttpMethod.enqueue(createCouchConnection(), OP_CONNECT | OP_WRITE, ro, ro.getFeedString());
    HttpMethod.init(args, topLevel);
  }

}
