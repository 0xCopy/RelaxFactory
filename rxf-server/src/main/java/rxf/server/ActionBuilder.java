package rxf.server;

import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.concurrent.SynchronousQueue;

/**
 * User: jim
 * Date: 5/29/12
 * Time: 1:58 PM
 */
public abstract class ActionBuilder<T> {

  private Rfc822HeaderState state;
  private SelectionKey key;
  private static ThreadLocal<ActionBuilder> currentAction = new ThreadLocal<ActionBuilder>();
  private SynchronousQueue[] synchronousQueues;

  public ActionBuilder(SynchronousQueue... synchronousQueues) {
    this.synchronousQueues = synchronousQueues;
    currentAction.set(this);
  }

  public SynchronousQueue sync() {
    return (0 >= this.synchronousQueues.length ? (synchronousQueues = many(new SynchronousQueue())) : synchronousQueues)[0];
  }

  private SynchronousQueue[] many(SynchronousQueue... ts) {
    return ts;
  }

  @Override
  public String toString() {
    return "ActionBuilder{" +
        "state=" + state +
        ", key=" + key +
        ", synchronousQueues=" + (synchronousQueues == null ? null : Arrays.asList(synchronousQueues)) +
        '}';
  }

  public Rfc822HeaderState state() {
    return this.state == null ? this.state = new Rfc822HeaderState(BlobAntiPatternObject.COOKIE, "ETag") : state;
  }

  public SelectionKey key() {
    return this.key;
  }

  abstract protected <B extends TerminalBuilder<T>> B fire() throws Exception;


  protected <B extends ActionBuilder<T>> B state(Rfc822HeaderState state) {
    this.state = state;
    return (B) this;
  }


  protected <B extends ActionBuilder<T>> B key(SelectionKey key) {
    this.key = key;

    return (B) this;
  }

  public static <T, B extends ActionBuilder<T>> B get() {
    return (B) currentAction.get();
  }

  public <B extends ActionBuilder<T>> B sync(SynchronousQueue... ts) {
    this.synchronousQueues = ts;
    return (B) this;
  }
}
