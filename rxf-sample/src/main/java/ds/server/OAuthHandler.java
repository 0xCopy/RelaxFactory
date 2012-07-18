package ds.server;

import java.net.*;
import java.nio.*;
import java.nio.channels.*;

import one.xio.AsioVisitor.Impl;
import rxf.server.*;
import rxf.server.Rfc822HeaderState.HttpRequest;

import static ds.server.OAuthVerifier.REMOTE;
import static java.nio.channels.SelectionKey.*;
import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpMethod.*;
import static rxf.server.BlobAntiPatternObject.*;

/**
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
              SocketChannel goog = (SocketChannel) SocketChannel.open().configureBlocking(false);
                goog.connect(REMOTE);
                goog.register(getSelector(), OP_WRITE | OP_CONNECT, new OAuthVerifier(this, key));
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
