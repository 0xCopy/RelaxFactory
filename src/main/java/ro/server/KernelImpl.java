package ro.server;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.text.MessageFormat;
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
      Map o = (Map) ThreadLocalSetCookies.get();
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

  private static void scrapeGeoIpDbArchiveUrlFromMaxMind() throws IOException, XPathExpressionException {
    String s1 = "http://www.maxmind.com/app/geolitecity";
    String s = "//ul[@class=\"lstSquare\"][2]/li[2]/a[2]";

    URL oracle = new URL(s1);
    URLConnection yc = oracle.openConnection();
    InputStream is = yc.getInputStream();
    is = oracle.openStream();
    Tidy tidy = new Tidy();
    tidy.setQuiet(true);
    tidy.setShowWarnings(false);
    Document tidyDOM = (Document) tidy.parseDOM(is, null);
    XPathFactory xPathFactory = XPathFactory.newInstance();
    XPath xPath = xPathFactory.newXPath();
    String expression = s;
    XPathExpression xPathExpression = xPath.compile(expression);

    Object evaluate = xPathExpression.evaluate(tidyDOM, XPathConstants.NODE);
    Element e = (Element) evaluate;
    String href = e.getAttribute("href");

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
    do {
      nextEntry = tarArchiveInputStream.getNextTarEntry();
      if (null == nextEntry)
        break;
      String name = nextEntry.getName();
      long total = nextEntry.getSize();
      long size = total;
      System.err.println(MessageFormat.format("name: {0}\n size: {1}", name, size));
      if (!nextEntry.isDirectory()) {
        int remaining = (int) total;
        byte[] content = new byte[(int) remaining];

        while (0 != remaining) {
          int read = tarArchiveInputStream.read(content);
          remaining -= read;
        }
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(content);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(byteArrayInputStream));

        for(int c=0;c<16;c++){
          String s2 = bufferedReader.readLine();
          System.err.println(s2);
          c++;
        }
      }
    } while (true);
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


}

