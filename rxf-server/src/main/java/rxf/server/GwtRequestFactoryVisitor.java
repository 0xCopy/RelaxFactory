package rxf.server;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import com.google.web.bindery.requestfactory.server.ServiceLayer;
import com.google.web.bindery.requestfactory.server.SimpleRequestProcessor;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpHeaders;
import one.xio.MimeType;
import rxf.server.Rfc822HeaderState.HttpRequest;

import static one.xio.HttpMethod.UTF8;
import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.HEADER_TERMINATOR;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;

/**
 * User: jim
 * Date: 6/3/12
 * Time: 7:42 PM
 */
public class GwtRequestFactoryVisitor extends Impl implements PreRead {
  public static SimpleRequestProcessor SIMPLE_REQUEST_PROCESSOR = new SimpleRequestProcessor(ServiceLayer.create());
  HttpRequest req;
  ByteBuffer cursor = null;
  private SocketChannel channel;
  String payload;


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
    cursor = null == cursor ? ByteBuffer.allocateDirect(getReceiveBufferSize()) : cursor.hasRemaining() ? cursor : ByteBuffer.allocateDirect(cursor.capacity() << 1).put((ByteBuffer) cursor.rewind());
    int read = channel.read(cursor);
    if (read == -1)
      key.cancel();
    Buffer flip = cursor.duplicate().flip();
    req = (HttpRequest) ActionBuilder.get().state().$req().apply((ByteBuffer) flip);
    if (!BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, req.headerBuf())) {
      return;
    }
    cursor = ((ByteBuffer) cursor).slice();
    int remaining = Integer.parseInt(req.headerString(HttpHeaders.Content$2dLength));
    final GwtRequestFactoryVisitor prev = this;
    if (cursor.remaining() != remaining) key.attach(new Impl() {
      @Override
      public void onRead(SelectionKey key) throws Exception {
        int read1 = channel.read(cursor);
        if (read1 == -1)
          key.cancel();
        if (!cursor.hasRemaining()) {
          key.interestOps(SelectionKey.OP_WRITE).attach(prev);
          return;
        }
      }

    });
    key.interestOps(SelectionKey.OP_WRITE);
  }

  @Override
  public void onWrite(final SelectionKey key) throws Exception {
    if (payload == null) {
      key.interestOps(0);
      EXECUTOR_SERVICE.submit(new Runnable() {
        @Override
        public void run() {
          try {

            payload = SIMPLE_REQUEST_PROCESSOR.process(UTF8.decode((ByteBuffer) cursor.rewind()).toString());
            ByteBuffer pbuf = (ByteBuffer) UTF8.encode(payload).rewind();
            ByteBuffer as = req.$res().headerString(HttpHeaders.Content$2dLength, String.valueOf(cursor.limit())).headerString(HttpHeaders.Content$2dType, MimeType.json.contentType).as(ByteBuffer.class);
            int needed = as.rewind().limit() + pbuf.rewind().limit();
            cursor = (ByteBuffer) ((ByteBuffer) (cursor.capacity() >= needed ? cursor.clear().limit(needed) : ByteBuffer.allocateDirect(needed))).put(as).put(pbuf).rewind();

            key.interestOps(SelectionKey.OP_WRITE);
          } catch (Exception e) {
            key.cancel();
            e.printStackTrace();  //todo: verify for a purpose
          } finally {
          }
        }
      });
      return;
    }
    int write = channel.write(cursor);
    if (!cursor.hasRemaining()) {
      /*Socket socket = channel.socket();
      socket.getOutputStream().flush();
      socket.close();*/
      key.interestOps(SelectionKey.OP_READ).attach(null);
    }

  }
}