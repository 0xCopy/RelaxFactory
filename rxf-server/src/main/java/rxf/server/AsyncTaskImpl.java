package rxf.server;

import java.nio.channels.SelectionKey;
import java.util.concurrent.SynchronousQueue;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;

/**
 * User: jim
 * Date: 4/29/12
 * Time: 12:34 PM
 */
public abstract class AsyncTaskImpl<T> implements Task<T> {
  private AsioVisitor asioVisitor;
  private T[] data;      //optional preload


  public AsyncTaskImpl(AsioVisitor... asioVisitor) {
    this.asioVisitor = asioVisitor.length > 0 ? asioVisitor[0] : null;
  }

  public void onRead(SelectionKey selectionKey) throws Exception {
    asioVisitor.onRead(selectionKey);
  }

  public void onConnect(SelectionKey selectionKey) throws Exception {
    asioVisitor.onConnect(selectionKey);
  }

  public void onWrite(SelectionKey selectionKey) throws Exception {
    asioVisitor.onWrite(selectionKey);
  }

  public void onAccept(SelectionKey selectionKey) throws Exception {
    asioVisitor.onAccept(selectionKey);
  }

  @Override
  public SynchronousQueue<T> getQ() {
    return null;  //todo: verify for a purpose
  }


  public AsioVisitor getAsioVisitor() {
    return asioVisitor;
  }

  public void setAsioVisitor(AsioVisitor asioVisitor) {
    this.asioVisitor = asioVisitor;
  }

  public Sink<T> createSink() {
    return new Sink<T>() {
      @Override
      public T put(T t) throws Exception {
        checkThread();
        getQ().put(t);
        return t;  //todo: verify for a purpose
      }
    };
  }


  public Source<T> createSource() {
    return new Source<T>() {
      @Override
      public T take() throws Exception {
        checkThread();
        return getQ().take();  //todo: verify for a purpose
      }
    };
  }

  private void checkThread() {
    if (Thread.currentThread() == HttpMethod.selectorThread)

    {
      throw new Error("running blocking task from selector thread");
    }
  }

  public T[] getData() {
    return data;
  }

  public void setData(T[] data) {
    this.data = data;
  }
}
