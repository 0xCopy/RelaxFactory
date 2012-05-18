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
import java.util.EnumMap;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static one.xio.HttpMethod.GET;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.wheresWaldo;

/**
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
public class BlobAntiPatternObject {
  public static final CouchLocator<Visitor> VISITOR_LOCATOR = Visitor.createLocator();
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
  //  private static ByteBuffer locBuf;
  public static InetAddress LOOPBACK = null;

  public static final VisitorPropertiesAccess SESSION_PROPERTIES_ACCESS = new VisitorPropertiesAccess();
  public static final EnumMap<HttpMethod, LinkedHashMap<Pattern, AsioVisitor>> NAMESPACE = new EnumMap<HttpMethod, LinkedHashMap<Pattern, AsioVisitor>>(HttpMethod.class) {
    {
      final Pattern passthroughExpr = Pattern.compile("^/i(/.*)$");
      put(GET, new LinkedHashMap<Pattern, AsioVisitor>() {
        {
          put(passthroughExpr, new AsioVisitor.Impl() {
            @Override
            public void onWrite(final SelectionKey browserKey) throws Exception {

              browserKey.selector().wakeup();
              browserKey.interestOps(OP_READ);
              String path;
              ByteBuffer headers;
              final ByteBuffer dst;
              //receives impl,path,headers,first block
              final Object attachment = browserKey.attachment();
              if (!(attachment instanceof Object[])) {
                throw new UnsupportedOperationException("this GET proxy requires attach(this,path,headers,block0) to function correctly");
              }
              Object[] objects = (Object[]) attachment;
              path = (String) objects[1];
              headers = (ByteBuffer) objects[2];
              dst = (ByteBuffer) objects[3];
              final Matcher matcher = passthroughExpr.matcher(path);
              if (matcher.matches()) {
                String link = matcher.group(1);

                final String req = "GET " + link + " HTTP/1.1\r\n" +
                    "Accept: image/*, text/*\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";

                final SocketChannel couchConnection = createCouchConnection();
                HttpMethod.enqueue(couchConnection, OP_CONNECT | OP_WRITE,
                    new Impl() {
                      @Override
                      public void onRead(final SelectionKey couchKey) throws Exception {
                        final SocketChannel channel = (SocketChannel) couchKey.channel();
                        channel.read((ByteBuffer) dst.clear());
                        moveCaretToDoubleEol((ByteBuffer) dst.flip());
                        ByteBuffer headers = ((ByteBuffer) dst.duplicate().flip()).slice();

                        final Map<String, int[]> map = HttpHeaders.getHeaders((ByteBuffer) headers.rewind());
                        final int[] ints = map.get("Content-Length");
                        final int total = Integer.parseInt(UTF8.decode((ByteBuffer) headers.duplicate().clear().position(ints[0]).limit(ints[1])).toString().trim());
                        final SocketChannel browserChannel = (SocketChannel) browserKey.channel();
                        try {
                          browserChannel.write((ByteBuffer) headers.rewind());
                        } catch (IOException e) {
                          couchConnection.close();
                          return;
                        }

                        couchKey.selector().wakeup();
                        couchKey.interestOps(OP_READ).attach(new Impl() {
                          final ByteBuffer sharedBuf = ByteBuffer.allocateDirect(min(total, getReceiveBufferSize()));
                          private Impl browserSlave = new Impl() {
                            @Override
                            public void onWrite(SelectionKey key) throws Exception {
                              try {
                                final int write = browserChannel.write(dst);
                                if (!dst.hasRemaining() && remaining == 0)
                                  browserChannel.close();
                                browserKey.selector().wakeup();
                                browserKey.interestOps(0);
                                couchKey.selector().wakeup();
                                couchKey.interestOps(OP_READ).selector().wakeup();
                              } catch (Exception e) {
                                browserChannel.close();
                              } finally {
                              }
                            }
                          };
                          public int remaining = total; {
                            browserKey.attach(browserSlave);
                          }

                          @Override
                          public void onRead(final SelectionKey couchKey) throws Exception {

                            if (browserKey.isValid() && remaining != 0) {
                              dst.compact();//threadsafety guarantee by monothreaded selector

                              remaining -= couchConnection.read(dst);
                              dst.flip();
                              couchKey.selector().wakeup();
                              couchKey.interestOps(0);
                              browserKey.selector().wakeup();
                              browserKey.interestOps(OP_WRITE).selector().wakeup();

                            } else {
                              recycleChannel(couchConnection);
                            }
                          }
                        });
                      }

                      @Override
                      public void onWrite(SelectionKey couchKey) throws Exception {
                        couchConnection.write(UTF8.encode(req));
                        couchKey.selector().wakeup();
                        couchKey.interestOps(OP_READ);
                      }
                    });
              }
            }

          });
        }
      });


    }
  };

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

      if (headerIndex.containsKey(COOKIE)) {
        int[] optionalStartStopMarkers = headerIndex.get(COOKIE);
        id = getCookieAsString(VISITORSTRING, headerBuffer, optionalStartStopMarkers);
      }
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

              SESSION_PROPERTIES_ACCESS.setSessionProperty(InetAddress.class.getCanonicalName(), inet4Address.getCanonicalHostName());
              SESSION_PROPERTIES_ACCESS.setSessionProperty("geoip", geoip);

            } catch (Throwable ignored) {
            }
          }
        }, 250, MILLISECONDS);
      }
    }
    return id;
  }


  public static ByteBuffer rtrimByteBuffer(ByteBuffer b) {
    while (Character.isWhitespace(b.get(b.position() - 1))) b.position(b.position() - 1);
    b.flip();
    return b;
  }

  public static final BlockingDeque<SocketChannel> couchDq = new LinkedBlockingDeque<SocketChannel>(5);static {
    final Runnable task = new Runnable() {
      @Override
      public void run() {
        while (!HttpMethod.killswitch) {

          SocketChannel channel = null;

          System.err.println("opening " + new InetSocketAddress(LOOPBACK, 5984).toString());

          try {
            channel = lazyQueue.poll();
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

  public static <T> String deepToString(T... d) {
    return Arrays.deepToString(d) + wheresWaldo();
  }

  public static void recycleChannel(SocketChannel channel) {
    try {
      lazyQueue.add(channel);
      System.err.println("--- recycling" + wheresWaldo());
    } catch (Exception ignored) {
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

  public static AsioVisitor fetchHeadByPath(final String path, final SocketChannel channel, final SynchronousQueue<String> returnTo) throws ClosedChannelException {
    final String format = (MessageFormat.format("HEAD /{0} HTTP/1.1\r\n\r\n", path.trim()));
    return executeCouchRequest(channel, returnTo, format);
  }

  public static AsioVisitor fetchJsonByPath(final String path, final SocketChannel channel, final SynchronousQueue<String> returnTo) throws ClosedChannelException {
    final String format = (MessageFormat.format("GET /{0} HTTP/1.1\r\n\r\n", path.trim())).replace("//", "/");
    return executeCouchRequest(channel, returnTo, format);
  }

  public static AsioVisitor executeCouchRequest(final SocketChannel channel, final SynchronousQueue<String> returnTo, final String requestHeaders) throws ClosedChannelException {
    final AsioVisitor.Impl impl = new AsioVisitor.Impl() {
      @Override
      public void onWrite(final SelectionKey key) throws IOException {
        System.err.println("attempting connect: " + requestHeaders.trim());
        channel.write(UTF8.encode(requestHeaders));
        key.selector().wakeup();
        key.interestOps(OP_READ).attach(createJsonResponseReader(returnTo));
      }
    };
    HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, impl);
    return impl;
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
      recycleChannel(channel);
    }
    return GSON.fromJson(take, CouchTx.class);
  }

  public static Map fetchMapById(CouchLocator locator, String key) throws IOException, InterruptedException {
    SocketChannel channel = createCouchConnection();
    String take1;
    try {
      SynchronousQueue<String> retVal = new SynchronousQueue<String>();
      HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, fetchJsonByPath(locator.getPathPrefix() + '/' + key, channel, retVal));
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
          final ByteBuffer dst = ByteBuffer.allocateDirect(receiveBufferSize);
          int read = channel.read(dst);
          if (-1 == read) {
            channel.socket().close();
            final String o = GSON.toJson(new CouchTx() {{
              setError("closed socket");
              setReason("closed socket");
            }});
            returnTo.put(o);
            throw new IOException(o);
          }

          dst.flip();

          final String rescode = BlobAntiPatternObject.parseResponseCode(dst);

          BlobAntiPatternObject.moveCaretToDoubleEol(dst);
          final ByteBuffer[] headerBuf = {(ByteBuffer) dst.duplicate().flip()};
          System.err.println("result: " + UTF8.decode((ByteBuffer) headerBuf[0].rewind()));
          int[] bounds = HttpHeaders.getHeaders((ByteBuffer) headerBuf[0].rewind()).get("Content-Length");
          if (null == bounds) {

            bounds = HttpHeaders.getHeaders((ByteBuffer) headerBuf[0].rewind()).get("Transfer-Encoding");

            if (null != bounds) {

              key.attach(new Impl() {
                ByteBuffer cursor = dst.slice();

                private Impl prev = this;
                LinkedList<ByteBuffer> ret = new LinkedList<ByteBuffer>();

                @Override
                public void onRead(SelectionKey key) throws Exception {//chuksizeparser
                  if (cursor == null) {
                    cursor = ByteBuffer.allocate(receiveBufferSize);
                    final int read1 = channel.read(cursor);
                    cursor.flip();
                  }
                  System.err.println("chunking: " + UTF8.decode(cursor.duplicate()));
                  final int anchor = cursor.position();
                  while (cursor.hasRemaining() && cursor.get() != '\n') ;
                  final ByteBuffer line = (ByteBuffer) cursor.duplicate().position(anchor).limit(cursor.position());
                  String res = UTF8.decode(line).toString().trim();
                  long chunkSize = 0;
                  try {

                    chunkSize = Long.parseLong(res, 0x10);


                    if (0 == chunkSize) {
                        //send the unwrap to threadpool.
                      EXECUTOR_SERVICE.submit(new Callable() {
                        public Void call() throws InterruptedException {
                      int sum = 0;
                      for (ByteBuffer byteBuffer : ret) {
                        sum += byteBuffer.limit();
                      }
                      final ByteBuffer allocate = ByteBuffer.allocate(sum);
                      for (ByteBuffer byteBuffer : ret) {
                        allocate.put((ByteBuffer) byteBuffer.flip());
                      }

                      final String o = UTF8.decode((ByteBuffer) allocate.flip()).toString();
                      System.err.println("total chunked bundle was: " + o);
                      returnTo.put(o);
                      return null; } }); key.selector().wakeup(); key.interestOps(OP_READ);
                      key.attach(null);
                      return;
                    }
                  } catch (NumberFormatException e) {


                  }
                  final ByteBuffer dest = ByteBuffer.allocate((int) chunkSize);
                  if (!(chunkSize < cursor.remaining())) {//fragments to assemble

                    dest.put(cursor);
                    key.attach(new Impl() {
                      @Override
                      public void onRead(SelectionKey key) throws Exception {
                        final int read1 = channel.read(dest);
                        key.selector().wakeup();
                        if (!dest.hasRemaining()) {
                          key.attach(prev);
                          cursor = null;
                          ret.add(dest);
                        }
                      }
                    });
                  } else {
                    ByteBuffer src = (ByteBuffer) cursor.slice().limit((int) chunkSize);
                    cursor.position((int) (cursor.position() + chunkSize + 2));
//                      cursor = dest;
                    dest.put(src);
                    ret.add(dest);
                    onRead(key);      // a goto
                  }

                }
              });

            }//doChunked


          } else {
            total = Long.parseLong(UTF8.decode((ByteBuffer) dst.duplicate().limit(bounds[1]).position(bounds[0])).toString().trim());
            remaining = total - dst.remaining();

            ByteBuffer payload;
            if (remaining <= 0) {
              payload = dst.slice();
              BlobAntiPatternObject.returnJsonStringOrErrorResponse(returnTo, key, rescode, payload);
            } else {
              final LinkedList<ByteBuffer> ll = new LinkedList<ByteBuffer>();
              ll.add(dst.slice());
              key.selector().wakeup();
              key.interestOps(SelectionKey.OP_READ).attach(new Impl() {
                @Override
                public void onRead(SelectionKey key) throws InterruptedException, IOException {
                  ByteBuffer payload = ByteBuffer.allocateDirect(receiveBufferSize);
                  int read = channel.read(payload);
                  if (-1 == read) {
                    key.channel().close();
                    return;
                  }
                  ll.add(payload);
                  remaining -= read;
                  if (0 == remaining) {
                    payload = ByteBuffer.allocateDirect((int) total);
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
      returnTo.put(MessageFormat.format("'{'\"responseCode\":\"{0}\",\"orig\":{1}'}'", rescode, trim));
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

  static CouchTx setGenericDocumentProperty(String path, String key, String value) throws Exception {
    String ret;
    SocketChannel channel = null;
    try {
      channel = createCouchConnection();
      SynchronousQueue<String> retVal = new SynchronousQueue<String>();
      HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, fetchJsonByPath(path, channel, retVal));
      ret = retVal.take();
    } finally {
      recycleChannel(channel);
    }
    String take = ret;
    LinkedHashMap linkedHashMap1 = GSON.fromJson(take, LinkedHashMap.class);
    if (2 == linkedHashMap1.size() && linkedHashMap1.containsKey("responseCode")) {
      linkedHashMap1.clear();
      linkedHashMap1.put(key, value);
      return sendJson(GSON.toJson(linkedHashMap1), path);
    }
    {
      linkedHashMap1.put(key, value);
      return sendJson(GSON.toJson(linkedHashMap1), path, inferRevision(linkedHashMap1));
    }
  }

  public static String getGenericDocumentProperty(String path, String key) throws IOException {
    SocketChannel couchConnection = null;
    try {
      final SynchronousQueue<String> returnTo = new SynchronousQueue<String>();
      couchConnection = createCouchConnection();
      fetchJsonByPath(path, couchConnection, returnTo);
      final String take = returnTo.take();
      final Map map = GSON.fromJson(take, Map.class);
      return String.valueOf(map.get(key));
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      recycleChannel(couchConnection);
    }
    return path;
  }

  public static EnumMap<HttpMethod, LinkedHashMap<Pattern, AsioVisitor>> getNamespace() {
    return NAMESPACE;
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


      public Object call() throws IOException, InterruptedException {
        String id;
        {
          CouchLocator<Visitor> roSessionLocator = Visitor.createLocator();
          Visitor roSession = roSessionLocator.create(Visitor.class);
          id = roSession.getId();
          String s = GSON.toJson(roSession);
          System.err.println("created: " + s);
          {
            roSessionLocator = Visitor.createLocator();
            try {
              final CouchTx persist = roSessionLocator.persist(roSession);

              id = persist.getId();
              s = GSON.toJson(persist);
              System.err.println("persisted: " + s);
            } catch (Exception ignored) {

            }
          }
        }

        {
          CouchLocator<Visitor> roSessionLocator = Visitor.createLocator();

          Visitor roSession = roSessionLocator.find(Visitor.class, id);
          String s = GSON.toJson(roSession);
          System.err.println("find: " + s);
        }

        {
          final SynchronousQueue<String> returnTo = new SynchronousQueue<String>();
          final SocketChannel couchConnection = createCouchConnection();
          final AsioVisitor asioVisitor = fetchHeadByPath("/geoip/current", couchConnection, returnTo);
          System.err.println("head: " + returnTo.take());
          recycleChannel(couchConnection);
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
    final SessionCouchAgent<Visitor, String, Class<Visitor>> ro = new SessionCouchAgent<Visitor, String, Class<Visitor>>(VISITOR_LOCATOR);
    HttpMethod.enqueue(createCouchConnection(), OP_CONNECT | OP_WRITE, ro, ro.getFeedString());
    HttpMethod.init(args, topLevel);
  }
}
