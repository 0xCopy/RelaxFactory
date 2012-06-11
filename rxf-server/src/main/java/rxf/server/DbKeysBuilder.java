package rxf.server;

import javax.validation.ValidationException;

import rxf.server.DbKeys.etype;

/**
 * User: jim
 * Date: 5/29/12
 * Time: 1:58 PM
 */
public abstract class DbKeysBuilder<T> {
  protected static ThreadLocal<DbKeysBuilder> currentKeys = new InheritableThreadLocal<DbKeysBuilder>();
  protected final java.util.EnumMap<DbKeys.etype, Object> parms = new java.util.EnumMap<DbKeys.etype, Object>(DbKeys.etype.class);

  protected abstract ActionBuilder<T> to(/*  defanged... almost useless until we move to complex sync.... SynchronousQueue... clients*/);

  public DbKeysBuilder() {
    currentKeys.set(this);
  }

  public boolean validate() {
    for (etype etype : parms.keySet()) {
      final Object o = parms().get(etype);
      if (!
          etype.validate(o)) throw new ValidationException("!!! " + etype + " fails with value: " + o);
    }
    return true;
  }

  public java.util.EnumMap<DbKeys.etype, Object> parms() {
    return parms;
  }

  public static <T, B extends DbKeysBuilder<T>> B get() {
    return (B) currentKeys.get();
  }

}