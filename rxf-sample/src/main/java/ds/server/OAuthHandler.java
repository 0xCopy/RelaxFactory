package ds.server;

import one.xio.AsioVisitor.Impl;
import one.xio.MimeType;
import rxf.server.ActionBuilder;
import rxf.server.BlobAntiPatternObject;
import rxf.server.PreRead;
import rxf.server.Rfc822HeaderState;
import rxf.server.Rfc822HeaderState.HttpRequest;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpHeaders.Content$2dType;
import static one.xio.HttpMethod.UTF8;
import static rxf.server.BlobAntiPatternObject.*;

/**
 * this requiers some https, but sslEngine done right would require a keystore operation in the build process, so I'm
 * tabling SSLEngine and using URL connection for now....
 * <p/>
 * User: jim
 * Date: 7/16/12
 * Time: 6:11 PM
 */
public class OAuthHandler extends Impl implements PreRead {

    HttpRequest req;
    ByteBuffer cursor = null;
    private SocketChannel channel;
    String payload;
    ByteBuffer output;


    @Override
    public void onRead(final SelectionKey key) throws Exception {
        channel = (SocketChannel) key.channel();
        if (cursor == null) {
            if (key.attachment() instanceof Object[]) {
                Object[] ar = (Object[]) key.attachment();
                for (Object o : ar) {
                    if (o instanceof ByteBuffer) {
                        cursor = (ByteBuffer) o;
                    } else if (o instanceof Rfc822HeaderState) {
                        req = ((Rfc822HeaderState) o).$req();
                    }
                }
            }
            key.attach(this);
        }
        cursor = null == cursor ? ByteBuffer.allocateDirect(getReceiveBufferSize()) : cursor.hasRemaining() ? cursor : ByteBuffer.allocateDirect(cursor.capacity() << 2).put((ByteBuffer) cursor.rewind());
        int read = channel.read(cursor);
        if (read == -1) {
            key.cancel();
            return;
        }
        Buffer flip = cursor.duplicate().flip();
        req = (HttpRequest) ActionBuilder.get().state().$req().apply((ByteBuffer) flip);
        if (!BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, req.headerBuf())) {
            return;
        }
        cursor = cursor.slice();
        int remaining = 0;
        if (!req.headerStrings().containsKey(Content$2dLength.getHeader())) {

            String method = req.method();
            if ("GET".equals(method)) {
                //use the existing linkedhashmap to store the query parms and figure out what to do with it later...
                final String query = new URL("http://" + req.path()).getQuery();
                if (null == query) {
                    key.cancel();
                    return;
                }
                String[] decl = query.split("\\&");
                for (String pair : decl) {
                    String[] kv = pair.split("\\=", 2);
                    req.headerString(kv[0], kv[1]);
                }
                // do something
                System.err.println("??? " + deepToString("results of oauth: ", req.headerStrings()));

                key.interestOps(0);
//                POST https://www.googleapis.com/identitytoolkit/v1/relyingparty/verifyAssertion
//              SocketChannel goog = (SocketChannel) SocketChannel.open().configureBlocking(false);
//                goog.connect(REMOTE);
//                goog.register(getSelector(), OP_WRITE | OP_CONNECT, new OAuthVerifier(this, key));
                EXECUTOR_SERVICE.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
//                            System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
//                            java.security.Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
                            URL url = new URL("https://www.googleapis.com/identitytoolkit/v1/relyingparty/verifyAssertion?key=AIzaSyBIRwIq-3Op3r5taLhE2_t_fbjRmGbmMNY");
                            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                            connection.setDoInput(true);
                            connection.setDoOutput(true);
//                            String cookieHeader = connection.getHeaderField("set-cookie");
//                            if (cookieHeader != null) {
//                                int index = cookieHeader.indexOf(";");
//                                String cuki = "";
//                                if (index >= 0)
//                                    cuki = cookieHeader.substring(0, index);
//                                connection.setRequestProperty("Cookie", cuki);
//                            }
                            //                    .headerString(Content$2dType, MimeType.json.contentType)

                            connection.setRequestProperty(Content$2dType.getHeader(), MimeType.json.contentType);
//                            connection.setRequestProperty("Host", "www.googleapis.com");
                            connection.setRequestMethod("POST");
                            connection.setFollowRedirects(true);
                            String s = "{\n" +
                                    "  \"requestUri\": \"" + req.path() + "\",\n" +
                                    "  \"postBody\": \"" + UTF8.decode((ByteBuffer) cursor.duplicate().rewind()).toString().trim() + "\",\n" +
                                    "  \"returnOauthToken\": \"true\"\n" +
                                    "}";
                            final byte[] bytes = s.getBytes(UTF8);
                            connection.setRequestProperty(Content$2dLength.getHeader(), String.valueOf(bytes.length));
//                            String query = "UserID=" + URLEncoder.encode("williamalex@hotmail.com");
//                            query += "&";
//                            query += "password=" + URLEncoder.encode("password");
//                            query += "&";
//                            query += "UserChk=" + URLEncoder.encode("Bidder");
// This particular website I was working with, required that the referrel URL should be from this URL
// as specified the previousURL. If you do not have such requirement you may omit it.
//                            query += "&";
//                            query += "PreviousURL=" + URLEncoder.encode("https://www.anysecuresite.com.sg/auct.cfm");

//connection.setRequestProperty("Accept-Language","it");
                            connection.setRequestProperty("Accept", MimeType.json.contentType);
                            connection.setRequestProperty("Accept-Encoding", "gzip");

//                            connection.setRequestProperty("Content-length", String.valueOf(query.length()));
//                            connection.setRequestProperty("Content-Type", "application/x-www- form-urlencoded");
//                            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 5.0; Windows 98; DigExt)");

// open up the output stream of the connection
                            DataOutputStream output = new DataOutputStream(connection.getOutputStream());

// write out the data
//                            int queryLength = bytes.length();
                            output.write(bytes);
//output.close();

                            System.out.println("Resp Code:" + connection.getResponseCode());
                            System.out.println("Resp Message:" + connection.getResponseMessage());

// get ready to read the response from the cgi script
                            DataInputStream input = new DataInputStream(connection.getInputStream());

// read in each character until end-of-stream is detected
                            for (int c = input.read(); c != -1; c = input.read())
                                System.out.print((char) c);
                            input.close();
                        } catch (IOException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        } finally {
                        }

                    }
                });
            }
            return;
        } else {
            remaining = Integer.parseInt(req.headerString(Content$2dLength));
        }
        final Impl prev = this;
        if (cursor.remaining() != remaining) {
            key.attach(new Impl() {
                @Override
                public void onRead(SelectionKey key) throws Exception {
                    int read1 = channel.read(cursor);
                    if (read1 == -1) {
                        key.cancel();
                        return;
                    }
                    if (!cursor.hasRemaining()) {
                        key.interestOps(SelectionKey.OP_WRITE).attach(prev);
                    }
                }
            });
        }
        key.interestOps(SelectionKey.OP_WRITE);
    }

    @Override
    public String toString() {
        return "OAuthHandler{" +
                "req=" + req +
                ", cursor=" + cursor +
                ", channel=" + channel +
                ", payload='" + payload + '\'' +
                '}';
    }

    @Override
    public void onWrite(SelectionKey key) throws Exception {
        System.err.println(deepToString("???", "payload:", UTF8.decode((ByteBuffer) output.duplicate().flip())));
        key.cancel();
    }

}
