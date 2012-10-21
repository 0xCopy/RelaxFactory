package rxf.server;

import static java.nio.channels.SelectionKey.OP_ACCEPT;

import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import junit.framework.TestCase;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import rxf.server.web.inf.ProtocolMethodDispatch;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

public class WebServerTest extends TestCase {
	private static final String host = "localhost";
	private static final int port = 8080;

	private ScheduledExecutorService exec;
	private ServerSocketChannel serverSocketChannel;
	private WebClient webClient;

	public void setUp() throws Exception {
		webClient = new WebClient(BrowserVersion.FIREFOX_3);
		webClient.setJavaScriptEnabled(true);
		webClient.setThrowExceptionOnFailingStatusCode(true);
		webClient.setThrowExceptionOnScriptError(true);

		BlobAntiPatternObject.DEBUG_SENDJSON = true;
		HttpMethod.killswitch = false;
		exec = Executors.newScheduledThreadPool(2);
		exec.submit(new Runnable() {
			public void run() {
				AsioVisitor topLevel = new ProtocolMethodDispatch();
				try {
					serverSocketChannel = ServerSocketChannel.open();
					serverSocketChannel.socket().bind(
							new InetSocketAddress(host, port));
					serverSocketChannel.configureBlocking(false);

					HttpMethod
							.enqueue(serverSocketChannel, OP_ACCEPT, topLevel);
					HttpMethod.init(topLevel/*, 1000*/);

				} catch (Exception e) {
					System.out.println("failed startup");
				}
			}
		});
	}

	public void tearDown() throws Exception {
		try {
			HttpMethod.killswitch = true;
			HttpMethod.getSelector().close();
			serverSocketChannel.close();

			exec.shutdown();
		} catch (Exception ignore) {
			fail(ignore.getMessage());
		}
	}

	public void testRequestSlash() throws Exception {
		HtmlPage page = webClient.getPage("http://localhost:8080/");
		assertEquals("Sample App", page.getTitleText());
	}

	public void testRequestIndexHtml() throws Exception {
		HtmlPage page = webClient.getPage("http://localhost:8080/index.html");
		assertEquals("Sample App", page.getTitleText());
	}

	public void testRequestFileWithQuerystring() throws Exception {
		HtmlPage page = webClient
				.getPage("http://localhost:8080/?some=params&others=true");

		assertEquals("Sample App", page.getTitleText());
		HtmlPage page2 = webClient
				.getPage("http://localhost:8080/index.html?some=params&others=true");
		assertEquals("Sample App", page2.getTitleText());
	}

	public void testRequestFileWithFragment() throws Exception {
		HtmlPage page = webClient
				.getPage("http://localhost:8080/#startSomewhere");

		assertEquals("Sample App", page.getTitleText());
		HtmlPage page2 = webClient
				.getPage("http://localhost:8080/index.html#startSomewhere");
		assertEquals("Sample App", page2.getTitleText());
	}

}