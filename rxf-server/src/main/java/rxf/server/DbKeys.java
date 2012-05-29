package rxf.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.SynchronousQueue;

import one.xio.MimeType;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;

@Retention(RetentionPolicy.RUNTIME)
@Target({FIELD, LOCAL_VARIABLE, TYPE, ANNOTATION_TYPE, CONSTRUCTOR, PACKAGE, PARAMETER})
public @interface DbKeys {
  enum etype {

    opaque, db, docId, rev, designDocId, view, validjson, mimetype {{
      clazz = MimeType.class;
    }}, blob {{
      clazz = ByteBuffer.class;
    }};

    <T> boolean validate(T... data) {
      return true;
    }

    Class clazz = String.class;
  }


  etype[] value();


  @Retention(RetentionPolicy.RUNTIME)
  @Target({FIELD, LOCAL_VARIABLE, TYPE, ANNOTATION_TYPE, CONSTRUCTOR, PACKAGE, PARAMETER})
  @interface DbResultUnit {
    Class value();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({FIELD, LOCAL_VARIABLE, TYPE, ANNOTATION_TYPE, CONSTRUCTOR, PACKAGE, PARAMETER})
  @interface DbInputUnit {
    Class value();
  }

  public static abstract class ReturnAction<T> {

    static ThreadLocal<ReturnAction> currentResults = new ThreadLocal<ReturnAction>();

    public ReturnAction() {
      currentResults.set(this);
    }
  }

  abstract class ActionBuilder<T> {

    Rfc822HeaderState state;
    SelectionKey key;
    static ThreadLocal<ActionBuilder> currentAction = new ThreadLocal<ActionBuilder>();
    private SynchronousQueue<T>[] synchronousQueues;

    ActionBuilder() {
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


    public abstract TerminalBuilder<T> fire();


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

  abstract class DbKeysBuilder<T> {
    public static ThreadLocal<DbKeysBuilder> currentKeys = new ThreadLocal<DbKeysBuilder>();

    public abstract ActionBuilder<T> to(SynchronousQueue<T>... clients);

    protected DbKeysBuilder() {
      currentKeys.set(this);
    }

    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public ThreadLocal<? extends DbKeysBuilder> getCurrentKeys() {
      return this.currentKeys;
    }


    public java.util.EnumMap<etype, Object> getParms() {
      return this.parms;
    }

    public DbKeysBuilder setParms(java.util.EnumMap<etype, Object> parms) {
      this.parms = parms;
      return this;
    }
  }


}

