package rxf.web.inf;

import one.xio.AsioVisitor;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;

/**
 * Created by jim on 5/21/14.
 */
public abstract class FinishRead extends AsioVisitor.Impl implements HasSuccess {
  public FinishRead(ByteBuffer cursor) {
    this.cursor = cursor;
  }

  ByteBuffer cursor;

  @Override
  public void onRead(SelectionKey key) throws Exception {
    int read = ((ReadableByteChannel) key.channel()).read(cursor);
    if (read == -1)
      key.cancel();
    if (!cursor.hasRemaining())
      onSuccess();
  }

  public static FinishRead finishRead(ByteBuffer payload, final Runnable success) {
    return new FinishRead(payload) {

      @Override
      public void onSuccess() {
        success.run();
      }
    };
  }

  public static Void finishRead(SelectionKey key, ByteBuffer payload, Runnable runnable) {
    key.interestOps(SelectionKey.OP_READ).attach(finishRead(payload, runnable));
    return null;
  }
}
