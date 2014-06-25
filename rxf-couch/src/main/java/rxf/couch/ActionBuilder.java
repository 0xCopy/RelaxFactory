package rxf.couch;

import rxf.core.Rfc822HeaderState;

import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicReference;

import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpHeaders.ETag;

/**
 * User: jim Date: 5/29/12 Time: 1:58 PM
 */
public abstract class ActionBuilder {

  public static final String[] HEADER_INTEREST = Rfc822HeaderState.staticHeaderStrings(ETag,
      Content$2dLength);
  private AtomicReference<Rfc822HeaderState> state = new AtomicReference<Rfc822HeaderState>();
  private SelectionKey key;
  protected static ThreadLocal<ActionBuilder> currentAction =
      new InheritableThreadLocal<ActionBuilder>();

  public ActionBuilder() {
    currentAction.set(this);
  }

  public abstract TerminalBuilder fire();

  @Override
  public String toString() {
    return "ActionBuilder{" + "state=" + state + ", key=" + key + '}';
  }

  public Rfc822HeaderState state() {
    Rfc822HeaderState ret = this.state.get();
    if (null == ret)
      state.set(ret = new Rfc822HeaderState(HEADER_INTEREST));
    return ret;
  }

  public SelectionKey key() {
    return this.key;
  }

  public ActionBuilder state(Rfc822HeaderState state) {
    this.state.set(state);
    return this;
  }

  public ActionBuilder key(SelectionKey key) {
    this.key = key;

    return this;
  }

  public static ActionBuilder get() {
    if (currentAction.get() == null)
      currentAction.set(new ActionBuilder() {
        @Override
        public TerminalBuilder fire() {
          throw new AbstractMethodError(
              "This is a ActionBuilder with no DbKeysBuilder and therefore now Terminal");
        }
      });
    return (ActionBuilder) currentAction.get();
  }

}
