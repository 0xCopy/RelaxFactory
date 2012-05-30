package rxf.server;

import java.util.concurrent.SynchronousQueue;

/**
 * User: jim
 * Date: 5/29/12
 * Time: 1:58 PM
 */
public abstract class DbKeysBuilder<T> {
    public static ThreadLocal<DbKeysBuilder> currentKeys = new ThreadLocal<DbKeysBuilder>();

    public abstract ActionBuilder<T> to(SynchronousQueue<T>... clients);

    public DbKeysBuilder() {
        currentKeys.set(this);
    }

    java.util.EnumMap<DbKeys.etype, Object> parms = new java.util.EnumMap<DbKeys.etype, Object>(DbKeys.etype.class);

    public ThreadLocal<? extends DbKeysBuilder> getCurrentKeys() {
        return currentKeys;
    }


    public java.util.EnumMap<DbKeys.etype, Object> getParms() {
        return this.parms;
    }

    public DbKeysBuilder setParms(java.util.EnumMap<DbKeys.etype, Object> parms) {
        this.parms = parms;
        return this;
    }
}
