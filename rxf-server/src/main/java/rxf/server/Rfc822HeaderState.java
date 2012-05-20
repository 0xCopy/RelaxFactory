package rxf.server;

import java.nio.ByteBuffer;
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
  private ByteBuffer cursor;
  private ByteBuffer headerBuf;
  private Map headerStrings;
  private Map<String, String> cookieStrings;
  private String methodProtocol;
  private String pathRescode;

  public Rfc822HeaderState(String... headers) {

    this.headers = headers;
  }

  public ByteBuffer getCursor() {
    return cursor;
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
    Arrays.sort( cookies);
    for (String cookie : headersNamed) {

      final String[] split = cookie.split("^[^=]*=", 2);
      final String tag = split[0];
                          if(Arrays.binarySearch(cookies, tag.trim())>0)
                          cookieStrings.put(tag,split[1]);
    }
    return  this;
  }

  public Rfc822HeaderState apply(ByteBuffer cursor) {
    if (!cursor.hasRemaining()) cursor.flip();
    final int anchor = cursor.position();
    final ByteBuffer slice1 = cursor.duplicate().slice();
    while (slice1.get() != ' ') ;
    methodProtocol = UTF8.decode((ByteBuffer) slice1.flip()).toString().trim();
    while (cursor.get() != ' ') ; //method/proto
    final ByteBuffer slice = cursor.slice();
    while (slice.get() != ' ') ;
    pathRescode = UTF8.decode((ByteBuffer) slice.flip()).toString().trim();
    headerBuf = null;
    final boolean wantsCookies = cookies.length > 1;
    final boolean wantsHeaders = wantsCookies || headers.length > 1;
    headerBuf = (ByteBuffer) moveCaretToDoubleEol(cursor).duplicate().flip();
    headerStrings = null;
    cookieStrings = null;
    if (wantsHeaders) {
      Map headerMap = HttpHeaders.getHeaders((ByteBuffer) headerBuf.rewind());
      headerStrings = new LinkedHashMap<String, List>();
      for (Object o : headerMap.keySet()) {
        headerStrings.put(o, headerMap.get(o));
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
}
