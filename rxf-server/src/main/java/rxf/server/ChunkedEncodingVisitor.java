package rxf.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;

import one.xio.AsioVisitor;

import static one.xio.HttpMethod.UTF8;
import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;

/**
 * User: jim
 * Date: 5/30/12
 * Time: 7:09 PM
 */
class ChunkedEncodingVisitor extends AsioVisitor.Impl {
  ByteBuffer cursor;

  private Impl prev;
  LinkedList<ByteBuffer> ret;
  private final ByteBuffer dst;
  private final int receiveBufferSize;
  // private final SocketChannel channel;
  private SynchronousQueue<String> returnTo;

  public ChunkedEncodingVisitor(ByteBuffer dst, int receiveBufferSize, SynchronousQueue<String> returnTo) {
    this.dst = dst;
    this.receiveBufferSize = receiveBufferSize;
//   this.channel = channel;
    this.returnTo = returnTo;
    cursor = dst.slice();
    prev = this;
    ret = new LinkedList<ByteBuffer>();
  }

  @Override
  public void onRead(SelectionKey key) throws Exception {//chuksizeparser
    final SocketChannel channel = (SocketChannel) key.channel();

    if (null == cursor) {
      cursor = ByteBuffer.allocate(receiveBufferSize);
      int read1 = channel.read(cursor);
      cursor.flip();
    }
    System.err.println("chunking: " + UTF8.decode(cursor.duplicate()));
    int anchor = cursor.position();
    while (cursor.hasRemaining() && '\n' != cursor.get()) ;
    ByteBuffer line = (ByteBuffer) cursor.duplicate().position(anchor).limit(cursor.position());
    String res = UTF8.decode(line).toString().trim();
    long chunkSize = 0;
    try {

      chunkSize = Long.parseLong(res, 0x10);


      if (0 == chunkSize) {
        //send the unwrap to threadpool.
        EXECUTOR_SERVICE.submit(new Callable() {
          public Void call() throws InterruptedException {
            int sum = 0;
            for (ByteBuffer byteBuffer : ret) {
              sum += byteBuffer.limit();
            }
            ByteBuffer allocate = ByteBuffer.allocate(sum);
            for (ByteBuffer byteBuffer : ret) {
              allocate.put((ByteBuffer) byteBuffer.flip());
            }

            String o = UTF8.decode((ByteBuffer) allocate.flip()).toString();
            System.err.println("total chunked bundle was: " + o);
            returnTo.put(o);
            return null;
          }
        });
        key.selector().wakeup();
        key.interestOps(SelectionKey.OP_READ).attach(null);
        return;
      }
    } catch (NumberFormatException ignored) {


    }
    final ByteBuffer dest = ByteBuffer.allocate((int) chunkSize);
    if (!(chunkSize < cursor.remaining())) {//fragments to assemble

      dest.put(cursor);
      key.attach(new Impl() {
        @Override
        public void onRead(SelectionKey key) throws Exception {
          int read1 = channel.read(dest);
          key.selector().wakeup();
          if (!dest.hasRemaining()) {
            key.attach(prev);
            cursor = null;
            ret.add(dest);
          }
        }
      });
    } else {
      ByteBuffer src = (ByteBuffer) cursor.slice().limit((int) chunkSize);
      cursor.position((int) (cursor.position() + chunkSize + 2));
//                      cursor = dest;
      dest.put(src);
      ret.add(dest);
      onRead(key);      // a goto
    }

  }
}
