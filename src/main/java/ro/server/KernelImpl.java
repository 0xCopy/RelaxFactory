package ro.server;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.tidy.Tidy;
import org.xml.sax.SAXException;
import ro.model.RoSession;

import static java.lang.Math.abs;
import static one.xio.HttpMethod.UTF8;

/**
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
public class KernelImpl {
  public static final RoSessionLocator RO_SESSION_LOCATOR = new RoSessionLocator();

  public static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();
  public static final ThreadLocal<ByteBuffer> ThreadLocalHeaders = new ThreadLocal<ByteBuffer>();
  public static final ThreadLocal<Map<String, String>> ThreadLocalSetCookies = new ThreadLocal<Map<String, String>>();
  private static final String MYSESSIONSTRING = KernelImpl.class.getCanonicalName();

  private static int geoIpHeaderOffset;
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


  public static final String MAXMIND_URL = "http://www.maxmind.com/app/geolitecity";
  public static final String DOWNLOAD_LINK = "//ul[@class=\"lstSquare\"][2]/li[2]/a[2]";
  public static final String GEOIP_ROOTNODE = "/rogeoip/current";
  public static final long IPMASK = 0xffffffffl;


  static public RoSession getCurrentSession() {
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
      ThreadLocalSetCookies.get().put(MYSESSIONSTRING, MessageFormat.format("{0}; path=/ ; expires=\"{1}\" ; HttpOnly", roSession.getId(), s));
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


    scrapeGeoIpDbArchiveUrlFromMaxMind("rogeoip");


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
   * download both files.  index the smaller one over top of the larger one.
   *
   * @param dbinstance
   * @throws IOException
   * @throws XPathExpressionException
   */
  static void scrapeGeoIpDbArchiveUrlFromMaxMind(final String dbinstance) throws IOException, XPathExpressionException, InterruptedException {
    SocketChannel connection = createCouchConnection();
    final SynchronousQueue<String> synchronousQueue = new SynchronousQueue<String>();

    HttpMethod.enqueue(connection, SelectionKey.OP_CONNECT, new AsioVisitor.Impl() {
      public void onRead(final SelectionKey selectionKey) throws IOException, InterruptedException {
        final AsioVisitor parent = this;
        final SocketChannel channel = (SocketChannel) selectionKey.channel();
        final ByteBuffer dst = ByteBuffer.allocateDirect(channel.socket().getReceiveBufferSize());
        int read = channel.read(dst);
        dst.flip();
        System.err.println("response: " + UTF8.decode((ByteBuffer) dst.duplicate().rewind()));
        while (!Character.isWhitespace(dst.get())) ;
        ByteBuffer d2 = dst.duplicate();
        while (!Character.isWhitespace(dst.get())) ;
        d2.limit(dst.position());
        String s1 = UTF8.decode(d2).toString().trim();
        int resultCode = Integer.parseInt(s1);

        switch (resultCode) {
          case 200:
          case 201: {

            final String keyDocument = GEOIP_ROOTNODE;
            selectionKey.attach(new Impl() {

              @Override
              public void onConnect(SelectionKey key) {
                try {
                  if (((SocketChannel) key.channel()).finishConnect()) {
                    key.interestOps(SelectionKey.OP_WRITE);
                  }
                } catch (IOException e) {
                  e.printStackTrace();  //todo: verify for a purpose
                }
              }

              @Override
              public void onWrite(final SelectionKey selectionKey) {

                try {
                  String format = (MessageFormat.format("GET /{0} HTTP/1.1\r\n\r\n", keyDocument));
                  System.err.println("attempting connect: " + format.trim());
                  channel.write(UTF8.encode(format));
                } catch (IOException e) {
                  e.printStackTrace();  //todo: verify for a purpose
                }
                selectionKey.attach(new JsonResponseReader(synchronousQueue));
                selectionKey.interestOps(SelectionKey.OP_READ);
              }
            });
            selectionKey.interestOps(SelectionKey.OP_WRITE);

            Callable<Object> callable = new Callable<Object>() {
              public Object call() throws Exception {

                String take = synchronousQueue.take();
                selectionKey.attach(this);
                System.err.println("rootnode: " + take);
                Map<? extends Object, ? extends Object> map = GSON.fromJson(take, Map.class);
                if (map.containsKey("responseCode")) {
                  bootstrapGeoIp();
                } else {
                  return null;
                }
                return null;
              }
            };

            EXECUTOR_SERVICE.submit(callable);
          }
          break;
          default:
            selectionKey.interestOps(SelectionKey.OP_WRITE);
            selectionKey.attach(new Impl() {
              @Override
              public void onWrite(SelectionKey selectionKey) throws IOException {

                String format = MessageFormat.format("PUT /{0} HTTP/1.1\r\nContent-Length: 0\r\nContent-type: application/json\r\n\r\n", dbinstance);
                int write = ((SocketChannel) selectionKey.channel()).write(UTF8.encode(format));
                selectionKey.interestOps(SelectionKey.OP_READ);
                selectionKey.attach(parent);
              }
            });

            break;
        }

      }

      @Override
      public void onWrite(SelectionKey selectionKey) throws IOException {
        String s = "GET /" + dbinstance + " HTTP/1.1\r\nConnection:keep-alive\r\n\r\n";
        ByteBuffer encode = UTF8.encode(s);
        int write = ((SocketChannel) selectionKey.channel()).write(encode);

        System.err.println("wrote " + write + " bytes for " + s);
        selectionKey.interestOps(SelectionKey.OP_READ);

      }

      @Override
      public void onConnect(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        if (channel.finishConnect()) {
          selectionKey.interestOps(SelectionKey.OP_WRITE);
        }
      }
    });

  }

  private static void bootstrapGeoIp() throws IOException, XPathExpressionException {
    String href = scrapeMaxMindUrl();
//    System.err.println(href);
//    long l = System.currentTimeMillis();
//
    ByteArrayOutputStream archiveBuffer = downloadMaxMindBinaryTarXz(href);
    Triple<Integer[], ByteBuffer, ByteBuffer> locations = buildGeoIpFirstPass(archiveBuffer);
    buildGeoIpSecondPass(locations);

  }

  static enum geoIpRecord {
    ipBlock(4), offset(4);

    int len;
    int pos;
    static int reclen;

    geoIpRecord(int len) {
      this.len = len;
      init(len);
    }

    private void init(int len) {
      pos = reclen;
      reclen += len;
    }

    static int count(ByteBuffer b) {
      return b.limit() / reclen;
    }

    static class Geo {
      Geo(int recordNum, ByteBuffer indexBuf) {
        this.recordNum = recordNum;
        this.indexBuf = indexBuf;
      }

      int recordNum;
      ByteBuffer indexBuf;


      public Long getBlock() {
        indexBuf.position(reclen * recordNum);
        return IPMASK & indexBuf.getInt();
      }


      public String getCsv(ByteBuffer csvBuf) {

        indexBuf.position(reclen * recordNum + 4);
        int anInt = indexBuf.getInt();
        ByteBuffer buffer = (ByteBuffer) csvBuf.duplicate().clear().position(anInt);
        while (buffer.hasRemaining() && '\n' != buffer.get()) ;
        return UTF8.decode((ByteBuffer) buffer.limit(buffer.position()).position(anInt)).toString().trim();

      }


    }

    static String lookup(Inet4Address byAddress, final ByteBuffer indexBuf, ByteBuffer csvBuf) {
      AbstractList<Long> abstractList = new AbstractList<Long>() {
        @Override
        public Long get(int index) {

            int anInt = indexBuf.getInt(index * reclen);
            long l = IPMASK & anInt;
            return l;

        }

        @Override
        public int size() {
          int i = indexBuf.limit() / reclen;
          return i;  //todo: verify for a purpose
        }
      };
      long l = IPMASK &
          ByteBuffer.wrap(byAddress.getAddress()).getInt();


      int abs = abs(Collections.binarySearch(abstractList, l));
      String csv = new Geo(abs, indexBuf).getCsv(csvBuf);
      return csv;
    }


  }

  private static Pair<ByteBuffer, ByteBuffer> buildGeoIpSecondPass(Triple<Integer[], ByteBuffer, ByteBuffer> triple) throws UnknownHostException {
    ByteBuffer indexBuf = null;
    ByteBuffer locBuf = null;
    ArrayList<Long> l1 = null;
    ArrayList<Integer> l2 = null;
    long l = System.currentTimeMillis();
    try {
      Integer[] index = triple.getA();
      indexBuf = triple.getB();
      locBuf = triple.getC();
      l1 = new ArrayList<Long>();
      l2 = new ArrayList<Integer>();

      while (indexBuf.hasRemaining()) {
        l1.add(IPMASK & indexBuf.getInt());
        indexBuf.mark();
        int anInt = indexBuf.getInt();
        indexBuf.reset();
        Integer value = index[anInt-1];
        indexBuf.putInt(value);
        l2.add(value);
      }
      indexBuf.rewind();
    } catch (Throwable e) {
      e.printStackTrace();
    } finally {
      System.err.println("pass2 arrays (ms):" + (System.currentTimeMillis() - l));
    }

    assert l1 != null;
    System.err.println("read back: " + l1.size());


    String s2 = "127.0.0.1";

    InetAddress loopBackAddr = Inet4Address.getByAddress(new byte[]{127, 0, 0, 1});
    Inet4Address martinez = (Inet4Address) Inet4Address.getByAddress(new byte[]{67, (byte) 174, (byte) 244, 99});
    ByteBuffer indexBuf2 = (ByteBuffer) indexBuf.rewind();
    System.err.println(mapLookup(l2, l1.toArray(new Long[l1.size()]), loopBackAddr, ((ByteBuffer) locBuf.duplicate())));
    System.err.println(mapLookup(l2, l1.toArray(new Long[l1.size()]), martinez, ((ByteBuffer) locBuf.duplicate())));


    InetAddress byAddress = loopBackAddr;
    String lookup = geoIpRecord.lookup((Inet4Address) byAddress, indexBuf2, locBuf);
    System.err.println("localhost: " + lookup);


    lookup = geoIpRecord.lookup(martinez, indexBuf2, locBuf);
    System.err.println("martinez: " + lookup);

    return new Pair<ByteBuffer, ByteBuffer>(locBuf, indexBuf2);
  }

  private static String mapLookup(ArrayList<Integer> l2, Long[] longs, InetAddress byAddress, ByteBuffer csvData) {

    byte[] address = byAddress.getAddress();
    ByteBuffer ipadd = ByteBuffer.wrap(address);

    long z = IPMASK & ipadd.slice().getInt();

    int i = Arrays.binarySearch(longs, z);

    int abs = abs(i);
    Integer integer = l2.get(abs);

    ByteBuffer bb = (ByteBuffer) csvData.duplicate().clear().position(integer);
    while (bb.hasRemaining() && bb.get() != '\n') ;
    return UTF8.decode((ByteBuffer) bb.flip().position(integer)).toString();

  }

  private static Triple<Integer[], ByteBuffer, ByteBuffer> buildGeoIpFirstPass(ByteArrayOutputStream archiveBuffer) throws IOException {
    TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new ByteArrayInputStream(archiveBuffer.toByteArray()));
    ArchiveEntry nextEntry;
    Integer[] locations = new Integer[0];
    ByteBuffer indexBuf = null;
    ByteBuffer locBuf = null;
    do {
      nextEntry = tarArchiveInputStream.getNextTarEntry();
      if (null == nextEntry)
        break;
      String name = nextEntry.getName();
      long total = nextEntry.getSize();
      long size = total;
      System.err.println(MessageFormat.format("name: {0}\nsize: {1}", name, size));
      if (!nextEntry.isDirectory()) {
        int remaining = (int) total;
        byte[] content = new byte[remaining];

        while (0 != remaining) {
          int read = tarArchiveInputStream.read(content);
          remaining -= read;
        }
        if (nextEntry.getName().endsWith("GeoLiteCity-Location.csv")) {
          locBuf = ByteBuffer.wrap(content);
          long l1 = System.currentTimeMillis();
          ArrayList<Integer> a = new ArrayList<Integer>();
          while (locBuf.get() != '\n') ;//copyright
          while (locBuf.get() != ',') ; //start with country
          geoIpHeaderOffset = locBuf.position();
          while (locBuf.get() != '\n') ;//headers

          while (locBuf.hasRemaining()) {
            while (locBuf.hasRemaining() && ',' != locBuf.get()) ;
            int position = locBuf.position();
            a.add(position);
            while (locBuf.hasRemaining() && locBuf.get() != '\n') ;
          }
          locations = a.toArray(new Integer[a.size()]);
          System.err.println("loc index time: " + (System.currentTimeMillis() - l1) + " (ms) lc: " + a.size());
        } else if (nextEntry.getName().endsWith("GeoLiteCity-Blocks.csv")) {
          ByteBuffer blockBuf = ByteBuffer.wrap(content);
          ByteBuffer tBuf = blockBuf.duplicate();
          indexBuf = (ByteBuffer) blockBuf.duplicate().clear();

          //http://geolite.maxmind.com/download/geoip/database/GeoLiteCity_CSV/GeoLiteCity_20120403.tar.xz
          //decompressionTime: 6,394
          //name: GeoLiteCity_20120403/GeoLiteCity-Blocks.csv
          //size: 62,806,636
          //tc: 5728 (ms) lc: 3749610


          //without string.split() etc
          //  tc: 1016 (ms) lc: 1874805

          while (blockBuf.get() != '\n') ;//copyright
          while (blockBuf.get() != '\n') ;//headers
          long l1 = System.currentTimeMillis();
          while (blockBuf.hasRemaining()) {
            tBuf.clear().position(blockBuf.position() + 1);
            while (blockBuf.get() != ',') ;//f1
            tBuf.limit(blockBuf.position() - 2);
            long value = Long.parseLong(UTF8.decode(tBuf).toString());
            while (blockBuf.get() != ',') ;//f2


            tBuf.clear().position(blockBuf.position() + 1);
            while (blockBuf.get() != '\n') ;
            tBuf.limit(blockBuf.position() - 2);
            int value1 = Integer.parseInt(UTF8.decode(tBuf).toString());
            indexBuf.putInt((int) (value & IPMASK));
            indexBuf.putInt(value1);

          }
          blockCount = indexBuf.flip().limit() / 8;
          System.err.println("blockindex time: " + (System.currentTimeMillis() - l1) + " (ms) lc: " + geoIpRecord.count(indexBuf) + "@ writeBufSize: " + indexBuf.limit());
        }
      }
    } while (true);
    return new Triple<Integer[], ByteBuffer, ByteBuffer>(locations, indexBuf, locBuf);
  }

  /**
   * Simple pair class.
   *
   * @param <A> any type
   * @param <B> any type
   */
  static class Pair<A, B> {
    private final A a;
    private final B b;

    public Pair(A a, B b) {
      this.a = a;
      this.b = b;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof Pair<?, ?>)) {
        return false;
      }
      Pair<?, ?> other = (Pair<?, ?>) o;
      return a.equals(other.a) && b.equals(other.b);
    }

    public A getA() {
      return a;
    }

    public B getB() {
      return b;
    }

    @Override
    public int hashCode() {
      return a.hashCode() * 13 + b.hashCode() * 7;
    }
  }

  /**
   * A pair with extra data.
   */
  static class Triple<A, B, C> extends Pair<A, B> {
    private final C c;

    public Triple(A a, B b, C... c) {
      super(a, b);
      this.c = c[0];
    }

    public C getC() {
      return c;
    }
  }

  private static ByteArrayOutputStream downloadMaxMindBinaryTarXz(String href) throws IOException {
    long l = System.currentTimeMillis();
    BufferedInputStream in = new BufferedInputStream(new URL(href).openStream());
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    XZCompressorInputStream xzIn = new XZCompressorInputStream(in);
    final byte[] buffer = new byte[4096];
    int n = 0;
    while (-1 != (n = xzIn.read(buffer))) {
      out.write(buffer, 0, n);
    }
    out.close();
    xzIn.close();
    System.err.println(MessageFormat.format("decompressionTime: {0}", System.currentTimeMillis() - l));
    return out;
  }

  private static String scrapeMaxMindUrl() throws IOException, XPathExpressionException {
    Tidy tidy = new Tidy();
    tidy.setQuiet(true);
    tidy.setShowWarnings(false);
    Document tidyDOM = tidy.parseDOM(new URL(MAXMIND_URL).openStream(), null);
    XPathFactory xPathFactory = XPathFactory.newInstance();
    XPath xPath = xPathFactory.newXPath();
    String expression = DOWNLOAD_LINK;
    XPathExpression xPathExpression = xPath.compile(expression);

    Object evaluate = xPathExpression.evaluate(tidyDOM, XPathConstants.NODE);
    Element e = (Element) evaluate;
    return e.getAttribute("href");
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

