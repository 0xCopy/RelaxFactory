package rxf.server.gen;
//generated


import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import rxf.server.*;
import rxf.server.an.DbKeys;

import static rxf.server.BlobAntiPatternObject.avoidStarvation;

/**
 * generated drivers
 */
public interface CouchDriver {
  java.nio.ByteBuffer DbCreate(java.lang.String db);

  public class DbCreate extends DbKeysBuilder<java.nio.ByteBuffer> {
    private DbCreate() {
    }

    static public DbCreate

    $() {
      return new DbCreate();
    }

    public interface DbCreateTerminalBuilder extends TerminalBuilder<java.nio.ByteBuffer> {
      CouchTx tx();

      void oneWay();
    }

    public class DbCreateActionBuilder extends ActionBuilder<java.nio.ByteBuffer> {
      public DbCreateActionBuilder() {
        super();
      }

      @Override
      public DbCreateTerminalBuilder fire() {
        return new DbCreateTerminalBuilder() {
          public CouchTx tx() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(rxf.server.driver.CouchMetaDriver.DbCreate.visit()).toString(), CouchTx.class);
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
                  rxf.server.driver.CouchMetaDriver.DbCreate.visit(/*dbKeysBuilder,actionBuilder*/);
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
      public DbCreateActionBuilder key(java.nio.channels.SelectionKey key) {
        return (DbCreateActionBuilder) super.key(key);
      }
    }

    @Override
    public DbCreateActionBuilder to() {
      if (parms.size() >= parmsCount) return new DbCreateActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db]");
    }


    static private final int parmsCount = 1;

    public DbCreate db(java.lang.String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer DbDelete(java.lang.String db);

  public class DbDelete extends DbKeysBuilder<java.nio.ByteBuffer> {
    private DbDelete() {
    }

    static public DbDelete

    $() {
      return new DbDelete();
    }

    public interface DbDeleteTerminalBuilder extends TerminalBuilder<java.nio.ByteBuffer> {
      CouchTx tx();

      void oneWay();
    }

    public class DbDeleteActionBuilder extends ActionBuilder<java.nio.ByteBuffer> {
      public DbDeleteActionBuilder() {
        super();
      }

      @Override
      public DbDeleteTerminalBuilder fire() {
        return new DbDeleteTerminalBuilder() {
          public CouchTx tx() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(rxf.server.driver.CouchMetaDriver.DbDelete.visit()).toString(), CouchTx.class);
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
                  rxf.server.driver.CouchMetaDriver.DbDelete.visit(/*dbKeysBuilder,actionBuilder*/);
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
      public DbDeleteActionBuilder key(java.nio.channels.SelectionKey key) {
        return (DbDeleteActionBuilder) super.key(key);
      }
    }

    @Override
    public DbDeleteActionBuilder to() {
      if (parms.size() >= parmsCount) return new DbDeleteActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db]");
    }


    static private final int parmsCount = 1;

    public DbDelete db(java.lang.String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer DocFetch(java.lang.String db, java.lang.String docId);

  public class DocFetch extends DbKeysBuilder<java.nio.ByteBuffer> {
    private DocFetch() {
    }

    static public DocFetch

    $() {
      return new DocFetch();
    }

    public interface DocFetchTerminalBuilder extends TerminalBuilder<java.nio.ByteBuffer> {
      java.nio.ByteBuffer pojo();

      Future<ByteBuffer> future();

      String json();
    }

    public class DocFetchActionBuilder extends ActionBuilder<java.nio.ByteBuffer> {
      public DocFetchActionBuilder() {
        super();
      }

      @Override
      public DocFetchTerminalBuilder fire() {
        return new DocFetchTerminalBuilder() {
          public java.nio.ByteBuffer pojo() {
            try {
              return rxf.server.driver.CouchMetaDriver.DocFetch.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<ByteBuffer> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public java.nio.ByteBuffer call() throws Exception {
                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return rxf.server.driver.CouchMetaDriver.DocFetch.visit(dbKeysBuilder, actionBuilder);
                }
              }
              );
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public String json() {
            try {
              ByteBuffer visit = rxf.server.driver.CouchMetaDriver.DocFetch.visit();
              return null == visit ? null : one.xio.HttpMethod.UTF8.decode(avoidStarvation(visit)).toString();
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
      public DocFetchActionBuilder key(java.nio.channels.SelectionKey key) {
        return (DocFetchActionBuilder) super.key(key);
      }
    }

    @Override
    public DocFetchActionBuilder to() {
      if (parms.size() >= parmsCount) return new DocFetchActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }


    static private final int parmsCount = 2;

    public DocFetch db(java.lang.String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public DocFetch docId(java.lang.String stringParam) {
      parms.put(DbKeys.etype.docId, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer RevisionFetch(java.lang.String db, java.lang.String docId);

  public class RevisionFetch extends DbKeysBuilder<java.nio.ByteBuffer> {
    private RevisionFetch() {
    }

    static public RevisionFetch

    $() {
      return new RevisionFetch();
    }

    public interface RevisionFetchTerminalBuilder extends TerminalBuilder<java.nio.ByteBuffer> {
      String json();

      Future<ByteBuffer> future();
    }

    public class RevisionFetchActionBuilder extends ActionBuilder<java.nio.ByteBuffer> {
      public RevisionFetchActionBuilder() {
        super();
      }

      @Override
      public RevisionFetchTerminalBuilder fire() {
        return new RevisionFetchTerminalBuilder() {
          public String json() {
            try {
              ByteBuffer visit = rxf.server.driver.CouchMetaDriver.RevisionFetch.visit();
              return null == visit ? null : one.xio.HttpMethod.UTF8.decode(avoidStarvation(visit)).toString();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<ByteBuffer> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public java.nio.ByteBuffer call() throws Exception {
                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return rxf.server.driver.CouchMetaDriver.RevisionFetch.visit(dbKeysBuilder, actionBuilder);
                }
              }
              );
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
      public RevisionFetchActionBuilder key(java.nio.channels.SelectionKey key) {
        return (RevisionFetchActionBuilder) super.key(key);
      }
    }

    @Override
    public RevisionFetchActionBuilder to() {
      if (parms.size() >= parmsCount) return new RevisionFetchActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }


    static private final int parmsCount = 2;

    public RevisionFetch db(java.lang.String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public RevisionFetch docId(java.lang.String stringParam) {
      parms.put(DbKeys.etype.docId, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer DocPersist(java.lang.String db, java.lang.String validjson);

  public class DocPersist extends DbKeysBuilder<java.nio.ByteBuffer> {
    private DocPersist() {
    }

    static public DocPersist

    $() {
      return new DocPersist();
    }

    public interface DocPersistTerminalBuilder extends TerminalBuilder<java.nio.ByteBuffer> {
      CouchTx tx();

      void oneWay();

      Future<ByteBuffer> future();
    }

    public class DocPersistActionBuilder extends ActionBuilder<java.nio.ByteBuffer> {
      public DocPersistActionBuilder() {
        super();
      }

      @Override
      public DocPersistTerminalBuilder fire() {
        return new DocPersistTerminalBuilder() {
          public CouchTx tx() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(rxf.server.driver.CouchMetaDriver.DocPersist.visit()).toString(), CouchTx.class);
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
                  rxf.server.driver.CouchMetaDriver.DocPersist.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public Future<ByteBuffer> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public java.nio.ByteBuffer call() throws Exception {
                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return rxf.server.driver.CouchMetaDriver.DocPersist.visit(dbKeysBuilder, actionBuilder);
                }
              }
              );
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
      public DocPersistActionBuilder key(java.nio.channels.SelectionKey key) {
        return (DocPersistActionBuilder) super.key(key);
      }
    }

    @Override
    public DocPersistActionBuilder to() {
      if (parms.size() >= parmsCount) return new DocPersistActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, validjson]");
    }


    static private final int parmsCount = 2;

    public DocPersist db(java.lang.String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public DocPersist validjson(java.lang.String stringParam) {
      parms.put(DbKeys.etype.validjson, stringParam);
      return this;
    }

    public DocPersist docId(java.lang.String stringParam) {
      parms.put(DbKeys.etype.docId, stringParam);
      return this;
    }

    public DocPersist rev(java.lang.String stringParam) {
      parms.put(DbKeys.etype.rev, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer DocDelete(java.lang.String db, java.lang.String docId, java.lang.String rev);

  public class DocDelete extends DbKeysBuilder<java.nio.ByteBuffer> {
    private DocDelete() {
    }

    static public DocDelete

    $() {
      return new DocDelete();
    }

    public interface DocDeleteTerminalBuilder extends TerminalBuilder<java.nio.ByteBuffer> {
      CouchTx tx();

      void oneWay();

      Future<ByteBuffer> future();
    }

    public class DocDeleteActionBuilder extends ActionBuilder<java.nio.ByteBuffer> {
      public DocDeleteActionBuilder() {
        super();
      }

      @Override
      public DocDeleteTerminalBuilder fire() {
        return new DocDeleteTerminalBuilder() {
          public CouchTx tx() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(rxf.server.driver.CouchMetaDriver.DocDelete.visit()).toString(), CouchTx.class);
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
                  rxf.server.driver.CouchMetaDriver.DocDelete.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public Future<ByteBuffer> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public java.nio.ByteBuffer call() throws Exception {
                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return rxf.server.driver.CouchMetaDriver.DocDelete.visit(dbKeysBuilder, actionBuilder);
                }
              }
              );
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
      public DocDeleteActionBuilder key(java.nio.channels.SelectionKey key) {
        return (DocDeleteActionBuilder) super.key(key);
      }
    }

    @Override
    public DocDeleteActionBuilder to() {
      if (parms.size() >= parmsCount) return new DocDeleteActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, docId, rev]");
    }


    static private final int parmsCount = 3;

    public DocDelete db(java.lang.String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public DocDelete docId(java.lang.String stringParam) {
      parms.put(DbKeys.etype.docId, stringParam);
      return this;
    }

    public DocDelete rev(java.lang.String stringParam) {
      parms.put(DbKeys.etype.rev, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer DesignDocFetch(java.lang.String db, java.lang.String designDocId);

  public class DesignDocFetch extends DbKeysBuilder<java.nio.ByteBuffer> {
    private DesignDocFetch() {
    }

    static public DesignDocFetch

    $() {
      return new DesignDocFetch();
    }

    public interface DesignDocFetchTerminalBuilder extends TerminalBuilder<java.nio.ByteBuffer> {
      java.nio.ByteBuffer pojo();

      Future<ByteBuffer> future();

      String json();
    }

    public class DesignDocFetchActionBuilder extends ActionBuilder<java.nio.ByteBuffer> {
      public DesignDocFetchActionBuilder() {
        super();
      }

      @Override
      public DesignDocFetchTerminalBuilder fire() {
        return new DesignDocFetchTerminalBuilder() {
          public java.nio.ByteBuffer pojo() {
            try {
              return rxf.server.driver.CouchMetaDriver.DesignDocFetch.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<ByteBuffer> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public java.nio.ByteBuffer call() throws Exception {
                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return rxf.server.driver.CouchMetaDriver.DesignDocFetch.visit(dbKeysBuilder, actionBuilder);
                }
              }
              );
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public String json() {
            try {
              ByteBuffer visit = rxf.server.driver.CouchMetaDriver.DesignDocFetch.visit();
              return null == visit ? null : one.xio.HttpMethod.UTF8.decode(avoidStarvation(visit)).toString();
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
      public DesignDocFetchActionBuilder key(java.nio.channels.SelectionKey key) {
        return (DesignDocFetchActionBuilder) super.key(key);
      }
    }

    @Override
    public DesignDocFetchActionBuilder to() {
      if (parms.size() >= parmsCount) return new DesignDocFetchActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, designDocId]");
    }


    static private final int parmsCount = 2;

    public DesignDocFetch db(java.lang.String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public DesignDocFetch designDocId(java.lang.String stringParam) {
      parms.put(DbKeys.etype.designDocId, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer ViewFetch(java.lang.String db, java.lang.String view);

  public class ViewFetch extends DbKeysBuilder<java.nio.ByteBuffer> {
    private ViewFetch() {
    }

    static public ViewFetch

    $() {
      return new ViewFetch();
    }

    public interface ViewFetchTerminalBuilder extends TerminalBuilder<java.nio.ByteBuffer> {
      rxf.server.CouchResultSet rows();

      Future<ByteBuffer> future();

      void continuousFeed();

    }

    public class ViewFetchActionBuilder extends ActionBuilder<java.nio.ByteBuffer> {
      public ViewFetchActionBuilder() {
        super();
      }

      @Override
      public ViewFetchTerminalBuilder fire() {
        return new ViewFetchTerminalBuilder() {
          public rxf.server.CouchResultSet rows() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(rxf.server.driver.CouchMetaDriver.ViewFetch.visit()).toString(),
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

          public Future<ByteBuffer> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public java.nio.ByteBuffer call() throws Exception {
                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return rxf.server.driver.CouchMetaDriver.ViewFetch.visit(dbKeysBuilder, actionBuilder);
                }
              }
              );
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
      public ViewFetchActionBuilder key(java.nio.channels.SelectionKey key) {
        return (ViewFetchActionBuilder) super.key(key);
      }
    }

    @Override
    public ViewFetchActionBuilder to() {
      if (parms.size() >= parmsCount) return new ViewFetchActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, view]");
    }


    static private final int parmsCount = 2;

    public ViewFetch db(java.lang.String stringParam) {
      parms.put(DbKeys.etype.db, stringParam);
      return this;
    }

    public ViewFetch view(java.lang.String stringParam) {
      parms.put(DbKeys.etype.view, stringParam);
      return this;
    }

    public ViewFetch type(java.lang.Class classParam) {
      parms.put(DbKeys.etype.type, classParam);
      return this;
    }

  }

  java.nio.ByteBuffer JsonSend(java.lang.String opaque, java.lang.String validjson);

  public class JsonSend extends DbKeysBuilder<java.nio.ByteBuffer> {
    private JsonSend() {
    }

    static public JsonSend

    $() {
      return new JsonSend();
    }

    public interface JsonSendTerminalBuilder extends TerminalBuilder<java.nio.ByteBuffer> {
      CouchTx tx();

      void oneWay();

      rxf.server.CouchResultSet rows();

      String json();

      Future<ByteBuffer> future();

      void continuousFeed();

    }

    public class JsonSendActionBuilder extends ActionBuilder<java.nio.ByteBuffer> {
      public JsonSendActionBuilder() {
        super();
      }

      @Override
      public JsonSendTerminalBuilder fire() {
        return new JsonSendTerminalBuilder() {
          public CouchTx tx() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(rxf.server.driver.CouchMetaDriver.JsonSend.visit()).toString(), CouchTx.class);
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
                  rxf.server.driver.CouchMetaDriver.JsonSend.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public rxf.server.CouchResultSet rows() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(rxf.server.driver.CouchMetaDriver.JsonSend.visit()).toString(),
                  new ParameterizedType() {
                    public Type getRawType() {
                      return CouchResultSet.class;
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

          public String json() {
            try {
              ByteBuffer visit = rxf.server.driver.CouchMetaDriver.JsonSend.visit();
              return null == visit ? null : one.xio.HttpMethod.UTF8.decode(avoidStarvation(visit)).toString();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<ByteBuffer> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public java.nio.ByteBuffer call() throws Exception {
                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return rxf.server.driver.CouchMetaDriver.JsonSend.visit(dbKeysBuilder, actionBuilder);
                }
              }
              );
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
      public JsonSendActionBuilder key(java.nio.channels.SelectionKey key) {
        return (JsonSendActionBuilder) super.key(key);
      }
    }

    @Override
    public JsonSendActionBuilder to() {
      if (parms.size() >= parmsCount) return new JsonSendActionBuilder();
      throw new IllegalArgumentException("required parameters are: [opaque, validjson]");
    }


    static private final int parmsCount = 2;

    public JsonSend opaque(java.lang.String stringParam) {
      parms.put(DbKeys.etype.opaque, stringParam);
      return this;
    }

    public JsonSend validjson(java.lang.String stringParam) {
      parms.put(DbKeys.etype.validjson, stringParam);
      return this;
    }

    public JsonSend type(java.lang.Class classParam) {
      parms.put(DbKeys.etype.type, classParam);
      return this;
    }

  }
}
