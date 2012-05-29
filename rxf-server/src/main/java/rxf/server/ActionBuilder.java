package rxf.server;

import java.nio.channels.SelectionKey;
import java.util.concurrent.SynchronousQueue;

/**
 * User: jim
 * Date: 5/29/12
 * Time: 1:58 PM
 */
public abstract class ActionBuilder<T> {

  Rfc822HeaderState state;
  SelectionKey key;
  static ThreadLocal<ActionBuilder> currentAction = new ThreadLocal<ActionBuilder>();
  private SynchronousQueue<T>[] synchronousQueues;

  public ActionBuilder(SynchronousQueue<T>... synchronousQueues) {
    this.synchronousQueues = synchronousQueues;
    currentAction.set(this);
  }

  public SynchronousQueue<T>[] sync() {
    return this.synchronousQueues;
  }

  public Rfc822HeaderState state() {
    return this.state;
  }

  public ActionBuilder<T> state(Rfc822HeaderState state) {
    this.state = state;
    return this;
  }

  public SelectionKey key() {
    return this.key;
  }

  public ActionBuilder<T> key(SelectionKey key) {
    this.key = key;

    return this;
  }


  public abstract TerminalBuilder<T> fire() throws Exception;


  public Rfc822HeaderState getState() {
    return this.state;
  }

  public ActionBuilder setState(Rfc822HeaderState state) {
    this.state = state;
    return this;
  }

  public SelectionKey getKey() {
    return this.key;
  }

  public ActionBuilder setKey(SelectionKey key) {
    this.key = key;
    return this;
  }

}
