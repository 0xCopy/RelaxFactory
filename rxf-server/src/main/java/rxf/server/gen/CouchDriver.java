package rxf.server.gen;
//generated


import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import rxf.server.*;
import rxf.server.an.DbKeys.etype;
import rxf.server.driver.CouchMetaDriver;

import static rxf.server.BlobAntiPatternObject.avoidStarvation;

/**
 * generated drivers
 */
public interface CouchDriver {
  java.nio.ByteBuffer DbCreate(String db);

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


      public DbCreateTerminalBuilder fire() {
        return new DbCreateTerminalBuilder() {
          public CouchTx tx() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(CouchMetaDriver.DbCreate.visit()).toString(), CouchTx.class);
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
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  CouchMetaDriver.DbCreate.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        };
      }


      public DbCreateActionBuilder state(Rfc822HeaderState state) {
        return (DbCreateActionBuilder) super.state(state);
      }


      public DbCreateActionBuilder key(java.nio.channels.SelectionKey key) {
        return (DbCreateActionBuilder) super.key(key);
      }
    }


    public DbCreateActionBuilder to() {
      if (parms.size() >= parmsCount) return new DbCreateActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db]");
    }


    static private final int parmsCount = 1;

    public DbCreate db(String stringParam) {
      parms.put(etype.db, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer DbDelete(String db);

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


      public DbDeleteTerminalBuilder fire() {
        return new DbDeleteTerminalBuilder() {
          public CouchTx tx() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(CouchMetaDriver.DbDelete.visit()).toString(), CouchTx.class);
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
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  CouchMetaDriver.DbDelete.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        };
      }


      public DbDeleteActionBuilder state(Rfc822HeaderState state) {
        return (DbDeleteActionBuilder) super.state(state);
      }


      public DbDeleteActionBuilder key(java.nio.channels.SelectionKey key) {
        return (DbDeleteActionBuilder) super.key(key);
      }
    }


    public DbDeleteActionBuilder to() {
      if (parms.size() >= parmsCount) return new DbDeleteActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db]");
    }


    static private final int parmsCount = 1;

    public DbDelete db(String stringParam) {
      parms.put(etype.db, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer DocFetch(String db, String docId);

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


      public DocFetchTerminalBuilder fire() {
        return new DocFetchTerminalBuilder() {
          public java.nio.ByteBuffer pojo() {
            try {
              return CouchMetaDriver.DocFetch.visit();
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
                  return CouchMetaDriver.DocFetch.visit(dbKeysBuilder, actionBuilder);
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
              ByteBuffer visit = CouchMetaDriver.DocFetch.visit();
              return null == visit ? null : one.xio.HttpMethod.UTF8.decode(avoidStarvation(visit)).toString();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }
        };
      }


      public DocFetchActionBuilder state(Rfc822HeaderState state) {
        return (DocFetchActionBuilder) super.state(state);
      }


      public DocFetchActionBuilder key(java.nio.channels.SelectionKey key) {
        return (DocFetchActionBuilder) super.key(key);
      }
    }


    public DocFetchActionBuilder to() {
      if (parms.size() >= parmsCount) return new DocFetchActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }


    static private final int parmsCount = 2;

    public DocFetch db(String stringParam) {
      parms.put(etype.db, stringParam);
      return this;
    }

    public DocFetch docId(String stringParam) {
      parms.put(etype.docId, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer RevisionFetch(String db, String docId);

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


      public RevisionFetchTerminalBuilder fire() {
        return new RevisionFetchTerminalBuilder() {
          public String json() {
            try {
              ByteBuffer visit = CouchMetaDriver.RevisionFetch.visit();
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
                  return CouchMetaDriver.RevisionFetch.visit(dbKeysBuilder, actionBuilder);
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


      public RevisionFetchActionBuilder state(Rfc822HeaderState state) {
        return (RevisionFetchActionBuilder) super.state(state);
      }


      public RevisionFetchActionBuilder key(java.nio.channels.SelectionKey key) {
        return (RevisionFetchActionBuilder) super.key(key);
      }
    }


    public RevisionFetchActionBuilder to() {
      if (parms.size() >= parmsCount) return new RevisionFetchActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }


    static private final int parmsCount = 2;

    public RevisionFetch db(String stringParam) {
      parms.put(etype.db, stringParam);
      return this;
    }

    public RevisionFetch docId(String stringParam) {
      parms.put(etype.docId, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer DocPersist(String db, String validjson);

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


      public DocPersistTerminalBuilder fire() {
        return new DocPersistTerminalBuilder() {
          public CouchTx tx() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(CouchMetaDriver.DocPersist.visit()).toString(), CouchTx.class);
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
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  CouchMetaDriver.DocPersist.visit();
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
                  return CouchMetaDriver.DocPersist.visit(dbKeysBuilder, actionBuilder);
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


      public DocPersistActionBuilder state(Rfc822HeaderState state) {
        return (DocPersistActionBuilder) super.state(state);
      }


      public DocPersistActionBuilder key(java.nio.channels.SelectionKey key) {
        return (DocPersistActionBuilder) super.key(key);
      }
    }


    public DocPersistActionBuilder to() {
      if (parms.size() >= parmsCount) return new DocPersistActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, validjson]");
    }


    static private final int parmsCount = 2;

    public DocPersist db(String stringParam) {
      parms.put(etype.db, stringParam);
      return this;
    }

    public DocPersist validjson(String stringParam) {
      parms.put(etype.validjson, stringParam);
      return this;
    }

    public DocPersist docId(String stringParam) {
      parms.put(etype.docId, stringParam);
      return this;
    }

    public DocPersist rev(String stringParam) {
      parms.put(etype.rev, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer DocDelete(String db, String docId, String rev);

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


      public DocDeleteTerminalBuilder fire() {
        return new DocDeleteTerminalBuilder() {
          public CouchTx tx() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(CouchMetaDriver.DocDelete.visit()).toString(), CouchTx.class);
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
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  CouchMetaDriver.DocDelete.visit();
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
                  return CouchMetaDriver.DocDelete.visit(dbKeysBuilder, actionBuilder);
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


      public DocDeleteActionBuilder state(Rfc822HeaderState state) {
        return (DocDeleteActionBuilder) super.state(state);
      }


      public DocDeleteActionBuilder key(java.nio.channels.SelectionKey key) {
        return (DocDeleteActionBuilder) super.key(key);
      }
    }


    public DocDeleteActionBuilder to() {
      if (parms.size() >= parmsCount) return new DocDeleteActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, docId, rev]");
    }


    static private final int parmsCount = 3;

    public DocDelete db(String stringParam) {
      parms.put(etype.db, stringParam);
      return this;
    }

    public DocDelete docId(String stringParam) {
      parms.put(etype.docId, stringParam);
      return this;
    }

    public DocDelete rev(String stringParam) {
      parms.put(etype.rev, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer DesignDocFetch(String db, String designDocId);

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


      public DesignDocFetchTerminalBuilder fire() {
        return new DesignDocFetchTerminalBuilder() {
          public java.nio.ByteBuffer pojo() {
            try {
              return CouchMetaDriver.DesignDocFetch.visit();
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
                  return CouchMetaDriver.DesignDocFetch.visit(dbKeysBuilder, actionBuilder);
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
              ByteBuffer visit = CouchMetaDriver.DesignDocFetch.visit();
              return null == visit ? null : one.xio.HttpMethod.UTF8.decode(avoidStarvation(visit)).toString();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }
        };
      }


      public DesignDocFetchActionBuilder state(Rfc822HeaderState state) {
        return (DesignDocFetchActionBuilder) super.state(state);
      }


      public DesignDocFetchActionBuilder key(java.nio.channels.SelectionKey key) {
        return (DesignDocFetchActionBuilder) super.key(key);
      }
    }


    public DesignDocFetchActionBuilder to() {
      if (parms.size() >= parmsCount) return new DesignDocFetchActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, designDocId]");
    }


    static private final int parmsCount = 2;

    public DesignDocFetch db(String stringParam) {
      parms.put(etype.db, stringParam);
      return this;
    }

    public DesignDocFetch designDocId(String stringParam) {
      parms.put(etype.designDocId, stringParam);
      return this;
    }

  }

  java.nio.ByteBuffer ViewFetch(String db, String view);

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


      public ViewFetchTerminalBuilder fire() {
        return new ViewFetchTerminalBuilder() {
          public rxf.server.CouchResultSet rows() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(avoidStarvation(CouchMetaDriver.ViewFetch.visit())).toString(),
                  new java.lang.reflect.ParameterizedType() {
                    public Type getRawType() {
                      return CouchResultSet.class;
                    }

                    public Type getOwnerType() {
                      return null;
                    }

                    public Type[] getActualTypeArguments() {
                      return new Type[]{ViewFetch.this.<Class>get(etype.type)};
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
                  return CouchMetaDriver.ViewFetch.visit(dbKeysBuilder, actionBuilder);
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


      public ViewFetchActionBuilder state(Rfc822HeaderState state) {
        return (ViewFetchActionBuilder) super.state(state);
      }


      public ViewFetchActionBuilder key(java.nio.channels.SelectionKey key) {
        return (ViewFetchActionBuilder) super.key(key);
      }
    }


    public ViewFetchActionBuilder to() {
      if (parms.size() >= parmsCount) return new ViewFetchActionBuilder();
      throw new IllegalArgumentException("required parameters are: [db, view]");
    }


    static private final int parmsCount = 2;

    public ViewFetch db(String stringParam) {
      parms.put(etype.db, stringParam);
      return this;
    }

    public ViewFetch view(String stringParam) {
      parms.put(etype.view, stringParam);
      return this;
    }

    public ViewFetch type(java.lang.Class classParam) {
      parms.put(etype.type, classParam);
      return this;
    }

  }

  java.nio.ByteBuffer JsonSend(String opaque, String validjson);

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


      public JsonSendTerminalBuilder fire() {
        return new JsonSendTerminalBuilder() {
          public CouchTx tx() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(CouchMetaDriver.JsonSend.visit()).toString(), CouchTx.class);
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
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  CouchMetaDriver.JsonSend.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public rxf.server.CouchResultSet rows() {
            try {
              return BlobAntiPatternObject.GSON.fromJson(one.xio.HttpMethod.UTF8.decode(avoidStarvation(CouchMetaDriver.JsonSend.visit())).toString(),
                  new java.lang.reflect.ParameterizedType() {
                    public Type getRawType() {
                      return CouchResultSet.class;
                    }

                    public Type getOwnerType() {
                      return null;
                    }

                    public Type[] getActualTypeArguments() {
                      return new Type[]{JsonSend.this.<Class>get(etype.type)};
                    }
                  });
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public String json() {
            try {
              ByteBuffer visit = CouchMetaDriver.JsonSend.visit();
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
                  return CouchMetaDriver.JsonSend.visit(dbKeysBuilder, actionBuilder);
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


      public JsonSendActionBuilder state(Rfc822HeaderState state) {
        return (JsonSendActionBuilder) super.state(state);
      }


      public JsonSendActionBuilder key(java.nio.channels.SelectionKey key) {
        return (JsonSendActionBuilder) super.key(key);
      }
    }


    public JsonSendActionBuilder to() {
      if (parms.size() >= parmsCount) return new JsonSendActionBuilder();
      throw new IllegalArgumentException("required parameters are: [opaque, validjson]");
    }


    static private final int parmsCount = 2;

    public JsonSend opaque(String stringParam) {
      parms.put(etype.opaque, stringParam);
      return this;
    }

    public JsonSend validjson(String stringParam) {
      parms.put(etype.validjson, stringParam);
      return this;
    }

    public JsonSend type(java.lang.Class classParam) {
      parms.put(etype.type, classParam);
      return this;
    }

  }
}