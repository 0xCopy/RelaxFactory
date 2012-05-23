package rxf.server;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
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
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Exchanger;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
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
import static one.xio.HttpMethod.$DBG;
import static one.xio.HttpMethod.GET;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.enqueue;
import static one.xio.HttpMethod.getSelector;
import static one.xio.HttpMethod.wheresWaldo;

/**
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
public class BlobAntiPatternObject {
  public static final rxf.server.CouchLocator<rxf.server.Visitor> VISITOR_LOCATOR = rxf.server.Visitor.createLocator();
  public static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 3);
  private static Queue<SocketChannel> lazyQueue = new ConcurrentLinkedQueue<SocketChannel>();
  public static final ThreadLocal<Rfc822HeaderState> ThreadLocalHeaders = new ThreadLocal<Rfc822HeaderState>();
  //  public static ThreadLocal<InetAddress> ThreadLocalInetAddress = new ThreadLocal<InetAddress>();
//  public static final ThreadLocal<Map<String, String>> ThreadLocalSetCookies = new ThreadLocal<Map<String, String>>();
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

  public static final String CONTENT_LENGTH = "Content-Length";
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
              Object[] attachment = (Object[]) browserKey.attachment();
              if (!(attachment instanceof Object[])) {
                throw new UnsupportedOperationException("this GET proxy requires attach(this,path,headers,block0) to function correctly"); //todo: tailcall and inner classes
              }
              Object[] objects = attachment;
              path = (String) objects[1];
              headers = (ByteBuffer) objects[2];
              dst = (ByteBuffer) objects[3];
              Matcher matcher = passthroughExpr.matcher(path);
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
                        SocketChannel channel = (SocketChannel) couchKey.channel();
                        channel.read((ByteBuffer) dst.clear());
                        moveCaretToDoubleEol((ByteBuffer) dst.flip());
                        ByteBuffer headers = ((ByteBuffer) dst.duplicate().flip()).slice();

                        Map<String, int[]> map = HttpHeaders.getHeaders((ByteBuffer) headers.rewind());
                        int[] ints = map.get(CONTENT_LENGTH);
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
                                int write = browserChannel.write(dst);
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
                          public void onRead(SelectionKey couchKey) throws Exception {

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
  public static final Charset UTF8CHARSET = Charset.forName(UTF8.name());
  public static final VisitorPropertiesAccess VISITOR_PROPERTIES_ACCESS = new VisitorPropertiesAccess();
  public static final boolean DEBUG_COUCH_POOL = System.getenv().containsKey("DEBUG_COUCH_POOL");
  public static final String TRANSFER_ENCODING = "Transfer-Encoding";

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

  static public String getSessionCookieId() throws ExecutionException, InterruptedException {
    Rfc822HeaderState rfc822HeaderPrefix = ThreadLocalHeaders.get();

    final AtomicReference<String> id = new AtomicReference<String>();


    String newValue = rfc822HeaderPrefix.getCookieStrings().get(VISITORSTRING);
    id.set(newValue);

    if (null == id.get()) {
      id.set(System.nanoTime() + "." + UUID.randomUUID().toString() + "." + rfc822HeaderPrefix.getSourceRoute().getCanonicalHostName());
      EXECUTOR_SERVICE.submit(new Runnable() {
        @Override
        public void run() {
          Visitor visitor = new Visitor();
          visitor.setCreation(new Date());
          visitor.setId(id.get());
          String json = GSON.toJson(visitor);
          try {
            CouchTx couchTx = sendJson(json, "rxf_visitor", id.get());
            assert couchTx.getOk();
          } catch (Exception e) {
            e.printStackTrace();  //todo: verify for a purpose
          }
        }


      });
      rfc822HeaderPrefix.setDirty(true);
      Map<String, String> cookieStrings = rfc822HeaderPrefix.getCookieStrings();
      cookieStrings.clear();
      Date expire = new Date(TimeUnit.DAYS.toMillis(14) + System.currentTimeMillis());
      String cookietext = MessageFormat.format("{0} ; path=/ ; expires={1} ; HttpOnly", id.get(), expire.toGMTString());
      cookieStrings.put(VISITORSTRING, cookietext);
      try {
        InetAddress sourceRoute = rfc822HeaderPrefix.getSourceRoute();
        if (null != sourceRoute) {
          CharBuffer charBuffer = GeoIpService.mapAddressLookup(sourceRoute);
          String geoip = MessageFormat.format("{0} ; path=/ ; expires={1}", charBuffer, expire.toGMTString());
          if (null != charBuffer)
            cookieStrings.put(MYGEOIPSTRING, geoip);
        }
      } catch (Exception e) {
        e.printStackTrace();  //todo: verify for a purpose
      }
    }
    return id.get();
  }

  public static ByteBuffer rtrimByteBuffer(ByteBuffer b) {
    while (Character.isWhitespace(b.get(b.position() - 1))) b.position(b.position() - 1);
    b.flip();
    return b;
  }

  public static final BlockingDeque<SocketChannel> couchDq = new LinkedBlockingDeque<SocketChannel>(5);static {
    Runnable task = new Runnable() {
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
        e.printStackTrace();
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
    if ($DBG) {
      Rfc822HeaderState rfc822HeaderState = RfPostWrapper.ORIGINS.get(channel.keyFor(getSelector()));
      if (null != rfc822HeaderState) {
        throw new Error("accidental recycle !!!!!!!! " + rfc822HeaderState.getPathRescode() + " !!! " + wheresWaldo(5));
      }
    }
    try {
      if (DEBUG_COUCH_POOL) {
        channel.socket().close();
      } else {

        System.err.println("--- recycling" + wheresWaldo());

        lazyQueue.add(channel);
      }
    } catch (Throwable ignored) {
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

  public static String getPathIdVer(String... pathIdVer) {
    if ($DBG) {


      System.err.println("pathIdVer building for " + arrToString(pathIdVer) + wheresWaldo(4));
    }
    ;
    int c = 0;
    StringBuilder r = new StringBuilder();
    for (String s : pathIdVer) {
      switch (c) {
        case 2:
          if (null != s && !"null".equals(s)) {
            r.append("?rev=").append(s);
          }
          break;
        case 0:
          r.append('/').append(s).append(s.matches(".*[\\?|#]") ? "" : "/");
          break;
        default:
          r.append('/').append(s);
          break;

      }
      c++;
    }
    return r.toString().trim().replace("//", "/");
  }

  public static AsioVisitor fetchHeadByPath(SocketChannel channel, Exchanger<String> returnTo, String... pathIdVer) throws ClosedChannelException {
    String format = new StringBuilder().append("HEAD ").append(getPathIdVer(pathIdVer)).append(" HTTP/1.1\r\n\r\n").toString();
    return executeCouchRequest(channel, returnTo, format);
  }

  public static AsioVisitor fetchJsonByPath(SocketChannel channel, Exchanger returnTo, String... pathIdVer) throws ClosedChannelException {
    String format = "GET " + getPathIdVer(pathIdVer) + " HTTP/1.1\r\nAccept: */*\r\n\r\n".replace("//", "/");
    return executeCouchRequest(channel, returnTo, format);
  }

  public static AsioVisitor executeCouchRequest(final SocketChannel channel, final Exchanger<String> returnTo, final String requestHeaders) throws ClosedChannelException {
    AsioVisitor.Impl impl = new AsioVisitor.Impl() {
      @Override
      public void onWrite(SelectionKey key) throws IOException {
        System.err.println("recv: " + requestHeaders.trim());
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
  public static CouchTx sendJson(final String json, final String... pathIdVer) throws Exception {
    Callable<?> callable = new Callable<Object>() {
      public CouchTx call() throws Exception {
        String take;
        SocketChannel channel = null;
        try {
          channel = createCouchConnection();
          Exchanger<? extends String> retVal = new Exchanger<String>();
          HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, new SendJsonVisitor(json, retVal, pathIdVer));
          take = (String) retVal.exchange(null, 3, TimeUnit.SECONDS);/*retVal.poll(250, MILLISECONDS);*/
        } finally {
          recycleChannel(channel);
        }
        return GSON.fromJson(take, CouchTx.class);

      }
    };
    return (CouchTx) EXECUTOR_SERVICE.submit(callable).get();
  }

  public static Map fetchMapById(CouchLocator locator, String key) throws ExecutionException, InterruptedException {
    return EXECUTOR_SERVICE.submit(new Callable<Map>() {
      public Map call() throws IOException {
        SocketChannel channel = createCouchConnection();
        String take1 = null;
        try {
          Exchanger retVal = new Exchanger();
          HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, fetchJsonByPath(channel, retVal));
          take1 = (String) retVal.exchange(null, 3, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
          e.printStackTrace();  //todo: verify for a purpose
        } catch (InterruptedException e) {
          e.printStackTrace();  //todo: verify for a purpose
        } catch (ClosedChannelException e) {
          e.printStackTrace();  //todo: verify for a purpose
        } finally {
          recycleChannel(channel);
        }
        String take = take1;
        Map<? extends Object, ? extends Object> linkedHashMap = GSON.fromJson(take, LinkedHashMap.class);
        if (2 == linkedHashMap.size() && linkedHashMap.containsKey("responseCode")) {
          throw new IOException(deepToString(linkedHashMap));
        }
        return linkedHashMap;

      }
    }).get();
  }

  public static AsioVisitor createJsonResponseReader(final Exchanger<String> returnTo) {
    return new AsioVisitor.Impl() {
      public long total;
      public long remaining;

      @Override
      public void onRead(final SelectionKey key) throws IOException, InterruptedException {
        EXECUTOR_SERVICE.submit(new Callable<Object>() {
          public Object call() throws Exception {
            final SocketChannel channel = (SocketChannel) key.channel();
            final int receiveBufferSize = BlobAntiPatternObject.getReceiveBufferSize();
            ByteBuffer dst = ByteBuffer.allocateDirect(receiveBufferSize);
            int read = channel.read(dst);
            if (-1 == read) {
              key.cancel();
              returnTo.exchange("{\"error\":\"connection closed\" ,\"reason\":\"buggered\"}");
              return null;
            }

            final Rfc822HeaderState rfc822HeaderState = new Rfc822HeaderState(CONTENT_LENGTH, TRANSFER_ENCODING, "Etag", "Host");
            rfc822HeaderState.apply((ByteBuffer) dst.flip());

            BlobAntiPatternObject.moveCaretToDoubleEol(dst);
            ByteBuffer[] headerBuf = {((ByteBuffer) dst.duplicate().flip()).slice()};
            if (SendJsonVisitor.DEBUG_SENDJSON) {
              System.err.println("result: " + UTF8.decode((ByteBuffer) headerBuf[0].rewind()));
            }

            int[] bounds = HttpHeaders.getHeaders((ByteBuffer) headerBuf[0].rewind()).get(CONTENT_LENGTH);
            if (null == bounds) {

              bounds = HttpHeaders.getHeaders((ByteBuffer) headerBuf[0].rewind()).get(TRANSFER_ENCODING);

              if (null != bounds) {
                key.selector().wakeup();
                key.interestOps(OP_READ).attach(new ChunkedEncodingVisitor(dst, receiveBufferSize, channel, returnTo));

              }//doChunked
            } else {
              total = Long.parseLong(UTF8.decode((ByteBuffer) dst.duplicate().limit(bounds[1]).position(bounds[0])).toString().trim());
              remaining = total - dst.remaining();

              ByteBuffer payload;
              if (remaining <= 0) {
                payload = dst.slice();
                String rescode = rfc822HeaderState.getPathRescode();
                BlobAntiPatternObject.returnJsonString(returnTo, key, rescode, payload);
              } else {
                final LinkedList<ByteBuffer> ll = new LinkedList<ByteBuffer>();
                ll.add(dst.slice());
                key.selector().wakeup();
                key.interestOps(SelectionKey.OP_READ).attach(new Impl() {
                  @Override
                  public void onRead(SelectionKey key) throws InterruptedException, IOException, ExecutionException {
                    ByteBuffer payload = ByteBuffer.allocateDirect(receiveBufferSize);
                    int read = channel.read(payload);
                    if (-1 == read) {
                      key.channel().close();
                      if ($DBG && RfPostWrapper.ORIGINS.containsKey(key)) {
                        Rfc822HeaderState rfc822HeaderState = RfPostWrapper.ORIGINS.get(key);
                        System.err.println("closing " + arrToString(rfc822HeaderState) + wheresWaldo());
                      }
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
                          payload.put(buffer);
                      }
                      BlobAntiPatternObject.returnJsonString(returnTo, key, rfc822HeaderState.getPathRescode(), payload);
                    }
                  }
                });
              }
            }
            return null;
          }
        });

      }
    };
  }

  static String returnJsonString(final Exchanger<String> returnTo, final SelectionKey key, final String rescode, final ByteBuffer payload) throws InterruptedException, ExecutionException {
    Callable<Object> callable = new Callable<Object>() {
      public Object call() throws Exception {
        key.attach(null);
        String decode = UTF8.decode((ByteBuffer) payload.duplicate().rewind()).toString().trim();
        System.err.println("payload: " + decode);
        if (!payload.hasRemaining())
          payload.rewind();

        returnTo.exchange(UTF8.decode(payload).toString().trim());

        return rescode;
      }

    };
    return
        (String) EXECUTOR_SERVICE.submit(callable).get();
  }


  static CouchTx setGenericDocumentProperty(final String key, final String value, final String... pathIdVer) throws Exception {
    Callable<Object> callable = new Callable<Object>() {
      public Object call() throws Exception {
        String ret;
        SocketChannel channel = null;
        try {
          channel = createCouchConnection();
          Exchanger retVal = new Exchanger();
          HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, fetchJsonByPath(channel, retVal, pathIdVer));
          ret = (String) retVal.exchange(null, 3, TimeUnit.SECONDS);
        } finally {
          recycleChannel(channel);
        }
        String take = ret;
        LinkedHashMap linkedHashMap1 = GSON.fromJson(take, LinkedHashMap.class);
        if (2 != linkedHashMap1.size() || !linkedHashMap1.containsKey("responseCode")) {//success
          linkedHashMap1.put(key, value);
          List<String> strings = Arrays.asList(pathIdVer);
          strings.add(inferRevision(linkedHashMap1));
          return sendJson(GSON.toJson(linkedHashMap1), strings.toArray(new String[strings.size()]));
        } else {//failure
          linkedHashMap1.clear();
          linkedHashMap1.put(key, value);
          return sendJson(GSON.toJson(linkedHashMap1), pathIdVer);
        }

      }
    };
    return (CouchTx) EXECUTOR_SERVICE.submit(callable).get();
  }

  public static String getGenericDocumentProperty(final String path, final String key) throws IOException, ExecutionException, InterruptedException {
    Callable<String> callable = new Callable<String>() {
      public String call() throws Exception {
        SocketChannel couchConnection = null;
        try {
          Exchanger returnTo = new Exchanger();
          couchConnection = createCouchConnection();
          fetchJsonByPath(couchConnection, returnTo);
          String take = (String) returnTo.exchange(null, 3, TimeUnit.SECONDS);
          Map map = GSON.fromJson(take, Map.class);
          return String.valueOf(map.get(key));
        } catch (Exception e) {
          e.printStackTrace();
        } finally {
          recycleChannel(couchConnection);
        }
        return path;

      }
    };
    return (String) EXECUTOR_SERVICE.submit(callable).get();
  }

  public static EnumMap<HttpMethod, LinkedHashMap<Pattern, AsioVisitor>> getNamespace() {
    return NAMESPACE;
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
              CouchTx persist = roSessionLocator.persist(roSession);

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
          Exchanger<String> returnTo = new Exchanger<String>();
          SocketChannel couchConnection = createCouchConnection();
          AsioVisitor asioVisitor = fetchHeadByPath(couchConnection, returnTo, "/geoip/current");
          //System.err.println("head: " + returnTo.take());
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
    SessionCouchAgent<Visitor> ro = new SessionCouchAgent<Visitor>(VISITOR_LOCATOR);
    HttpMethod.enqueue(createCouchConnection(), OP_CONNECT | OP_WRITE, ro, ro.getFeedString());
    HttpMethod.init(args, topLevel, 1000);
  }

  public static String fetchJsonByPathV1(final String path, final Rfc822HeaderState... youCanHaz) throws IOException, InterruptedException, TimeoutException {

    String ret = null;
    try {
      final Exchanger<String> exchanger = new Exchanger<String>();
      Callable<Object> callable = new Callable<Object>() {
        public Object call() throws Exception {
          SocketChannel channel = null;
          if ($DBG) {
            System.err.println(deepToString(path, youCanHaz));
          }
          enqueue(createCouchConnection(), OP_CONNECT | OP_WRITE, new AsioVisitor.Impl() {
            @Override
            public void onWrite(SelectionKey key) throws Exception {
              final SocketChannel channel = (SocketChannel) key.channel();

              //todo: youCanHaz headers here

              ByteBuffer wrap = ByteBuffer.wrap(("GET " + path + " HTTP/1.1\r\nAccept: */*\r\n\r\n").getBytes(UTF8));
              int write = channel.write(wrap);
              key.interestOps(OP_READ).attach(new Impl() {
                @Override
                public void onRead(SelectionKey key) throws Exception {

                  Rfc822HeaderState state = null;
                  for (Rfc822HeaderState rfc822HeaderState : youCanHaz) {
                    state = rfc822HeaderState;
                    break;
                  }
                  boolean sendItBack = null != state;
                  if (!sendItBack)
                    state = new Rfc822HeaderState("Content-Length", "Encoding-Type"); //minimum for GET if sent in
                  //todo: youCanHaz headers here

                  ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());


                  int read = channel.read(dst);
                  if (read > 0) {
                    channel.close();

                    exchanger.exchange(null);
                    return;
                  }
                  assert state != null;
                  state.apply((ByteBuffer) dst.flip());
                  if (state.getPathRescode().startsWith("20")) {
//              final String o = state.getHeaderStrings().;
                    if (state.getHeaderStrings().containsKey(TRANSFER_ENCODING)) {
                      key.attach(new ChunkedEncodingVisitor(dst, 0, channel, exchanger));

                    } else {
                      String cl = state.getHeaderStrings().get(CONTENT_LENGTH);
                      long l = Long.parseLong(cl);
                      final ByteBuffer cursor = ByteBuffer.allocateDirect((int) l).put(dst);

                      if (cursor.hasRemaining()) {
                        key.interestOps(OP_READ).attach(new Impl() {
                          @Override
                          public void onRead(SelectionKey key) throws Exception {
                            channel.read(cursor);
                            if (!cursor.hasRemaining()) {
                              exchanger.exchange(UTF8.decode((ByteBuffer) cursor.flip()).toString().trim());
                            }
                          }
                        });
                      } else {
                        String trim = UTF8.decode((ByteBuffer) cursor.flip()).toString().trim();
                        exchanger.exchange(trim);
                      }
                    }
                  }
                }
              });
            }
          });
          String ret = null;

          return null;
        }
      };
      EXECUTOR_SERVICE.submit(callable);
      ret = (String) exchanger.exchange(null, 3, TimeUnit.SECONDS);
    } catch (Exception ignored) {
    }
    return ret;
  }


  private static class ChunkedEncodingVisitor extends AsioVisitor.Impl {

    private Impl prev;
    LinkedList<ByteBuffer> ret;
    private ByteBuffer cursor;
    private final int receiveBufferSize;
    private final SocketChannel channel;
    private final Exchanger<String> returnTo;

    public ChunkedEncodingVisitor(ByteBuffer cursor, int receiveBufferSize, SocketChannel channel, Exchanger<String> returnTo) {
      this.cursor = cursor;
      this.receiveBufferSize = receiveBufferSize;
      this.channel = channel;
      this.returnTo = returnTo;
      this.cursor = cursor.slice();
      prev = this;
      ret = new LinkedList<ByteBuffer>();
    }

    @Override
    public void onRead(SelectionKey key) throws IOException {//chunksizeparser
      if (cursor == null) {
        cursor = ByteBuffer.allocate(receiveBufferSize);
        int read1 = channel.read(cursor);
        cursor.flip();
      }
      System.err.println("chunking: " + UTF8.decode(cursor.duplicate()));
      int anchor = cursor.position();
      while (cursor.hasRemaining() && cursor.get() != '\n') ;
      ByteBuffer line = (ByteBuffer) cursor.duplicate().position(anchor).limit(cursor.position());
      String res = UTF8.decode(line).toString().trim();
      long chunkSize = 0;
      try {

        chunkSize = Long.parseLong(res, 0x10);


        if (0 == chunkSize) {
          //send the unwrap to threadpool.
          EXECUTOR_SERVICE.submit(new Callable() {
            public Object call() throws InterruptedException {
              int sum = 0;
              for (ByteBuffer byteBuffer : ret) {
                sum += byteBuffer.limit();
              }
              ByteBuffer allocate = ByteBuffer.allocate(sum);
              for (ByteBuffer byteBuffer : ret) {
                allocate.put((ByteBuffer) byteBuffer.flip());
              }

              String o = UTF8.decode((ByteBuffer) allocate.flip()).toString();
              System.err.println("total chunked bundle was: " + o);
              returnTo.exchange(o);
              return null;
            }
          });
          key.selector().wakeup();
          key.interestOps(OP_READ);
          key.attach(null);
          return;
        }
      } catch (NumberFormatException ignored) {


      }
      final ByteBuffer dest = ByteBuffer.allocate((int) chunkSize);
      if (!(chunkSize < cursor.remaining())) {//fragments to assemble

        dest.put(cursor);
        key.attach(new Impl() {
          @Override
          public void onRead(SelectionKey key) throws Exception {
            int read1 = channel.read(dest);
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
  }
}
