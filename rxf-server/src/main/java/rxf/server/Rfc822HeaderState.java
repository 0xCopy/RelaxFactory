package rxf.server;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.Map.Entry;

import one.xio.HttpHeaders;
import one.xio.HttpMethod;

import static one.xio.HttpMethod.UTF8;
import static rxf.server.BlobAntiPatternObject.COOKIE;
import static rxf.server.BlobAntiPatternObject.moveCaretToDoubleEol;

/**
 * long overdue utility class to parse a request header or response header according to declared need of header/cookie
 * <p/>
 * preload headers and cookies, send response and request initial onRead for .apply()
 * <p/>
 * User: jim
 * Date: 5/19/12
 * Time: 10:00 PM
 */
public class Rfc822HeaderState {
  public boolean dirty;
  public String[] headers = {};
  private String[] cookies = {};

  private InetAddress sourceRoute;


  private ByteBuffer headerBuf;
  private Map<String, String> headerStrings;
  private Map<String, String> cookieStrings;
  private String methodProtocol;
  private String pathRescode;

  public Rfc822HeaderState(String... headers) {

    this.headers = headers;
  }

  public Rfc822HeaderState sourceKey(SelectionKey key
  ) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    InetAddress inetAddress1 = channel.socket().getInetAddress();
    InetAddress inetAddress = sourceRoute = inetAddress1;
    return this;
  }

  public ByteBuffer headerBuf() {
    return headerBuf;
  }

  public Map<String, String> getHeaderStrings() {
    return headerStrings;
  }

  List<String> getHeadersNamed(String header) {
    String decode = UTF8.decode((ByteBuffer) headerBuf().rewind()).toString();
    String prefix = new StringBuilder().append(header).append(": ").toString();

    String[] lines = decode.split("\n[^ \t]");
    Arrays.sort(lines);
    ArrayList<String> a = new ArrayList<String>();
    for (String line : lines) {
      boolean trigger = false;
      if (line.startsWith(prefix))
        trigger = a.add(line.substring(prefix.length()));
      else if (!trigger) break;
    }
    return a;
  }


  public Rfc822HeaderState cookies(String... cookies) {
    this.cookies = cookies;
    List<? extends String> headersNamed = getHeadersNamed(COOKIE);
    cookieStrings = new LinkedHashMap<String, String>();
    Arrays.sort(cookies);
    for (String cookie : headersNamed) {

      for (String s : cookie.split(";")) {
        String[] split = s.split("^[^=]*=", 2);
        for (String s1 : split) {
          cookieStrings.put(split[0].trim(), split[1].trim());
        }
      }
    }
    return this;
  }

  public Rfc822HeaderState apply(ByteBuffer cursor) {
    if (!cursor.hasRemaining()) cursor.flip();
    int anchor = cursor.position();
    ByteBuffer slice1 = cursor.duplicate().slice();
    while (slice1.hasRemaining() && slice1.get() != ' ') ;
    methodProtocol = UTF8.decode((ByteBuffer) slice1.flip()).toString().trim();
    while (cursor.hasRemaining() && cursor.get() != ' ') ; //method/proto
    ByteBuffer slice = cursor.slice();
    while (slice.hasRemaining() && slice.get() != ' ') ;
    pathRescode = UTF8.decode((ByteBuffer) slice.flip()).toString().trim();
    headerBuf = null;
    boolean wantsCookies = cookies != null && cookies.length > 0;
    boolean wantsHeaders = wantsCookies || headers.length > 0;
    headerBuf = (ByteBuffer) moveCaretToDoubleEol(cursor).duplicate().flip();
    headerStrings = null;
    cookieStrings = null;
    if (wantsHeaders) {
      Map<String, int[]> headerMap = HttpHeaders.getHeaders((ByteBuffer) headerBuf.rewind());
      headerStrings = new LinkedHashMap<String, String>();
      for (String o : headers) {
        int[] o1 = headerMap.get(o);
        if (null != o1)
          headerStrings.put(o, UTF8.decode((ByteBuffer) headerBuf.duplicate().clear().position(o1[0]).limit(o1[1])).toString().trim());
      }

    }
    return this;
  }


  public Rfc822HeaderState headers(String... headers) {
    this.headers = headers;
    return this;
  }


  public boolean dirty() {
    return this.dirty;
  }

  public Rfc822HeaderState dirty(boolean dirty) {
    this.dirty = dirty;
    return this;
  }

  public String[] headers() {
    return this.headers;
  }

  public String[] cookies() {
    return this.cookies;
  }

  public InetAddress sourceRoute() {
    return this.sourceRoute;
  }

  public Rfc822HeaderState sourceRoute(InetAddress sourceRoute) {
    this.sourceRoute = sourceRoute;
    return this;
  }

  public Rfc822HeaderState headerBuf(ByteBuffer headerBuf) {
    this.headerBuf = headerBuf;
    return this;
  }

  public Rfc822HeaderState headerStrings(Map<String, String> headerStrings) {
    this.headerStrings = headerStrings;
    return this;
  }

  public Map<String, String> headerStrings() {
    return headerStrings;
  }

  public Map<String, String> cookieStrings() {
    return this.cookieStrings;
  }

  public Rfc822HeaderState cookieStrings(Map<String, String> cookieStrings) {
    this.cookieStrings = cookieStrings;
    return this;
  }

  public String methodProtocol() {
    return this.methodProtocol;
  }

  public Rfc822HeaderState methodProtocol(String methodProtocol) {
    this.methodProtocol = methodProtocol;
    return this;
  }

  public String pathRescode() {
    return this.pathRescode;
  }

  public Rfc822HeaderState pathResCode(String pathRescode) {
    this.pathRescode = pathRescode;
    return this;
  }

  @Override
  public String toString() {
    return "Rfc822HeaderState{" +
        "dirty=" + dirty +
        ", headers=" + (headers == null ? null : Arrays.asList(headers)) +
        ", cookies=" + (cookies == null ? null : Arrays.asList(cookies)) +
        ", sourceRoute=" + sourceRoute +
        ", headerBuf=" + headerBuf +
        ", headerStrings=" + headerStrings +
        ", cookieStrings=" + cookieStrings +
        ", methodProtocol='" + methodProtocol + '\'' +
        ", pathRescode='" + pathRescode + '\'' +
        '}';
  }

  /**
   * writes method, headersStrings, and cookieStrings to a string
   *
   * @return http headers for use with http 1.1
   */
  public ByteBuffer asRequestHeaders() {
    String s = methodProtocol() + " " + pathRescode() + " HTTP/1.1\r\n";
    if (null != headerStrings && !headerStrings.isEmpty())
      for (Entry<String, String> stringStringEntry : headerStrings().entrySet()) {
        s += stringStringEntry.getKey() + ": " + stringStringEntry.getValue() + "\r\n";
      }
    if (null != cookieStrings && !cookieStrings.isEmpty())

      for (Entry<String, String> stringStringEntry : cookieStrings.entrySet()) {
        s += COOKIE + ": " + stringStringEntry.getKey() + "=" + stringStringEntry.getValue() + "\r\n";
      }

    s += "\r\n";
    return ByteBuffer.wrap(s.getBytes(HttpMethod.UTF8));
  }

  public String headerString(String key) {
    return headerStrings().get(key); //To change body of created methods use File | Settings | File Templates.
  }
}
