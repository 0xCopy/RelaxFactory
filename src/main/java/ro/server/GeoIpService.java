package ro.server;

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
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.tidy.Tidy;

import static java.lang.Math.abs;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static ro.server.KernelImpl.EXECUTOR_SERVICE;
import static ro.server.KernelImpl.GSON;
import static ro.server.KernelImpl.createCouchConnection;

/**
 * User: jim
 * Date: 4/24/12
 * Time: 10:33 AM
 */
public class GeoIpService {
  public static final String GEOIP_ROOTNODE = "/geoip/current";
  static int geoIpHeaderOffset;
  public static final String MAXMIND_URL = "http://www.maxmind.com/app/geolitecity";
  public static final String DOWNLOAD_META_SOURCE = "//ul[@class=\"lstSquare\"][2]/li[2]/a[2]";
  public static final long IPMASK = 0xffffffffl;
  public static final Random RANDOM = new Random();

  public static void createGeoIpIndex() throws IOException, XPathExpressionException, ExecutionException, InterruptedException {
    String href = scrapeMaxMindUrl();
    Triple<Integer[], ByteBuffer, ByteBuffer> indexIndexLocTrip = buildGeoIpFirstPass(downloadMaxMindBinaryTarXz(href));
    final Pair<ByteBuffer, ByteBuffer> indexLocPair = buildGeoIpSecondPass(indexIndexLocTrip);

    Callable<Map> callable = new Callable<Map>() {
      public Map call() throws Exception {
        Map m = null;
        try {
          final SocketChannel couchConnection = KernelImpl.createCouchConnection();
          final SynchronousQueue<String> synchronousQueue = new SynchronousQueue<String>();
          FetchJsonByIdVisitor fetchJsonByIdVisitor = new FetchJsonByIdVisitor(GEOIP_ROOTNODE, couchConnection, synchronousQueue);

          m = GSON.fromJson(synchronousQueue.take(), Map.class);
          if (2 == m.size() && m.containsKey("responseCode")) {
            Map map = new HashMap();
            //noinspection unchecked
            map.put("created", new Date());
            HttpMethod.enqueue(couchConnection, OP_WRITE, new SendJsonVisitor(GSON.toJson(map).trim(), synchronousQueue, GEOIP_ROOTNODE));
            String json = synchronousQueue.take();
            m = GSON.fromJson(json, Map.class);

          }
        } catch (Throwable e) {
          e.printStackTrace();  //todo: verify for a purpose
        } finally {
        }
        return m;
      }
    };
    Map map = EXECUTOR_SERVICE.submit(callable).get();
    String revision = getRevision(map);


    SocketChannel couchConnection;

    final SynchronousQueue<String> synchronousQueue = new SynchronousQueue<String>();
    {
      couchConnection = createCouchConnection();
      final String finalRevision = revision;
      HttpMethod.enqueue(couchConnection, OP_CONNECT,
          new AsioVisitor.Impl() {
            @Override
            public void onConnect(SelectionKey key) throws Exception {
              if (((SocketChannel) key.channel()).finishConnect()) key.interestOps(OP_WRITE);
            }

            @Override
            public void onWrite(SelectionKey key) throws Exception {

              final ByteBuffer d2 = (ByteBuffer) indexLocPair.getB().duplicate().rewind();
              String fn = "/geoip/current/locations.csv";
              int limit = d2.limit();
              String ctype = "application/octet-stream";
              String push = getBlobPutString(fn, limit, ctype, finalRevision);
              System.err.println("pushing: " + push);
              putFile(key, d2, push, synchronousQueue);
            }


          });
    }
    final String take = synchronousQueue.take();
    final CouchTx couchTx = GSON.fromJson(take, CouchTx.class);
    revision = couchTx.rev;
    {
      couchConnection = createCouchConnection();
      final String finalRevision = revision;
      HttpMethod.enqueue(couchConnection, OP_CONNECT,
          new AsioVisitor.Impl() {
            @Override
            public void onConnect(SelectionKey key) throws Exception {
              if (((SocketChannel) key.channel()).finishConnect()) key.interestOps(OP_WRITE);
            }

            @Override
            public void onWrite(SelectionKey key) throws Exception {

              final ByteBuffer d2 = (ByteBuffer) indexLocPair.getA().duplicate().rewind();
              String fn = "/geoip/current/index";
              int limit = d2.limit();
              String ctype = "application/octet-stream";
              String push = getBlobPutString(fn, limit, ctype, finalRevision);
              System.err.println("pushing: " + push);

              putFile(key, d2, push, synchronousQueue);
            }
          });
    }
  }
  static void putFile(SelectionKey key, final ByteBuffer d2, String push, final SynchronousQueue<String> synchronousQueue) throws IOException {
              final SocketChannel channel = (SocketChannel) key.channel();

              int write = (channel).write(UTF8.encode(push));
              key.interestOps(OP_READ);
              key.attach(new AsioVisitor.Impl() {
                @Override
                public void onRead(SelectionKey key) throws Exception {
                  SelectableChannel channel1 = key.channel();
                  ByteBuffer dst = ByteBuffer.allocateDirect(((SocketChannel) channel1).socket().getReceiveBufferSize());
                  int read = ((SocketChannel) channel1).read(dst);
                  System.err.println("Expected 100-continue.  Got(" + read + "): " + UTF8.decode((ByteBuffer) dst.flip()).toString().trim());
                  key.interestOps(OP_WRITE);
                  key.attach(new Impl() {


                    @Override
                    public void onWrite(final SelectionKey key) {
                      try {
                        int write = channel.write(d2);
                      } catch (IOException e) {
                        key.interestOps(OP_READ);
                        e.printStackTrace();  //todo: verify for a purpose
                      }
                      if (!d2.hasRemaining()) {
                        Callable<Map> callable = new Callable<Map>() {
                          public Map call() throws Exception {
                            key.attach(new JsonResponseReader(synchronousQueue));
                            key.interestOps(OP_READ);

                            return null;
                          }
                        };
                        EXECUTOR_SERVICE.submit(callable);
                      }
                    }

                  });
                }   });
            }
  public static String getBlobPutString(String fn, int limit, String ctype, String revision) {
    return new StringBuilder().append("PUT ").append(fn).append("?rev=").append(revision)
        .append(" HTTP/1.1\r\nContent-Length: ").append(limit)
        .append("\r\nContent-Type: ").append(ctype)
        .append("\r\nExpect: 100-continue\r\nAccept: */*\r\n\r\n").toString();
  }

  public static String getRevision(Map map) {
    String rev = null;
    rev = (String) map.get("_rev");
    if (null == rev)
      rev = (String) map.get("version");
    if (null == rev) rev = (String) map.get("ver");
    return rev;
  }

  static Pair<ByteBuffer, ByteBuffer> buildGeoIpSecondPass(Triple<Integer[], ByteBuffer, ByteBuffer> triple) throws UnknownHostException {
    try {
      ByteBuffer indexBuf = null;
      ByteBuffer locBuf = null;
      long[] l1 = null;
      int[] l2 = null;

      long l = System.currentTimeMillis();
      try {
        Integer[] index = triple.getA();
        indexBuf = triple.getB();
        locBuf = triple.getC();
        int count = GeoIpIndexRecord.count(indexBuf);
        l1 = new long[count];
        l2 = new int[count];

        int i = 0;
        while (indexBuf.hasRemaining()) {
          l1[i] = (IPMASK & indexBuf.getInt());
          indexBuf.mark();
          int anInt = indexBuf.getInt();
          indexBuf.reset();
          Integer value = index[anInt - 1];
          indexBuf.putInt(value);
          l2[i++] = (value);
        }
        indexBuf.rewind();
      } catch (Throwable e) {
        e.printStackTrace();
      } finally {
        System.err.println("pass2 arrays (ms):" + (System.currentTimeMillis() - l));
      }

      assert l1 != null;
      System.err.println("read back: " + l1.length);


      String s2 = "127.0.0.1";

      InetAddress loopBackAddr = Inet4Address.getByAddress(new byte[]{127, 0, 0, 1});
      Inet4Address martinez = (Inet4Address) Inet4Address.getByAddress(new byte[]{67, (byte) 174, (byte) 244, 99});
      final ByteBuffer indexBuf2 = (ByteBuffer) indexBuf.rewind();
      System.err.println(arraysLookup(l2, l1, loopBackAddr, locBuf.duplicate()));
      System.err.println(arraysLookup(l2, l1, martinez, locBuf.duplicate()));


      String lookup = KernelImpl.lookupInetAddress(loopBackAddr, locBuf, indexBuf2);
      System.err.println("localhost: " + lookup);


      lookup = GeoIpIndexRecord.lookup(martinez, indexBuf2, locBuf);

      List<InetAddress> inetAddresses = new ArrayList<InetAddress>();
      System.err.println("martinez: " + lookup);
      if (null != System.getenv("BENCHMARK"))
        runGeoIpLookupBenchMark(locBuf, l1, l2, indexBuf2, inetAddresses);
      return new Pair<ByteBuffer, ByteBuffer>(indexBuf, locBuf);
    } catch (Throwable e) {
      e.printStackTrace();  //todo: verify for a purpose
    } finally {
    }
    return null;
  }

  public static void runGeoIpLookupBenchMark(ByteBuffer locBuf, long[] l1, int[] l2, ByteBuffer indexBuf2, List<InetAddress> inetAddresses) throws UnknownHostException {
    byte[][] bytes1 = new byte[1000][4];
    {
      long l3 = System.currentTimeMillis();
      byte[] bytes = new byte[4];
      for (int i = 0; i < bytes1.length; i++) {

        RANDOM.nextBytes(bytes);
        bytes1[i] = bytes;
      }

      System.err.println("random generator overhead: " + (System.currentTimeMillis() - l3));
    }
    {
      long l3 = System.currentTimeMillis();
      for (byte[] bytes : bytes1) {
        inetAddresses.add(Inet4Address.getByAddress(bytes));
      }
      System.err.println("inataddr overhead: " + (System.currentTimeMillis() - l3));
    }

    {
      long l3 = System.currentTimeMillis();


      for (InetAddress inetAddress : inetAddresses) {

        KernelImpl.lookupInetAddress(inetAddress, locBuf, indexBuf2);
      }
      System.err.println("lookup benchmark: " + (System.currentTimeMillis() - l3));
    }
    {
      long l3 = System.currentTimeMillis();

      for (InetAddress inetAddress : inetAddresses) {
        arraysLookup(l2, l1, inetAddress, locBuf);
      }
      System.err.println("arrays Benchmark: " + (System.currentTimeMillis() - l3));
    }
  }

  /**
   * download both files.  index the smaller one over top of the larger one.
   *
   * @param dbinstance
   * @throws java.io.IOException
   * @throws javax.xml.xpath.XPathExpressionException
   *
   */
  static void scrapeGeoIpDbArchiveUrlFromMaxMind(final String dbinstance) throws IOException, XPathExpressionException, InterruptedException {
    SocketChannel connection = KernelImpl.createCouchConnection();
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
                    key.interestOps(OP_WRITE);
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
                selectionKey.interestOps(OP_READ);
              }
            });
            selectionKey.interestOps(OP_WRITE);

            Callable<Object> callable = new Callable<Object>() {
              public Object call() throws Exception {

                String take = synchronousQueue.take();
                selectionKey.attach(this);
                System.err.println("rootnode: " + take);
                Map<? extends Object, ? extends Object> map = GSON.fromJson(take, Map.class);
                if (map.containsKey("responseCode") || null != System.getenv("DEBUG_CREATEGEOIPINDEX")) {
                  createGeoIpIndex();
                } else {
                  return null;
                }
                return null;
              }
            };

            KernelImpl.EXECUTOR_SERVICE.submit(callable);
          }
          break;
          default:
            selectionKey.interestOps(OP_WRITE);
            selectionKey.attach(new Impl() {
              @Override
              public void onWrite(SelectionKey selectionKey) throws IOException {

                String format = MessageFormat.format("PUT /{0} HTTP/1.1\r\nContent-Length: 0\r\nContent-type: application/json\r\n\r\n", dbinstance);
                int write = ((SocketChannel) selectionKey.channel()).write(UTF8.encode(format));
                selectionKey.interestOps(OP_READ);
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
        selectionKey.interestOps(OP_READ);

      }

      @Override
      public void onConnect(SelectionKey selectionKey) throws IOException {
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        if (channel.finishConnect()) {
          selectionKey.interestOps(OP_WRITE);
        }
      }
    });

  }

  static String scrapeMaxMindUrl() throws IOException, XPathExpressionException {
    Tidy tidy = new Tidy();
    tidy.setQuiet(true);
    tidy.setShowWarnings(false);
    Document tidyDOM = tidy.parseDOM(new URL(MAXMIND_URL).openStream(), null);
    XPathFactory xPathFactory = XPathFactory.newInstance();
    XPath xPath = xPathFactory.newXPath();
    String expression = DOWNLOAD_META_SOURCE;
    XPathExpression xPathExpression = xPath.compile(expression);

    Object evaluate = xPathExpression.evaluate(tidyDOM, XPathConstants.NODE);
    Element e = (Element) evaluate;
    return e.getAttribute("href");
  }

  public static String arraysLookup(int[] l2, long[] longs, InetAddress byAddress, ByteBuffer csvData) {

    byte[] address = byAddress.getAddress();
    ByteBuffer ipadd = ByteBuffer.wrap(address);

    long z = IPMASK & ipadd.slice().getInt();

    int i = Arrays.binarySearch(longs, z);

    int abs = abs(i);
    Integer integer = l2[abs];

    ByteBuffer bb = (ByteBuffer) csvData.duplicate().clear().position(integer);
    while (bb.hasRemaining() && bb.get() != '\n') ;
    return UTF8.decode((ByteBuffer) bb.flip().position(integer)).toString();

  }

  public static ByteArrayOutputStream downloadMaxMindBinaryTarXz(String href) throws IOException {
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

  public static Triple<Integer[], ByteBuffer, ByteBuffer> buildGeoIpFirstPass(ByteArrayOutputStream archiveBuffer) throws IOException {
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
          int blockCount = indexBuf.flip().limit() / 8;
          System.err.println("blockindex time: " + (System.currentTimeMillis() - l1) + " (ms) lc: " + GeoIpIndexRecord.count(indexBuf) + "@ writeBufSize: " + indexBuf.limit());
        }
      }
    } while (true);
    return new Triple<Integer[], ByteBuffer, ByteBuffer>(locations, indexBuf, locBuf);
  }

}
