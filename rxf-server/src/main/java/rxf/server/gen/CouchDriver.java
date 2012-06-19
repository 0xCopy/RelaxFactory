package rxf.server.gen;
//generated

import java.lang.AbstractMethodError;
import java.lang.Class;
import java.lang.Exception;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.Runnable;
import java.lang.String;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import rxf.server.*;
import rxf.server.an.DbKeys;
import rxf.server.driver.CouchMetaDriver;

/**
 * generated drivers
 */
public interface CouchDriver {

  CouchTx DbCreate(String db);

  public class DbCreate extends DbKeysBuilder<CouchTx> {
    private DbCreate() {
    }

    static public DbCreate

    $() {
      return new DbCreate();
    }

    public interface DbCreateTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();

      void oneWay();
    }

    public class DbCreateActionBuilder extends ActionBuilder<CouchTx> {
      public DbCreateActionBuilder() {
        super();
      }

      @Override
      public DbCreateTerminalBuilder fire() {
        return new DbCreateTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.DbCreate.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void oneWay() {
            final DbKeysBuilder<Object> dbKeysBuilder = (DbKeysBuilder<Object>) DbKeysBuilder.get();
            final ActionBuilder<Object> actionBuilder = ActionBuilder.get();
            dbKeysBuilder.validate();
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
              @Override
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  CouchMetaDriver.DbCreate.visit(/*dbKeysBuilder,actionBuilder*/);
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
        return (DbCreateActionBuilder) super.state(state);
      }

      @Override
      public DbCreateActionBuilder key(SelectionKey key) {
        return (DbCreateActionBuilder) super.key(key);
      }
    }

    @Override
    public DbCreateActionBuilder to() {
      if (parms.size() >= parmsCount) return new DbCreateActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db]");
    }

    static private final int parmsCount = 1;

    public DbCreate db(String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

  }

  CouchTx DbDelete(String db);

  public class DbDelete extends DbKeysBuilder<CouchTx> {
    private DbDelete() {
    }

    static public DbDelete

    $() {
      return new DbDelete();
    }

    public interface DbDeleteTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();

      void oneWay();
    }

    public class DbDeleteActionBuilder extends ActionBuilder<CouchTx> {
      public DbDeleteActionBuilder() {
        super();
      }

      @Override
      public DbDeleteTerminalBuilder fire() {
        return new DbDeleteTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.DbDelete.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void oneWay() {
            final DbKeysBuilder<Object> dbKeysBuilder = (DbKeysBuilder<Object>) DbKeysBuilder.get();
            final ActionBuilder<Object> actionBuilder = ActionBuilder.get();
            dbKeysBuilder.validate();
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
              @Override
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  CouchMetaDriver.DbDelete.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        };
      }

      @Override
      public DbDeleteActionBuilder state(Rfc822HeaderState state) {
        return (DbDeleteActionBuilder) super.state(state);
      }

      @Override
      public DbDeleteActionBuilder key(SelectionKey key) {
        return (DbDeleteActionBuilder) super.key(key);
      }
    }

    @Override
    public DbDeleteActionBuilder to() {
      if (parms.size() >= parmsCount) return new DbDeleteActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db]");
    }

    static private final int parmsCount = 1;

    public DbDelete db(String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

  }

  String DocFetch(String db, String docId);

  public class DocFetch extends DbKeysBuilder<String> {
    private DocFetch() {
    }

    static public DocFetch

    $() {
      return new DocFetch();
    }

    public interface DocFetchTerminalBuilder extends TerminalBuilder<String> {
      String pojo();

      Future<String> future();
    }

    public class DocFetchActionBuilder extends ActionBuilder<String> {
      public DocFetchActionBuilder() {
        super();
      }

      @Override
      public DocFetchTerminalBuilder fire() {
        return new DocFetchTerminalBuilder() {
          public String pojo() {
            try {
              return (String) CouchMetaDriver.DocFetch.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<String> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<String>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public String call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (String) CouchMetaDriver.DocFetch.visit(dbKeysBuilder, actionBuilder);
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
        return (DocFetchActionBuilder) super.state(state);
      }

      @Override
      public DocFetchActionBuilder key(SelectionKey key) {
        return (DocFetchActionBuilder) super.key(key);
      }
    }

    @Override
    public DocFetchActionBuilder to() {
      if (parms.size() >= parmsCount) return new DocFetchActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }

    static private final int parmsCount = 2;

    public DocFetch db(String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public DocFetch docId(String stringParam) {
      parms.put(DbKeys.etype.docId, stringParam);
      return this;
    }

  }

  String RevisionFetch(String db, String docId);

  public class RevisionFetch extends DbKeysBuilder<String> {
    private RevisionFetch() {
    }

    static public RevisionFetch

    $() {
      return new RevisionFetch();
    }

    public interface RevisionFetchTerminalBuilder extends TerminalBuilder<String> {
      CouchTx tx();

      Future<String> future();
    }

    public class RevisionFetchActionBuilder extends ActionBuilder<String> {
      public RevisionFetchActionBuilder() {
        super();
      }

      @Override
      public RevisionFetchTerminalBuilder fire() {
        return new RevisionFetchTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.RevisionFetch.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<String> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<String>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public String call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (String) CouchMetaDriver.RevisionFetch.visit(dbKeysBuilder, actionBuilder);
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
        return (RevisionFetchActionBuilder) super.state(state);
      }

      @Override
      public RevisionFetchActionBuilder key(SelectionKey key) {
        return (RevisionFetchActionBuilder) super.key(key);
      }
    }

    @Override
    public RevisionFetchActionBuilder to() {
      if (parms.size() >= parmsCount) return new RevisionFetchActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }

    static private final int parmsCount = 2;

    public RevisionFetch db(String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public RevisionFetch docId(String stringParam) {
      parms.put(DbKeys.etype.docId, stringParam);
      return this;
    }

  }

  CouchTx DocPersist(String db, String validjson);

  public class DocPersist extends DbKeysBuilder<CouchTx> {
    private DocPersist() {
    }

    static public DocPersist

    $() {
      return new DocPersist();
    }

    public interface DocPersistTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();

      void oneWay();

      Future<CouchTx> future();
    }

    public class DocPersistActionBuilder extends ActionBuilder<CouchTx> {
      public DocPersistActionBuilder() {
        super();
      }

      @Override
      public DocPersistTerminalBuilder fire() {
        return new DocPersistTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.DocPersist.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void oneWay() {
            final DbKeysBuilder<Object> dbKeysBuilder = (DbKeysBuilder<Object>) DbKeysBuilder.get();
            final ActionBuilder<Object> actionBuilder = ActionBuilder.get();
            dbKeysBuilder.validate();
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
              @Override
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  CouchMetaDriver.DocPersist.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public Future<CouchTx> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<CouchTx>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public CouchTx call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (CouchTx) CouchMetaDriver.DocPersist.visit(dbKeysBuilder, actionBuilder);
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
        return (DocPersistActionBuilder) super.state(state);
      }

      @Override
      public DocPersistActionBuilder key(SelectionKey key) {
        return (DocPersistActionBuilder) super.key(key);
      }
    }

    @Override
    public DocPersistActionBuilder to() {
      if (parms.size() >= parmsCount) return new DocPersistActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, validjson]");
    }

    static private final int parmsCount = 2;

    public DocPersist db(String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public DocPersist validjson(String stringParam) {
      parms.put(DbKeys.etype.validjson, stringParam);
      return this;
    }

    public DocPersist docId(String stringParam) {
      parms.put(DbKeys.etype.docId, stringParam);
      return this;
    }

    public DocPersist rev(String stringParam) {
      parms.put(DbKeys.etype.rev, stringParam);
      return this;
    }

  }

  CouchTx DocDelete(String db, String docId, String rev);

  public class DocDelete extends DbKeysBuilder<CouchTx> {
    private DocDelete() {
    }

    static public DocDelete

    $() {
      return new DocDelete();
    }

    public interface DocDeleteTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();

      void oneWay();

      Future<CouchTx> future();
    }

    public class DocDeleteActionBuilder extends ActionBuilder<CouchTx> {
      public DocDeleteActionBuilder() {
        super();
      }

      @Override
      public DocDeleteTerminalBuilder fire() {
        return new DocDeleteTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.DocDelete.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void oneWay() {
            final DbKeysBuilder<Object> dbKeysBuilder = (DbKeysBuilder<Object>) DbKeysBuilder.get();
            final ActionBuilder<Object> actionBuilder = ActionBuilder.get();
            dbKeysBuilder.validate();
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
              @Override
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  CouchMetaDriver.DocDelete.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public Future<CouchTx> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<CouchTx>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public CouchTx call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (CouchTx) CouchMetaDriver.DocDelete.visit(dbKeysBuilder, actionBuilder);
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
      public DocDeleteActionBuilder state(Rfc822HeaderState state) {
        return (DocDeleteActionBuilder) super.state(state);
      }

      @Override
      public DocDeleteActionBuilder key(SelectionKey key) {
        return (DocDeleteActionBuilder) super.key(key);
      }
    }

    @Override
    public DocDeleteActionBuilder to() {
      if (parms.size() >= parmsCount) return new DocDeleteActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, docId, rev]");
    }

    static private final int parmsCount = 3;

    public DocDelete db(String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public DocDelete docId(String stringParam) {
      parms.put(DbKeys.etype.docId, stringParam);
      return this;
    }

    public DocDelete rev(String stringParam) {
      parms.put(DbKeys.etype.rev, stringParam);
      return this;
    }

  }

  String DesignDocFetch(String db, String designDocId);

  public class DesignDocFetch extends DbKeysBuilder<String> {
    private DesignDocFetch() {
    }

    static public DesignDocFetch

    $() {
      return new DesignDocFetch();
    }

    public interface DesignDocFetchTerminalBuilder extends TerminalBuilder<String> {
      String pojo();

      Future<String> future();
    }

    public class DesignDocFetchActionBuilder extends ActionBuilder<String> {
      public DesignDocFetchActionBuilder() {
        super();
      }

      @Override
      public DesignDocFetchTerminalBuilder fire() {
        return new DesignDocFetchTerminalBuilder() {
          public String pojo() {
            try {
              return (String) CouchMetaDriver.DesignDocFetch.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<String> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<String>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public String call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (String) CouchMetaDriver.DesignDocFetch.visit(dbKeysBuilder, actionBuilder);
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
      public DesignDocFetchActionBuilder state(Rfc822HeaderState state) {
        return (DesignDocFetchActionBuilder) super.state(state);
      }

      @Override
      public DesignDocFetchActionBuilder key(SelectionKey key) {
        return (DesignDocFetchActionBuilder) super.key(key);
      }
    }

    @Override
    public DesignDocFetchActionBuilder to() {
      if (parms.size() >= parmsCount) return new DesignDocFetchActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, designDocId]");
    }

    static private final int parmsCount = 2;

    public DesignDocFetch db(String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public DesignDocFetch designDocId(String stringParam) {
      parms.put(DbKeys.etype.designDocId, stringParam);
      return this;
    }

  }

  CouchResultSet ViewFetch(String db, String view);

  public class ViewFetch<T> extends DbKeysBuilder<CouchResultSet<T>> {
    private ViewFetch() {
    }

    static public <T extends Object> ViewFetch<T>

    $() {
      return new ViewFetch<T>();
    }

    public interface ViewFetchTerminalBuilder<T> extends TerminalBuilder<CouchResultSet<T>> {
      CouchResultSet<T> rows();

      Future<CouchResultSet<T>> future();

      void continuousFeed();

    }

    public class ViewFetchActionBuilder extends ActionBuilder<CouchResultSet<T>> {
      public ViewFetchActionBuilder() {
        super();
      }

      @Override
      public ViewFetchTerminalBuilder<T> fire() {
        return new ViewFetchTerminalBuilder<T>() {
          public CouchResultSet<T> rows() {
            try {
              return (CouchResultSet<T>) BlobAntiPatternObject.GSON.fromJson((String) CouchMetaDriver.ViewFetch.visit(),
                  new ParameterizedType() {
                    public Type getRawType() {
                      return CouchResultSet.class;
                    }

                    public Type getOwnerType() {
                      return null;
                    }

                    public Type[] getActualTypeArguments() {
                      return new Type[]{ViewFetch.this.<Class>get(DbKeys.etype.type)};
                    }
                  });
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<CouchResultSet<T>> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<CouchResultSet<T>>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public CouchResultSet<T> call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (CouchResultSet<T>) CouchMetaDriver.ViewFetch.visit(dbKeysBuilder, actionBuilder);
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
        return (ViewFetchActionBuilder) super.state(state);
      }

      @Override
      public ViewFetchActionBuilder key(SelectionKey key) {
        return (ViewFetchActionBuilder) super.key(key);
      }
    }

    @Override
    public ViewFetchActionBuilder to() {
      if (parms.size() >= parmsCount) return new ViewFetchActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, view]");
    }

    static private final int parmsCount = 2;

    public ViewFetch<T> db(String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public ViewFetch<T> view(String stringParam) {
      parms.put(DbKeys.etype.view, stringParam);
      return this;
    }

    public ViewFetch<T> type(Class classParam) {
      parms.put(DbKeys.etype.type, classParam);
      return this;
    }

  }

  CouchTx JsonSend(String opaque, String validjson);

  public class JsonSend extends DbKeysBuilder<CouchTx> {
    private JsonSend() {
    }

    static public JsonSend

    $() {
      return new JsonSend();
    }

    public interface JsonSendTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();

      void oneWay();

      CouchTx rows();

      Future<CouchTx> future();

      void continuousFeed();

    }

    public class JsonSendActionBuilder extends ActionBuilder<CouchTx> {
      public JsonSendActionBuilder() {
        super();
      }

      @Override
      public JsonSendTerminalBuilder fire() {
        return new JsonSendTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.JsonSend.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void oneWay() {
            final DbKeysBuilder<Object> dbKeysBuilder = (DbKeysBuilder<Object>) DbKeysBuilder.get();
            final ActionBuilder<Object> actionBuilder = ActionBuilder.get();
            dbKeysBuilder.validate();
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
              @Override
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  CouchMetaDriver.JsonSend.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public CouchTx rows() {
            try {
              return (CouchTx) BlobAntiPatternObject.GSON.fromJson((String) CouchMetaDriver.JsonSend.visit(),
                  new ParameterizedType() {
                    public Type getRawType() {
                      return CouchTx.class;
                    }

                    public Type getOwnerType() {
                      return null;
                    }

                    public Type[] getActualTypeArguments() {
                      return new Type[]{JsonSend.this.<Class>get(DbKeys.etype.type)};
                    }
                  });
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<CouchTx> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<CouchTx>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public CouchTx call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (CouchTx) CouchMetaDriver.JsonSend.visit(dbKeysBuilder, actionBuilder);
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
        return (JsonSendActionBuilder) super.state(state);
      }

      @Override
      public JsonSendActionBuilder key(SelectionKey key) {
        return (JsonSendActionBuilder) super.key(key);
      }
    }

    @Override
    public JsonSendActionBuilder to() {
      if (parms.size() >= parmsCount) return new JsonSendActionBuilder();
      throw new IllegalArgumentException("required parameters are: [opaque, validjson]");
    }

    static private final int parmsCount = 2;

    public JsonSend opaque(String stringParam) {
      parms.put(DbKeys.etype.opaque, stringParam);
      return this;
    }

    public JsonSend validjson(String stringParam) {
      parms.put(DbKeys.etype.validjson, stringParam);
      return this;
    }

    public JsonSend type(Class classParam) {
      parms.put(DbKeys.etype.type, classParam);
      return this;
    }

  }

  Rfc822HeaderState BlobSend(String db, String docId, String opaque, String mimetype, ByteBuffer blob);

  public class BlobSend extends DbKeysBuilder<Rfc822HeaderState> {
    private BlobSend() {
    }

    static public BlobSend

    $() {
      return new BlobSend();
    }

    public interface BlobSendTerminalBuilder extends TerminalBuilder<Rfc822HeaderState> {
      CouchTx tx();

      Future<Rfc822HeaderState> future();

      void oneWay();
    }

    public class BlobSendActionBuilder extends ActionBuilder<Rfc822HeaderState> {
      public BlobSendActionBuilder() {
        super();
      }

      @Override
      public BlobSendTerminalBuilder fire() {
        return new BlobSendTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.BlobSend.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<Rfc822HeaderState> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<Rfc822HeaderState>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public Rfc822HeaderState call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (Rfc822HeaderState) CouchMetaDriver.BlobSend.visit(dbKeysBuilder, actionBuilder);
                }
              });
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public void oneWay() {
            final DbKeysBuilder<Object> dbKeysBuilder = (DbKeysBuilder<Object>) DbKeysBuilder.get();
            final ActionBuilder<Object> actionBuilder = ActionBuilder.get();
            dbKeysBuilder.validate();
            BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
              @Override
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  CouchMetaDriver.BlobSend.visit(/*dbKeysBuilder,actionBuilder*/);
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
        return (BlobSendActionBuilder) super.state(state);
      }

      @Override
      public BlobSendActionBuilder key(SelectionKey key) {
        return (BlobSendActionBuilder) super.key(key);
      }
    }

    @Override
    public BlobSendActionBuilder to() {
      if (parms.size() >= parmsCount) return new BlobSendActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, docId, opaque, mimetype, blob]");
    }

    static private final int parmsCount = 5;

    public BlobSend db(String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public BlobSend docId(String stringParam) {
      parms.put(DbKeys.etype.docId, stringParam);
      return this;
    }

    public BlobSend opaque(String stringParam) {
      parms.put(DbKeys.etype.opaque, stringParam);
      return this;
    }

    public BlobSend mimetype(String stringParam) {
      parms.put(DbKeys.etype.mimetype, stringParam);
      return this;
    }

    public BlobSend blob(ByteBuffer bytebufferParam) {
      parms.put(DbKeys.etype.blob, bytebufferParam);
      return this;
    }

  }
}
