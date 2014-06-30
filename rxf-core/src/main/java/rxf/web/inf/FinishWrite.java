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
    if (x.length > 1) {
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
    if (write == -1)
      key.cancel();
    if (!cursor.hasRemaining())
      onSuccess();

  }
}
