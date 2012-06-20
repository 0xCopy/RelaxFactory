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
import rxf.server.driver.CouchMetaDriver;

import static one.xio.HttpMethod.UTF8;
import static rxf.server.BlobAntiPatternObject.COOKIE;
import static rxf.server.BlobAntiPatternObject.moveCaretToDoubleEol;

/**
 * this is a utility class to parse a request header or
 * response header according to declared need of
 * header/cookies downstream.
 * <p/>
 * much of what is in {@link BlobAntiPatternObject} can
 * be teased into this class peicemeal.
 * <p/>
 * since java string parsing can be expensive and headerInterest
 * can be numerous this class is designed to parse only
 * what is necessary or typical and enable slower dynamic
 * grep operations to suit against a captured
 * {@link ByteBuffer} as needed (still cheap)
 * <p/>
 * preload headerInterest and cookies, send response
 * and request initial onRead for .apply()
 * <p/>
 * <p/>
 * <p/>
 * User: jim
 * Date: 5/19/12
 * Time: 10:00 PM
 */
public class Rfc822HeaderState {
  public boolean dirty;
  public String[] headerInterest;
  private String[] cookies = {};
  /**
   * the source route froom the active socket.
   * <p/>
   * this is necessary to look up {@link GeoIpService } queries among other things
   */
  private InetAddress sourceRoute;

  /**
   * stored buffer from which things are parsed and later grepped.
   */
  private ByteBuffer headerBuf;
  /**
   * parsed valued post-{@link #apply(java.nio.ByteBuffer)}
   */
  private Map<String, String> headerStrings;
  /**
   * parsed cookie values post-{@link #apply(java.nio.ByteBuffer)}
   */
  public Map<String, String> cookieStrings;
  /**
   * dual purpose HTTP protocol header token found on the first line of a request/response in the first position.
   * <p/>
   * contains either the method (request) or a the "HTTP/1.1" string (the protocol) on responses.
   * <p/>
   * user is responsible for populating this on outbound headerInterest
   */
  private String methodProtocol;

  /**
   * dual purpose HTTP protocol header token found on the first line of a request/response in the second position
   * <p/>
   * contains either the path (request) or a the numeric result code on responses.
   * <p/>
   * user is responsible for populating this on outbound headerInterest
   */
  private String pathRescode;

  /**
   * Dual purpose HTTP protocol header token found on the first line of a request/response in the third position.
   * <p/>
   * Contains either the protocol (request) or a status line message (response)
   */
  private String protocolStatus;
  /**
   * passed in on 0.0.0.0 dispatch to tie the header state to an nio object, to provide a socketchannel handle, and to lookup up the incoming source route
   */
  private SelectionKey sourceKey;
  /**
   * terminates header keys
   */
  public static final String PREFIX = ": ";

  /**
   * default ctor populates {@link #headerInterest}
   *
   * @param headerInterest keys placed in     {@link #headerInterest} which will be parsed on {@link #apply(java.nio.ByteBuffer)}
   */
  public Rfc822HeaderState(String... headerInterest) {

    this.headerInterest = headerInterest;
  }

  /**
   * assigns a state parser to a  {@link SelectionKey} and attempts to grab the source route froom the active socket.
   * <p/>
   * this is necessary to look up {@link GeoIpService } queries among other things
   *
   * @param key a NIO select key
   * @return self
   * @throws IOException
   */
  public Rfc822HeaderState sourceKey(SelectionKey key) throws IOException {
    sourceKey = key;
    SocketChannel channel = (SocketChannel) sourceKey.channel();
    sourceRoute = channel.socket().getInetAddress();
    return this;
  }

  /**
   * the actual {@link ByteBuffer} associated with the state.
   * <p/>
   * this buffer must start at position 0 in most cases requiring {@link   java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)}
   *
   * @return what is sent to {@link #apply(java.nio.ByteBuffer)}
   */

  public ByteBuffer headerBuf() {
    return headerBuf;
  }

  /**
   * header values which are pre-parsed during {@link #apply(java.nio.ByteBuffer)}.
   * <p/>
   * headerInterest in the request/response not so named in this list will be passed over.
   * <p/>
   * the value of a header appearing more than once is unspecified.
   * <p/>
   * multiple occuring headerInterest require {@link #getHeadersNamed(String)}
   *
   * @return the parsed values designated by the {@link #headerInterest} list of keys.  headerInterest present in {@link #headerInterest}
   *         not appearing in the {@link ByteBuffer} input will not be in this map.
   */
  public Map<String, String> getHeaderStrings() {
    return headerStrings;
  }

  /**
   * this is agrep of the full header state to find one or more headerInterest of a given name.
   * <p/>
   * todo: regex?
   *
   * @param header a header name
   * @return a list of values
   */
  List<String> getHeadersNamed(String header) {
    final ByteBuffer byteBuffer = headerBuf();
    List<String> ret;
    if (byteBuffer != null) {
      String decode = UTF8.decode((ByteBuffer) byteBuffer.rewind()).toString();

      String[] lines = decode.split("\n[^ \t]");
      Arrays.sort(lines);
      ArrayList<String> a = new ArrayList<String>();
      for (String line : lines) {
        boolean trigger = false;
        if (line.startsWith(PREFIX)) {
          trigger = a.add(line.substring(PREFIX.length()));
        } else {
          if (!trigger) break;
        }
      }
      ret = a;
    } else {
      ret = Arrays.asList();
    }
    return ret;
  }

  /**
   * fluent setter
   *
   * @param cookies a list of cookies registered to be auto-parsed
   * @return self
   */
  public Rfc822HeaderState cookies(String... cookies) {
    this.cookies = cookies;
    List<String> headersNamed = getHeadersNamed(COOKIE);
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

  /**
   * direction-agnostic RFC822 header state is mapped from a ByteBuffer with tolerance for HTTP method and results in the first line.
   * <p/>
   * {@link #headerInterest } contains a list of headerInterest that will be converted to a {@link Map} and available via {@link rxf.server.Rfc822HeaderState#getHeaderStrings()}
   * <p/>
   * {@link #cookies } contains a list of cookies from which to parse from Cookie header into {@link #cookieStrings}
   * <p/>
   * setting cookies for a response header is possible by setting {@link #dirty } to true and setting  {@link #cookieStrings} map values.
   * <p/>
   * currently this is  done inside of {@link rxf.server.web.inf.ProtocolMethodDispatch } surrounding {@link com.google.web.bindery.requestfactory.server.SimpleRequestProcessor#process(String)}
   *
   * @param cursor
   * @return this
   */
  public Rfc822HeaderState apply(ByteBuffer cursor) {
    if (!cursor.hasRemaining()) cursor.flip();
    int anchor = cursor.position();
    ByteBuffer slice = cursor.duplicate().slice();
    while (slice.hasRemaining() && ' ' != slice.get()) ;
    methodProtocol = UTF8.decode((ByteBuffer) slice.flip()).toString().trim();

    while (cursor.hasRemaining() && ' ' != cursor.get()) ; //method/proto
    slice = cursor.slice();
    while (slice.hasRemaining() && ' ' != slice.get()) ;
    pathRescode = UTF8.decode((ByteBuffer) slice.flip()).toString().trim();

    while (cursor.hasRemaining() && ' ' != cursor.get()) ;
    slice = cursor.slice();
    while (slice.hasRemaining() && '\n' != slice.get()) ;
    protocolStatus = UTF8.decode((ByteBuffer) slice.flip()).toString().trim();

    headerBuf = null;
    boolean wantsCookies = 0 < cookies().length;
    boolean wantsHeaders = wantsCookies || 0 < headerInterest.length;
    headerBuf = (ByteBuffer) moveCaretToDoubleEol(cursor).duplicate().flip();
    headerStrings = null;
    cookieStrings = null;
    if (wantsHeaders) {
      Map<String, int[]> headerMap = HttpHeaders.getHeaders((ByteBuffer) headerBuf.rewind());
      headerStrings = new LinkedHashMap<String, String>();
      for (String o : headerInterest) {
        int[] o1 = headerMap.get(o);
        if (null != o1)
          headerStrings.put(o, UTF8.decode((ByteBuffer) headerBuf.duplicate().clear().position(o1[0]).limit(o1[1])).toString().trim());
      }

    }
    return this;
  }

  public Rfc822HeaderState headers(String header) {
    String[] temp = new String[headerInterest.length + 1];
    System.arraycopy(this.headerInterest, 0, temp, 0, this.headerInterest.length);
    temp[this.headerInterest.length] = header;
    this.headerInterest = temp;
    return this;
  }

  /**
   * Appends to the list of header keys this parser is interested in mapping to strings.
   * <p/>
   * these headerInterest are mapped at cardinality<=1 when  {@link #apply(java.nio.ByteBuffer)}  }is called.
   * <p/>
   * for cardinality=>1  headerInterest {@link #getHeadersNamed(String)} is a pure grep over the entire ByteBuffer.
   * <p/>
   *
   * @param headers
   * @return
   * @see #getHeadersNamed(String)
   * @see #apply(java.nio.ByteBuffer)
   */
  public Rfc822HeaderState headerInterest(String... headers) {
    String[] temp = new String[headers.length + this.headerInterest.length];
    System.arraycopy(this.headerInterest, 0, temp, 0, this.headerInterest.length);
    System.arraycopy(headers, 0, temp, this.headerInterest.length, headers.length);
    this.headerInterest = temp;
    return this;
  }

  /**
   * @return
   * @see #dirty
   */
  public boolean dirty() {
    return dirty;
  }

  /**
   * indicate whether or not we want to rewrite the cookies and push SetCookie to client.  if this is set, the contents of {@link #cookieStrings} will be written each as cookies during {@link #asResponseHeaderByteBuffer()}
   *
   * @param dirty
   * @return
   */
  public Rfc822HeaderState dirty(boolean dirty) {
    this.dirty = dirty;
    return this;
  }

  /**
   * @return
   * @see #headerInterest
   */

  public String[] headers() {
    return headerInterest;
  }

  /**
   * @return
   * @see #cookies
   */
  public String[] cookies() {
    return cookies == null ? cookies = new String[0] : cookies;
  }

  /**
   * @return inet4 addr
   * @see #sourceRoute
   */
  public InetAddress sourceRoute() {
    return sourceRoute;
  }

  /**
   * this holds an inet address which may be inferred diuring {@link #sourceKey(java.nio.channels.SelectionKey)} as well as directly
   *
   * @param sourceRoute an internet ipv4 address
   * @return self
   */
  public Rfc822HeaderState sourceRoute(InetAddress sourceRoute) {
    this.sourceRoute = sourceRoute;
    return this;
  }

  /**
   * this is what has been sent to {@link #apply(java.nio.ByteBuffer)}.
   * <p/>
   * care must be taken to avoid {@link java.nio.ByteBuffer#compact()} during the handling of
   * the dst/cursor found in AsioVisitor code if this is sent in without a clean ByteBuffer.
   *
   * @param headerBuf an immutable  {@link  ByteBuffer}
   * @return self
   */
  public Rfc822HeaderState headerBuf(ByteBuffer headerBuf) {
    this.headerBuf = headerBuf;
    return this;
  }

  /**
   * holds the values parsed during {@link #apply(java.nio.ByteBuffer)} and holds the key-values created as headerInterest in
   * {@link #asRequestHeaderByteBuffer()} and {@link #asResponseHeaderByteBuffer()}
   *
   * @return
   */
  public Rfc822HeaderState headerStrings(Map<String, String> headerStrings) {
    this.headerStrings = headerStrings;
    return this;
  }

  /**
   * fluent lazy getter
   *
   * @return {@link #headerStrings}
   * @see #headerStrings
   */
  public Map<String, String> headerStrings() {
    return null == headerStrings ? headerStrings = new LinkedHashMap<String, String>() : headerStrings;
  }

  /**
   * fluent lazy getter
   *
   * @return
   * @see #cookieStrings
   */
  public Map<String, String> cookieStrings() {
    return null == cookieStrings ? cookieStrings = new LinkedHashMap<String, String>() : cookieStrings;
  }

  /**
   * fluent setter
   *
   * @param cookieStrings
   * @return self
   * @see #cookieStrings
   */

  public Rfc822HeaderState cookieStrings(Map<String, String> cookieStrings) {
    this.cookieStrings = cookieStrings;
    return this;
  }

  /**
   * @return
   * @see #methodProtocol
   */
  public String methodProtocol() {
    return methodProtocol;
  }

  /**
   * @return
   * @see #methodProtocol
   */
  public Rfc822HeaderState methodProtocol(String methodProtocol) {
    this.methodProtocol = methodProtocol;
    return this;
  }

  /**
   * dual purpose HTTP protocol header token found on the first line of a request/response in the second position
   * contains either the path (request) or a the numeric result code on responses.
   * user is responsible for populating this on outbound headerInterest
   *
   * @return
   * @see #pathRescode
   */

  public String pathResCode() {
    return pathRescode;
  }

  /**
   * @return
   * @see #pathRescode
   */
  public Rfc822HeaderState pathResCode(String pathRescode) {
    this.pathRescode = pathRescode;
    return this;
  }

  /**
   * Dual purpose HTTP protocol header token found on the first line of a request/response in the third position.
   * <p/>
   * Contains either the protocol (request) or a status line message (response)
   */
  public String protocolStatus() {
    return protocolStatus;
  }

  /**
   * @see Rfc822HeaderState#protocolStatus()
   */
  public Rfc822HeaderState protocolStatus(String protocolStatus) {
    this.protocolStatus = protocolStatus;
    return this;
  }

  /**
   * hard to explain what this does. very complicated.
   *
   * @return a string
   */
  @Override
  public String toString() {
    return "Rfc822HeaderState{" +
        "dirty=" + dirty +
        ", headerInterest=" + (null == headerInterest ? null : Arrays.asList(headerInterest)) +
        ", cookies=" + (null == cookies ? null : Arrays.asList(cookies)) +
        ", sourceRoute=" + sourceRoute +
        ", headerBuf=" + headerBuf +
        ", headerStrings=" + headerStrings +
        ", cookieStrings=" + cookieStrings +
        ", methodProtocol='" + methodProtocol + '\'' +
        ", pathResCode='" + pathRescode + '\'' +
        ", protocolStatus='" + protocolStatus + '\'' +
        '}';
  }


  /**
   * writes method, headersStrings, and cookieStrings to a {@link String } suitable for Response headerInterest
   * <p/>
   * populates headerInterest from {@link #headerStrings}
   * <p/>
   * if {@link #dirty} is set this will include SetCookie headerInterest (plural) one for each of {@link #cookieStrings()}
   *
   * @return http headerInterest for use with http 1.1
   */
  public String asResponseHeaderString() {
    String protocol = methodProtocol() + " " + pathResCode() + " " + protocolStatus() + "\r\n";
    for (Entry<String, String> stringStringEntry : headerStrings().entrySet()) {
      protocol += stringStringEntry.getKey() + ": " + stringStringEntry.getValue() + "\r\n";
    }
    for (Entry<String, String> stringStringEntry : cookieStrings().entrySet()) {
      protocol += CouchMetaDriver.SET_COOKIE + ": " + stringStringEntry.getKey() + "=" + stringStringEntry.getValue() + "\r\n";
    }

    protocol += "\r\n";
    return protocol;
  }

  /**
   * writes method, headersStrings, and cookieStrings to a {@link ByteBuffer} suitable for Response headerInterest
   * <p/>
   * populates headerInterest from {@link #headerStrings}
   * <p/>
   * if {@link #dirty} is set this will include SetCookie headerInterest (plural) one for each of {@link #cookieStrings()}
   *
   * @return http headerInterest for use with http 1.1
   */
  public ByteBuffer asResponseHeaderByteBuffer() {
    String protocol = asResponseHeaderString();
    return ByteBuffer.wrap(protocol.getBytes(HttpMethod.UTF8));
  }

  /**
   * writes method, headersStrings, and cookieStrings to a {@link String} suitable for RequestHeaders
   * <p/>
   * populates headerInterest from {@link #headerStrings}
   *
   * @return http headerInterest for use with http 1.1
   */
  public String asRequestHeaderString() {
    String protocol = methodProtocol() + " " + pathResCode() + " " + protocolStatus() + "\r\n";
    for (Entry<String, String> stringStringEntry : headerStrings().entrySet()) {
      protocol += stringStringEntry.getKey() + ": " + stringStringEntry.getValue() + "\r\n";
    }
    for (Entry<String, String> stringStringEntry : cookieStrings().entrySet()) {
      protocol += COOKIE + ": " + stringStringEntry.getKey() + "=" + stringStringEntry.getValue() + "\r\n";
    }

    protocol += "\r\n";
    return protocol;
  }

  /**
   * writes method, headersStrings, and cookieStrings to a {@link ByteBuffer} suitable for RequestHeaders
   * <p/>
   * populates headerInterest from {@link #headerStrings}
   *
   * @return http headerInterest for use with http 1.1
   */
  public ByteBuffer asRequestHeaderByteBuffer() {
    String protocol = asRequestHeaderString();
    return ByteBuffer.wrap(protocol.getBytes(HttpMethod.UTF8));
  }

  /**
   * utliity shortcut method to get the parsed value from the {@link #headerStrings} map
   *
   * @param headerKey name of a header presumed to be parsed during {@link #apply(java.nio.ByteBuffer)}
   * @return the parsed value from the {@link #headerStrings} map
   */
  public String headerString(String headerKey) {
    return headerStrings().get(headerKey); //To change body of created methods use File | Settings | File Templates.
  }

  /**
   * utility method to strip quotes off of things that makes couchdb choke
   *
   * @param headerKey name of a header
   * @return same string without quotes
   */
  public String dequotedHeader(String headerKey) {
    final String s = headerString(headerKey);
    return BlobAntiPatternObject.dequote(s);
  }

  /**
   * setter for a header (String)
   *
   * @param key headername
   * @param val header value
   * @return
   * @see #headerStrings
   */
  public Rfc822HeaderState headerString(String key, String val) {
    headerStrings().put(key, val);
    return this;
  }

  /**
   * @return the key
   * @see #sourceKey
   */
  public SelectionKey sourceKey() {
    return sourceKey;  //To change body of created methods use File | Settings | File Templates.
  }
}
