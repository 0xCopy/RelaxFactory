package rxf.server;

import java.nio.channels.SelectionKey;

import one.xio.AsioVisitor;

public class ThreadedSelectorDelegate<T> extends AsioVisitor.Impl {
  private AsioVisitor surrogate;

  public ThreadedSelectorDelegate(AsioVisitor surrogate) {


    this.surrogate = surrogate;
  }

  @Override
  public void onRead(final SelectionKey key) throws Exception {
    CouchMetaDriver.currentSync.set(ActionBuilder.currentAction.get().sync());
    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
      public void run() {
        try {

          surrogate.onRead(key);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }


  @Override
  public void onWrite(final SelectionKey key) throws Exception {
    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
      public void run() {
        try {
          surrogate.onWrite(key);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }


}
