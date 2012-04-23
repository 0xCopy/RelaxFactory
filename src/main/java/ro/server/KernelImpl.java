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
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
import static ro.server.CouchChangesClient.GSON;

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

  private static ByteBuffer blockBuf;
  private static int geoIpHeaderOffset;
  private static ByteBuffer indexBuf;
  private static int blockCount;
  private static ByteBuffer locBuf;


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
    scrapeGeoIpDbArchiveUrlFromMaxMind();


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
   * @throws IOException
   * @throws XPathExpressionException
   */

  private static void scrapeGeoIpDbArchiveUrlFromMaxMind() throws IOException, XPathExpressionException {
    String s1 = "http://www.maxmind.com/app/geolitecity";
    String s = "//ul[@class=\"lstSquare\"][2]/li[2]/a[2]";
    Tidy tidy = new Tidy();
    tidy.setQuiet(true);
    tidy.setShowWarnings(false);
    Document tidyDOM = tidy.parseDOM(new URL(s1).openStream(), null);
    XPathFactory xPathFactory = XPathFactory.newInstance();
    XPath xPath = xPathFactory.newXPath();
    String expression = s;
    XPathExpression xPathExpression = xPath.compile(expression);

    Object evaluate = xPathExpression.evaluate(tidyDOM, XPathConstants.NODE);
    Element e = (Element) evaluate;
    String href = e.getAttribute("href");
    System.err.println(href);
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
    TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(new ByteArrayInputStream(out.toByteArray()));
    ArchiveEntry nextEntry;
    Integer[] locations = new Integer[0];
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
            while (',' != locBuf.get()) ;
            a.add(locBuf.position());
            while (locBuf.get() != '\n') ;
          }
          locations = a.toArray(new Integer[0]);
          System.err.println("loc index time: " + (System.currentTimeMillis() - l1) + " (ms) lc: " + a.size());
        } else if (nextEntry.getName().endsWith("GeoLiteCity-Blocks.csv")) {
          blockBuf = ByteBuffer.wrap(content);
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
            indexBuf.putLong(value);
            indexBuf.putInt(value1);

          }
          blockCount = indexBuf.flip().limit() / 12;
          System.err.println("blockindex time: " + (System.currentTimeMillis() - l1) + " (ms) lc: " + blockCount + "@ writeBufSize: " + indexBuf.limit());
        }
      }
    } while (true);

    {

      indexBuf.rewind();
      ArrayList<Long> l1 = new ArrayList<Long>();
      ArrayList<Integer> l2 = new ArrayList<Integer>();
      while (indexBuf.hasRemaining()) {
        l1.add(indexBuf.getLong());
        l2.add(locations[indexBuf.getInt()-1]);
      }
      System.err.println("read back: " + l1.size());


      String s2 = "127.0.0.1";

      System.err.println("" + (String) mapLookup(l2, s2, l1.toArray(new Long[l1.size()]), Inet4Address.getByAddress(new byte[]{127, 0, 0, 1})));
      System.err.println("" + (String) mapLookup(l2, s2, l1.toArray(new Long[l1.size()]), Inet4Address.getByAddress(new byte[]{67, (byte) 174, (byte) 244,99})));


    }
  }

  private static String mapLookup(ArrayList<Integer> ints, String s2, Long[] a, InetAddress byAddress) throws UnknownHostException {
    byte[] address = byAddress.getAddress();
    long accum = 0;
    for (int i = 0; i < address.length; i++) {
      int b = address[i] & 0xff;
      accum |= b << (address.length - 1 - i) * 8;
    }
    int result = abs(Arrays.binarySearch(a, accum));
    ByteBuffer dbuf = (ByteBuffer) locBuf.duplicate().clear().position(ints.get(result));
    ByteBuffer d2buf = dbuf.duplicate();
    while ('\n' != dbuf.get()) ;
    String s3 = UTF8.decode((ByteBuffer) d2buf.limit(dbuf.position() - 1)).toString();

    System.err.println(s2 + " maps to " + s3);

    return s3;
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


  private static class LoadGeoIpArrays {
    private long[] longs;
    private int[] ints;

    public long[] getLongs() {
      return longs;
    }

    public int[] getInts() {
      return ints;
    }

    public LoadGeoIpArrays invoke() {
      longs = new long[blockCount];
      ints = new int[blockCount];
      blockBuf.rewind();
      for (int i = 0; i < ints.length; i++) {
        longs[i] = blockBuf.getLong();
        ints[i] = blockBuf.getInt();
      }
      return this;
    }
  }
}

