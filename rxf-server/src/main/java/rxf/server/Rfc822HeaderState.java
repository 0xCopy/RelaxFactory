package rxf.server;

import com.google.web.bindery.requestfactory.server.SimpleRequestProcessor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import one.xio.HttpStatus;
import rxf.server.web.inf.ProtocolMethodDispatch;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Math.abs;
import static one.xio.HttpHeaders.Cookie;
import static one.xio.HttpHeaders.Set$2dCookie;
import static one.xio.HttpMethod.UTF8;

/**
 * this is a utility class to parse a HttpRequest header or
 * $res header according to declared need of
 * header/cookies downstream.
 * <p/>
 * much of what is in {@link BlobAntiPatternObject} can
 * be teased into this class peicemeal.
 * <p/>
 * since java string parsing can be expensive and addHeaderInterest
 * can be numerous this class is designed to parse only
 * what is necessary or typical and enable slower dynamic
 * grep operations to suit against a captured
 * {@link ByteBuffer} as needed (still cheap)
 * <p/>
 * preload addHeaderInterest and cookies, send $res
 * and HttpRequest initial onRead for .apply()
 * <p/>
 * <p/>
 * <p/>
 * User: jim
 * Date: 5/19/12
 * Time: 10:00 PM
 */
public class Rfc822HeaderState {

    public static final String[] EMPTY = new String[0];

    public String headerString(HttpHeaders httpHeader) {
        return headerString(httpHeader.getHeader()); //To change body of created methods use File | Settings | File Templates.
    }

    @SuppressWarnings({"RedundantCast"})
    public static class HttpRequest extends Rfc822HeaderState {
        public HttpRequest(Rfc822HeaderState proto) {
            super(proto);
            String protocol = protocol();
            if (null != protocol && !protocol.startsWith("HTTP"))
                protocol(null);
        }

        public String method() {
            return methodProtocol(); //To change body of overridden methods use File | Settings | File Templates.
        }

        public HttpRequest method(HttpMethod method) {
            return method(method.name()); //To change body of overridden methods use File | Settings | File Templates.
        }

        public HttpRequest method(String s) {
            return (HttpRequest) methodProtocol(s);
        }

        public String path() {
            return pathResCode(); //To change body of overridden methods use File | Settings | File Templates.
        }

        public HttpRequest path(String path) {
            return (HttpRequest) pathResCode(path);
        }

        public String protocol() {
            return protocolStatus(); //To change body of overridden methods use File | Settings | File Templates.
        }

        public HttpRequest protocol(String protocol) {
            return (HttpRequest) protocolStatus(protocol); //To change body of overridden methods use File | Settings | File Templates.
        }

        @Override
        public String toString() {
            return asRequestHeaderString();
        }

        public <T> T as(Class<T> clazz) {
            if (ByteBuffer.class.equals(clazz)) {
                if (null == protocol())
                    protocol("HTTP/1.1");
                return (T) asRequestHeaderByteBuffer();
            }
            return (T) super.as(clazz);
        }

    }

    @SuppressWarnings({"RedundantCast"})
    public static class HttpResponse extends Rfc822HeaderState {

        public HttpResponse(Rfc822HeaderState proto) {
            super(proto);
            String protocol = protocol();
            if (null != protocol && !protocol.startsWith("HTTP"))
                protocol(null);
        }

        public HttpStatus statusEnum() {
            try {
                return HttpStatus.valueOf('$' + resCode());
            } catch (Exception e) {
                e.printStackTrace(); //todo: verify for a purpose
            }
            return null;
        }

        @Override
        public String toString() {
            return asResponseHeaderString();
        }

        public String protocol() {
            return methodProtocol();
        }

        public String resCode() {
            return pathResCode();
        }

        public String status() {
            return protocolStatus();
        }

        public HttpResponse protocol(String protocol) {
            return (HttpResponse) methodProtocol(protocol);
        }

        public HttpResponse resCode(String res) {
            return (HttpResponse) pathResCode(res);
        }

        public HttpResponse resCode(HttpStatus resCode) {
            return (HttpResponse) pathResCode(resCode.name().substring(1));
        }

        public HttpResponse status(String status) {
            return (HttpResponse) protocolStatus(status);
        }

        /**
         * convenience method ^2 -- sets rescode and status captions from same enum
         *
         * @param httpStatus
         * @return
         */
        public HttpResponse status(HttpStatus httpStatus) {
            return ((HttpResponse) protocolStatus(httpStatus.caption))
                    .resCode(httpStatus);
        }

        @Override
        public <T> T as(Class<T> clazz) {
            if (ByteBuffer.class.equals(clazz)) {
                if (null == protocol()) {
                    protocol("HTTP/1.1");
                }
                return (T) asResponseHeaderByteBuffer();
            }
            return super.as(clazz); //To change body of overridden methods use File | Settings | File Templates.

        }
    }

    /**
     * simple wrapper for HttpRequest setters
     */
    public HttpRequest $req() {
        return HttpRequest.class == this.getClass()
                ? (HttpRequest) this
                : new HttpRequest(this);
    }

    /**
     * simple wrapper for HttpRequest setters
     */
    public HttpResponse $res() {
        return HttpResponse.class == this.getClass()
                ? (HttpResponse) this
                : new HttpResponse(this);
    }

    public <T> T as(Class<T> clazz) {
        if (HttpResponse.class.equals(clazz)) {
            return (T) $res();

        } else if (HttpRequest.class.equals(clazz)) {
            return (T) $req();
        } else if (String.class.equals(clazz)) {
            return (T) toString();
        }
        if (ByteBuffer.class.equals(clazz))
            throw new UnsupportedOperationException(
                    "must promote to as((HttpRequest|HttpResponse)).class first");
        throw new UnsupportedOperationException("don't know how to infer "
                + clazz.getCanonicalName());

    }

    /**
     * copy ctor
     * <p/>
     * jrn: moved most things to atomic state soas to provide letter-envelope abstraction without
     * undue array[1] members to do the same thing.
     *
     * @param proto the original Rfc822HeaderState
     */
    public Rfc822HeaderState(Rfc822HeaderState proto) {
        cookies = proto.cookies;
        cookieStrings = proto.cookieStrings;
        dirty = proto.dirty;
        headerBuf = proto.headerBuf;
        headerInterest = proto.headerInterest;
        headerStrings = proto.headerStrings;
        methodProtocol = proto.methodProtocol;
        pathRescode = proto.pathRescode;
        //this.PREFIX                =proto.PREFIX                       ;
        protocolStatus = proto.protocolStatus;
        sourceKey = proto.sourceKey;
        sourceRoute = proto.sourceRoute;
    }

    public AtomicBoolean dirty = new AtomicBoolean();
    public AtomicReference<String[]> headerInterest = new AtomicReference<>(
            EMPTY),
            cookies = new AtomicReference<>(EMPTY);
    /**
     * the source route from the active socket.
     * <p/>
     * this is necessary to look up  GeoIpService queries among other things
     */
    private AtomicReference<InetAddress> sourceRoute = new AtomicReference<>();

    /**
     * stored buffer from which things are parsed and later grepped.
     * <p/>
     * NOT atomic.
     */
    private ByteBuffer headerBuf;
    /**
     * parsed valued post-{@link #apply(ByteBuffer)}
     */
    private AtomicReference<Map<String, String>> headerStrings = new AtomicReference<>();
    /**
     * parsed cookie values post-{@link #apply(ByteBuffer)}
     */
    public AtomicReference<Map<String, String>> cookieStrings = new AtomicReference<>();
    /**
     * dual purpose HTTP protocol header token found on the first line of a HttpRequest/$res in the first position.
     * <p/>
     * contains either the method (HttpRequest) or a the "HTTP/1.1" string (the protocol) on responses.
     * <p/>
     * user is responsible for populating this on outbound addHeaderInterest
     */
    private AtomicReference<String> methodProtocol = new AtomicReference<>();

    /**
     * dual purpose HTTP protocol header token found on the first line of a HttpRequest/$res in the second position
     * <p/>
     * contains either the path (HttpRequest) or a the numeric result code on responses.
     * <p/>
     * user is responsible for populating this on outbound addHeaderInterest
     */
    private AtomicReference<String> pathRescode = new AtomicReference<>();

    /**
     * Dual purpose HTTP protocol header token found on the first line of a HttpRequest/$res in the third position.
     * <p/>
     * Contains either the protocol (HttpRequest) or a status line message ($res)
     */
    private AtomicReference<String> protocolStatus = new AtomicReference<>();
    /**
     * passed in on 0.0.0.0 dispatch to tie the header state to an nio object, to provide a socketchannel handle, and to lookup up the incoming source route
     */
    private AtomicReference<SelectionKey> sourceKey = new AtomicReference<>();
    /**
     * terminates header keys
     */
    public static final String PREFIX = ": ";

    public Rfc822HeaderState headerString(HttpHeaders hdrEnum, String s) {
        return headerString(hdrEnum.getHeader().trim(), s); //To change body of created methods use File | Settings | File Templates.
    }

    /**
     * default ctor populates {@link #headerInterest}
     *
     * @param headerInterest keys placed in     {@link #headerInterest} which will be parsed on {@link #apply(ByteBuffer)}
     */
    public Rfc822HeaderState(String... headerInterest) {

        this.headerInterest.set(headerInterest);
    }

    /**
     * assigns a state parser to a  {@link SelectionKey} and attempts to grab the source route froom the active socket.
     * <p/>
     * this is necessary to look up GeoIpService queries among other things
     *
     * @param key a NIO select key
     * @return self
     * @throws IOException
     */
    public Rfc822HeaderState sourceKey(SelectionKey key) throws IOException {
        sourceKey.set(key);
        SocketChannel channel = (SocketChannel) sourceKey.get().channel();
        sourceRoute.set(channel.socket().getInetAddress());
        return this;
    }

    /**
     * the actual {@link ByteBuffer} associated with the state.
     * <p/>
     * this buffer must start at position 0 in most cases requiring {@link   ReadableByteChannel#read(ByteBuffer)}
     *
     * @return what is sent to {@link #apply(ByteBuffer)}
     */

    public ByteBuffer headerBuf() {
        return headerBuf;
    }

    /**
     * header values which are pre-parsed during {@link #apply(ByteBuffer)}.
     * <p/>
     * addHeaderInterest in the HttpRequest/HttpResponse not so named in this list will be passed over.
     * <p/>
     * the value of a header appearing more than once is unspecified.
     * <p/>
     * multiple occuring addHeaderInterest require {@link #getHeadersNamed(String)}
     *
     * @return the parsed values designated by the {@link #headerInterest} list of keys.  addHeaderInterest present in {@link #headerInterest}
     *         not appearing in the {@link ByteBuffer} input will not be in this map.
     */
    public Map<String, String> getHeaderStrings() {
        return headerStrings.get();
    }

    /**
     * this is agrep of the full header state to find one or more addHeaderInterest of a given name.
     * <p/>
     *
     * @param header a header name
     * @return a list of values
     */
    List<String> getHeadersNamed(String header) {
        ByteBuffer byteBuffer = headerBuf();
        List<String> ret;
        if (null == byteBuffer) {
            ret = Arrays.asList();
        } else {
            String decode = UTF8.decode((ByteBuffer) byteBuffer.rewind())
                    .toString();

            String[] lines = decode.split("\n[^ \t]");
            Arrays.sort(lines);
            ArrayList<String> a = new ArrayList<>();
            String s = header + PREFIX;
            for (String line : lines) {
                boolean added = false;

                if (line.startsWith(s)) {
                    added = a.add(line.substring(s.length()));
                } else {
                    if (added)
                        break;
                }
            }
            ret = a;
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
        this.cookies.set(cookies);
        List<String> headersNamed = getHeadersNamed(Cookie.getHeader());
        cookieStrings.set(new LinkedHashMap<String, String>());
        Arrays.sort(cookies);
        for (String cookie : headersNamed)
            for (String s : cookie.split(";")) {
                String[] split = s.split("^[^=]*=", 2);
                /*for (String ignored : split) */
                cookieStrings.get().put(split[0].trim(), split[1].trim());

            }
        return this;
    }

    /**
     * direction-agnostic RFC822 header state is mapped from a ByteBuffer with tolerance for HTTP method and results in the first line.
     * <p/>
     * {@link #headerInterest } contains a list of addHeaderInterest that will be converted to a {@link Map} and available via {@link Rfc822HeaderState#getHeaderStrings()}
     * <p/>
     * {@link #cookies } contains a list of cookies from which to parse from Cookie header into {@link #cookieStrings}
     * <p/>
     * setting cookies for a HttpResponse header is possible by setting {@link #dirty } to true and setting  {@link #cookieStrings} map values.
     * <p/>
     * currently this is  done inside of {@link ProtocolMethodDispatch } surrounding {@link SimpleRequestProcessor#process(String)}
     *
     * @param cursor
     * @return this
     */
    public Rfc822HeaderState apply(ByteBuffer cursor) {
        if (!cursor.hasRemaining())
            cursor.flip();
        int anchor = cursor.position();
        ByteBuffer slice = cursor.duplicate().slice();
        while (slice.hasRemaining() && ' ' != slice.get()) ;
        methodProtocol.set(UTF8.decode((ByteBuffer) slice.flip()).toString()
                .trim());

        while (cursor.hasRemaining() && ' ' != cursor.get()) ; //method/proto
        slice = cursor.slice();
        while (slice.hasRemaining() && ' ' != slice.get()) ;
        pathRescode.set(UTF8.decode((ByteBuffer) slice.flip()).toString()
                .trim());

        while (cursor.hasRemaining() && ' ' != cursor.get()) ;
        slice = cursor.slice();
        while (slice.hasRemaining() && '\n' != slice.get()) ;
        protocolStatus.set(UTF8.decode((ByteBuffer) slice.flip()).toString()
                .trim());

        headerBuf = null;
        boolean wantsCookies = 0 < cookies().length;
        boolean wantsHeaders = wantsCookies || 0 < headerInterest.get().length;
        headerBuf = (ByteBuffer) moveCaretToDoubleEol(cursor).duplicate()
                .flip();
        headerStrings().clear();
        cookieStrings().clear();
        if (wantsHeaders) {
            Map<String, int[]> headerMap = HttpHeaders
                    .getHeaders((ByteBuffer) headerBuf.rewind());
            headerStrings.set(new LinkedHashMap<String, String>());
            for (String o : headerInterest.get()) {
                int[] o1 = headerMap.get(o);
                if (null != o1)
                    headerStrings.get().put(
                            o,
                            UTF8.decode(
                                    (ByteBuffer) headerBuf.duplicate().clear()
                                            .position(o1[0]).limit(o1[1]))
                                    .toString().trim());
            }

        }
        return this;
    }

    public Rfc822HeaderState headerInterest(HttpHeaders... replaceInterest) {
        final String[] strings = staticHeaderStrings(replaceInterest);
        return headerInterest(strings);
    }

    public static String[] staticHeaderStrings(HttpHeaders... replaceInterest) {
        final String[] strings = new String[replaceInterest.length];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = replaceInterest[i].getHeader();

        }
        return strings;
    }

    public Rfc822HeaderState headerInterest(String... replaceInterest) {
        headerInterest.set(replaceInterest);
        return this;
    }

    public Rfc822HeaderState addHeaderInterest(HttpHeaders... appendInterest) {
        final String[] strings = staticHeaderStrings(appendInterest);
        return addHeaderInterest(strings);
    }

    /**
     * Appends to the Set of header keys this parser is interested in mapping to strings.
     * <p/>
     * these addHeaderInterest are mapped at cardinality<=1 when  {@link #apply(ByteBuffer)}  }is called.
     * <p/>
     * for cardinality=>1  addHeaderInterest {@link #getHeadersNamed(String)} is a pure grep over the entire ByteBuffer.
     * <p/>
     *
     * @param newInterest
     * @return
     * @see #getHeadersNamed(String)
     * @see #apply(ByteBuffer)
     */
    public Rfc822HeaderState addHeaderInterest(String... newInterest) {

        //adds a few more instructions than the blind append but does what was desired
        Set<String> theCow = new CopyOnWriteArraySet<>(Arrays
                .<String>asList(headerInterest.get()));
        theCow.addAll(Arrays.asList(newInterest));
        String[] strings = theCow.toArray(new String[theCow.size()]);
        Arrays.sort(strings);
        headerInterest.set(strings);

        //    String[] temp = new String[headerInterest.length + this.addHeaderInterest.length];
        //    System.arraycopy(this.addHeaderInterest, 0, temp, 0, this.addHeaderInterest.length);
        //    System.arraycopy(headerInterest, 0, temp, this.addHeaderInterest.length, headerInterest.length);
        //    this.addHeaderInterest = temp;
        return this;
    }

    /**
     * @return
     * @see #dirty
     */
    public boolean dirty() {
        return dirty.get();
    }

    /**
     * indicate whether or not we want to rewrite the cookies and push SetCookie to client.  if this is set, the contents of {@link #cookieStrings} will be written each as cookies during {@link #asResponseHeaderByteBuffer()}
     *
     * @param dirty
     * @return
     */
    public Rfc822HeaderState dirty(boolean dirty) {
        this.dirty.set(dirty);
        return this;
    }

    /**
     * @return
     * @see #headerInterest
     */

    public String[] headerInterest() {
        return headerInterest.get();
    }

    /**
     * @return
     * @see #cookies
     */
    public String[] cookies() {

        return cookies.get();
    }

    /**
     * @return inet4 addr
     * @see #sourceRoute
     */
    public InetAddress sourceRoute() {
        return sourceRoute.get();
    }

    /**
     * this holds an inet address which may be inferred diuring {@link #sourceKey(SelectionKey)} as well as directly
     *
     * @param sourceRoute an internet ipv4 address
     * @return self
     */
    public Rfc822HeaderState sourceRoute(InetAddress sourceRoute) {
        this.sourceRoute.set(sourceRoute);
        return this;
    }

    /**
     * this is what has been sent to {@link #apply(ByteBuffer)}.
     * <p/>
     * care must be taken to avoid {@link ByteBuffer#compact()} during the handling of
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
     * holds the values parsed during {@link #apply(ByteBuffer)} and holds the key-values created as addHeaderInterest in
     * {@link #asRequestHeaderByteBuffer()} and {@link #asResponseHeaderByteBuffer()}
     *
     * @return
     */
    public Rfc822HeaderState headerStrings(Map<String, String> headerStrings) {
        this.headerStrings.set(headerStrings);
        return this;
    }

    /**
     * fluent lazy getter
     *
     * @return {@link #headerStrings}
     * @see #headerStrings
     */
    public Map<String, String> headerStrings() {

        headerStrings.compareAndSet(null, new LinkedHashMap<String, String>());
        return headerStrings.get();
    }

    /**
     * fluent lazy getter
     *
     * @return
     * @see #cookieStrings
     */
    public Map<String, String> cookieStrings() {

        cookieStrings.compareAndSet(null, new LinkedHashMap<String, String>());
        return cookieStrings.get();
    }

    /**
     * fluent setter
     *
     * @param cookieStrings
     * @return self
     * @see #cookieStrings
     */

    public Rfc822HeaderState cookieStrings(Map<String, String> cookieStrings) {
        this.cookieStrings.set(cookieStrings);
        return this;
    }

    /**
     * @return
     * @see #methodProtocol
     */
    public String methodProtocol() {
        return methodProtocol.get();
    }

    /**
     * @return
     * @see #methodProtocol
     */
    public Rfc822HeaderState methodProtocol(String methodProtocol) {
        this.methodProtocol.set(methodProtocol);
        return this;
    }

    /**
     * dual purpose HTTP protocol header token found on the first line of a HttpRequest/HttpResponse in the second position
     * contains either the path (HttpRequest) or a the numeric result code on responses.
     * user is responsible for populating this on outbound addHeaderInterest
     *
     * @return
     * @see #pathRescode
     */

    public String pathResCode() {
        return pathRescode.get();
    }

    /**
     * @return
     * @see #pathRescode
     */
    public Rfc822HeaderState pathResCode(String pathRescode) {
        this.pathRescode.set(pathRescode);
        return this;
    }

    /**
     * Dual purpose HTTP protocol header token found on the first line of a HttpRequest/HttpResponse in the third position.
     * <p/>
     * Contains either the protocol (HttpRequest) or a status line message (HttpResponse)
     */
    public String protocolStatus() {
        return protocolStatus.get();
    }

    /**
     * @see Rfc822HeaderState#protocolStatus()
     */
    public Rfc822HeaderState protocolStatus(String protocolStatus) {
        this.protocolStatus.set(protocolStatus);
        return this;
    }

    @Override
    public String toString() {
        return "Rfc822HeaderState{" + "dirty=" + dirty + ", headerInterest="
                + Arrays.asList(headerInterest.get()) + ", cookies="
                + Arrays.asList(cookies.get()) + ", sourceRoute=" + sourceRoute
                + ", headerBuf=" + headerBuf + ", headerStrings="
                + headerStrings + ", cookieStrings=" + cookieStrings
                + ", methodProtocol='" + methodProtocol + '\''
                + ", pathRescode='" + pathRescode + '\'' + ", protocolStatus='"
                + protocolStatus + '\'' + ", sourceKey=" + sourceKey + '}';
    }

    /**
     * writes method, headersStrings, and cookieStrings to a {@link String } suitable for Response addHeaderInterest
     * <p/>
     * populates addHeaderInterest from {@link #headerStrings}
     * <p/>
     * if {@link #dirty} is set this will include SetCookie addHeaderInterest (plural) one for each of {@link #cookieStrings()}
     *
     * @return http addHeaderInterest for use with http 1.1
     */
    public String asResponseHeaderString() {
        String protocol = (null == methodProtocol()
                ? "HTTP/1.1"
                : methodProtocol())
                + " " + pathResCode() + " " + protocolStatus() + "\r\n";
        for (Entry<String, String> stringStringEntry : headerStrings()
                .entrySet()) {
            protocol += stringStringEntry.getKey() + ": "
                    + stringStringEntry.getValue() + "\r\n";
        }
        for (Entry<String, String> stringStringEntry : cookieStrings()
                .entrySet()) {
            protocol += Set$2dCookie.getHeader() + ": "
                    + stringStringEntry.getKey() + "="
                    + stringStringEntry.getValue() + "\r\n";
        }

        protocol += "\r\n";
        return protocol;
    }

    /**
     * writes method, headersStrings, and cookieStrings to a {@link ByteBuffer} suitable for Response addHeaderInterest
     * <p/>
     * populates addHeaderInterest from {@link #headerStrings}
     * <p/>
     * if {@link #dirty} is set this will include SetCookie addHeaderInterest (plural) one for each of {@link #cookieStrings()}
     *
     * @return http addHeaderInterest for use with http 1.1
     */
    public ByteBuffer asResponseHeaderByteBuffer() {
        String protocol = asResponseHeaderString();
        return ByteBuffer.wrap(protocol.getBytes(HttpMethod.UTF8));
    }

    /**
     * writes method, headersStrings, and cookieStrings to a {@link String} suitable for RequestHeaders
     * <p/>
     * populates addHeaderInterest from {@link #headerStrings}
     *
     * @return http addHeaderInterest for use with http 1.1
     */
    public String asRequestHeaderString() {
        String protocol = methodProtocol() + " " + pathResCode() + " "
                + (null == protocolStatus() ? "HTTP/1.1" : protocolStatus())
                + "\r\n";
        for (Entry<String, String> stringStringEntry : headerStrings()
                .entrySet()) {
            protocol += stringStringEntry.getKey() + ": "
                    + stringStringEntry.getValue() + "\r\n";
        }
        for (Entry<String, String> stringStringEntry : cookieStrings()
                .entrySet()) {
            protocol += Cookie.getHeader() + ": " + stringStringEntry.getKey() + "="
                    + stringStringEntry.getValue() + "\r\n";
        }

        protocol += "\r\n";
        return protocol;
    }

    /**
     * writes method, headersStrings, and cookieStrings to a {@link ByteBuffer} suitable for RequestHeaders
     * <p/>
     * populates addHeaderInterest from {@link #headerStrings}
     *
     * @return http addHeaderInterest for use with http 1.1
     */
    public ByteBuffer asRequestHeaderByteBuffer() {
        String protocol = asRequestHeaderString();
        return ByteBuffer.wrap(protocol.getBytes(HttpMethod.UTF8));
    }

    /**
     * utliity shortcut method to get the parsed value from the {@link #headerStrings} map
     *
     * @param headerKey name of a header presumed to be parsed during {@link #apply(ByteBuffer)}
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
        String s = headerString(headerKey);
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
        return sourceKey.get(); //To change body of created methods use File | Settings | File Templates.
    }

    public static ByteBuffer moveCaretToDoubleEol(ByteBuffer buffer) {
        int distance;
        int eol = buffer.position();

        do {
            int prev = eol;
            while (buffer.hasRemaining() && '\n' != buffer.get()) ;
            eol = buffer.position();
            distance = abs(eol - prev);
            if (2 == distance && '\r' == buffer.get(eol - 2)) {
                break;
            }
        } while (buffer.hasRemaining() && 1 < distance);
        return buffer;
    }
}
