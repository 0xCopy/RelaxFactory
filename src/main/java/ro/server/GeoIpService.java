package ro.server;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
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
import static ro.server.GeoIpIndexRecord.reclen;
import static ro.server.KernelImpl.EXECUTOR_SERVICE;
import static ro.server.KernelImpl.GSON;
import static ro.server.KernelImpl.createCouchConnection;
import static ro.server.KernelImpl.moveCaretToDoubleEol;

//code smell

/**
 * User: jim
 * Date: 4/24/12
 * Time: 10:33 AM
 */
public class GeoIpService {
  public static final String GEOIP_ROOTNODE = "/geoip/current";
  public static final String GEOIP_CURRENT_LOCATIONS_CSV = "/geoip/current/locations.csv";
  public static final String GEOIP_CURRENT_INDEX = "/geoip/current/index";
  public static final String DEBUG_CREATEGEOIPINDEX = "DEBUG_CREATEGEOIPINDEX";
  public static final String MAXMIND_URL = "http://www.maxmind.com/app/geolitecity";
  public static final String DOWNLOAD_META_SOURCE = "//ul[@class=\"lstSquare\"][2]/li[2]/a[2]";
  public static final long IPMASK = 0xffffffffl;
  public static final Random RANDOM = new Random();
  public static final String GEOIP_BENCHMARK_ON_STARTUP = "GEOIP_BENCHMARK_ON_STARTUP";
  static MappedByteBuffer indexMMBuf;
  static MappedByteBuffer locationMMBuf;
  static int geoIpHeaderOffset;
  static List<Long> bufAbstraction = new AbstractList<Long>() {
    @Override
    public Long get(int index) {
      return IPMASK & indexMMBuf.getInt(index * reclen);
    }

    @Override
    public int size() {
      int limit = indexMMBuf.limit();
      int i1 = limit % reclen;
      limit -= i1;
      int i = limit / reclen;
      return i;
    }
  };


  public static void createGeoIpIndex() throws IOException, XPathExpressionException, ExecutionException, InterruptedException {
    String href = scrapeMaxMindUrl();
    Triple<Integer[], ByteBuffer, ByteBuffer> indexIndexLocTrip = buildGeoIpFirstPass(downloadMaxMindBinaryTarXz(href));
    final Pair<ByteBuffer, ByteBuffer> indexLocPair = buildGeoIpSecondPass(indexIndexLocTrip);

    Callable<Map> callable = new Callable<Map>() {
      public Map call() throws Exception {
        Map m = null;
        try {
          SocketChannel couchConnection = KernelImpl.createCouchConnection();
          SynchronousQueue<String> retVal = new SynchronousQueue<String>();
          FetchJsonByIdVisitor fetchJsonByIdVisitor = new FetchJsonByIdVisitor(GEOIP_ROOTNODE, couchConnection, retVal);

          m = GSON.fromJson(retVal.take(), Map.class);
          if (2 == m.size() && m.containsKey("responseCode")) {
            Map<String, Date> map = new HashMap<String, Date>();
            //noinspection unchecked
            map.put("created", new Date());
            HttpMethod.enqueue(couchConnection, OP_WRITE, new SendJsonVisitor(GSON.toJson(map).trim(), retVal, GEOIP_ROOTNODE));
            String json = retVal.take();
            m = GSON.fromJson(json, Map.class);

          }
        } catch (Throwable e) {
          e.printStackTrace();  //todo: verify for a purpose
        } finally {
        }
        return m;
      }
    };
    final Map map = EXECUTOR_SERVICE.submit(callable).get();


    final SynchronousQueue<String> retVal = new SynchronousQueue<String>();
    SocketChannel couchConnection;
    {
      couchConnection = createCouchConnection();
      HttpMethod.enqueue(couchConnection, OP_CONNECT,
          new AsioVisitor.Impl() {
            @Override
            public void onConnect(SelectionKey key) throws Exception {
              if (((SocketChannel) key.channel()).finishConnect()) key.interestOps(OP_WRITE);
            }

            @Override
            public void onWrite(SelectionKey key) throws Exception {

              String ctype = "text/csv; charset=" + KernelImpl.ISO88591.name();
//              String ctype = "application/octet-stream ; charset="+KernelImpl.ISO88591.name() ;
//              String ctype = "application/octet-stream";// charset="+KernelImpl.ISO88591.name() ;

              ByteBuffer d2 = (ByteBuffer) indexLocPair.getB().duplicate().rewind();

              String fn = GEOIP_CURRENT_LOCATIONS_CSV;
              int limit = d2.limit();
              String push = getBlobPutString(fn, limit, ctype, getRevision(map));
              System.err.println("pushing: " + push);
              putFile(key, d2, push, retVal);
            }
          });
    }
    String take = retVal.take();
    final CouchTx couchTx = GSON.fromJson(take, CouchTx.class);
    {
      couchConnection = createCouchConnection();
      HttpMethod.enqueue(couchConnection, OP_CONNECT,
          new AsioVisitor.Impl() {
            @Override
            public void onConnect(SelectionKey key) throws Exception {
              if (((SocketChannel) key.channel()).finishConnect()) key.interestOps(OP_WRITE);
            }

            @Override
            public void onWrite(SelectionKey key) throws Exception {

              ByteBuffer d2 = (ByteBuffer) indexLocPair.getA().duplicate().rewind();
              String fn = GEOIP_CURRENT_INDEX;
              int limit = d2.limit();
              String ctype = "application/octet-stream";
              String push = getBlobPutString(fn, limit, ctype, couchTx.rev);
              System.err.println("pushing: " + push);

              putFile(key, d2, push, retVal);
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
            } catch (Throwable e) {
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
      }
    });
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
      rev = (String) map.get("rev");
    if (null == rev) {
      rev = (String) map.get("version");
    }
    if (null == rev)
      rev = (String) map.get("ver");
    return rev;
  }

  static Pair<ByteBuffer, ByteBuffer> buildGeoIpSecondPass(Triple<Integer[], ByteBuffer, ByteBuffer> triple) throws UnknownHostException {

    ByteBuffer indexBuf = null;
    ByteBuffer locBuf = null;
    long[] l1 = null;
    int[] l2 = null;

    long l = System.currentTimeMillis();

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


    return new Pair<ByteBuffer, ByteBuffer>(indexBuf, locBuf);
  }

  static void testMartinez(final ByteBuffer ix, ByteBuffer loc, long[] l1, int[] l2) throws UnknownHostException {

    try {
      String s2 = "127.0.0.1";
      InetAddress loopBackAddr = Inet4Address.getByAddress(new byte[]{127, 0, 0, 1});
      InetAddress martinez = Inet4Address.getByAddress(new byte[]{67, (byte) 174, (byte) 244, 11});
      if (null != l1 && null != l2) {
        System.err.println(arraysLookup(l2, l1, loopBackAddr, loc.duplicate()));
        System.err.println(arraysLookup(l2, l1, martinez, loc.duplicate()));
      }
      try {
        {
          ByteBuffer tmp = (ByteBuffer) loc.clear().position(KernelImpl.lookupInetAddress(loopBackAddr, ix, bufAbstraction)).mark();
          int p = tmp.position();
          while (tmp.get() != '\n') ;
          String trim = UTF8.decode((ByteBuffer) tmp.flip().position(p)).toString().trim();

          System.err.println("localhost: " + trim);
        }
        {
          ByteBuffer tmp = (ByteBuffer) loc.clear().position(KernelImpl.lookupInetAddress(martinez, ix, bufAbstraction)).mark();
          int p = tmp.position();
          while (tmp.get() != '\n') ;
          String trim = UTF8.decode((ByteBuffer) tmp.flip().position(p)).toString().trim();

          System.err.println("martinez: " + trim);
        }
      } catch (Throwable e) {
        e.printStackTrace();  //todo: verify for a purpose
      } finally {
      }

//
//
//      lookup = KernelImpl.lookupInetAddress(martinez, indexMMBuf );
//
//      System.err.println("martinez: " + lookup);
    } catch (Throwable e) {
      e.printStackTrace();  //todo: verify for a purpose
    } finally {
    }

  }

  public static void runGeoIpLookupBenchMark(ByteBuffer loc, long[] l1, int[] l2, final ByteBuffer ix) throws UnknownHostException {
    byte[][] bytes1 = new byte[1000][4];
    {
      long l3 = System.currentTimeMillis();
//      byte[] bytes = new byte[4];
      for (int i = 0; i < bytes1.length; i++) {

        RANDOM.nextBytes(bytes1[i] = new byte[4]);
      }

      System.err.println("random generator overhead: " + (System.currentTimeMillis() - l3));
    }
    InetAddress[] inetAddresses = new InetAddress[1000];
    {
      long l3 = System.currentTimeMillis();
      for (int i = 0, bytes1Length = bytes1.length; i < bytes1Length; i++) {
        inetAddresses[i] = InetAddress.getByAddress(bytes1[i]);

      }
      System.err.println("inataddr overhead: " + (System.currentTimeMillis() - l3));
    }

    {
      try {
        long l3 = System.currentTimeMillis();


        for (InetAddress inetAddress : inetAddresses) {

          KernelImpl.lookupInetAddress(inetAddress, ix, bufAbstraction);
        }
        System.err.println("list benchmark: " + (System.currentTimeMillis() - l3));
      } catch (Throwable e) {
        e.printStackTrace();  //todo: verify for a purpose
      } finally {
      }
    }
    if (null != l1 && null != l2) {
      long l3 = System.currentTimeMillis();

      for (InetAddress inetAddress : inetAddresses) {
        arraysLookup(l2, l1, inetAddress, loc);
      }
      System.err.println("arrays Benchmark: " + (System.currentTimeMillis() - l3));
    }
  }

  /**
   * download both files.  index buffer is incidentally rewritten as it is iterated.
   *
   * @param dbinstance
   * @throws java.io.IOException
   * @throws javax.xml.xpath.XPathExpressionException
   *
   */
  static void startGeoIpService(final String dbinstance) throws IOException, XPathExpressionException, InterruptedException {
    SocketChannel connection = KernelImpl.createCouchConnection();
    final SynchronousQueue<String> retVal = new SynchronousQueue<String>();

    HttpMethod.enqueue(connection, SelectionKey.OP_CONNECT, new AsioVisitor.Impl() {
      public void onRead(final SelectionKey selectionKey) throws IOException, InterruptedException {
        final AsioVisitor parent = this;
        final SocketChannel channel = (SocketChannel) selectionKey.channel();
        ByteBuffer dst = ByteBuffer.allocateDirect(channel.socket().getReceiveBufferSize());
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
              public void onWrite(final SelectionKey selectionKey) {

                try {
                  String format = (MessageFormat.format("GET /{0} HTTP/1.1\r\n\r\n", keyDocument));
                  System.err.println("attempting connect: " + format.trim());
                  channel.write(UTF8.encode(format));
                } catch (IOException e) {
                  e.printStackTrace();  //todo: verify for a purpose
                }
                selectionKey.attach(new JsonResponseReader(retVal));
                selectionKey.interestOps(OP_READ);
              }
            });
            selectionKey.interestOps(OP_WRITE);

            Callable<Object> callable = new Callable<Object>() {
              public Object call() throws Exception {

                String take = retVal.take();
                selectionKey.attach(this);
                System.err.println("rootnode: " + take);
                Map map = GSON.fromJson(take, Map.class);

                //happens if we need to create the 'current' geoip database.
                if (map.containsKey("responseCode") || null != System.getenv(DEBUG_CREATEGEOIPINDEX)) {
                  createGeoIpIndex();
                }
//                happens every time we start
                {
                  ArrayList<Callable<MappedByteBuffer>> cc = new ArrayList<Callable<MappedByteBuffer>>();
                  cc.add(getMappedIndexFile(GEOIP_CURRENT_INDEX));
                  cc.add(getMappedIndexFile(GEOIP_CURRENT_LOCATIONS_CSV));
                  List<Future<MappedByteBuffer>> futures = EXECUTOR_SERVICE.invokeAll(cc);
                  indexMMBuf = futures.get(0).get();
                  locationMMBuf = futures.get(1).get();

                  ByteBuffer ix = (ByteBuffer) indexMMBuf.duplicate().clear();
                  ByteBuffer loc = (ByteBuffer) locationMMBuf.duplicate().clear();


                  //this should report 'Martinez'
                  testMartinez(ix, loc, null, null);


                  if (null != System.getenv(GEOIP_BENCHMARK_ON_STARTUP)) {
                    for (int i = 0; i < 1000; i++) {
                      long l = System.currentTimeMillis();
                      runGeoIpLookupBenchMark((ByteBuffer) loc.clear(), null, null, (ByteBuffer) ix.clear());
                      System.err.println(MessageFormat.format("{0}: {1} (ms) -----------------------------", i, System.currentTimeMillis() - l));
                    }


                  }
                  return new Pair<ByteBuffer, ByteBuffer>(ix, loc);
                }
              }

              Callable<MappedByteBuffer> getMappedIndexFile(final String path) throws IOException {
                SocketChannel couchConnection = createCouchConnection();
                final SynchronousQueue<MappedByteBuffer> retVal = new SynchronousQueue<MappedByteBuffer>();

                Callable<MappedByteBuffer> callable = new Callable<MappedByteBuffer>() {
                  @Override
                  public MappedByteBuffer call() throws Exception {
                    return retVal.take();  //todo: verify for a purpose
                  }
                };


                HttpMethod.enqueue(couchConnection, OP_CONNECT, new Impl() {

                  @Override
                  public void onWrite(SelectionKey selectionKey) throws Exception {
                    mapTmpFile(selectionKey, path);

                  }

                  void mapTmpFile(SelectionKey selectionKey, String path) throws IOException {
                    String req = "GET " + path + " HTTP/1.1\r\n\r\n";
                    int write = ((SocketChannel) selectionKey.channel()).write(UTF8.encode(req));
                    selectionKey.interestOps(OP_READ);
                    selectionKey.attach(new Impl() {
                      @Override
                      public void onRead(SelectionKey key) throws Exception {
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer dst1 = ByteBuffer.allocateDirect(channel.socket().getReceiveBufferSize());
                        int read1 = channel.read(dst1);
                        final long l2 = System.currentTimeMillis();
//                          System.err.println("response for "+path+": "+UTF8.decode((ByteBuffer) dst1.flip()))


                        moveCaretToDoubleEol((ByteBuffer) dst1.flip());
                        ByteBuffer headers = (ByteBuffer) dst1.duplicate().flip();
                        while (!Character.isWhitespace(headers.get())) ;
                        ByteBuffer h2 = (ByteBuffer) headers.duplicate().position(headers.position());
                        while (!Character.isWhitespace(headers.get())) ;
                        h2.limit(headers.position() - 1);
                        int rc = Integer.parseInt(UTF8.decode(h2).toString().trim());
                        if (200 == rc) {
                          Map<String, int[]> hm = HttpHeaders.getHeaders((ByteBuffer) headers.rewind());
                          int[] ints = hm.get("Content-Length");
                          String cl = UTF8.decode((ByteBuffer) h2.clear().position(ints[0]).limit(ints[1])).toString().trim();
                          final long total = Long.parseLong(cl);

                          final File geoip = File.createTempFile("geoip", ".index");
                          try {
                            geoip.createNewFile();
                          } catch (IOException e) {
                            e.printStackTrace();  //todo: verify for a purpose
                          }
                          final RandomAccessFile randomAccessFile = new RandomAccessFile(geoip, "rw");
                          final FileChannel fileChannel = randomAccessFile.getChannel();
                          int write1 = fileChannel.write(dst1);
                          final float pos1 = write1;

                          key.attach(new Impl() {
                            long pos = (long) pos1;

                            private final SynchronousQueue<MappedByteBuffer> returnTo = retVal;

                            @Override
                            public void onRead(SelectionKey key) throws IOException, InterruptedException {
                              long l = fileChannel.transferFrom((ReadableByteChannel) key.channel(), pos, 16 * 1024 * 1024);
                              pos += l;
                              if (pos >= total) {
                                MappedByteBuffer map1 = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, total);
                                returnTo.put(map1);
                                key.attach(null);
                                long l1 = System.currentTimeMillis() - l2;
                                System.err.println(MessageFormat.format("file write ended: {0} {1}/{2} in {3} (ms) @ {4}M/s", geoip, total, randomAccessFile.length(), l1, (total / 1024. * 1024.) / l1 / 1000.));
                                geoip.deleteOnExit();
                              }
                            }
                          });
                        }

                      }
                    });
                  }
                });
                return callable;
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
    byte[] buffer = new byte[4096];
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
