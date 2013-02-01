package ds.server;

import one.xio.AsioVisitor.Impl;
import one.xio.MimeType;
import rxf.server.ActionBuilder;
import rxf.server.PreRead;
import rxf.server.Rfc822HeaderState;
import rxf.server.Rfc822HeaderState.HttpRequest;
import rxf.server.gen.CouchDriver;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;

import static one.xio.HttpHeaders.*;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpStatus.$200;
import static rxf.server.BlobAntiPatternObject.*;
import static rxf.server.driver.CouchMetaDriver.HEADER_TERMINATOR;
import static rxf.server.gen.CouchDriver.GSON;

/**
 * this requiers some https, but sslEngine done right would require a keystore operation in the build process, so I'm
 * tabling SSLEngine and using URL connection for now....
 * <p/>
 * User: jim
 * Date: 7/16/12
 * Time: 6:11 PM
 */
public class OAuthHandler extends Impl implements PreRead {

    public static final String WWW_GOOGLEAPIS_COM = "www.googleapis.com";
    HttpRequest req;
    ByteBuffer cursor;
    private SocketChannel channel;
    private Map payload;
    private ByteBuffer outgoing;

    static {
//        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
//        System.setProperty("javax.net.debug", "all");
    }

    @Override
    public void onRead(final SelectionKey key) throws Exception {
        channel = (SocketChannel) key.channel();
        if (null == cursor) {
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
        if (-1 == read) {
            key.cancel();
            return;
        }
        Buffer flip = cursor.duplicate().flip();
        req = (HttpRequest) ActionBuilder.get().state().$req().apply((ByteBuffer) flip);
        if (suffixMatchChunks(HEADER_TERMINATOR, req.headerBuf())) {
            cursor = cursor.slice();
            int remaining = 0;
            if (!req.headerStrings().containsKey(Content$2dLength.getHeader())) {

                String method = req.method();
                if ("GET".equals(method)) {
                    String query = new URL("http://" + req.path()).getQuery();
                    if (null == query) {
                        key.cancel();
                        return;
                    }
                    String[] decl = query.split("\\&");
                    for (String pair : decl) {
                        String[] kv = pair.split("\\=", 2);
                        req.headerString(kv[0], kv[1]);
                    }
                    System.err.println("??? " + deepToString("results of oauth: ", req.headerStrings()));

                    key.interestOps(0);
                    EXECUTOR_SERVICE.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                URL url = new URL("https://www.googleapis.com/identitytoolkit/v1/relyingparty/verifyAssertion?key=AIzaSyBIRwIq-3Op3r5taLhE2_t_fbjRmGbmMNY");
                                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                                connection.setDoInput(true);
                                connection.setDoOutput(true);

                                connection.setRequestProperty(Content$2dType.getHeader(), MimeType.json.contentType);
                                connection.setRequestMethod("POST");
                                connection.setFollowRedirects(true);
                                String s = "{\n" +
                                        "  \"requestUri\": \"" + req.path() + "\",\n" +
                                        "  \"postBody\": \"" + UTF8.decode((ByteBuffer) cursor.duplicate().rewind()).toString().trim() + "\",\n" +
                                        "  \"returnOauthToken\": \"true\"\n" +
                                        "}";
                                byte[] bytes = s.getBytes(UTF8);
                                connection.setRequestProperty(Content$2dLength.getHeader(), String.valueOf(bytes.length));
                                connection.setRequestProperty(Accept.getHeader(), MimeType.json.contentType);
                                connection.setRequestProperty(Accept$2dEncoding.getHeader(), "gzip");
                                DataOutputStream output = new DataOutputStream(connection.getOutputStream());

                                output.write(bytes);

                                System.out.println("Resp Code:" + connection.getResponseCode());
                                System.out.println("Resp Message:" + connection.getResponseMessage());

    // get ready to read the response from the cgi script
                                BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                                StringBuilder out = new StringBuilder();
    // read in each character until end-of-stream is detected
                                for (int c = input.read(); -1 != c; c = input.read()) {
                                    out.append((char) c);
                                }
    //                            System.err.println("+++ received " + out);

                                payload = GSON.fromJson(out.toString(), Map.class);
                                key.interestOps(SelectionKey.OP_WRITE);
                                connection.disconnect();
                            } catch (IOException e) {
                                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                            }
                        }
                    });
                }
                return;
            }
            remaining = Integer.parseInt(req.headerString(Content$2dLength));
            final Impl prev = this;
            if (cursor.remaining() != remaining) {
                key.attach(new Impl() {
                    @Override
                    public void onRead(SelectionKey key) throws Exception {
                        int read1 = channel.read(cursor);
                        if (-1 == read1) {
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
        } else {
            return;
        }
    }

    @Override
    public String toString() {
        return CouchDriver.GSON.toJson(this);
    }

    @Override
    public void onWrite(SelectionKey key) throws Exception {
        if (null == outgoing) {
            //todo: assign login to user ****** HERE ******
            /*
    , cursor=java.nio.DirectByteBuffer[pos=0 lim=31864 cap=31864], channel=java.nio.channels.SocketChannel[
    connected local=/127.0.0.1:8080 remote=/127.0.0.1:5716], payload='{
    kind=identitytoolkit#relyingparty, identifier=https://accounts.google.com/9752836876294756278345,
    authority=https://accounts.google.com, verifiedEmail=sdahfdfldh@gmail.com,
    displayName=James Northrup, firstName=asjdk, lastName=sdal, fullName=James Northrup,
    profilePicture=https://jhdflaahdfaldhasd/photo.jpg,
    photoUrl=https://lafsdhalsdafsd/photo.jpg,
    oauthAccessToken=ya29.AHES6ZTiZhF6mF_SBsqtvPaHwbF63DzP5DYVkmbRasadC1boWQ, oauthExpireIn=3599.0,
    context={"rp_target":"callback","rp_purpose":"signin"}}'}

            */

            //per https://developers.google.com/identity-toolkit/v1/acguide#open_id_callback_logic_pseudocode
            outgoing = UTF8.encode(

                    "<script type='text/javascript' src='https://ajax.googleapis.com/jsapi'></script>\n" +
                            "<script type='text/javascript'>" +
                            "google.load(\"identitytoolkit\", \"1.0\", {packages: [\"notify\"]});\n" +
                            "</script>\n" +
                            "<script type='text/javascript'>\n" +
                            "window.google.identitytoolkit.notifyFederatedSuccess({ \"email\": \"" + payload.get("verifiedEmail") +
                            "\", \"registered\": true });\n" +
                            "// use window.google.identitytoolkit.notifyFederatedError(); in case of error\n" +
                            "alert('logged in?');"+
                            "</script>");
            Rfc822HeaderState.HttpResponse res = ((Rfc822HeaderState.HttpResponse) req.$res()).status($200);
            ByteBuffer as = res.headerString(Content$2dLength, String.valueOf(outgoing.limit()))
                    .headerString(Content$2dType, MimeType.html.contentType).as(ByteBuffer.class);

            ByteBuffer put = ByteBuffer.allocateDirect(as.limit() + outgoing.limit()).put((ByteBuffer) as.rewind()).put((ByteBuffer) outgoing.rewind());
            outgoing = (ByteBuffer) put.rewind();
        }

        int write = channel.write(outgoing);
        if (!outgoing.hasRemaining()) {
            key.interestOps(SelectionKey.OP_READ);
        }

    }

}
