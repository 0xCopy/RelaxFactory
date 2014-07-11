package rxf.sandbox;

/*
 * Based on: A single threaded Handler that performs accepts
 * SocketChannels and registers the Channels with the read/write
 * Selector.
 *
 * @author Mark Reinhold
 * @author Brad R. Wetmore
 * @version 1.2, 04/07/26
 */


import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The main server base class.
 * <P>
 * This class is responsible for setting up most of the server state
 * before the actual server subclasses take over.
 *
 * @author Mark Reinhold
 * @author Brad R. Wetmore
 * @version 1.2, 04/07/26
 */

public abstract class Server {

    protected final ServerSocketChannel ssc;
    SSLContext sslContext = null;

    private static int PORT = 8000;
    private static int BACKLOG = 1024;
    private static boolean SECURE = false;

    Server (int port, int backlog, boolean secure) throws GeneralSecurityException, IOException {

        if (secure) createSSLContext();

        ssc = ServerSocketChannel.open();
        ssc.socket().setReuseAddress(true);
        ssc.socket().bind(new InetSocketAddress(port), backlog);
    }

    /*
     * If this is a secure server, we now setup the SSLContext we'll
     * use for creating the SSLEngines throughout the lifetime of
     * this process.
     */
    private void createSSLContext() throws GeneralSecurityException, IOException {

        char[] passphrase = "passphrase".toCharArray();

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream("testkeys"), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
    }

    abstract void runServer() throws IOException;

    private static void usage() {
        System.out.println(
                "Usage:  Server <type> [options]\n"
                        + "	type:\n"
                        + "		B1	Blocking/Single-threaded Server\n"
                        + "		BN	Blocking/Multi-threaded Server\n"
                        + "		BP	Blocking/Pooled-Thread Server\n"
                        + "		N1	Nonblocking/Single-threaded Server\n"
                        + "		N2	Nonblocking/Dual-threaded Server\n"
                        + "\n"
                        + "	options:\n"
                        + "		-port port		port number\n"
                        + "		    default:  " + PORT + "\n"
                        + "		-backlog backlog	backlog\n"
                        + "		    default:  " + BACKLOG + "\n"
                        + "		-secure			encrypt with SSL/TLS");
        System.exit(1);
    }

    /*
     * Parse the arguments, decide what type of server to run,
     * see if there are any defaults to change.
     */
    private static Server createServer (String args[]) throws GeneralSecurityException, IOException {
        if (args.length < 1) {
            usage();
        }

        int port = PORT;
        int backlog = BACKLOG;
        boolean secure = SECURE;

        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("-port")) {
                checkArgs(i, args.length);
                port = Integer.valueOf(args[++i]);
            } else if (args[i].equals("-backlog")) {
                checkArgs(i, args.length);
                backlog = Integer.valueOf(args[++i]);
            } else if (args[i].equals("-secure")) {
                secure = true;
            } else {
                usage();
            }
        }

        Server server = null;

       /* if (args[0].equals("B1")) {
            server = new B1(port, backlog, secure);
        } else if (args[0].equals("BN")) {
            server = new BN(port, backlog, secure);
        } else if (args[0].equals("BP")) {
            server = new BP(port, backlog, secure);
        } else */if (args[0].equals("N1")) {
            server = new N1(port, backlog, secure);
        }/* else if (args[0].equals("N2")) {
            server = new N2(port, backlog, secure);
        }
*/
        return server;
    }

    private static void checkArgs(int i, int len) {
        if ((i + 1) >= len) {
            usage();
        }
    }

    public static void main (String args[]) throws GeneralSecurityException, IOException {
        final Server server = createServer(args);
        if (server == null) {
            usage();
        } else {
            System.out.println("Server started.");
            server.runServer();
        }
    }
}

/**
 * A single-threaded dispatcher.
 * <P>
 * When a SelectionKey is ready, it dispatches the job in this
 * thread.
 *
 * @author Mark Reinhold
 * @author Brad R. Wetmore
 * @version 1.2, 04/07/26
 */
  final class Dispatcher1 implements Dispatcher {

    private final Selector sel;

    Dispatcher1() throws IOException {
        sel = Selector.open();
    }

    // Doesn't really need to be runnable
    public void run() {
        for (;;) {
            try {
                dispatch();
            } catch (IOException x) {
                x.printStackTrace();
            }
        }
    }

    private void dispatch() throws IOException {
        sel.select();
        for (Iterator i = sel.selectedKeys().iterator(); i.hasNext(); ) {
            final SelectionKey sk = (SelectionKey)i.next();
            i.remove();
            final Handler h = (Handler)sk.attachment();
            h.handle(sk);
        }
    }

    public void register (final SelectableChannel ch, int ops, Handler h) throws IOException {
        ch.register(sel, ops, h);
    }
}
/**
 * A non-blocking/single-threaded server.  All accept() and
 * read()/write() operations are performed by a single thread, but only
 * after being selected for those operations by a Selector.
 *
 * @author Mark Reinhold
 * @author Brad R. Wetmore
 * @version 1.2, 04/07/26
 */
  final class N1 extends Server {

    N1 (final int port, final int backlog, final boolean secure) throws GeneralSecurityException, IOException {
        super(port, backlog, secure);
        ssc.configureBlocking(false);
    }

    void runServer() throws IOException {
        final Dispatcher d = new Dispatcher1();
        d.register (ssc, SelectionKey.OP_ACCEPT, new AcceptHandler(ssc, d, sslContext));
        d.run();
    }
}
/**
 * A Content type that provides for transferring files.
 *
 * @author Mark Reinhold
 * @author Brad R. Wetmore
 * @version 1.2, 04/07/26
 */
 final class FileContent implements Content {

    //  Files in this directory are found by the WWW server
     static final File ROOT = new File ("root");

     final File fn;
     final String type;

    public FileContent (URI uri) {
        fn = new File(ROOT, uri.getPath().replace('/',File.separatorChar));
        type = determineContentType (fn.getName());
    }

     static String determineContentType (final String nm) {
        if (nm.endsWith(".html")) {
            return "text/html; charset=iso-8859-1";
        } else if ((nm.indexOf('.') < 0) || nm.endsWith(".txt")) {
            return "text/plain; charset=iso-8859-1";
        } else {
            return "application/octet-stream";
        }
    }

    public String type() { return type; }

     FileChannel fc = null;
     long length = -1;
     long position = -1;		// NB only; >= 0 if transferring

    public long length() {
        return length;
    }

    public void prepare() throws IOException {
        if (fc == null) fc = new RandomAccessFile(fn, "r").getChannel();
        length = fc.size();
        position = 0;			// NB only
    }

    public boolean send (final ChannelIO cio) throws IOException {
        if (fc == null) throw new IllegalStateException();
        if (position < 0)		// NB only
            throw new IllegalStateException();

        // Quit if we're already done.
        if (position >= length) return false;

        position += cio.transferTo (fc, position, length-position);
        return (position < length);
    }

    public void release() throws IOException {
        if (fc != null) {
            fc.close();
            fc = null;
        }
    }
}
/**
 * A Content type that provides for transferring Strings.
 *
 * @author Mark Reinhold
 * @author Brad R. Wetmore
 * @version 1.2, 04/07/26
 */
class StringContent implements Content {

     static final Charset ascii = Charset.forName("US-ASCII");

     final String type;		// MIME type
     final String content;

    StringContent (CharSequence c, String t) {
        content = c.toString() + (c.charAt(c.length()-1)=='\n'?"":'\n');
        type = t + "; charset=iso-8859-1";
    }

    StringContent (CharSequence c) {
        this(c, "text/plain");
    }

    StringContent (Exception x) {
        StringWriter sw = new StringWriter();
        x.printStackTrace(new PrintWriter(sw));
        type = "text/plain; charset=iso-8859-1";
        content = sw.toString();
    }

    public String type() {
        return type;
    }

     ByteBuffer bb = null;

     void encode() {
        if (bb == null) bb = ascii.encode(CharBuffer.wrap(content));
    }

    public long length() {
        encode();
        return bb.remaining();
    }

    public void prepare() {
        encode();
        bb.rewind();
    }

    public boolean send (final ChannelIO cio) throws IOException {
        if (bb == null) throw new IllegalStateException(); // call encode first
        cio.write(bb);
        return bb.hasRemaining();
    }

    public void release() throws IOException {}
}interface Sendable {

    void prepare() throws IOException;

    // Sends (some) content to the given channel.
    // Returns true if more bytes remain to be written.
    // Throws IllegalStateException if not prepared.
    //
    boolean send (ChannelIO cio) throws IOException;

    void release() throws IOException;
}interface Content extends Sendable {

    String type();

    // Returns -1 until prepare() invoked
    long length();

}

/**
 * An object used for sending Content to the requestor.
 *
 * @author Mark Reinhold
 * @author Brad R. Wetmore
 * @version 1.2, 04/07/26
 */
class Reply implements Sendable {

    /**
     * A helper class which define the HTTP response codes
     */
    static class Code {

        final int number;
        final String reason;
        Code (int i, String r) { number = i; reason = r; }
       public String toString() { return number + " " + reason; }

       public static final Code OK = new Code(200, "OK");
       public static final Code BAD_REQUEST = new Code(400, "Bad Request");
       public static final Code NOT_FOUND = new Code(404, "Not Found");
       public static final Code METHOD_NOT_ALLOWED = new Code(405, "Method Not Allowed");
    }

     final Code code;
     final Content content;
     final boolean headersOnly;

    Reply (Code rc, Content c) {
	this(rc, c, null);
    }

    Reply(Code rc, Content c, Request.Action head) {
	code = rc;
	content = c;
	headersOnly = (head == Request.Action.HEAD);
    }

     static String CRLF = "\r\n";
     static Charset ascii = Charset.forName("US-ASCII");

     ByteBuffer hbb = null;

     ByteBuffer headers() {
	CharBuffer cb = CharBuffer.allocate(1024);
	for (;;) {
	    try {
		cb.put("HTTP/1.0 ").put(code.toString()).put(CRLF);
		cb.put("Server: niossl/0.1").put(CRLF);
		cb.put("Content-type: ").put(content.type()).put(CRLF);
		cb.put("Content-length: ").put(Long.toString(content.length())).put(CRLF);
		cb.put(CRLF);
		break;
	    } catch (BufferOverflowException x) {
		assert(cb.capacity() < (1 << 16));
		cb = CharBuffer.allocate(cb.capacity() * 2);
		continue;
	    }
	}
	cb.flip();  // prepare for channel write
	return ascii.encode(cb);
    }

    public void prepare() throws IOException {
	content.prepare();
	hbb = headers();
    }

    public boolean send (final ChannelIO cio) throws IOException {

	if (hbb == null) throw new IllegalStateException();

	if (hbb.hasRemaining()) {
	    if (cio.write(hbb) <= 0) return true;
	}

	if (!headersOnly) {
	    if (content.send(cio)) return true;
	}

	if (!cio.dataFlush()) return true;

	return false;
    }

    public void release() throws IOException {
	content.release();
    }
}
/**
 * Exception class used when a request can't be properly parsed.
 *
 * @author Mark Reinhold
 * @author Brad R. Wetmore
 * @version 1.2, 04/07/26
 */
class MalformedRequestException extends Exception {

    MalformedRequestException() { }

    MalformedRequestException(String msg) {
	super(msg);
    }

    MalformedRequestException(Exception x) {
	super(x);
    }
}

/**
 * An encapsulation of the request received.
 * <P>
 * The static method parse() is responsible for creating this
 * object.
 *
 * @author Mark Reinhold
 * @author Brad R. Wetmore
 * @version 1.2, 04/07/26
 */
class Request {

    /**
     * A helper class for parsing HTTP command actions.
     */
    static class Action {

	 String name;
	 Action(String name) { this.name = name; }
	public String toString() { return name; }

	static Action GET = new Action("GET");
	static Action PUT = new Action("PUT");
	static Action POST = new Action("POST");
	static Action HEAD = new Action("HEAD");

	static Action parse(String s) {
	    if (s.equals("GET"))
		return GET;
	    if (s.equals("PUT"))
		return PUT;
	    if (s.equals("POST"))
		return POST;
	    if (s.equals("HEAD"))
		return HEAD;
	    throw new IllegalArgumentException(s);
	}
    }

     Action action;
     String version;
     URI uri;

    Action action() { return action; }
    String version() { return version; }
    URI uri() { return uri; }

     Request(Action a, String v, URI u) {
	action = a;
	version = v;
	uri = u;
    }

    public String toString() {
	return (action + " " + version + " " + uri);
    }

    static boolean isComplete(ByteBuffer bb) {
	int p = bb.position() - 4;
	if (p < 0)
	    return false;
	return (((bb.get(p + 0) == '\r') &&
		 (bb.get(p + 1) == '\n') &&
		 (bb.get(p + 2) == '\r') &&
		 (bb.get(p + 3) == '\n')));
    }

     static Charset ascii = Charset.forName("US-ASCII");

    /*
     * The expected message format is first compiled into a pattern,
     * and is then compared against the inbound character buffer to
     * determine if there is a match.  This convienently tokenizes
     * our request into usable pieces.
     *
     * This uses Matcher "expression capture groups" to tokenize
     * requests like:
     *
     *     GET /dir/file HTTP/1.1
     *     Host: hostname
     *
     * into:
     *
     *     group[1] = "GET"
     *     group[2] = "/dir/file"
     *     group[3] = "1.1"
     *     group[4] = "hostname"
     *
     * The text in between the parens are used to captured the regexp text.
     */
     static Pattern requestPattern
	= Pattern.compile("\\A([A-Z]+) +([^ ]+) +HTTP/([0-9\\.]+)$"
                    + ".*^Host: ([^ ]+)$.*\r\n\r\n\\z",
            Pattern.MULTILINE | Pattern.DOTALL);

    static Request parse(ByteBuffer bb) throws MalformedRequestException {

	CharBuffer cb = ascii.decode(bb);
	Matcher m = requestPattern.matcher(cb);
	if (!m.matches())
	    throw new MalformedRequestException();
	Action a;
	try {
	    a = Action.parse(m.group(1));
	} catch (IllegalArgumentException x) {
	    throw new MalformedRequestException();
	}
	URI u;
	try {
	    u = new URI("http://"
			+ m.group(4)
			+ m.group(2));
	} catch (URISyntaxException x) {
	    throw new MalformedRequestException();
	}
	return new Request(a, m.group(3), u);
    }
}
/*
 * @(#)RequestHandler.java	1.2 04/07/26
 *
 * Copyright (c) 2004 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * -Redistribution of source code must retain the above copyright notice, this
 *  list of conditions and the following disclaimer.
 *
 * -Redistribution in binary form must reproduce the above copyright notice,
 *  this list of conditions and the following disclaimer in the documentation
 *  and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or intended
 * for use in the design, construction, operation or maintenance of any
 * nuclear facility.
 */


/**
 * Primary driver class used by non-blocking Servers to receive,
 * prepare, send, and shutdown requests.
 *
 * @author Mark Reinhold
 * @author Brad R. Wetmore
 * @version 1.2, 04/07/26
 */
class RequestHandler implements Handler {

     ChannelIO cio;
     ByteBuffer rbb = null;

     boolean requestReceived = false;
     Request request = null;
     Reply reply = null;

     static int created = 0;

    RequestHandler(ChannelIO cio) {
        this.cio = cio;

        // Simple heartbeat to let user know we're alive.
        synchronized (RequestHandler.class) {
            created++;
            if ((created % 50) == 0) {
                System.out.println(".");
                created = 0;
            } else {
                System.out.print(".");
            }
        }
    }

    // Returns true when request is complete
    // May expand rbb if more room required
    //
     boolean receive(SelectionKey sk) throws IOException {
        ByteBuffer tmp = null;

        if (requestReceived) {
            return true;
        }

        if (!cio.doHandshake(sk)) {
            return false;
        }

        if ((cio.read() < 0) || Request.isComplete(cio.getReadBuf())) {
            rbb = cio.getReadBuf();
            return (requestReceived = true);
        }
        return false;
    }

    // When parse is successfull, saves request and returns true
    //
     boolean parse() throws IOException {
        try {
            request = Request.parse(rbb);
            return true;
        } catch (MalformedRequestException x) {
            reply = new Reply(Reply.Code.BAD_REQUEST,
                    new StringContent(x));
        }
        return false;
    }

    // Ensures that reply field is non-null
    //
     void build() throws IOException {
        Request.Action action = request.action();
        if ((action != Request.Action.GET) &&
                (action != Request.Action.HEAD)) {
            reply = new Reply(Reply.Code.METHOD_NOT_ALLOWED,
                    new StringContent(request.toString()));
        }
        reply = new Reply(Reply.Code.OK,
                new FileContent(request.uri()), action);
    }

    public void handle(SelectionKey sk) throws IOException {
        try {

            if (request == null) {
                if (!receive(sk))
                    return;
                rbb.flip();
                if (parse())
                    build();
                try {
                    reply.prepare();
                } catch (IOException x) {
                    reply.release();
                    reply = new Reply(Reply.Code.NOT_FOUND,
                            new StringContent(x));
                    reply.prepare();
                }
                if (send()) {
                    // More bytes remain to be written
                    sk.interestOps(SelectionKey.OP_WRITE);
                } else {
                    // Reply completely written; we're done
                    if (cio.shutdown()) {
                        cio.close();
                        reply.release();
                    }
                }
            } else {
                if (!send()) {	// Should be rp.send()
                    if (cio.shutdown()) {
                        cio.close();
                        reply.release();
                    }
                }
            }
        } catch (IOException x) {
            String m = x.getMessage();
            if (!m.equals("Broken pipe") &&
                    !m.equals("Connection reset by peer")) {
                System.err.println("RequestHandler: " + x.toString());
            }

            try {
		/*
		 * We had a failure here, so we'll try to be nice
		 * before closing down and send off a close_notify,
		 * but if we can't get the message off with one try,
		 * we'll just shutdown.
		 */
                cio.shutdown();
            } catch (IOException e) {
                // ignore
            }

            cio.close();
            if (reply !=  null) {
                reply.release();
            }
        }

    }

     boolean send() throws IOException {
        try {
            return reply.send(cio);
        } catch (IOException x) {
            if (x.getMessage().startsWith("Resource temporarily")) {
                System.err.println("## RTA");
                return true;
            }
            throw x;
        }
    }
}
/**
 * A helper class which performs I/O using the SSLEngine API.
 * <P>
 * Each connection has a SocketChannel and a SSLEngine that is
 * used through the lifetime of the Channel.  We allocate byte buffers
 * for use as the outbound and inbound network buffers.
 *
 * <PRE>
 *               Application Data
 *               src      requestBB
 *                |           ^
 *                |     |     |
 *                v     |     |
 *           +----+-----|-----+----+
 *           |          |          |
 *           |       SSL|Engine    |
 *   wrap()  |          |          |  unwrap()
 *           | OUTBOUND | INBOUND  |
 *           |          |          |
 *           +----+-----|-----+----+
 *                |     |     ^
 *                |     |     |
 *                v           |
 *            outNetBB     inNetBB
 *                   Net data
 * </PRE>
 *
 * These buffers handle all of the intermediary data for the SSL
 * connection.  To make things easy, we'll require outNetBB be
 * completely flushed before trying to wrap any more data, but we
 * could certainly remove that restriction by using larger buffers.
 * <P>
 * There are many, many ways to handle compute and I/O strategies.
 * What follows is a relatively simple one.  The reader is encouraged
 * to develop the strategy that best fits the application.
 * <P>
 * In most of the non-blocking operations in this class, we let the
 * Selector tell us when we're ready to attempt an I/O operation (by the
 * application repeatedly calling our methods).  Another option would be
 * to attempt the operation and return from the method when no forward
 * progress can be made.
 * <P>
 * There's lots of room for enhancements and improvement in this example.
 * <P>
 * We're checking for SSL/TLS end-of-stream truncation attacks via
 * sslEngine.closeInbound().  When you reach the end of a input stream
 * via a read() returning -1 or an IOException, we call
 * sslEngine.closeInbound() to signal to the sslEngine that no more
 * input will be available.  If the peer's close_notify message has not
 * yet been received, this could indicate a trucation attack, in which
 * an attacker is trying to prematurely close the connection.   The
 * closeInbound() will throw an exception if this condition were
 * present.
 *
 * @author Brad R. Wetmore
 * @author Mark Reinhold
 * @version 1.2, 04/07/26
 */
class ChannelIOSecure extends ChannelIO {

    final SSLEngine sslEngine;

    int appBBSize;
    int netBBSize;

   /*
    * All I/O goes through these buffers.
    * <P>
    * It might be nice to use a cache of ByteBuffers so we're
    * not alloc/dealloc'ing ByteBuffer's for each new SSLEngine.
    * <P>
    * We use our superclass' requestBB for our application input buffer.
    * Outbound application data is supplied to us by our callers.
    */
    final ByteBuffer inNetBB;
    final ByteBuffer outNetBB;

   /*
    * An empty ByteBuffer for use when one isn't available, say
    * as a source buffer during initial handshake wraps or for close
    * operations.
    */
    static final ByteBuffer hsBB = ByteBuffer.allocate(0);

   /*
    * The FileChannel we're currently transferTo'ing (reading).
    */
    ByteBuffer fileChannelBB = null;

   /*
    * During our initial handshake, keep track of the next
    * SSLEngine operation that needs to occur:
    *
    *     NEED_WRAP/NEED_UNWRAP
    *
    * Once the initial handshake has completed, we can short circuit
    * handshake checks with initialHSComplete.
    */
    HandshakeStatus initialHSStatus;
    boolean initialHSComplete;

   /*
    * Constructor for a secure ChannelIO variant.
    */
   protected ChannelIOSecure (SocketChannel sc, boolean blocking, SSLContext sslc) throws IOException {
      super(sc, blocking);

      /*
       * We're a server, so no need to use host/port variant.
       *
       * The first call for a server is a NEED_UNWRAP.
       */
      sslEngine = sslc.createSSLEngine();
      sslEngine.setUseClientMode(false);
      initialHSStatus = HandshakeStatus.NEED_UNWRAP;
      initialHSComplete = false;

      netBBSize = sslEngine.getSession().getPacketBufferSize();
      inNetBB  = ByteBuffer.allocate(netBBSize);
      outNetBB = ByteBuffer.allocate(netBBSize);
      outNetBB.position(0);
      outNetBB.limit(0);
   }

   /*
    * Static factory method for creating a secure ChannelIO object.
    * <P>
    * We need to allocate different sized application data buffers
    * based on whether we're secure or not.  We can't determine
    * this until our sslEngine is created.
    */
   public static ChannelIOSecure getInstance(SocketChannel sc, boolean blocking, SSLContext sslc) throws IOException {
      ChannelIOSecure cio = new ChannelIOSecure(sc, blocking, sslc);
      cio.appBBSize = cio.sslEngine.getSession().getApplicationBufferSize();
      cio.requestBB = ByteBuffer.allocate(cio.appBBSize);
      return cio;
   }

   /*
    * Calls up to the superclass to adjust the buffer size
    * by an appropriate increment.
    */
   protected void resizeRequestBB() {
      resizeRequestBB(appBBSize);
   }

   /*
    * Writes bb to the SocketChannel.
    * <P>
    * Returns true when the ByteBuffer has no remaining data.
    */
    boolean tryFlush(ByteBuffer bb) throws IOException {
      super.write(bb);
      return !bb.hasRemaining();
   }

   /*
    * Perform any handshaking processing.
    * <P>
    * This variant is for Servers without SelectionKeys (e.g.
    * blocking).
    */
   boolean doHandshake() throws IOException {
      return doHandshake(null);
   }

   /*
    * Perform any handshaking processing.
    * <P>
    * If a SelectionKey is passed, register for selectable
    * operations.
    * <P>
    * In the blocking case, our caller will keep calling us until
    * we finish the handshake.  Our reads/writes will block as expected.
    * <P>
    * In the non-blocking case, we just received the selection notification
    * that this channel is ready for whatever the operation is, so give
    * it a try.
    * <P>
    * return:
    *		true when handshake is done.
    *		false while handshake is in progress
    */
   boolean doHandshake(SelectionKey sk) throws IOException {

      SSLEngineResult result;

      if (initialHSComplete) {
         return initialHSComplete;
      }

      /*
       * Flush out the outgoing buffer, if there's anything left in
       * it.
       */
      if (outNetBB.hasRemaining()) {

         if (!tryFlush(outNetBB)) {
            return false;
         }

         // See if we need to switch from write to read mode.

         switch (initialHSStatus) {

	    /*
	     * Is this the last buffer?
	     */
         case FINISHED:
            initialHSComplete = true;
            // Fall-through to reregister need for a Read.

         case NEED_UNWRAP:
            if (sk != null) {
               sk.interestOps(SelectionKey.OP_READ);
            }
            break;
         }

         return initialHSComplete;
      }


      switch (initialHSStatus) {

      case NEED_UNWRAP:
         if (sc.read(inNetBB) == -1) {
            sslEngine.closeInbound();
            return initialHSComplete;
         }

         needIO:
         while (initialHSStatus == HandshakeStatus.NEED_UNWRAP) {
            /*
             * Don't need to resize requestBB, since no app data should
             * be generated here.
             */
            inNetBB.flip();
            result = sslEngine.unwrap(inNetBB, requestBB);
            inNetBB.compact();

            initialHSStatus = result.getHandshakeStatus();

            switch (result.getStatus()) {

            case OK:
               switch (initialHSStatus) {
               case NOT_HANDSHAKING:
                  throw new IOException("Not handshaking during initial handshake");

               case NEED_TASK:
                  initialHSStatus = doTasks();
                  break;

               case FINISHED:
                  initialHSComplete = true;
                  break needIO;
               }

               break;

            case BUFFER_UNDERFLOW:
               /*
                * Need to go reread the Channel for more data.
                */
               if (sk != null) {
                  sk.interestOps(SelectionKey.OP_READ);
               }
               break needIO;

            default: // BUFFER_OVERFLOW/CLOSED:
               throw new IOException("Received" + result.getStatus() +
                                     "during initial handshaking");
            }
         }  // "needIO" block.

	    /*
	     * Just transitioned from read to write.
	     */
         if (initialHSStatus != HandshakeStatus.NEED_WRAP) {
            break;
         }

         // Fall through and fill the write buffers.

      case NEED_WRAP:
         /*
          * The flush above guarantees the out buffer to be empty
          */
         outNetBB.clear();
         result = sslEngine.wrap(hsBB, outNetBB);
         outNetBB.flip();

         initialHSStatus = result.getHandshakeStatus();

         switch (result.getStatus()) {
         case OK:

            if (initialHSStatus == HandshakeStatus.NEED_TASK) {
               initialHSStatus = doTasks();
            }

            if (sk != null) {
               sk.interestOps(SelectionKey.OP_WRITE);
            }

            break;

         default: // BUFFER_OVERFLOW/BUFFER_UNDERFLOW/CLOSED:
            throw new IOException("Received" + result.getStatus() +
                                  "during initial handshaking");
         }
         break;

      default: // NOT_HANDSHAKING/NEED_TASK/FINISHED
         throw new RuntimeException("Invalid Handshaking State" + initialHSStatus);
      } // switch

      return initialHSComplete;
   }

   /*
    * Do all the outstanding handshake tasks in the current Thread.
    */
    SSLEngineResult.HandshakeStatus doTasks() {
      /*
       * We could run this in a separate thread, but
       * do in the current for now.
       */
      for (;;) {
         final Runnable runnable = sslEngine.getDelegatedTask();
         if (runnable==null) break;
         runnable.run();
      }
      return sslEngine.getHandshakeStatus();
   }

   /*
    * Read the channel for more information, then unwrap the
    * (hopefully application) data we get.
    * <P>
    * If we run out of data, we'll return to our caller (possibly using
    * a Selector) to get notification that more is available.
    * <P>
    * Each call to this method will perform at most one underlying read().
    */
   int read() throws IOException {

      if (!initialHSComplete) throw new IllegalStateException();

      final int pos = requestBB.position();

      if (sc.read(inNetBB) == -1) {
         sslEngine.closeInbound();  // probably throws exception
         return -1;
      }

      SSLEngineResult result;
      do {
         resizeRequestBB();    // guarantees enough room for unwrap
         inNetBB.flip();
         result = sslEngine.unwrap(inNetBB, requestBB);
         inNetBB.compact();

         /*
          * Could check here for a renegotation, but we're only
          * doing a simple read/write, and won't have enough state
          * transitions to do a complete handshake, so ignore that
          * possibility.
          */
         switch (result.getStatus()) {

         case BUFFER_UNDERFLOW:
         case OK:
            if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
               doTasks();
            }
            break;

         default:
            throw new IOException("sslEngine error during data read: " + result.getStatus());
         }
      } while ((inNetBB.position() != 0) && result.getStatus() != Status.BUFFER_UNDERFLOW);

      return (requestBB.position() - pos);
   }

   /*
    * Try to write out as much as possible from the src buffer.
    */
   int write(ByteBuffer src) throws IOException {

      if (!initialHSComplete) {
         throw new IllegalStateException();
      }

      return doWrite(src);
   }

   /*
    * Try to flush out any existing outbound data, then try to wrap
    * anything new contained in the src buffer.
    * <P>
    * Return the number of bytes actually consumed from the buffer,
    * but the data may actually be still sitting in the output buffer,
    * waiting to be flushed.
    */
    int doWrite(ByteBuffer src) throws IOException {
      int retValue = 0;

      if (outNetBB.hasRemaining() && !tryFlush(outNetBB)) {
         return retValue;
      }

      /*
       * The data buffer is empty, we can reuse the entire buffer.
       */
      outNetBB.clear();

      final SSLEngineResult result = sslEngine.wrap(src, outNetBB);
      retValue = result.bytesConsumed();

      outNetBB.flip();

      switch (result.getStatus()) {

      case OK:
         if (result.getHandshakeStatus() == HandshakeStatus.NEED_TASK) {
            doTasks();
         }
         break;

      default:
         throw new IOException("sslEngine error during data write: " +
                               result.getStatus());
      }

      /*
       * Try to flush the data, regardless of whether or not
       * it's been selected.  Odds of a write buffer being full
       * is less than a read buffer being empty.
       */
      if (outNetBB.hasRemaining()) {
         tryFlush(outNetBB);
      }

      return retValue;
   }

   /*
    * Perform a FileChannel.TransferTo on the socket channel.
    * <P>
    * We have to copy the data into an intermediary app ByteBuffer
    * first, then send it through the SSLEngine.
    * <P>
    * We return the number of bytes actually read out of the
    * filechannel.  However, the data may actually be stuck
    * in the fileChannelBB or the outNetBB.  The caller
    * is responsible for making sure to call dataFlush()
    * before shutting down.
    */
   long transferTo (final FileChannel fc, long pos, long len) throws IOException {

      if (!initialHSComplete) {
         throw new IllegalStateException();
      }

      if (fileChannelBB == null) {
         fileChannelBB = ByteBuffer.allocate(appBBSize);
         fileChannelBB.limit(0);
      }

      fileChannelBB.compact();
      final long fileRead = fc.read(fileChannelBB);
      fileChannelBB.flip();

      /*
       * We ignore the return value here, we return the
       * number of bytes actually consumed from the the file.
       * We'll flush the output buffer before we start shutting down.
       */
      doWrite(fileChannelBB);

      return fileRead;
   }

   /*
    * Flush any remaining data.
    * <P>
    * Return true when the fileChannelBB and outNetBB are empty.
    */
   boolean dataFlush() throws IOException {
      boolean fileFlushed = true;

      if ((fileChannelBB != null) && fileChannelBB.hasRemaining()) {
         doWrite(fileChannelBB);
         fileFlushed = !fileChannelBB.hasRemaining();
      } else if (outNetBB.hasRemaining()) {
         tryFlush(outNetBB);
      }

      return (fileFlushed && !outNetBB.hasRemaining());
   }

   /*
    * We have received the shutdown request by our caller, and have
    * closed our outbound side.
    */
    boolean shutdown = false;

   /*
    * Begin the shutdown process.
    * <P>
    * Close out the SSLEngine if not already done so, then
    * wrap our outgoing close_notify message and try to send it on.
    * <P>
    * Return true when we're done passing the shutdown messsages.
    */
   boolean shutdown() throws IOException {

      if (!shutdown) {
         sslEngine.closeOutbound();
         shutdown = true;
      }

      if (outNetBB.hasRemaining() && tryFlush(outNetBB)) {
         return false;
      }

      /*
       * By RFC 2616, we can "fire and forget" our close_notify
       * message, so that's what we'll do here.
       */
      outNetBB.clear();
      final SSLEngineResult result = sslEngine.wrap(hsBB, outNetBB);
      if (result.getStatus() != Status.CLOSED) {
         throw new SSLException("Improper close state");
      }
      outNetBB.flip();

      /*
       * We won't wait for a select here, but if this doesn't work,
       * we'll cycle back through on the next select.
       */
      if (outNetBB.hasRemaining()) {
         tryFlush(outNetBB);
      }

      return (!outNetBB.hasRemaining() && (result.getHandshakeStatus() != HandshakeStatus.NEED_WRAP));
   }

   /*
    * close() is not overridden
    */
}
/**
 * A helper class for properly sizing inbound byte buffers and
 * redirecting I/O calls to the proper SocketChannel call.
 * <P>
 * Many of these calls may seem unnecessary until you consider
 * that they are placeholders for the secure variant, which is much
 * more involved.  See ChannelIOSecure for more information.
 *
 * @author Brad R. Wetmore
 * @author Mark Reinhold
 * @version 1.2, 04/07/26
 */
class ChannelIO {

    protected final SocketChannel sc;

    /*
     * All of the inbound request data lives here until we determine
     * that we've read everything, then we pass that data back to the
     * caller.
     */
    protected ByteBuffer requestBB;
    static  int requestBBSize = 4096;

    protected ChannelIO (final SocketChannel sc, final boolean blocking) throws IOException {
	this.sc = sc;
	sc.configureBlocking (blocking);
    }

    SocketChannel getSocketChannel() {
	return sc;
    }

    /*
     * Return a ByteBuffer with "remaining" space to work.  If you have to
     * reallocate the ByteBuffer, copy the existing info into the new buffer.
     */
    protected void resizeRequestBB (final int remaining) {
       while(requestBB.remaining() < remaining) {
          // Expand buffer for large request

           requestBB=ByteBuffer.allocate(requestBB.capacity() * 2).put((ByteBuffer) requestBB.flip());
       }
    }

    /*
     * Perform any handshaking processing.
     * <P>
     * This variant is for Servers without SelectionKeys (e.g.
     * blocking).
     * <P>
     * return true when we're done with handshaking.
     */
    boolean doHandshake() throws IOException {
	return true;
    }

    /*
     * Perform any handshaking processing.
     * <P>
     * This variant is for Servers with SelectionKeys, so that
     * we can register for selectable operations (e.g. selectable
     * non-blocking).
     * <P>
     * return true when we're done with handshaking.
     */
    boolean doHandshake(SelectionKey sk) throws IOException {
	return true;
    }

    /*
     * Resize (if necessary) the inbound data buffer, and then read more
     * data into the read buffer.
     */
    int read() throws IOException {
	/*
	 * Allocate more space if less than 5% remains
	 */
	resizeRequestBB(requestBBSize/20);
	return sc.read(requestBB);
    }

    /*
     * All data has been read, pass back the request in one buffer.
     */
    ByteBuffer getReadBuf() {
	return requestBB;
    }

    /*
     * Write the src buffer into the socket channel.
     */
    int write(ByteBuffer src) throws IOException {
	return sc.write(src);
    }

    /*
     * Perform a FileChannel.TransferTo on the socket channel.
     */
    long transferTo (final FileChannel fc, long pos, long len) throws IOException {
	return fc.transferTo(pos, len, sc);
    }

    /*
     * Flush any outstanding data to the network if possible.
     * <P>
     * This isn't really necessary for the insecure variant, but needed
     * for the secure one where intermediate buffering must take place.
     * <P>
     * Return true if successful.
     */
    boolean dataFlush() throws IOException {
	return true;
    }

    /*
     * Start any connection shutdown processing.
     * <P>
     * This isn't really necessary for the insecure variant, but needed
     * for the secure one where intermediate buffering must take place.
     * <P>
     * Return true if successful, and the data has been flushed.
     */
    boolean shutdown() throws IOException {
	return true;
    }

    /*
     * Close the underlying connection.
     */
    void close() throws IOException {
	sc.close();
    }

}
/*
 * Base class for the Handlers.
 *
 * @author Mark Reinhold
 * @author Brad R. Wetmore
 * @version 1.2, 04/07/26
 */
interface Handler {

    void handle (SelectionKey sk) throws IOException;

}
/**
 * Base class for the Dispatchers.
 * <P>
 * Servers use these to obtain ready status, and then to dispatch jobs.
 *
 * @author Mark Reinhold
 * @author Brad R. Wetmore
 * @version 1.2, 04/07/26
 */
interface Dispatcher extends Runnable {

    void register (SelectableChannel ch, int ops, Handler h) throws IOException;

}

final class AcceptHandler implements Handler {

     final ServerSocketChannel channel;
     final Dispatcher dsp;
     final SSLContext sslContext;

    AcceptHandler(ServerSocketChannel ssc, Dispatcher dsp, SSLContext sslContext) {
        channel = ssc;
        this.dsp = dsp;
        this.sslContext = sslContext;
    }

    public void handle (SelectionKey sk) throws IOException {
        if (!sk.isAcceptable()) return;

        final SocketChannel sc = channel.accept();
        if (sc == null) return;

        ChannelIOSecure cio1 = new ChannelIOSecure(sc, false, sslContext);
        cio1.appBBSize = cio1.sslEngine.getSession().getApplicationBufferSize();
        cio1.requestBB = ByteBuffer.allocate(cio1.appBBSize);
        ChannelIO cio2 = new ChannelIO(sc, false);
        cio2.requestBB = ByteBuffer.allocate(ChannelIO.requestBBSize);
        ChannelIO cio = (sslContext != null ?
                cio1 :
                cio2);

        RequestHandler rh = new RequestHandler(cio);
        dsp.register(cio.getSocketChannel(), SelectionKey.OP_READ, rh);
    }
}