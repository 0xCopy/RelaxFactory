package ds.server;

import javax.net.ssl.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.security.NoSuchAlgorithmException;

import one.xio.*;
import rxf.server.*;

import static java.nio.channels.SelectionKey.*;
import static one.xio.HttpHeaders.*;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpStatus.$200;
import static rxf.server.BlobAntiPatternObject.*;

/**
 * Created by IntelliJ IDEA.
 * User: jim
 * Date: 7/18/12
 * Time: 3:58 AM
 */
public class OAuthVerifier extends AsioVisitor.Impl {
  public static SSLContext sslContext;
  public static final String WWW_GOOGLEAPIS_COM = "www.googleapis.com";
  public static final InetSocketAddress REMOTE = new InetSocketAddress(WWW_GOOGLEAPIS_COM, 443);

  static {
    try {
      sslContext = SSLContext.getDefault();
    } catch (NoSuchAlgorithmException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }

  }

  private ByteBuffer idBuffer;
  private Rfc822HeaderState.HttpRequest httpRequest;
  public Rfc822HeaderState.HttpResponse res;
  private SocketChannel channel;
  private final SelectionKey parentKey;
  private OAuthHandler oAuthHandler;
  private SSLEngine sslEngine;
  private ByteBuffer sslBuf;

  public OAuthVerifier(OAuthHandler oAuthHandler, SelectionKey parentKey) {
    this.oAuthHandler = oAuthHandler;
    this.parentKey = parentKey;
    httpRequest = new Rfc822HeaderState().$req();

    sslEngine = sslContext.createSSLEngine(WWW_GOOGLEAPIS_COM, 443);
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
          .headerString("Host", WWW_GOOGLEAPIS_COM)
          .as(ByteBuffer.class);
      int limit = as.limit();
      int remaining = payload.limit() + as.limit();
      idBuffer = (ByteBuffer) ByteBuffer.allocateDirect(remaining).put((ByteBuffer) as.rewind()).put((ByteBuffer) payload.rewind()).rewind();
    }
    sslBuf = ByteBuffer.allocateDirect(idBuffer.capacity()<<2);
    SSLEngineResult wrap = sslEngine.wrap(idBuffer, sslBuf);
    if (DEBUG_SENDJSON) {
      System.err.println(deepToString("###", wrap, this,UTF8.decode((ByteBuffer) idBuffer.duplicate().rewind())).toString().trim());
    }
    int write = ((SocketChannel) selectionKey.channel()).write(sslBuf);
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

      sslBuf = ByteBuffer.allocateDirect(idBuffer.capacity());
      int read1 = channel.read(sslBuf);
      SSLEngineResult wrap = sslEngine.unwrap(sslBuf, idBuffer);
      if (DEBUG_SENDJSON) {
        System.err.println(deepToString("###", wrap, this));
      }
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
      sslBuf = ByteBuffer.allocateDirect(oAuthHandler.output.capacity());
          int read1 = channel.read(sslBuf);
          SSLEngineResult wrap = sslEngine.unwrap(sslBuf, oAuthHandler.output);
          if (DEBUG_SENDJSON) {
            System.err.println(deepToString("###", wrap, this));
          }
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
