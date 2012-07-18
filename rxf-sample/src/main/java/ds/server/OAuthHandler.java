package ds.server;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;
import one.xio.MimeType;
import rxf.server.ActionBuilder;
import rxf.server.BlobAntiPatternObject;
import rxf.server.PreRead;
import rxf.server.Rfc822HeaderState;
import rxf.server.Rfc822HeaderState.HttpRequest;
import rxf.server.Rfc822HeaderState.HttpResponse;

import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static java.nio.channels.SelectionKey.*;
import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpHeaders.Content$2dType;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpStatus.$200;
import static rxf.server.BlobAntiPatternObject.*;

/**
 * User: jim
 * Date: 7/16/12
 * Time: 6:11 PM
 */
public class OAuthHandler extends Impl implements PreRead {
    public static final InetSocketAddress REMOTE = new InetSocketAddress("www.googleapis.com", 80);
    HttpRequest req;
    ByteBuffer cursor = null;
    private SocketChannel channel;
    String payload;
    ByteBuffer output;
    private SocketChannel goog;


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
                // do something
                System.err.println("??? " + deepToString("results of oauth: ", req.headerStrings()));

                key.interestOps(0);
//                POST https://www.googleapis.com/identitytoolkit/v1/relyingparty/verifyAssertion
                goog = SocketChannel.open();

                HttpMethod.enqueue(goog, OP_WRITE | OP_CONNECT, new Impl() {

                    private CharBuffer decode = UTF8.decode((ByteBuffer) req.headerBuf().duplicate().rewind());
                    private ByteBuffer idBuffer;
                    private HttpRequest httpRequest = new Rfc822HeaderState().$req();
                    public HttpResponse res;
                    private SocketChannel channel1;

                    public void onWrite(SelectionKey selectionKey) throws Exception {
                        if (null == idBuffer) {
                            String s = "{\n" +
                                    "  \"requestUri\": " + req.path() + ",\n" +
                                    "  \"postBody\": " + UTF8.decode((ByteBuffer) cursor.duplicate().rewind()).toString().trim() + ",\n" +
                                    "  \"returnOauthToken\": \"true\"\n" +
                                    "}";
                            ByteBuffer payload = UTF8.encode(s);
                            ByteBuffer as = httpRequest.method(HttpMethod.POST)
                                    .path("/identitytoolkit/v1/relyingparty/verifyAssertion")
                                    .headerString(Content$2dLength, String.valueOf(payload.limit()))
                                    .headerString(Content$2dType, MimeType.json.contentType).as(ByteBuffer.class);
                            int limit = as.limit();
                            int remaining = payload.limit() + as.limit();
                            idBuffer = (ByteBuffer) ByteBuffer.allocateDirect(remaining).put((ByteBuffer) as.rewind()).put((ByteBuffer) payload.rewind()).rewind();
                        }
                        int write = ((SocketChannel) selectionKey.channel()).write(idBuffer);
                        if (-1 == write) {
                            selectionKey.cancel();
                            key.cancel();
                            return;
                        }
                        if (!idBuffer.hasRemaining()) {
                            selectionKey.interestOps(OP_READ);
                        }
                        idBuffer = null;
                    }


                    public void onRead(SelectionKey selectionKey) throws Exception {
                        //todo: pathological receive buffers...
                        if (output == null) {
                            if (null == idBuffer) {
                                idBuffer = ByteBuffer.allocateDirect(getReceiveBufferSize());
                                res = req.$res();
                                res.headerInterest(Content$2dLength);
                                channel1 = (SocketChannel) selectionKey.channel();
                            }
                            int read1 = channel1.read(idBuffer);
                            if (-1 == read1) {
                                selectionKey.cancel();
                                key.cancel();
                                return;
                            }
                            Rfc822HeaderState headerState = res.apply((ByteBuffer) idBuffer.duplicate().rewind());
                            if (!BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, headerState.headerBuf())) {
                                return;
                            }
                            if ($200 != res.statusEnum()) {
                                selectionKey.cancel();
                                key.cancel();
                                return;
                            }
                            int remaining = Integer.parseInt(res.headerString(Content$2dLength));
                            output = ByteBuffer.allocateDirect(remaining).put(idBuffer.slice());
                        } else {
                            int read1 = channel1.read(output);
                            if (-1 == read1) {
                                selectionKey.cancel();
                                key.cancel();
                                return;
                            }
                            if (!output.hasRemaining()) {
                                key.interestOps(OP_WRITE);// return a result somewhere...
                            }
                        }
                    }

                    public void onConnect(SelectionKey key) throws Exception {
                        key.interestOps(OP_WRITE);
                    }
                });
                                  goog.connect(REMOTE);
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
