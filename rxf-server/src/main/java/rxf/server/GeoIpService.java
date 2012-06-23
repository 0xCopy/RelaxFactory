package rxf.server;

import javax.xml.xpath.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import one.xio.*;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.tidy.Tidy;
import rxf.server.gen.CouchDriver.DocPersist;

import static java.lang.Math.abs;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.wheresWaldo;
import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.GSON;
import static rxf.server.BlobAntiPatternObject.ISO88591;
import static rxf.server.BlobAntiPatternObject.createCouchConnection;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;
import static rxf.server.BlobAntiPatternObject.moveCaretToDoubleEol;
import static rxf.server.BlobAntiPatternObject.sortableInetAddress;
import static rxf.server.driver.CouchMetaDriver.CONTENT_LENGTH;

//import java.util.concurrent.ConcurrentSkipListMap;

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
  //    private static ConcurrentSkipListMap<Long, Integer> geoipMap = new ConcurrentSkipListMap<Long, Integer>();  slower
  private final static NavigableMap<Long, Integer> geoipMap = new TreeMap<Long, Integer>();
  public static final int MEG = (1024 * 1024);


  public static void createGeoIpIndex() throws IOException, XPathExpressionException, ExecutionException, InterruptedException {
    String href = scrapeMaxMindUrl();
    System.err.println("grabbing " + href + wheresWaldo());
    long l = System.currentTimeMillis();
    ByteArrayOutputStream archiveBuffer = downloadMaxMindBinaryTarXz(href);
    long l1 = System.currentTimeMillis() - l;
    int size = archiveBuffer.size();
    int i = size / MEG;
    double v = l1 / 1000.;
    System.err.println(MessageFormat.format("download complete in {0} archive size: {1,number,#.##}Mb @{2,number,#.##} Mb/s", v, i / MEG, i / v));
    Triple<Integer[], ByteBuffer, ByteBuffer> indexIndexLocTrip = buildGeoIpFirstPass(archiveBuffer);
    Pair<ByteBuffer, ByteBuffer> indexLocPair = buildGeoIpSecondPass(indexIndexLocTrip);


    CouchTx tx = DocPersist.$().db("geoip").validjson(GSON.toJson(new HashMap() {{
      put("_id", "current");
    }})).to().fire().tx();
    System.err.println("### geo tx1: " + tx);

    // new code!

//    tx = BlobSend.$()
//        .db("geo")
//        .docId("current")
//        .opaque(GEOIP_CURRENT_LOCATIONS_CSV)
//        .mimetype("text/csv; charset=" + BlobAntiPatternObject.ISO88591.name())
//        .blob((ByteBuffer) indexLocPair.getB().duplicate().rewind())
//        .to().fire().tx();
//
//    System.err.println("### geo tx2: " + tx);

//    final SynchronousQueue retVal = new SynchronousQueue();
//    SocketChannel couchConnection;
//    {
//      couchConnection = createCouchConnection();
//      HttpMethod.enqueue(couchConnection, OP_CONNECT | OP_WRITE,
//          new AsioVisitor.Impl() {
//
//
//            @Override
//            public void onWrite(SelectionKey key) throws Exception {
//
//              String ctype = "text/csv; charset=" + BlobAntiPatternObject.ISO88591.name();
//
//
//              ByteBuffer d2 = (ByteBuffer) indexLocPair.getB().duplicate().rewind();
//
//              String fn = GEOIP_CURRENT_LOCATIONS_CSV;
//              int limit = d2.limit();
//              String push = getBlobPutString(fn, limit, ctype, inferRevision(map));
//              System.err.println("pushing: " + push);
//
//              putFile(key, d2, push, retVal);
//            }
//          };

//
////    String take = (String) retVal.poll(3, rxf.server.BlobAntiPatternObject.defaultCollectorTimeUnit);
//    final CouchTx couchTx = GSON.fromJson(take, CouchTx.class);
//    {
//      couchConnection = createCouchConnection();
//      HttpMethod.enqueue(couchConnection, OP_CONNECT | OP_WRITE,
//          new AsioVisitor.Impl() {
//
//
//            @Override
//            public void onWrite(SelectionKey key) throws Exception {

//              ByteBuffer d2 = (ByteBuffer) indexLocPair.getA().duplicate().rewind();
//              String fn = GEOIP_CURRENT_INDEX;
//              int limit = d2.limit();
//              String ctype = "application/octet-stream";
//              String push = getBlobPutString(fn, limit, ctype, couchTx.rev());
//              System.err.println("pushing: " + push);
//
//              putFile(key, d2, push, retVal);
//            }
//          });
//    }
//    take = (String) retVal.poll(3, rxf.server.BlobAntiPatternObject.defaultCollectorTimeUnit);

//new code!

//    tx = BlobSend.$()
//        .db("geo")
//        .docId("current")
//        .opaque(GEOIP_CURRENT_INDEX)
//        .mimetype("application/octet-stream")
//        .blob((ByteBuffer) indexLocPair.getA().duplicate().rewind())
//        .to().fire().tx();


    System.err.println("### geo tx3: " + tx);
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
      int locOffset = indexBuf.asIntBuffer().get();

      Integer value = index[locOffset - 1];
      indexBuf.putInt(value);
      l2[i++] = (value);
    }
    indexBuf.rewind();


    return new Pair<ByteBuffer, ByteBuffer>(indexBuf, locBuf);
  }

  static void testWalnutCreek(final ByteBuffer ix, ByteBuffer loc, long[] l1, int[] l2) throws UnknownHostException {

    try {
//      String s2 = "127.0.0.1";
//      InetAddress loopBackAddr = Inet4Address.getByAddress(new byte[]{127, 0, 0, 1});
      InetAddress walnutCreek = Inet4Address.getByAddress(new byte[]{67, (byte) 174, (byte) 244, 11});
//      System.err.println("|" + sortableInetAddress(Inet4Address.getByAddress(new byte[]{67, (byte) 174, (byte) 244, 103})));
//      System.err.println("|" + sortableInetAddress(Inet4Address.getByAddress(new byte[]{67, (byte) 174, (byte) 244, 103})));
//      System.err.println("|" + sortableInetAddress(Inet4Address.getByAddress(new byte[]{67, (byte) 174, (byte) 103, 11})));
//      if (null != l1 && null != l2) {
//        System.err.println(arraysLookup(l2, l1, loopBackAddr, loc.duplicate()));
//        System.err.println(arraysLookup(l2, l1, walnutCreek, loc.duplicate()));
//      }
      try {
        installLocalhostDeveloperGeoIp();
        {
          System.err.println("jim: " + mapAddressLookup(walnutCreek));
          System.err.println("c: " + mapAddressLookup(Inet4Address.getByName("ppp-69-217-127-68.dsl.chcgil.ameritech.net")));
        }

      } catch (Throwable e) {
        e.printStackTrace();  //todo: verify for a purpose
      } finally {
      }

//
//
//      lookup = KernelImpl.lookupInetAddress(walnutCreek, indexMMBuf );
//
//      System.err.println("walnutCreek: " + lookup);
    } catch (Throwable e) {
      e.printStackTrace();  //todo: verify for a purpose
    } finally {
    }

  }

  /**
   * this installs the csv addHeaderInterest on geo lookups for 127.0.0.1
   */
  public static void installLocalhostDeveloperGeoIp() throws UnknownHostException {
    long key = sortableInetAddress(Inet4Address.getByAddress(new byte[]{(byte) 127, (byte) 0, (byte) 0, (byte) 1}));

    System.err.println("lo lookup should be ... " + key);
    locationMMBuf.rewind();
    while (',' != locationMMBuf.get()) ;//skip to first comma in header line
    geoipMap.put(key, locationMMBuf.position());

    System.err.println("geoip addHeaderInterest: " + mapAddressLookup(BlobAntiPatternObject.LOOPBACK));
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
//                long l3 = System.currentTimeMillis();
//
//
//                for (InetAddress inetAddress : inetAddresses) {
//
//                    KernelImpl.lookupInetAddress(inetAddress, ix, bufAbstraction);
//                }
//                System.err.println("list benchmark: " + (System.currentTimeMillis() - l3));
      } catch (Throwable e) {
        e.printStackTrace();  //todo: verify for a purpose
      } finally {
      }
    }

    if (null != GeoIpService.indexMMBuf && null != locationMMBuf) {

      benchMarkMap(inetAddresses, new ConcurrentSkipListMap<Long, Integer>());
      benchMarkMap(inetAddresses, new TreeMap<Long, Integer>());


      /*
long l3 = System.currentTimeMillis();

for (InetAddress inetAddress : inetAddresses) {
    arraysLookup(l2, l1, inetAddress, loc);
}
System.err.println("arrays Benchmark: " + (System.currentTimeMillis() - l3));*/
    }
    if (null != l1 && null != l2) {
      long l3 = System.currentTimeMillis();

      for (InetAddress inetAddress : inetAddresses) {
        arraysLookup(l2, l1, inetAddress, loc);
      }
      System.err.println("arrays Benchmark: " + (System.currentTimeMillis() - l3));
    }
  }

  private static void benchMarkMap(InetAddress[] inetAddresses, NavigableMap<Long, Integer> navigableMap) {
    long l = System.currentTimeMillis();
    NavigableMap<Long, Integer> map = navigableMap;
    ByteBuffer duplicate = indexMMBuf.duplicate();
    while (duplicate.hasRemaining()) {
      map.put(duplicate.getInt() & 0xffffffffl, duplicate.getInt());
    }

    System.err.println((map.getClass().getName() + " induction time: " + (System.currentTimeMillis() - l)));
    long l3 = System.currentTimeMillis();


    try {
      for (InetAddress inetAddress : inetAddresses) {
        CharBuffer charBuffer = lookupMappedAddress(inetAddress, navigableMap);

      }
    } catch (Throwable e) {
      e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
    } finally {
    }
    System.err.println(map.getClass().getName() + " benchmark: " + (System.currentTimeMillis() - l3));
  }

  public static CharBuffer mapAddressLookup(InetAddress inetAddress) {
    return lookupMappedAddress(inetAddress, geoipMap);
  }

  private static CharBuffer lookupMappedAddress(InetAddress inetAddress, NavigableMap<Long, Integer> map) {
    long l4 = sortableInetAddress(inetAddress);

    Map.Entry<Long, Integer> longIntegerEntry = map.floorEntry(IPMASK & l4);
    Integer integer = null == longIntegerEntry ? map.firstEntry().getValue() : longIntegerEntry.getValue();
    ByteBuffer slice = ((ByteBuffer) (locationMMBuf.position(integer))).slice();
    while (slice.hasRemaining() && '\n' != slice.get()) ;
    return ISO88591.decode((ByteBuffer) slice.flip());
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
//    final SynchronousQueue retVal = new SynchronousQueue();
    SocketChannel connection = BlobAntiPatternObject.createCouchConnection();
    final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
    HttpMethod.enqueue(connection, OP_CONNECT | OP_WRITE, new AsioVisitor.Impl() {
      final public AtomicReference<String> payload = new AtomicReference<String>();

      public void onRead(final SelectionKey key) throws IOException, InterruptedException {
        final AsioVisitor parent = this;
        final SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer dst = ByteBuffer.allocateDirect(BlobAntiPatternObject.getReceiveBufferSize());
        int read = channel.read(dst);
        dst.flip();
        System.err.println("$res: " + UTF8.decode((ByteBuffer) dst.duplicate().rewind()));
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

            key.selector().wakeup();
            key.interestOps(OP_WRITE).attach(new Impl() {


              @Override
              public void onWrite(final SelectionKey key) {

                try {
                  String format = (MessageFormat.format("GET /{0} HTTP/1.1\r\n\r\n", keyDocument));
                  System.err.println("attempting connect: " + format.trim());
                  channel.write(UTF8.encode(format));
                } catch (IOException e) {
                  e.printStackTrace();  //todo: verify for a purpose
                }
//                key.attach(BlobAntiPatternObject.createJsonResponseReader(retVal));
                key.selector().wakeup();
                key.interestOps(OP_READ).attach(new Impl() {
                  @Override
                  public void onRead(SelectionKey key) throws Exception {
                    final ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
                    int read1 = channel.read(dst);
                    Rfc822HeaderState state = new Rfc822HeaderState().addHeaderInterest(CONTENT_LENGTH);
                    state.apply((ByteBuffer) dst.flip());

                    EXECUTOR_SERVICE.submit(new Callable<Object>() {
                      public Object call() throws Exception {
                        cyclicBarrier.await();//  ------------------------------>    //V
                        payload.set(UTF8.decode(dst.slice()).toString().trim());     //V
                        return null;                                                 //V
                      }                                                              //V
                    });                                                              //V
                  }                                                                  //V
                });                                                                  //V
              }                                                                      //V
            });                                                                      //V
            Callable<Object> callable = new Callable<Object>() {       //              V
              public Object call() throws Exception {                  //            //V
                //                                                                   //V
                //                                                                   //V
                String take = payload.get();//                                         V
                cyclicBarrier.await(3, BlobAntiPatternObject.getDefaultCollectorTimeUnit());
                key.attach(this);
                System.err.println("rootnode: " + take);
                Map map = GSON.fromJson(take, Map.class);

                //happens if we need to create the 'current' geoip database.
                if (map.containsKey("responseCode") || null != System.getenv(DEBUG_CREATEGEOIPINDEX)) {
                  createGeoIpIndex();
                }
//                happens every time we start
                {
//                  ArrayList<Callable<MappedByteBuffer>> cc = new ArrayList<Callable<MappedByteBuffer>>();
                  /*cc.add*/
                  indexMMBuf =
                      EXECUTOR_SERVICE.submit(
                          getMappedIndexFile(GEOIP_CURRENT_INDEX)).get();
                  locationMMBuf = EXECUTOR_SERVICE.submit(
                      getMappedIndexFile(GEOIP_CURRENT_LOCATIONS_CSV)).get();
//                  List<Future<MappedByteBuffer>> futures = EXECUTOR_SERVICE.invokeAll(cc);

                  ByteBuffer ix = (ByteBuffer) indexMMBuf.duplicate().clear();
                  ByteBuffer loc = (ByteBuffer) locationMMBuf.duplicate().clear();

                  indexMMBuf.clear();
                  IntBuffer intBuffer = indexMMBuf.asIntBuffer();
                  while (intBuffer.hasRemaining())
                    geoipMap.put(intBuffer.get() & IPMASK, intBuffer.get());

                  //this should report 'Martinez'
                  testWalnutCreek(ix, loc, null, null);


                  if (null != System.getenv(GEOIP_BENCHMARK_ON_STARTUP)) {
                    for (int i = 0; i < 1000; i++) {
                      long l = System.currentTimeMillis();

                      Runtime.getRuntime().gc();
                      long l1 = Runtime.getRuntime().freeMemory();
                      runGeoIpLookupBenchMark((ByteBuffer) loc.clear(), null, null, (ByteBuffer) ix.clear());
                      long l2 = Runtime.getRuntime().freeMemory();
                      System.err.println(MessageFormat.format("{0}: {1} (ms) -----------------------------", i, System.currentTimeMillis() - l));
                      System.err.println(MessageFormat.format("freemem delta current:{0} before{1} delta(Mb):{2}", l2, l1, (l2 - l1) / (1024 * 1024)));
                    }


                  }
                  return new Pair<ByteBuffer, ByteBuffer>(ix, loc);
                }
              }

              Callable<MappedByteBuffer> getMappedIndexFile(final String path) throws IOException {
                SocketChannel couchConnection = createCouchConnection();
                final SynchronousQueue<MappedByteBuffer> retVal = new SynchronousQueue<MappedByteBuffer>();

                Callable<MappedByteBuffer> callable = new Callable<MappedByteBuffer>() {

                  public MappedByteBuffer call() throws Exception {
                    return retVal.poll(2, rxf.server.BlobAntiPatternObject.getDefaultCollectorTimeUnit());  //todo: verify for a purpose
                  }
                };


                HttpMethod.enqueue(couchConnection, OP_CONNECT | OP_WRITE, new Impl() {

                  @Override
                  public void onWrite(SelectionKey selectionKey) throws Exception {
                    mapTmpFile(selectionKey, path);

                  }

                  void mapTmpFile(SelectionKey key, final String path) throws IOException {
                    String req = "GET " + path + " HTTP/1.1\r\n\r\n";
                    int write = ((SocketChannel) key.channel()).write(UTF8.encode(req));
                    key.selector().wakeup();
                    key.interestOps(OP_READ);
                    key.attach(new Impl() {
                      @Override
                      public void onRead(SelectionKey key) throws Exception {
                        SocketChannel channel = (SocketChannel) key.channel();
                        ByteBuffer dst1 = ByteBuffer.allocateDirect(BlobAntiPatternObject.getReceiveBufferSize());
                        int read1 = channel.read(dst1);
                        final long l2 = System.currentTimeMillis();
//                          System.err.println("$res for "+path+": "+UTF8.decode((ByteBuffer) dst1.flip()))


                        ByteBuffer headers = (ByteBuffer) moveCaretToDoubleEol((ByteBuffer) dst1.flip()).duplicate().flip();
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

                          final File geoip = File.createTempFile("geoip", path.substring(path.length() - 5));
                          try {
                            geoip.createNewFile();
                          } catch (IOException e) {
                            e.printStackTrace();  //todo: verify for a purpose
                          }
                          final RandomAccessFile randomAccessFile = new RandomAccessFile(geoip, "rw");
                          final FileChannel fileChannel = randomAccessFile.getChannel();
                          final float pos1 = fileChannel.write(dst1);

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

            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(callable);
          }
          break;
          default:
            key.selector().wakeup();
            key.interestOps(OP_WRITE);
            key.attach(new Impl() {
              @Override
              public void onWrite(SelectionKey key) throws IOException {

                String format = MessageFormat.format("PUT /{0} HTTP/1.1\r\nContent-Length: 0\r\nContent-type: application/json\r\n\r\n", dbinstance);
                int write = ((SocketChannel) key.channel()).write(UTF8.encode(format));
                key.selector().wakeup();
                key.interestOps(OP_READ);
                key.attach(parent);
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
        selectionKey.selector().wakeup();
        selectionKey.interestOps(OP_READ);

      }

      @Override
      public void onConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.finishConnect()) {
          key.selector().wakeup();
          key.interestOps(OP_WRITE);
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
    int c = 0;
    while (-1 != (n = xzIn.read(buffer))) {
      out.write(buffer, 0, n);
      if (0 == c++ % 100) System.err.print(".");
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
          while (locBuf.get() != '\n') ;//addHeaderInterest

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
          while (blockBuf.get() != '\n') ;//addHeaderInterest
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
