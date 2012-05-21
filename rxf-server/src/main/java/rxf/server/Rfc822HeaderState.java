package rxf.server;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import one.xio.HttpHeaders;

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
  private String[] headers = {};
  private String[] cookies = {};
  private InetAddress sourceRoute;

  @Override
  public String toString() {
    return "Rfc822HeaderState{" +
        "headers=" + (headers == null ? null : Arrays.asList(headers)) +
        ", cookies=" + (cookies == null ? null : Arrays.asList(cookies)) +
        ", sourceRoute=" + sourceRoute +
        ", headerBuf=" + headerBuf +
        ", headerStrings=" + headerStrings +
        ", cookieStrings=" + cookieStrings +
        ", methodProtocol='" + methodProtocol + '\'' +
        ", pathRescode='" + pathRescode + '\'' +
        '}';
  }

  private ByteBuffer headerBuf;
  private Map<String, String> headerStrings;
  private Map<String, String> cookieStrings;
  private String methodProtocol;
  private String pathRescode;

  public Rfc822HeaderState(String... headers) {

    this.headers = headers;
  }

  public Rfc822HeaderState  sourceKey(SelectionKey key
  ) throws IOException {
    final SocketChannel channel = (SocketChannel) key.channel();
    final InetAddress inetAddress1 = channel.socket().getInetAddress();
    final InetAddress inetAddress = sourceRoute = inetAddress1;
    return this;
  }

  public ByteBuffer getHeaderBuf() {
    return headerBuf;
  }

  public Map getHeaderStrings() {
    return headerStrings;
  }

  public Map<String, String> getCookieStrings() {
    return cookieStrings;
  }

  public String getPathRescode() {
    return pathRescode;
  }

  List<String> getHeadersNamed(String header) {
    final String decode = UTF8.decode((ByteBuffer) getHeaderBuf().rewind()).toString();
    final String prefix = new StringBuilder().append(header).append(": ").toString();

    final String[] lines = decode.split("\n[^ \t]");
    Arrays.sort(lines);
    final ArrayList<String> a = new ArrayList<String>();
    for (String line : lines) {
      boolean trigger = false;
      if (line.startsWith(prefix))
        trigger = a.add(line.substring(prefix.length()));
      else if (!trigger) break;
    }
    return a;
  }


  public Rfc822HeaderState cookies(String... cookies) {
    final List<? extends String> headersNamed = getHeadersNamed(COOKIE);
    cookieStrings = new LinkedHashMap<String, String>();
    Arrays.sort(cookies);
    for (String cookie : headersNamed) {

      final String[] split = cookie.split("^[^=]*=", 2);
      final String tag = split[0];
      if (Arrays.binarySearch(cookies, tag.trim()) > 0)
        cookieStrings.put(tag, split[1]);
    }
    return this;
  }

  public Rfc822HeaderState apply(ByteBuffer cursor) {
    if (!cursor.hasRemaining()) cursor.flip();
    final int anchor = cursor.position();
    final ByteBuffer slice1 = cursor.duplicate().slice();
    while (slice1.hasRemaining() && slice1.get() != ' ') ;
    methodProtocol = UTF8.decode((ByteBuffer) slice1.flip()).toString().trim();
    while (cursor.hasRemaining() && cursor.get() != ' ') ; //method/proto
    final ByteBuffer slice = cursor.slice();
    while (slice.hasRemaining() && slice.get() != ' ') ;
    pathRescode = UTF8.decode((ByteBuffer) slice.flip()).toString().trim();
    headerBuf = null;
    final boolean wantsCookies = cookies != null && cookies.length > 0;
    final boolean wantsHeaders = wantsCookies || headers.length > 0;
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

  public String getMethodProtocol() {
    return methodProtocol;
  }

  public void setMethodProtocol(String methodProtocol) {
    this.methodProtocol = methodProtocol;
  }

  public InetAddress getSourceRoute() {
    return sourceRoute;
  }

}
