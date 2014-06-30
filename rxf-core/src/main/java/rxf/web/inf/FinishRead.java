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
}
