package rxf.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Date;

import static one.xio.HttpMethod.UTF8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class Rfc822HeaderStateTest {
	static Gson GSON1 = new GsonBuilder().setDateFormat(
			"EEEE, dd-MMM-yy HH:mm:ss zzz").create();
	static Gson GSON2 = new GsonBuilder().setDateFormat(
			"EEE, dd MMM yyyy HH:mm:ss zzz").create();
	static Gson GSON3 = new GsonBuilder().setDateFormat(
			"EEE MMM d HH:mm:ss yyyy").create();
	static Gson GSON4 = new GsonBuilder().setDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss Z").create();

	@Test
	public void testIfModifiedStuff() {
		String[] x = {"'sun, 11-sep-01 08:49:37 EST'",// ; RFC 850, obsoleted by RFC 1036
				"'Sun, 1 Nov 1994 08:49:37 GMT'",//  ; RFC 822, updated by RFC 1123
				"'Sun November  1 08:49:37 1994'",//   ; ANSI C's asctime() format
				"'2007-03-01T13:00:00 PST8PDT'",//iso ?
				"'2007-03-01T13:00:00.000 UTC'",//iso ?
				"'2007-02-8T13:17:00Z'",//iso ?
		};
		String[] check = {"Tue Sep 11 06:49:37 PDT 2001",
				"Tue Nov 01 00:49:37 PST 1994", "Tue Nov 01 08:49:37 PST 1994",
				"Thu Mar 01 13:00:00 PST 2007", "Thu Mar 01 05:00:00 PST 2007",
				"Thu Feb 08 05:17:00 PST 2007"};
		for (int i = 0; i < x.length; i++) {
			String s = x[i];
			System.err.println("testing: " + s);
			Date date = bruteForceParseIfModifiedSinceDate(s);
			System.err.println("product: " + date);
			assertEquals(date.getTime(), new Date(check[i]).getTime());
		}

	}

	private Date bruteForceParseIfModifiedSinceDate(String s) {
		System.err.println("java date: " + s);
		try {
			Date date = GSON1.fromJson(s, Date.class);
			System.err.println("success 1");
			return date;
		} catch (JsonSyntaxException e) {
			try {
				Date date = GSON2.fromJson(s, Date.class);
				System.err.println("success 2");
				return date;
			} catch (JsonSyntaxException e1) {
				try {
					Date date = GSON3.fromJson(s, Date.class);
					System.err.println("success 3");
					return date;
				} catch (JsonSyntaxException e2) {
					try {
						Date date = GSON4.fromJson(s, Date.class);
						System.err.println("success ISO");
						return date;
					} catch (JsonSyntaxException e3) {
						Date date = BlobAntiPatternObject.GSON.fromJson(s,
								Date.class);
						System.err.println("success ISO+ms --BAPO");
						return date;
					}
				}
			}
		}
	}

	@Test
	public void testAppendHeadersOne() {
		Rfc822HeaderState state = new Rfc822HeaderState("One");
		assertEquals(1, state.headerInterest().length);
		state.addHeaderInterest("Two");
		assertEquals(2, state.headerInterest().length);
		assertArrayEquals(new String[]{"One", "Two"}, state.headerInterest());
	}

	@Test
	public void testAppendHeadersMany() {
		Rfc822HeaderState state = new Rfc822HeaderState("One");
		assertEquals(1, state.headerInterest().length);
		state.addHeaderInterest("Two", "Three");
		assertEquals(3, state.headerInterest().length);
		assertArrayEquals(new String[]{"One", "Three", "Two",}, state
				.headerInterest());
	}

	@Test
	public void testAsRequestHeaderByteBuffer() {
		Rfc822HeaderState req = new Rfc822HeaderState();
		req.methodProtocol("VERB").pathResCode("/noun").protocolStatus(
				"HTTP/1.0").headerString("Header", "value").headerString(
				"Header2", "value2");
		ByteBuffer buf = req.asRequestHeaderByteBuffer();
		String result = UTF8.decode(buf.duplicate()).toString();

		assertEquals(
				"VERB /noun HTTP/1.0\r\nHeader: value\r\nHeader2: value2\r\n\r\n",
				result);
	}

	@Test
	public void testApplySimpleResponse() {
		ByteBuffer simpleResponse = ByteBuffer
				.wrap("HTTP/1.0 200 OK\r\nServer: NotReallyAServer\r\n\r\n"
						.getBytes());

		Rfc822HeaderState state = new Rfc822HeaderState();
		state.addHeaderInterest("Server");
		state.apply(simpleResponse);

		final String actual = state.methodProtocol();
		assertEquals("HTTP/1.0", actual);
		final String actual1 = state.pathResCode();
		assertEquals("200", actual1);
		final String actual2 = state.protocolStatus();
		assertEquals("OK", actual2);
		final String server = state.headerString("Server");
		assertEquals("NotReallyAServer", server);
	}

	@Test
	public void testApplySimpleRequest() {
		ByteBuffer simpleRequest = ByteBuffer
				.wrap("GET /file/from/path.suffix HTTP/1.0\r\nContent-Type: application/json\r\n\r\n"
						.getBytes());

		Rfc822HeaderState state = new Rfc822HeaderState("Content-Type");
		state.apply(simpleRequest);

		assertEquals("GET", state.methodProtocol());
		assertEquals("/file/from/path.suffix", state.pathResCode());
		assertEquals("HTTP/1.0", state.protocolStatus());
		assertEquals("application/json", state.headerString("Content-Type"));
	}

	@Test
	public void testAsResponseHeaderByteBuffer() {
		Rfc822HeaderState resp = new Rfc822HeaderState();
		resp.methodProtocol("HTTP/1.0").pathResCode("501").protocolStatus(
				"Unsupported Method").headerString("Connection", "close");
		ByteBuffer buf = resp.asResponseHeaderByteBuffer();
		String result = UTF8.decode(buf).toString();
		assertEquals(
				"HTTP/1.0 501 Unsupported Method\r\nConnection: close\r\n\r\n",
				result);
	}
}
