package rxf.server;

import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.List;

import static junit.framework.Assert.assertEquals;

public class Rfc822HeaderStateTest {
  static {
    BlobAntiPatternObject.setDEBUG_SENDJSON(true);
  }

  public static final String TMID_8627085284984078588 = "_tmid=8627085284984078588";
  public static final String FBSTUFF1 =
      "datr=byr_UBMy1etuo2RL3W6RgjP0; reg_fb_gate=https%3A%2F%2Fwww.facebook.com%2F; reg_fb_ref=https%3A%2F%2Fwww.facebook.com%2F; highContrast=0; wd=1680x395";
    public static final String CP_FACEBOOK_DOES_NOT_HAVE_A_P3_P_POLICY_LEARN_WHY_HERE_HTTP_FB_ME_P3P = "CP=\"Facebook does\n not have a\n P3P policy.\n Learn why\n here:\n http://fb.me/p3p\"";
    String H1 = "POST /StreamReceiver/services HTTP/1.1\n"
      + "Host: inplay-rcv02.scanscout.com\n"
      + "User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:18.0) Gecko/20100101 Firefox/18.0 FirePHP/0.7.1\n"
      + "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\n"
      + "Accept-Language: en-US,en;q=0.5\n" + "Accept-Encoding: gzip, deflate\n" + "DNT: 1\n"
      + "Cookie: " + TMID_8627085284984078588 + "\n" + "x-insight: activate\n"
      + "Connection: keep-alive\n",
      H2 = "GET / HTTP/1.1\n"
          + "Host: www.facebook.com\n"
          + "User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:18.0) Gecko/20100101 Firefox/18.0 FirePHP/0.7.1\n"
          + "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\n"
          + "Accept-Language: en-US,en;q=0.5\n" + "Accept-Encoding: gzip, deflate\n" + "DNT: 1\n"
          + "Cookie: " + FBSTUFF1 + "\n" + "x-insight: activate\n" + "Connection: keep-alive\n"
          + "Cookie: " + TMID_8627085284984078588 + "\n" + "Cache-Control: max-age=0\n" + "\n";
  String H3 = "HTTP/1.1 200 OK\n"
      + "Cache-Control: private, no-cache, no-store, must-revalidate\n"
      + "Content-Encoding: gzip\n"
      + "Content-Type: text/html; charset=utf-8\n"
      + "Date: Wed, 23 Jan 2013 00:10:39 GMT\n"
      + "Expires: Sat, 01 Jan 2000 00:00:00 GMT\n"
      + "P3P: " + CP_FACEBOOK_DOES_NOT_HAVE_A_P3_P_POLICY_LEARN_WHY_HERE_HTTP_FB_ME_P3P + "\n"
      + "Pragma: no-cache\n"
      + "Set-Cookie: highContrast=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; path=/; domain=.facebook.com; httponly\n"
      + "reg_ext_ref=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; path=/; domain=.facebook.com\n"
      + "wd=deleted; expires=Thu, 01-Jan-1970 00:00:01 GMT; path=/; domain=.facebook.com; httponly\n"
      + "X-Content-Type-Options: nosniff\n" + "X-Frame-Options: DENY\n"
      + "X-XSS-Protection: 1; mode=block\n"
      + "X-FB-Debug: AyxIiMHG0EA+jrikSqkbs4GufvtsEsvtwdICkt4496U=\n" + "X-Firefox-Spdy: 2\n" + "\n",
      H4 = "POST /StreamReceiver/services HTTP/1.1\n"
          + "Host: inplay-rcv02.scanscout.com\n"
          + "User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:18.0) Gecko/20100101 Firefox/18.0 FirePHP/0.7.1\n"
          + "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8\n"
          + "Accept-Language: en-US,en;q=0.5\n" + "Accept-Encoding: gzip, deflate\n" + "DNT: 1\n"
          + "Cookie: _tmid=8627085284984088588\n" + "x-insight: activate\n"
          + "Connection: keep-alive\n";

  @Test
  public void testAppendHeadersOne() {
    Rfc822HeaderState state = new Rfc822HeaderState("One");
    assertEquals(1, state.headerInterest().length);
    state.addHeaderInterest("Two");
    assertEquals(2, state.headerInterest().length);
    String[] expecteds = {"One", "Two"};
    Assert.assertArrayEquals(expecteds, state.headerInterest());
  }

  @Test
  public void testAppendHeadersMany() {
    Rfc822HeaderState state = new Rfc822HeaderState("One");
    assertEquals(1, state.headerInterest().length);
    state.addHeaderInterest("Two", "Three");
    assertEquals(3, state.headerInterest().length);
    String[] expecteds = {"One", "Three", "Two",};
    Assert.assertArrayEquals(expecteds, state.headerInterest());
  }

  @Test
  public void testAsRequestHeaderByteBuffer() {
    Rfc822HeaderState req = new Rfc822HeaderState();
    req.methodProtocol("VERB").pathResCode("/noun").protocolStatus("HTTP/1.0").headerString(
        "Header", "value").headerString("Header2", "value2");
    ByteBuffer buf = req.asRequestHeaderByteBuffer();
    String result = HttpMethod.UTF8.decode(buf.duplicate()).toString();

    assertEquals("VERB /noun HTTP/1.0\r\nHeader: value\r\nHeader2: value2\r\n\r\n", result);
  }

  @Test
  public void testApplySimpleResponse() {
    ByteBuffer simpleResponse =
        ByteBuffer.wrap("HTTP/1.0 200 OK\r\nServer: NotReallyAServer\r\n\r\n".getBytes());

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
    ByteBuffer simpleRequest =
        ByteBuffer
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
    resp.methodProtocol("HTTP/1.0").pathResCode("501").protocolStatus("Unsupported Method")
        .headerString("Connection", "close");
    ByteBuffer buf = resp.asResponseHeaderByteBuffer();
    String result = HttpMethod.UTF8.decode(buf).toString();
    assertEquals("HTTP/1.0 501 Unsupported Method\r\nConnection: close\r\n\r\n", result);
  }

  @Test
  public void testSimpleHeader() {
    ByteBuffer buf = (ByteBuffer) HttpMethod.UTF8.encode(H1).rewind();
    buf.toString();

    Rfc822HeaderState apply = ActionBuilder.get().state().apply(buf);
    List<String> headersNamed = apply.$req().getHeadersNamed(HttpHeaders.Cookie);
    assertEquals(TMID_8627085284984078588, headersNamed.iterator().next());

  }

  @Test
  public void testMultiHeader() {
    ByteBuffer buf = (ByteBuffer) HttpMethod.UTF8.encode(H2).rewind();
    buf.toString();

    Rfc822HeaderState apply = ActionBuilder.get().state().apply(buf);
    List<String> headersNamed = apply.$req().getHeadersNamed(HttpHeaders.Cookie);
    Iterator<String> iterator = headersNamed.iterator();
    assertEquals(TMID_8627085284984078588, iterator.next());
    assertEquals(FBSTUFF1, iterator.next());
  }

  @Test
  public void testHeaderLineContinuations() {
    ByteBuffer buf = (ByteBuffer) HttpMethod.UTF8.encode(H3).rewind();
    buf.toString();

    Rfc822HeaderState apply = ActionBuilder.get().state().apply(buf);
    List<String> headersNamed = apply.$req().getHeadersNamed("P3P");
    Iterator<String> iterator = headersNamed.iterator();
    assertEquals(CP_FACEBOOK_DOES_NOT_HAVE_A_P3_P_POLICY_LEARN_WHY_HERE_HTTP_FB_ME_P3P, iterator.next());
  }
}
