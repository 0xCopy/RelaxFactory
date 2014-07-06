package rxf.couch;

import rxf.core.Tx;
import rxf.couch.driver.CouchMetaDriver.etype;
import rxf.rpc.RpcHelper;

import java.lang.IllegalArgumentException;
import java.util.EnumMap;

/**
 * User: jim Date: 5/29/12 Time: 1:58 PM
 */
public abstract class DbKeysBuilder {
  protected static ThreadLocal<DbKeysBuilder> currentKeys = new InheritableThreadLocal<>();
  protected final EnumMap<etype, Object> parms = new EnumMap<>(etype.class);

  private Throwable trace;

  protected abstract Tx to();

  public DbKeysBuilder() {
    currentKeys.set(this);
    if (RpcHelper.DEBUG_SENDJSON) {
      debug();
    }
  }

  public boolean validate() {
    for (etype etype : parms.keySet()) {
      Object o = get(etype);
      if (!etype.validate(o)) {
        throw new IllegalArgumentException("!!! " + etype + " fails with value: " + o);
      }
    }
    return true;
  }

  public static DbKeysBuilder get() {
    return currentKeys.get();
  }

  public <T> T get(etype key) {
    return (T) parms.get(key);
  }

  public <T> T put(etype k, T v) {
    return (T) parms.put(k, v);
  }

  public <T> T remove(etype designDocId) {
    return (T) parms.remove(designDocId);
  }

  /**
   * creates a trace object here and now.
   * 
   * @return
   */
  public DbKeysBuilder debug() {
    trace = new Throwable().fillInStackTrace();
    return this;
  }

  public Throwable trace() {
    return trace;
  }
}