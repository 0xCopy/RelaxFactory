package ds.server;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import one.xio.MimeType;
import rxf.server.BlobAntiPatternObject;
import rxf.server.Rfc822HeaderState;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpHeaders.Content$2dType;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpStatus.$200;
import static rxf.server.BlobAntiPatternObject.HEADER_TERMINATOR;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;

/**
 * Created by IntelliJ IDEA.
 * User: jim
 * Date: 7/18/12
 * Time: 3:58 AM
 * To change this template use File | Settings | File Templates.
 */
public class OAuthVerifier extends AsioVisitor.Impl {

    private CharBuffer decode;
    private ByteBuffer idBuffer;
    private Rfc822HeaderState.HttpRequest httpRequest;
    public Rfc822HeaderState.HttpResponse res;
    private SocketChannel channel;
    private final SelectionKey parentKey;
    private OAuthHandler oAuthHandler;

    public OAuthVerifier(OAuthHandler oAuthHandler, SelectionKey parentKey) {
        this.oAuthHandler = oAuthHandler;
        this.parentKey = parentKey;
        decode = UTF8.decode((ByteBuffer) oAuthHandler.req.headerBuf().duplicate().rewind());
        httpRequest = new Rfc822HeaderState().$req();
    }

    public void onConnect(SelectionKey selectionKey1) throws Exception {
        channel = (SocketChannel) selectionKey1.channel();
        if (channel.finishConnect())
            selectionKey1.interestOps(OP_WRITE);
    }

    public void onWrite(SelectionKey selectionKey) throws Exception {
        if (null == idBuffer) {
            String s = "{\n" +
                    "  \"requestUri\": \"" + oAuthHandler.req.path() + "\",\n" +
                    "  \"postBody\": \"" + UTF8.decode((ByteBuffer) oAuthHandler.cursor.duplicate().rewind()).toString().trim() + "\",\n" +
                    "  \"returnOauthToken\": \"true\"\n" +
                    "}";
            ByteBuffer payload = UTF8.encode(s);
            ByteBuffer as = httpRequest.method(HttpMethod.POST)
                    .path("/identitytoolkit/v1/relyingparty/verifyAssertion")
                    .headerString(Content$2dLength, String.valueOf(payload.limit()))
                    .headerString(Content$2dType, MimeType.json.contentType)
                    .headerString("Host", OAuthHandler.WWW_GOOGLEAPIS_COM)
                    .as(ByteBuffer.class);
            int limit = as.limit();
            int remaining = payload.limit() + as.limit();
            idBuffer = (ByteBuffer) ByteBuffer.allocateDirect(remaining).put((ByteBuffer) as.rewind()).put((ByteBuffer) payload.rewind()).rewind();
        }
        int write = ((SocketChannel) selectionKey.channel()).write(idBuffer);
        if (-1 == write) {
            selectionKey.cancel();
            parentKey.cancel();
            return;
        }
        if (!idBuffer.hasRemaining()) {
            selectionKey.interestOps(OP_READ);
        }
        idBuffer = null;
    }


    public void onRead(SelectionKey selectionKey) throws Exception {
        //todo: pathological receive buffers...
        if (oAuthHandler.output == null) {
            if (null == idBuffer) {
                idBuffer = ByteBuffer.allocateDirect(getReceiveBufferSize());
                res = oAuthHandler.req.$res();
                res.headerInterest(Content$2dLength);

            }
            int read1 = channel.read(idBuffer);
            if (-1 == read1) {
                selectionKey.cancel();
                parentKey.cancel();
                return;
            }
            Rfc822HeaderState headerState = res.apply((ByteBuffer) idBuffer.duplicate().rewind());
            if (!BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, headerState.headerBuf())) {
                return;
            }
            if ($200 != res.statusEnum()) {
                selectionKey.cancel();
                parentKey.cancel();
                return;
            }
            int remaining = Integer.parseInt(res.headerString(Content$2dLength));
            oAuthHandler.output = ByteBuffer.allocateDirect(remaining).put(idBuffer.slice());
        } else {
            int read1 = channel.read(oAuthHandler.output);
            if (-1 == read1) {
                selectionKey.cancel();
                parentKey.cancel();
                return;
            }
            if (!oAuthHandler.output.hasRemaining()) {
                parentKey.interestOps(OP_WRITE);// return a result somewhere...
            }
        }
    }

}
