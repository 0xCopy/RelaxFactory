package rxf.web.inf;

import one.xio.AsioVisitor;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;

/**
 * Created by jim on 5/21/14.
 */
public class FinishWrite extends AsioVisitor.Impl {
  public FinishWrite(ByteBuffer cursor, Runnable success) {
    this.cursor = cursor;

    this.success = success;
  }

  ByteBuffer cursor;
  private Runnable success;

  @Override
  public void onWrite(SelectionKey key) throws Exception {
    int write = ((WritableByteChannel) key.channel()).write(cursor);
    if (write == -1)
      key.cancel();
    if (!cursor.hasRemaining())
      success.run();

  }

}
