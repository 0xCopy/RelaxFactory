package rxf.web.inf;

import one.xio.AsioVisitor;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;

/**
 * Created by jim on 5/21/14.
 */
public abstract class FinishWrite extends AsioVisitor.Impl implements HasSuccess {

  public FinishWrite(ByteBuffer... x) {
    int total = 0;
    if (1 < x.length) {
      for (ByteBuffer byteBuffer : x) {
        total += byteBuffer.remaining();
      }
      cursor = ByteBuffer.allocateDirect(total);
      for (ByteBuffer byteBuffer : x) {
        cursor.put(byteBuffer);

      }
      cursor.rewind();
    } else
      this.cursor = x[0];
  }

  ByteBuffer cursor;

  @Override
  public void onWrite(SelectionKey key) throws Exception {
    int write = ((WritableByteChannel) key.channel()).write(cursor);
    if (-1 == write)
      key.cancel();
    if (!cursor.hasRemaining())
      onSuccess();

  }

  public static FinishWrite finishWrite(final Runnable success, ByteBuffer... payload) {

    return new FinishWrite(payload) {
      @Override
      public void onSuccess() {
        success.run();
      }
    };
  }

  public static FinishWrite finishWrite(ByteBuffer payload, Runnable onSuccess) {
    return finishWrite(onSuccess, payload);
  }

  public static Void finishWrite(SelectionKey key, Runnable onSuccess, ByteBuffer... payload) {
    key.interestOps(SelectionKey.OP_WRITE).attach(finishWrite(onSuccess, payload));
    return null;
  }

  public static Void finishWrite(SelectionKey key, ByteBuffer payload, Runnable onSuccess) {
    key.interestOps(SelectionKey.OP_WRITE).attach(finishWrite(onSuccess, payload));
    return null;
  }
}