package rxf.server;

import java.util.concurrent.SynchronousQueue;

/**
 * User: jim
 * Date: 5/29/12
 * Time: 1:58 PM
 */
public abstract class DbKeysBuilder<T> {
  public static ThreadLocal<DbKeysBuilder> currentKeys = new ThreadLocal<DbKeysBuilder>();
  protected final java.util.EnumMap<DbKeys.etype, Object> parms = new java.util.EnumMap<DbKeys.etype, Object>(DbKeys.etype.class);

  protected abstract ActionBuilder<T> to(SynchronousQueue<T>... clients);

  public DbKeysBuilder() {
    currentKeys.set(this);
  }

  public ThreadLocal<? extends DbKeysBuilder> getCurrentKeys() {
    return currentKeys;
  }


  public java.util.EnumMap<DbKeys.etype, Object> parms() {
    return parms;
  }

}
