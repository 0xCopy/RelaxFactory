/*
 * package rxf.web.inf;
 * 
 * import com.meterware.httpunit.GetMethodWebRequest; import com.meterware.httpunit.WebConversation; import
 * com.meterware.httpunit.WebResponse; import one.xio.AsioVisitor; import one.xio.AsyncSingletonServer; import
 * org.junit.AfterClass; import org.junit.BeforeClass; import org.junit.Test; import rxf.core.DateHeaderParser;
 * 
 * import java.net.InetSocketAddress; import java.nio.channels.ServerSocketChannel; import
 * java.util.concurrent.ExecutorService; import java.util.concurrent.Executors;
 * 
 * import static java.nio.channels.SelectionKey.OP_ACCEPT; import static one.xio.HttpHeaders.If$2dModified$2dSince;
 * import static one.xio.HttpHeaders.If$2dUnmodified$2dSince; import static org.junit.Assert.assertEquals; import static
 * org.junit.Assert.fail;
 * 
 * public class ContentRootImplTest { private static final String host = "localhost"; public static String base; public
 * static WebConversation wc; private static int port; private static ExecutorService exec; private static
 * ServerSocketChannel serverSocketChannel;
 * 
 * @BeforeClass static public void setUp() throws Exception {
 * 
 * System.err.println("setting up webconversation"); wc = new WebConversation();
 * 
 * // setDEBUG_SENDJSON(true); System.err.println("unsetting killswitch"); AsyncSingletonServer.killswitch.set(false);
 * 
 * System.err.println("opening socket"); serverSocketChannel = ServerSocketChannel.open();
 * serverSocketChannel.configureBlocking(false); System.err.println("binding socket"); final InetSocketAddress
 * serverSocket = new InetSocketAddress(host, 0); serverSocketChannel.socket().bind(serverSocket); port =
 * serverSocketChannel.socket().getLocalPort();
 * 
 * base = "http://localhost:" + port; System.err.println("setting base to " + base);
 * System.err.println("creating cachedthreadpool");
 * 
 * exec = Executors.newCachedThreadPool(); System.err.println("running runnable"); exec.submit(new Runnable() { public
 * void run() { try { System.err.println("enqueuing socketchannel with (null)");
 * AsyncSingletonServer.SingleThreadSingletonServer.enqueue(serverSocketChannel, OP_ACCEPT);
 * System.err.println("calling init with PMD"); AsyncSingletonServer.SingleThreadSingletonServer.init(new
 * ProtocolMethodDispatch()); } catch (Exception e) { System.out.println("failed startup"); } } }); }
 * 
 * @AfterClass static public void tearDown() throws Exception { try { AsyncSingletonServer.killswitch.set(true);
 * AsioVisitor.Helper.getSelector().close(); serverSocketChannel.close();
 * 
 * exec.shutdown(); } catch (Exception ignore) { fail(ignore.getMessage()); } }
 * 
 * @Test(timeout = 10000) public void testRequestSlash() throws Exception {
 * System.err.println("starting: RequestSlash() throws Exception "); System.err.println("entering testRequestSlash()");
 * GetMethodWebRequest req = new GetMethodWebRequest(base + '/'); WebResponse res = wc.getResponse(req);
 * assertEquals("Sample App", res.getTitle()); }
 * 
 * @Test(timeout = 10000) public void testRequestIndexHtml() throws Exception {
 * System.err.println("starting: RequestIndexHtml() throws Exception ");
 * 
 * GetMethodWebRequest req = new GetMethodWebRequest(base + "/index.html"); WebResponse res = wc.getResponse(req);
 * assertEquals("Sample App", res.getTitle()); }
 * 
 * @Test(timeout = 10000) public void testRequest304() throws Exception {
 * System.err.println("starting: Request304() throws Exception ");
 * 
 * String url = "http://localhost:" + port + "/index.html"; GetMethodWebRequest req = new GetMethodWebRequest(url);
 * req.setHeaderField(If$2dModified$2dSince.getHeader(), DateHeaderParser.formatHttpHeaderDate()); WebResponse res =
 * wc.getResponse(req); int responseCode = res.getResponseCode(); res.close(); assertEquals(304, responseCode);
 * 
 * req.setHeaderField(If$2dModified$2dSince.getHeader(), DateHeaderParser
 * .formatHttpHeaderDate(DateHeaderParser.parseDate("01/08/1990")));
 * 
 * res = wc.getResponse(req); responseCode = res.getResponseCode(); res.close(); assertEquals(200, responseCode); }
 * 
 * @Test(timeout = 10000) public void testRequest412() throws Exception {
 * System.err.println("starting: Request412() throws Exception ");
 * 
 * String url = "http://localhost:" + port + "/index.html"; GetMethodWebRequest req = new GetMethodWebRequest(url);
 * req.setHeaderField(If$2dUnmodified$2dSince.getHeader(), DateHeaderParser.formatHttpHeaderDate()); WebResponse res =
 * wc.getResponse(req); int responseCode = res.getResponseCode(); res.close(); assertEquals(200, responseCode);
 * 
 * req.setHeaderField(If$2dUnmodified$2dSince.getHeader(), DateHeaderParser
 * .formatHttpHeaderDate(DateHeaderParser.parseDate("01/08/1990")));
 * 
 * try { res = wc.getResponse(req); } catch (Exception e) { res.close(); return; } fail(); }
 * 
 * @Test(timeout = 10000) public void testRequestFileWithQuerystring() throws Exception {
 * System.err.println("starting: RequestFileWithQuerystring() throws Exception "); GetMethodWebRequest req = new
 * GetMethodWebRequest(base + "/?some=params&others=true"); WebResponse res = wc.getResponse(req);
 * assertEquals("Sample App", res.getTitle()); req = new GetMethodWebRequest(base +
 * "/index.html?some=params&others=true"); res = wc.getResponse(req); assertEquals("Sample App", res.getTitle()); }
 * 
 * @Test(timeout = 10000) public void testRequestFileWithFragment() throws Exception {
 * System.err.println("starting: RequestFileWithFragment() throws Exception "); GetMethodWebRequest req = new
 * GetMethodWebRequest(base + "/#true"); WebResponse res = wc.getResponse(req); assertEquals("Sample App",
 * res.getTitle()); req = new GetMethodWebRequest(base + "/index.html#true"); res = wc.getResponse(req);
 * assertEquals("Sample App", res.getTitle()); }
 * 
 * // I think this is failing from HtmlUnit not sending Accepts: headers
 * 
 * @Test(timeout = 10000) public void testRequestGzippedFile() throws Exception {
 * System.err.println("starting: RequestGzippedFile() throws Exception "); GetMethodWebRequest req = new
 * GetMethodWebRequest(base + "/gzipped/"); req.setHeaderField("Accept-Encoding", "gzip, deflate"); WebResponse res =
 * wc.getResponse(req); assertEquals("Sample App", res.getTitle()); req = new GetMethodWebRequest(base +
 * "/gzipped/index.html"); req.setHeaderField("Accept-Encoding", "gzip, deflate"); res = wc.getResponse(req);
 * assertEquals("Sample App", res.getTitle()); } }
 */
