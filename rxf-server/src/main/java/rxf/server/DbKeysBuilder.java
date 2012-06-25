package rxf.server;

import javax.validation.ValidationException;

import rxf.server.an.DbKeys;
import rxf.server.an.DbKeys.etype;

/**
 * User: jim
 * Date: 5/29/12
 * Time: 1:58 PM
 */
public abstract class DbKeysBuilder<T> {
  protected static ThreadLocal<DbKeysBuilder> currentKeys = new InheritableThreadLocal<DbKeysBuilder>();
  protected final java.util.EnumMap<DbKeys.etype, Object> parms = new java.util.EnumMap<DbKeys.etype, Object>(DbKeys.etype.class);

  private Throwable trace;

  protected abstract ActionBuilder<T> to();

  public DbKeysBuilder() {
    currentKeys.set(this);
    if (BlobAntiPatternObject.DEBUG_SENDJSON) debug();

  }

  public boolean validate() {
    for (etype etype : parms.keySet()) {
      Object o = get(etype);
      if (!etype.validate(o)) throw new ValidationException("!!! " + etype + " fails with value: " + o);
    }
    return true;
  }

  public static <T, B extends DbKeysBuilder<T>> B get() {
    return (B) currentKeys.get();
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

  public DbKeysBuilder<T> debug() {
    trace = new Throwable().fillInStackTrace();
    return this;
  }

  public Throwable trace() {
    return trace;
  }
}