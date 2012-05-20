package rxf.server;

import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import one.xio.HttpHeaders;

import static one.xio.HttpMethod.UTF8;
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
public class Rfc822HeaderPrefix {
  private String[] headers;
  private String[] cookies;
  private ByteBuffer cursor;
  private ByteBuffer headerBuf;
  private Map headerStrings;
  private Map cookieStrings;
  private String rescode;

  public Rfc822HeaderPrefix(String... headers) {

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

  public Map getCookieStrings() {
    return cookieStrings;
  }

  public String getRescode() {
    return rescode;
  }

  public Rfc822HeaderPrefix cookies(String... cookies) {

    this.cookies = cookies;
    {
      cookieStrings = new LinkedHashMap();
      //todo something to parse cookies
      cookieStrings.put("", "");
    }
    return this;
  }

  public Rfc822HeaderPrefix apply(ByteBuffer cursor) {

    while (cursor.get() != ' ') ;
    final ByteBuffer slice = cursor.slice();
    while (slice.get() != ' ') ;
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
    rescode = UTF8.decode((ByteBuffer) slice.flip()).toString().trim();
    return this;
  }
}
