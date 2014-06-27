package rxf.web.inf;

import one.xio.AsioVisitor;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;

/**
 * Created by jim on 5/21/14.
 */
public class FinishRead extends AsioVisitor.Impl {
  public FinishRead(ByteBuffer cursor, Runnable success) {
    this.cursor = cursor;

    this.success = success;
  }

  ByteBuffer cursor;
  private Runnable success;

  @Override
  public void onRead(SelectionKey key) throws Exception {
    int read = ((ReadableByteChannel) key.channel()).read(cursor);
    if (read == -1)
      key.cancel();
    if (!cursor.hasRemaining())
      success.run();
  }
}
