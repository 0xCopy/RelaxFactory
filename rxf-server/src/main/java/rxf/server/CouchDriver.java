package rxf.server;
//generated

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * generated drivers
 */
public interface CouchDriver {
  rxf.server.CouchTx DbCreate(java.lang.String db, java.lang.String validjson);


  public class DbCreate<T> extends DbKeysBuilder<rxf.server.CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;

    private DbCreate() {
    }

    static public <T> DbCreate<T> $() {
      return new DbCreate<T>();
    }

    public interface DbCreateTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
      CouchTx tx();

      void oneWay();
    }

    public class DbCreateActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
      public DbCreateActionBuilder( /*<rxf.server.CouchTx>*/) {
        super(/*synchronousQueues*/);
      }

      @Override
      public DbCreateTerminalBuilder fire() {
        return new DbCreateTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.DbCreate.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void oneWay() {
            final DbKeysBuilder<Object> dbKeysBuilder = (DbKeysBuilder<Object>) DbKeysBuilder.get();
            final ActionBuilder<Object> actionBuilder = (ActionBuilder<Object>) ActionBuilder.get();
            dbKeysBuilder.validate();
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {

              @Override
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  rxf.server.CouchMetaDriver.DbCreate.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        };
      }

      @Override
      public DbCreateActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public DbCreateActionBuilder key(java.nio.channels.SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public DbCreateActionBuilder to( /*<rxf.server.CouchTx>*/) {
      if (parms.size() == parmsCount)
        return new DbCreateActionBuilder(/*dest*/);

      throw new IllegalArgumentException("required parameters are: [db, validjson]");
    }

    static private final int parmsCount = 2;

    public DbCreate db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public DbCreate validjson(java.lang.String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  java.lang.String DocFetch(java.lang.String db, java.lang.String docId);


  public class DocFetch<T> extends DbKeysBuilder<java.lang.String> {
    private Rfc822HeaderState rfc822HeaderState;

    private DocFetch() {
    }

    static public <T> DocFetch<T> $() {
      return new DocFetch<T>();
    }

    public interface DocFetchTerminalBuilder extends TerminalBuilder<java.lang.String> {
      java.lang.String pojo();

      Future<java.lang.String> future();
    }

    public class DocFetchActionBuilder extends ActionBuilder<java.lang.String> {
      public DocFetchActionBuilder( /*<java.lang.String>*/) {
        super(/*synchronousQueues*/);
      }

      @Override
      public DocFetchTerminalBuilder fire() {
        return new DocFetchTerminalBuilder() {
          public java.lang.String pojo() {
            try {
              return (java.lang.String) rxf.server.CouchMetaDriver.DocFetch.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<java.lang.String> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<java.lang.String>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public java.lang.String call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (java.lang.String) rxf.server.CouchMetaDriver.DocFetch.visit(dbKeysBuilder, actionBuilder);
                }
              });
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }
        };
      }

      @Override
      public DocFetchActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public DocFetchActionBuilder key(java.nio.channels.SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public DocFetchActionBuilder to( /*<java.lang.String>*/) {
      if (parms.size() == parmsCount)
        return new DocFetchActionBuilder(/*dest*/);

      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }

    static private final int parmsCount = 2;

    public DocFetch db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public DocFetch docId(java.lang.String string) {
      parms.put(DbKeys.etype.docId, string);
      return this;
    }

  }

  java.lang.String RevisionFetch(java.lang.String db, java.lang.String docId);


  public class RevisionFetch<T> extends DbKeysBuilder<java.lang.String> {
    private Rfc822HeaderState rfc822HeaderState;

    private RevisionFetch() {
    }

    static public <T> RevisionFetch<T> $() {
      return new RevisionFetch<T>();
    }

    public interface RevisionFetchTerminalBuilder extends TerminalBuilder<java.lang.String> {
      CouchTx tx();

      Future<java.lang.String> future();
    }

    public class RevisionFetchActionBuilder extends ActionBuilder<java.lang.String> {
      public RevisionFetchActionBuilder( /*<java.lang.String>*/) {
        super(/*synchronousQueues*/);
      }

      @Override
      public RevisionFetchTerminalBuilder fire() {
        return new RevisionFetchTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.RevisionFetch.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<java.lang.String> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<java.lang.String>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public java.lang.String call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (java.lang.String) rxf.server.CouchMetaDriver.RevisionFetch.visit(dbKeysBuilder, actionBuilder);
                }
              });
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }
        };
      }

      @Override
      public RevisionFetchActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public RevisionFetchActionBuilder key(java.nio.channels.SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public RevisionFetchActionBuilder to( /*<java.lang.String>*/) {
      if (parms.size() == parmsCount)
        return new RevisionFetchActionBuilder(/*dest*/);

      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }

    static private final int parmsCount = 2;

    public RevisionFetch db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public RevisionFetch docId(java.lang.String string) {
      parms.put(DbKeys.etype.docId, string);
      return this;
    }

  }

  rxf.server.CouchTx DocPersist(java.lang.String db, java.lang.String validjson);


  public class DocPersist<T> extends DbKeysBuilder<rxf.server.CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;

    private DocPersist() {
    }

    static public <T> DocPersist<T> $() {
      return new DocPersist<T>();
    }

    public interface DocPersistTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
      CouchTx tx();

      void oneWay();

      Future<rxf.server.CouchTx> future();
    }

    public class DocPersistActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
      public DocPersistActionBuilder( /*<rxf.server.CouchTx>*/) {
        super(/*synchronousQueues*/);
      }

      @Override
      public DocPersistTerminalBuilder fire() {
        return new DocPersistTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.DocPersist.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void oneWay() {
            final DbKeysBuilder<Object> dbKeysBuilder = (DbKeysBuilder<Object>) DbKeysBuilder.get();
            final ActionBuilder<Object> actionBuilder = (ActionBuilder<Object>) ActionBuilder.get();
            dbKeysBuilder.validate();
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {

              @Override
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  rxf.server.CouchMetaDriver.DocPersist.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public Future<rxf.server.CouchTx> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.CouchTx>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public rxf.server.CouchTx call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.DocPersist.visit(dbKeysBuilder, actionBuilder);
                }
              });
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }
        };
      }

      @Override
      public DocPersistActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public DocPersistActionBuilder key(java.nio.channels.SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public DocPersistActionBuilder to( /*<rxf.server.CouchTx>*/) {
      if (parms.size() == parmsCount)
        return new DocPersistActionBuilder(/*dest*/);

      throw new IllegalArgumentException("required parameters are: [db, validjson]");
    }

    static private final int parmsCount = 2;

    public DocPersist db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public DocPersist validjson(java.lang.String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  java.lang.String DesignDocFetch(java.lang.String db, java.lang.String designDocId);


  public class DesignDocFetch<T> extends DbKeysBuilder<java.lang.String> {
    private Rfc822HeaderState rfc822HeaderState;

    private DesignDocFetch() {
    }

    static public <T> DesignDocFetch<T> $() {
      return new DesignDocFetch<T>();
    }

    public interface DesignDocFetchTerminalBuilder extends TerminalBuilder<java.lang.String> {
      CouchTx tx();
    }

    public class DesignDocFetchActionBuilder extends ActionBuilder<java.lang.String> {
      public DesignDocFetchActionBuilder( /*<java.lang.String>*/) {
        super(/*synchronousQueues*/);
      }

      @Override
      public DesignDocFetchTerminalBuilder fire() {
        return new DesignDocFetchTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.DesignDocFetch.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }
        };
      }

      @Override
      public DesignDocFetchActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public DesignDocFetchActionBuilder key(java.nio.channels.SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public DesignDocFetchActionBuilder to( /*<java.lang.String>*/) {
      if (parms.size() == parmsCount)
        return new DesignDocFetchActionBuilder(/*dest*/);

      throw new IllegalArgumentException("required parameters are: [db, designDocId]");
    }

    static private final int parmsCount = 2;

    public DesignDocFetch db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public DesignDocFetch designDocId(java.lang.String string) {
      parms.put(DbKeys.etype.designDocId, string);
      return this;
    }

  }

  rxf.server.CouchTx DesignDocPersist(java.lang.String db, java.lang.String designDocId, java.lang.String validjson);


  public class DesignDocPersist<T> extends DbKeysBuilder<rxf.server.CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;

    private DesignDocPersist() {
    }

    static public <T> DesignDocPersist<T> $() {
      return new DesignDocPersist<T>();
    }

    public interface DesignDocPersistTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
      CouchTx tx();

      void oneWay();
    }

    public class DesignDocPersistActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
      public DesignDocPersistActionBuilder( /*<rxf.server.CouchTx>*/) {
        super(/*synchronousQueues*/);
      }

      @Override
      public DesignDocPersistTerminalBuilder fire() {
        return new DesignDocPersistTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.DocPersist.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void oneWay() {
            final DbKeysBuilder<Object> dbKeysBuilder = (DbKeysBuilder<Object>) DbKeysBuilder.get();
            final ActionBuilder<Object> actionBuilder = (ActionBuilder<Object>) ActionBuilder.get();
            dbKeysBuilder.validate();
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {

              @Override
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  rxf.server.CouchMetaDriver.DocPersist.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        };
      }

      @Override
      public DesignDocPersistActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public DesignDocPersistActionBuilder key(java.nio.channels.SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public DesignDocPersistActionBuilder to( /*<rxf.server.CouchTx>*/) {
      if (parms.size() == parmsCount)
        return new DesignDocPersistActionBuilder(/*dest*/);

      throw new IllegalArgumentException("required parameters are: [db, designDocId, validjson]");
    }

    static private final int parmsCount = 3;

    public DesignDocPersist db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public DesignDocPersist designDocId(java.lang.String string) {
      parms.put(DbKeys.etype.designDocId, string);
      return this;
    }

    public DesignDocPersist validjson(java.lang.String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  rxf.server.CouchResultSet ViewFetch(java.lang.String db, java.lang.String view);


  public class ViewFetch<T> extends DbKeysBuilder<rxf.server.CouchResultSet> {
    private Rfc822HeaderState rfc822HeaderState;

    private ViewFetch() {
    }

    static public <T> ViewFetch<T> $() {
      return new ViewFetch<T>();
    }

    public interface ViewFetchTerminalBuilder extends TerminalBuilder<rxf.server.CouchResultSet> {
      CouchResultSet/*<rxf.server.CouchResultSet>*/ rows();

      Future<rxf.server.CouchResultSet> future();

      void continuousFeed();

    }

    public class ViewFetchActionBuilder extends ActionBuilder<rxf.server.CouchResultSet> {
      public ViewFetchActionBuilder( /*<rxf.server.CouchResultSet>*/) {
        super(/*synchronousQueues*/);
      }

      @Override
      public ViewFetchTerminalBuilder fire() {
        return new ViewFetchTerminalBuilder() {
          public CouchResultSet/*<rxf.server.CouchResultSet>*/ rows() {
            try {
              return (CouchResultSet/*<rxf.server.CouchResultSet>*/) BlobAntiPatternObject.GSON.fromJson((String) rxf.server.CouchMetaDriver.ViewFetch.visit(), CouchResultSet/*<rxf.server.CouchResultSet>*/.class);
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<rxf.server.CouchResultSet> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.CouchResultSet>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public rxf.server.CouchResultSet call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (rxf.server.CouchResultSet) rxf.server.CouchMetaDriver.ViewFetch.visit(dbKeysBuilder, actionBuilder);
                }
              });
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void continuousFeed() {
            throw new AbstractMethodError();
          }
        };
      }

      @Override
      public ViewFetchActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public ViewFetchActionBuilder key(java.nio.channels.SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public ViewFetchActionBuilder to( /*<rxf.server.CouchResultSet>*/) {
      if (parms.size() == parmsCount)
        return new ViewFetchActionBuilder(/*dest*/);

      throw new IllegalArgumentException("required parameters are: [db, view]");
    }

    static private final int parmsCount = 2;

    public ViewFetch db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public ViewFetch view(java.lang.String string) {
      parms.put(DbKeys.etype.view, string);
      return this;
    }

  }

  rxf.server.CouchTx JsonSend(java.lang.String opaque, java.lang.String validjson);


  public class JsonSend<T> extends DbKeysBuilder<rxf.server.CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;

    private JsonSend() {
    }

    static public <T> JsonSend<T> $() {
      return new JsonSend<T>();
    }

    public interface JsonSendTerminalBuilder extends TerminalBuilder<rxf.server.CouchTx> {
      CouchTx tx();

      void oneWay();

      CouchResultSet/*<rxf.server.CouchTx>*/ rows();

      Future<rxf.server.CouchTx> future();

      void continuousFeed();

    }

    public class JsonSendActionBuilder extends ActionBuilder<rxf.server.CouchTx> {
      public JsonSendActionBuilder( /*<rxf.server.CouchTx>*/) {
        super(/*synchronousQueues*/);
      }

      @Override
      public JsonSendTerminalBuilder fire() {
        return new JsonSendTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.JsonSend.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void oneWay() {
            final DbKeysBuilder<Object> dbKeysBuilder = (DbKeysBuilder<Object>) DbKeysBuilder.get();
            final ActionBuilder<Object> actionBuilder = (ActionBuilder<Object>) ActionBuilder.get();
            dbKeysBuilder.validate();
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {

              @Override
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  rxf.server.CouchMetaDriver.JsonSend.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public CouchResultSet/*<rxf.server.CouchTx>*/ rows() {
            try {
              return (CouchResultSet/*<rxf.server.CouchTx>*/) BlobAntiPatternObject.GSON.fromJson((String) rxf.server.CouchMetaDriver.JsonSend.visit(), CouchResultSet/*<rxf.server.CouchTx>*/.class);
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<rxf.server.CouchTx> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.CouchTx>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public rxf.server.CouchTx call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.JsonSend.visit(dbKeysBuilder, actionBuilder);
                }
              });
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void continuousFeed() {
            throw new AbstractMethodError();
          }
        };
      }

      @Override
      public JsonSendActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public JsonSendActionBuilder key(java.nio.channels.SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public JsonSendActionBuilder to( /*<rxf.server.CouchTx>*/) {
      if (parms.size() == parmsCount)
        return new JsonSendActionBuilder(/*dest*/);

      throw new IllegalArgumentException("required parameters are: [opaque, validjson]");
    }

    static private final int parmsCount = 2;

    public JsonSend opaque(java.lang.String string) {
      parms.put(DbKeys.etype.opaque, string);
      return this;
    }

    public JsonSend validjson(java.lang.String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  rxf.server.Rfc822HeaderState BlobSend(java.lang.String opaque, one.xio.MimeType mimetype, java.nio.ByteBuffer blob);


  public class BlobSend<T> extends DbKeysBuilder<rxf.server.Rfc822HeaderState> {
    private Rfc822HeaderState rfc822HeaderState;

    private BlobSend() {
    }

    static public <T> BlobSend<T> $() {
      return new BlobSend<T>();
    }

    public interface BlobSendTerminalBuilder extends TerminalBuilder<rxf.server.Rfc822HeaderState> {
      CouchTx tx();

      Future<rxf.server.Rfc822HeaderState> future();

      void oneWay();
    }

    public class BlobSendActionBuilder extends ActionBuilder<rxf.server.Rfc822HeaderState> {
      public BlobSendActionBuilder( /*<rxf.server.Rfc822HeaderState>*/) {
        super(/*synchronousQueues*/);
      }

      @Override
      public BlobSendTerminalBuilder fire() {
        return new BlobSendTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.BlobSend.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<rxf.server.Rfc822HeaderState> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.Rfc822HeaderState>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public rxf.server.Rfc822HeaderState call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (rxf.server.Rfc822HeaderState) rxf.server.CouchMetaDriver.BlobSend.visit(dbKeysBuilder, actionBuilder);
                }
              });
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void oneWay() {
            final DbKeysBuilder<Object> dbKeysBuilder = (DbKeysBuilder<Object>) DbKeysBuilder.get();
            final ActionBuilder<Object> actionBuilder = (ActionBuilder<Object>) ActionBuilder.get();
            dbKeysBuilder.validate();
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {

              @Override
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  rxf.server.CouchMetaDriver.BlobSend.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        };
      }

      @Override
      public BlobSendActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public BlobSendActionBuilder key(java.nio.channels.SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public BlobSendActionBuilder to( /*<rxf.server.Rfc822HeaderState>*/) {
      if (parms.size() == parmsCount)
        return new BlobSendActionBuilder(/*dest*/);

      throw new IllegalArgumentException("required parameters are: [opaque, mimetype, blob]");
    }

    static private final int parmsCount = 3;

    public BlobSend opaque(java.lang.String string) {
      parms.put(DbKeys.etype.opaque, string);
      return this;
    }

    public BlobSend mimetype(one.xio.MimeType mimetype) {
      parms.put(DbKeys.etype.mimetype, mimetype);
      return this;
    }

    public BlobSend blob(java.nio.ByteBuffer bytebuffer) {
      parms.put(DbKeys.etype.blob, bytebuffer);
      return this;
    }

  }
}
