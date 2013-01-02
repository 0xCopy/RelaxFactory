package rxf.server.web.inf;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rxf.server.BlobAntiPatternObject;
import rxf.server.DateHeaderParser;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ContentRootImplTest {
	private static final String host = "localhost";
	public static String base;
	public static WebConversation wc;
	private static int port;
	private static ScheduledExecutorService exec;
	private static ServerSocketChannel serverSocketChannel;

	@BeforeClass
	static public void setUp() throws Exception {
		wc = new WebConversation();

		BlobAntiPatternObject.DEBUG_SENDJSON = true;
		HttpMethod.killswitch = false;

		serverSocketChannel = ServerSocketChannel.open();
		final InetSocketAddress serverSocket = new InetSocketAddress(host, 0);
		serverSocketChannel.socket().bind(serverSocket);
		port = serverSocketChannel.socket().getLocalPort();
		serverSocketChannel.configureBlocking(false);

		exec = Executors.newScheduledThreadPool(2);
		exec.submit(new Runnable() {
			public void run() {
				AsioVisitor topLevel = new ProtocolMethodDispatch();
				try {

					HttpMethod
							.enqueue(serverSocketChannel, OP_ACCEPT, topLevel);
					HttpMethod.init(topLevel/*, 1000*/);

				} catch (Exception e) {
					System.out.println("failed startup");
				}
			}
		});
		base = "http://localhost:" + port;
	}

	@AfterClass
	static public void tearDown() throws Exception {
		try {
			HttpMethod.killswitch = true;
			HttpMethod.getSelector().close();
			serverSocketChannel.close();

			exec.shutdown();
		} catch (Exception ignore) {
			fail(ignore.getMessage());
		}
	}

	@Test
	public void testRequestSlash() throws Exception {
		GetMethodWebRequest req = new GetMethodWebRequest(base + '/');
		WebResponse res = wc.getResponse(req);
		assertEquals("Sample App", res.getTitle());
	}

	@Test
	public void testRequestIndexHtml() throws Exception {

		GetMethodWebRequest req = new GetMethodWebRequest(base + "/index.html");
		WebResponse res = wc.getResponse(req);
		assertEquals("Sample App", res.getTitle());
	}

	@Test
	public void testRequest304() throws Exception {
		final String oldDate = DateHeaderParser.formatHttpHeaderDate(new Date(
				"Jan 1 1990"));

		String url = "http://localhost:" + port + "/index.html";

		GetMethodWebRequest req = new GetMethodWebRequest(url);
		/*fail?       */req.setHeaderField(HttpHeaders.If$2dModified$2dSince
				.getHeader(), oldDate);
		// fai;l?        req.getHeaders().put(HttpHeaders.If$2dModified$2dSince.getHeader(), oldDate);
		//        wc.setHeaderField(HttpHeaders.If$2dModified$2dSince.getHeader(), oldDate);

		WebResponse res = wc.getResponse(req);
		int responseCode = res.getResponseCode();
		res.close();
		assertEquals(responseCode, 304);
	}

	@Test
	public void testRequestFileWithQuerystring() throws Exception {
		GetMethodWebRequest req = new GetMethodWebRequest(base
				+ "/?some=params&others=true");
		WebResponse res = wc.getResponse(req);
		assertEquals("Sample App", res.getTitle());
		req = new GetMethodWebRequest(base
				+ "/index.html?some=params&others=true");
		res = wc.getResponse(req);
		assertEquals("Sample App", res.getTitle());

	}

	@Test
	public void testRequestFileWithFragment() throws Exception {
		GetMethodWebRequest req = new GetMethodWebRequest(base + "/#true");
		WebResponse res = wc.getResponse(req);
		assertEquals("Sample App", res.getTitle());
		req = new GetMethodWebRequest(base + "/index.html#true");
		res = wc.getResponse(req);
		assertEquals("Sample App", res.getTitle());

	}

	//I think this is failing from HtmlUnit not sending Accepts: headers
	@Test
	public void testRequestGzippedFile() throws Exception {
		GetMethodWebRequest req = new GetMethodWebRequest(base + "/gzipped/");
		req.setHeaderField("Accept-Encoding", "gzip, deflate");
		WebResponse res = wc.getResponse(req);
		assertEquals("Sample App", res.getTitle());
		req = new GetMethodWebRequest(base + "/gzipped/index.html");
		req.setHeaderField("Accept-Encoding", "gzip, deflate");
		res = wc.getResponse(req);
		assertEquals("Sample App", res.getTitle());
	}

}