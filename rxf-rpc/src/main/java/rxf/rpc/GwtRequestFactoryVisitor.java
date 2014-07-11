package rxf.rpc;

import com.google.web.bindery.requestfactory.server.ServiceLayer;
import com.google.web.bindery.requestfactory.server.SimpleRequestProcessor;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpHeaders;
import one.xio.HttpStatus;
import one.xio.MimeType;
import rxf.core.Rfc822HeaderState;
import rxf.core.Rfc822HeaderState.HttpRequest;
import rxf.shared.PreRead;
import rxf.web.inf.ProtocolMethodDispatch;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * User: jim Date: 6/3/12 Time: 7:42 PM
 */
@PreRead
public class GwtRequestFactoryVisitor extends Impl {
  public static SimpleRequestProcessor SIMPLE_REQUEST_PROCESSOR = new SimpleRequestProcessor(
      ServiceLayer.create());
  private HttpRequest req;
  private ByteBuffer cursor = null;
  private SocketChannel channel;
  private String payload;

  @Override
  public void onRead(SelectionKey key) throws Exception {
    channel = (SocketChannel) key.channel();
    if (cursor == null) {
      if (key.attachment() instanceof Object[]) {
        Object[] ar = (Object[]) key.attachment();
        for (Object o : ar) {
          if (o instanceof ByteBuffer) {
            cursor = (ByteBuffer) o;
            continue;
          }
          if (o instanceof Rfc822HeaderState) {
            req = ((Rfc822HeaderState) o).$req();
          }
        }
      }
      key.attach(this);
    }
    cursor =
        null == cursor ? ByteBuffer.allocateDirect(4 << 10) : cursor.hasRemaining() ? cursor
            : ByteBuffer.allocateDirect(cursor.capacity() << 1).put((ByteBuffer) cursor.rewind());
    int read = FSM.read(key, cursor);
    if (read == -1)
      key.cancel();
    Buffer flip = cursor.duplicate().flip();
    req = (HttpRequest) req.headerInterest(HttpHeaders.Content$2dLength).read((ByteBuffer) flip);
    if (!Rfc822HeaderState.suffixMatchChunks(ProtocolMethodDispatch.HEADER_TERMINATOR, req
        .headerBuf())) {
      return;
    }
    cursor = cursor.slice();
    int remaining = Integer.parseInt(req.headerString(HttpHeaders.Content$2dLength));
    final GwtRequestFactoryVisitor prev = this;
    if (cursor.remaining() != remaining) {
      key.attach(new Impl() {
        @Override
        public void onRead(SelectionKey key) throws Exception {
          int read1 = FSM.read(key, cursor);
          if (read1 == -1) {
            key.cancel();
          }
          if (!cursor.hasRemaining()) {
            key.interestOps(SelectionKey.OP_WRITE).attach(prev);
          }
        }
      });
    } else {
      key.interestOps(SelectionKey.OP_WRITE);
    }
  }

  @Override
  public void onWrite(final SelectionKey key) throws Exception {
    if (payload == null) {
      key.interestOps(0);
      RpcHelper.getEXECUTOR_SERVICE().submit(new Runnable() {
        @Override
        public void run() {
          try {

            payload =
                SIMPLE_REQUEST_PROCESSOR.process(StandardCharsets.UTF_8.decode(
                    (ByteBuffer) cursor.rewind()).toString());
            ByteBuffer pbuf = (ByteBuffer) StandardCharsets.UTF_8.encode(payload).rewind();
            final int limit = pbuf.rewind().limit();
            Rfc822HeaderState.HttpResponse res = req.$res();
            res.status(HttpStatus.$200);
            ByteBuffer as =
                res.headerString(HttpHeaders.Content$2dType, MimeType.json.contentType)
                    .headerString(HttpHeaders.Content$2dLength, String.valueOf(limit)).as(
                        ByteBuffer.class);
            int needed = as.rewind().limit() + limit;

            cursor =
                (ByteBuffer) ((ByteBuffer) (cursor.capacity() >= needed ? cursor.clear().limit(
                    needed) : ByteBuffer.allocateDirect(needed))).put(as).put(pbuf).rewind();

            key.interestOps(SelectionKey.OP_WRITE);
          } catch (Exception e) {
            key.cancel();
            e.printStackTrace(); // todo: verify for a purpose
          } finally {
          }
        }
      });
      return;
    }
    int write = FSM.write(key, cursor);
    if (!cursor.hasRemaining()) {
      /*
       * Socket socket = channel.socket(); socket.getOutputStream().flush(); socket.close();
       */
      key.interestOps(SelectionKey.OP_READ).attach(null);
    }

  }
}