package ro.server;

import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;

import one.xio.AsioVisitor;

/**
 * User: jim
 * Date: 4/29/12
 * Time: 12:26 PM
 */
public interface Task<T> extends AsioVisitor, Callable<T> {
  SynchronousQueue<T> getQ();

  interface Sink<T> {
    T put(T t) throws InterruptedException, Exception;
  }

  interface Source<T> {
    T take() throws InterruptedException, Exception;
  }
}
