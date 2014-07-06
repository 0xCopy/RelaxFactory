package rxf.couch.gen;

// generated

import one.xio.MimeType;
import rxf.core.Tx;
import rxf.core.Rfc822HeaderState;
import rxf.core.TerminalBuilder;
import rxf.couch.*;
import rxf.couch.driver.CouchMetaDriver;
import rxf.rpc.RpcHelper;
import rxf.shared.CouchTx;

import java.lang.String;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static java.nio.charset.StandardCharsets.UTF_8;
import static rxf.core.Rfc822HeaderState.avoidStarvation;

/**
 * generated drivers
 */
public interface CouchDriver {

  // generated items

  class DbCreate extends DbKeysBuilder {

    public DbCreateActionBuilder to() {
      assert 1 <= parms.size() : "required parameters are: [db]";
      return new DbCreateActionBuilder();
    }

    public DbCreate db(String stringParam) {
      parms.put(CouchMetaDriver.etype.db, stringParam);
      return this;
    }

    public interface DbCreateTerminalBuilder extends TerminalBuilder {
      CouchTx tx();

      @Deprecated
      void oneWay();
    }

    public class DbCreateActionBuilder extends Tx {
      public DbCreateTerminalBuilder fire() {
        return new DbCreateTerminalBuilder() {
          Future<ByteBuffer> future = RpcHelper.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
            final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();

            public ByteBuffer call() throws Exception {
              DbKeysBuilder.currentKeys.set(dbKeysBuilder);

              CouchMetaDriver.DbCreate.visit(dbKeysBuilder, DbCreateActionBuilder.this);
              return payload();
            }
          });

          public CouchTx tx() {
            CouchTx r = null;
            try {
              r =
                  CouchMetaDriver.gson().fromJson(UTF_8.decode(future.get()).toString(),
                      CouchTx.class);
            } catch (Exception e) {
              if (RpcHelper.DEBUG_SENDJSON)
                e.printStackTrace();
            }
            return r;
          }

          @Deprecated
          public void oneWay() {
            final DbKeysBuilder dbKeysBuilder = DbKeysBuilder.get();

            RpcHelper.EXECUTOR_SERVICE.submit(new Runnable() {
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);

                  future.get();
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

      public DbCreateActionBuilder key(SelectionKey key) {
        return (DbCreateActionBuilder) super.key(key);
      }
    }

  }

  class DbDelete extends DbKeysBuilder {

    public DbDeleteActionBuilder to() {
      assert 1 <= parms.size() : "required parameters are: [db]";
      return new DbDeleteActionBuilder();
    }

    public DbDelete db(String stringParam) {
      parms.put(CouchMetaDriver.etype.db, stringParam);
      return this;
    }

    public interface DbDeleteTerminalBuilder extends TerminalBuilder {
      CouchTx tx();

      @Deprecated
      void oneWay();
    }

    public class DbDeleteActionBuilder extends Tx {

      public DbDeleteTerminalBuilder fire() {
        return new DbDeleteTerminalBuilder() {
          Future<ByteBuffer> future = RpcHelper.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
            final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();

            public ByteBuffer call() throws Exception {
              DbKeysBuilder.currentKeys.set(dbKeysBuilder);

              CouchMetaDriver.DbDelete.visit(dbKeysBuilder, DbDeleteActionBuilder.this);
              return payload();

            }
          });

          public CouchTx tx() {
            CouchTx r = null;
            try {
              r =
                  CouchMetaDriver.gson().fromJson(UTF_8.decode(future.get()).toString(),
                      CouchTx.class);
            } catch (Exception e) {
              if (!RpcHelper.DEBUG_SENDJSON)
                return r;
              e.printStackTrace();
            }
            return r;
          }

          @Deprecated
          public void oneWay() {
            final DbKeysBuilder dbKeysBuilder = DbKeysBuilder.get();

            RpcHelper.EXECUTOR_SERVICE.submit(new Runnable() {
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);

                  future.get();
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

      public DbDeleteActionBuilder key(SelectionKey key) {
        return (DbDeleteActionBuilder) super.key(key);
      }
    }

  }

  class DocFetch extends DbKeysBuilder {

    public DocFetchActionBuilder to() {
      assert 2 <= parms.size() : "required parameters are: [db, docId]";
      return new DocFetchActionBuilder();
    }

    public DocFetch db(String stringParam) {
      parms.put(CouchMetaDriver.etype.db, stringParam);
      return this;
    }

    public DocFetch docId(String stringParam) {
      parms.put(CouchMetaDriver.etype.docId, stringParam);
      return this;
    }

    public interface DocFetchTerminalBuilder extends TerminalBuilder {
      ByteBuffer pojo();

      Future<ByteBuffer> future();

      String json();
    }

    public class DocFetchActionBuilder extends Tx {

      public DocFetchTerminalBuilder fire() {
        return new DocFetchTerminalBuilder() {
          Future<ByteBuffer> future = RpcHelper.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
            final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();

            public ByteBuffer call() throws Exception {
              DbKeysBuilder.currentKeys.set(dbKeysBuilder);

              CouchMetaDriver.DocFetch.visit(dbKeysBuilder, DocFetchActionBuilder.this);
              return payload();
            }
          });

          public ByteBuffer pojo() {
            ByteBuffer r = null;
            try {
              r = future.get();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return r;
          }

          public Future<ByteBuffer> future() {
            return future;
          }

          public String json() {
            String r = null;
            try {
              ByteBuffer visit = future.get();
              r = null == visit ? null : UTF_8.decode(avoidStarvation(visit)).toString();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return r;
          }
        };
      }

      public DocFetchActionBuilder state(Rfc822HeaderState state) {
        return (DocFetchActionBuilder) super.state(state);
      }

      public DocFetchActionBuilder key(SelectionKey key) {
        return (DocFetchActionBuilder) super.key(key);
      }
    }

  }

  class RevisionFetch extends DbKeysBuilder {
    private static final int parmsCount = 2;

    public RevisionFetchActionBuilder to() {
      assert parmsCount <= parms.size() : "required parameters are: [db, docId]";
      return new RevisionFetchActionBuilder();
    }

    public RevisionFetch db(String stringParam) {
      parms.put(CouchMetaDriver.etype.db, stringParam);
      return this;
    }

    public RevisionFetch docId(String stringParam) {
      parms.put(CouchMetaDriver.etype.docId, stringParam);
      return this;
    }

    public interface RevisionFetchTerminalBuilder extends TerminalBuilder {
      String json();

      Future<ByteBuffer> future();
    }

    public class RevisionFetchActionBuilder extends Tx {

      public RevisionFetchTerminalBuilder fire() {
        return new RevisionFetchTerminalBuilder() {
          Future<ByteBuffer> future = RpcHelper.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
            final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();

            public ByteBuffer call() throws Exception {
              DbKeysBuilder.currentKeys.set(dbKeysBuilder);

              CouchMetaDriver.RevisionFetch.visit(dbKeysBuilder, RevisionFetchActionBuilder.this);
              return payload();
            }
          });

          public String json() {
            String r = null;
            try {
              ByteBuffer visit = future.get();
              r = null == visit ? null : UTF_8.decode(avoidStarvation(visit)).toString();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return r;
          }

          public Future<ByteBuffer> future() {
            return future;
          }
        };
      }

      public RevisionFetchActionBuilder state(Rfc822HeaderState state) {
        return (RevisionFetchActionBuilder) super.state(state);
      }

      public RevisionFetchActionBuilder key(SelectionKey key) {
        return (RevisionFetchActionBuilder) super.key(key);
      }
    }

  }

  class DocPersist extends DbKeysBuilder {

    public DocPersistActionBuilder to() {
      assert 2 <= parms.size() : "required parameters are: [db, validjson]";
      return new DocPersistActionBuilder();
    }

    public DocPersist db(String stringParam) {
      parms.put(CouchMetaDriver.etype.db, stringParam);
      return this;
    }

    public DocPersist validjson(String stringParam) {
      parms.put(CouchMetaDriver.etype.validjson, stringParam);
      return this;
    }

    public DocPersist docId(String stringParam) {
      parms.put(CouchMetaDriver.etype.docId, stringParam);
      return this;
    }

    public DocPersist rev(String stringParam) {
      parms.put(CouchMetaDriver.etype.rev, stringParam);
      return this;
    }

    public interface DocPersistTerminalBuilder extends TerminalBuilder {
      CouchTx tx();

      @Deprecated
      void oneWay();

      Future<ByteBuffer> future();
    }

    public class DocPersistActionBuilder extends Tx {

      public DocPersistTerminalBuilder fire() {
        return new DocPersistTerminalBuilder() {
          Future<ByteBuffer> future = RpcHelper.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
            final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();

            public ByteBuffer call() throws Exception {
              DbKeysBuilder.currentKeys.set(dbKeysBuilder);

              CouchMetaDriver.DocPersist.visit(dbKeysBuilder, DocPersistActionBuilder.this);
              return payload();
            }
          });

          public CouchTx tx() {
            CouchTx r = null;
            try {
              r =
                  CouchMetaDriver.gson().fromJson(UTF_8.decode(future.get()).toString(),
                      CouchTx.class);
            } catch (Exception e) {
              if (RpcHelper.DEBUG_SENDJSON)
                e.printStackTrace();
            }
            return r;
          }

          @Deprecated
          public void oneWay() {
            final DbKeysBuilder dbKeysBuilder = DbKeysBuilder.get();

            RpcHelper.EXECUTOR_SERVICE.submit(new Runnable() {
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);

                  future.get();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public Future<ByteBuffer> future() {
            return future;
          }
        };
      }

      public DocPersistActionBuilder state(Rfc822HeaderState state) {
        return (DocPersistActionBuilder) super.state(state);
      }

      public DocPersistActionBuilder key(SelectionKey key) {
        return (DocPersistActionBuilder) super.key(key);
      }
    }

  }

  class DocDelete extends DbKeysBuilder {
    private static final int parmsCount = 3;

    public DocDeleteActionBuilder to() {
      assert parmsCount <= parms.size() : "required parameters are: [db, docId, rev]";
      return new DocDeleteActionBuilder();
    }

    public DocDelete db(String stringParam) {
      parms.put(CouchMetaDriver.etype.db, stringParam);
      return this;
    }

    public DocDelete docId(String stringParam) {
      parms.put(CouchMetaDriver.etype.docId, stringParam);
      return this;
    }

    public DocDelete rev(String stringParam) {
      parms.put(CouchMetaDriver.etype.rev, stringParam);
      return this;
    }

    public interface DocDeleteTerminalBuilder extends TerminalBuilder {
      CouchTx tx();

      @Deprecated
      void oneWay();

      Future<ByteBuffer> future();
    }

    public class DocDeleteActionBuilder extends Tx {

      public DocDeleteTerminalBuilder fire() {
        return new DocDeleteTerminalBuilder() {
          Future<ByteBuffer> future = RpcHelper.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
            final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();

            public ByteBuffer call() throws Exception {
              DbKeysBuilder.currentKeys.set(dbKeysBuilder);

              CouchMetaDriver.DocDelete.visit(dbKeysBuilder, DocDeleteActionBuilder.this);
              return payload();
            }
          });

          public CouchTx tx() {
            CouchTx r = null;
            try {
              r =
                  CouchMetaDriver.gson().fromJson(UTF_8.decode(future.get()).toString(),
                      CouchTx.class);
            } catch (Exception e) {
              if (RpcHelper.DEBUG_SENDJSON)
                e.printStackTrace();
            }
            return r;
          }

          @Deprecated
          public void oneWay() {
            final DbKeysBuilder dbKeysBuilder = DbKeysBuilder.get();

            RpcHelper.EXECUTOR_SERVICE.submit(new Runnable() {
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);

                  future.get();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public Future<ByteBuffer> future() {
            return future;
          }
        };
      }

      public DocDeleteActionBuilder state(Rfc822HeaderState state) {
        return (DocDeleteActionBuilder) super.state(state);
      }

      public DocDeleteActionBuilder key(SelectionKey key) {
        return (DocDeleteActionBuilder) super.key(key);
      }
    }

  }

  class DesignDocFetch extends DbKeysBuilder {

    public DesignDocFetchActionBuilder to() {
      assert 2 <= parms.size() : "required parameters are: [db, designDocId]";
      return new DesignDocFetchActionBuilder();
    }

    public DesignDocFetch db(String stringParam) {
      parms.put(CouchMetaDriver.etype.db, stringParam);
      return this;
    }

    public DesignDocFetch designDocId(String stringParam) {
      parms.put(CouchMetaDriver.etype.designDocId, stringParam);
      return this;
    }

    public interface DesignDocFetchTerminalBuilder extends TerminalBuilder {
      ByteBuffer pojo();

      Future<ByteBuffer> future();

      String json();
    }

    public class DesignDocFetchActionBuilder extends Tx {

      public DesignDocFetchTerminalBuilder fire() {
        return new DesignDocFetchTerminalBuilder() {
          Future<ByteBuffer> future = RpcHelper.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
            final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();

            public ByteBuffer call() throws Exception {
              DbKeysBuilder.currentKeys.set(dbKeysBuilder);

              CouchMetaDriver.DesignDocFetch.visit(dbKeysBuilder, DesignDocFetchActionBuilder.this);
              return payload();
            }
          });

          public ByteBuffer pojo() {
            ByteBuffer r = null;
            try {
              r = future.get();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return r;
          }

          public Future<ByteBuffer> future() {
            return future;
          }

          public String json() {
            String r = null;
            try {
              ByteBuffer visit = future.get();
              r = null == visit ? null : UTF_8.decode(avoidStarvation(visit)).toString();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return r;
          }
        };
      }

      public DesignDocFetchActionBuilder state(Rfc822HeaderState state) {
        return (DesignDocFetchActionBuilder) super.state(state);
      }

      public DesignDocFetchActionBuilder key(SelectionKey key) {
        return (DesignDocFetchActionBuilder) super.key(key);
      }
    }

  }
  // rnewson
  // "Note: Multiple keys request to a reduce function only supports group=true and NO group_level (identical to group_level=exact). The resulting error is "Multi-key
  // fetchs for reduce view must include group=true""

  class ViewFetch extends DbKeysBuilder {

    public ViewFetchActionBuilder to() {
      assert 2 <= parms.size() : "required parameters are: [db, view]";
      return new ViewFetchActionBuilder();
    }

    public ViewFetch db(String stringParam) {
      parms.put(CouchMetaDriver.etype.db, stringParam);
      return this;
    }

    public ViewFetch view(String stringParam) {
      parms.put(CouchMetaDriver.etype.view, stringParam);
      return this;
    }

    public ViewFetch type(Type typeParam) {
      parms.put(CouchMetaDriver.etype.type, typeParam);
      return this;
    }

    public ViewFetch keyType(Type typeParam) {
      parms.put(CouchMetaDriver.etype.keyType, typeParam);
      return this;
    }

    public interface ViewFetchTerminalBuilder extends TerminalBuilder {
      CouchResultSet rows();

      Future<ByteBuffer> future();

      void continuousFeed();

    }

    public class ViewFetchActionBuilder extends Tx {

      public ViewFetchTerminalBuilder fire() {

        return new ViewFetchTerminalBuilder() {
          Future<ByteBuffer> future = RpcHelper.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
            final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();

            public ByteBuffer call() throws Exception {
              DbKeysBuilder.currentKeys.set(dbKeysBuilder);

              CouchMetaDriver.ViewFetch.visit(dbKeysBuilder, ViewFetchActionBuilder.this);
              return payload();
            }
          });

          public CouchResultSet rows() {
            CouchResultSet r = null;
            try {
              ByteBuffer buf = future.get();
              // System.err.println("???? "+ HttpMethod.UTF8.decode(buf));
              r =
                  CouchMetaDriver.gson().fromJson(UTF_8.decode(avoidStarvation(buf)).toString(),
                      new ParameterizedType() {
                        public Type getRawType() {
                          return CouchResultSet.class;
                        }

                        public Type getOwnerType() {
                          return null;
                        }

                        public Type[] getActualTypeArguments() {
                          Type key = (Type) ViewFetch.this.get(CouchMetaDriver.etype.keyType);
                          return new Type[] {
                              null == key ? Object.class : key,
                              (Type) ViewFetch.this.get(CouchMetaDriver.etype.type)};
                        }
                      });
            } catch (Exception e) {
              e.printStackTrace();
            }
            return r;
          }

          public Future<ByteBuffer> future() {
            return future;
          }

          public void continuousFeed() {
            throw new AbstractMethodError();
          }
        };
      }

      public ViewFetchActionBuilder state(Rfc822HeaderState state) {
        return (ViewFetchActionBuilder) super.state(state);
      }

      public ViewFetchActionBuilder key(SelectionKey key) {
        return (ViewFetchActionBuilder) super.key(key);
      }
    }

  }

  class JsonSend extends DbKeysBuilder {

    public JsonSendActionBuilder to() {
      assert 2 <= parms.size() : "required parameters are: [opaque, validjson]";
      return new JsonSendActionBuilder();
    }

    public JsonSend opaque(String stringParam) {
      parms.put(CouchMetaDriver.etype.opaque, stringParam);
      return this;
    }

    public JsonSend validjson(String stringParam) {
      parms.put(CouchMetaDriver.etype.validjson, stringParam);
      return this;
    }

    public JsonSend type(Type typeParam) {
      parms.put(CouchMetaDriver.etype.type, typeParam);
      return this;
    }

    public JsonSend keyType(Type typeParam) {
      parms.put(CouchMetaDriver.etype.keyType, typeParam);
      return this;
    }

    public interface JsonSendTerminalBuilder extends TerminalBuilder {
      CouchTx tx();

      @Deprecated
      void oneWay();

      CouchResultSet rows();

      String json();

      Future<ByteBuffer> future();

      void continuousFeed();

    }

    public class JsonSendActionBuilder extends Tx {

      public JsonSendTerminalBuilder fire() {
        return new JsonSendTerminalBuilder() {
          Future<ByteBuffer> future = RpcHelper.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
            final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();

            public ByteBuffer call() throws Exception {
              DbKeysBuilder.currentKeys.set(dbKeysBuilder);

              CouchMetaDriver.JsonSend.visit(dbKeysBuilder, JsonSendActionBuilder.this);
              return payload();
            }
          });

          public CouchTx tx() {
            CouchTx r = null;
            try {
              r =
                  CouchMetaDriver.gson().fromJson(UTF_8.decode(future.get()).toString(),
                      CouchTx.class);
            } catch (Exception e) {
              if (RpcHelper.DEBUG_SENDJSON)
                e.printStackTrace();
            }
            return r;
          }

          @Deprecated
          public void oneWay() {
            final DbKeysBuilder dbKeysBuilder = DbKeysBuilder.get();

            RpcHelper.EXECUTOR_SERVICE.submit(new Runnable() {
              public void run() {
                try {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);

                  future.get();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public CouchResultSet rows() {
            CouchResultSet r = null;
            try {
              r =
                  CouchMetaDriver.gson().fromJson(
                      UTF_8.decode(avoidStarvation(future.get())).toString(),
                      new ParameterizedType() {
                        public Type getRawType() {
                          return CouchResultSet.class;
                        }

                        public Type getOwnerType() {
                          return null;
                        }

                        public Type[] getActualTypeArguments() {
                          Type key = (Type) JsonSend.this.get(CouchMetaDriver.etype.keyType);
                          return new Type[] {
                              null == key ? Object.class : key,
                              (Type) JsonSend.this.get(CouchMetaDriver.etype.type)};
                        }
                      });
            } catch (Exception e) {
              e.printStackTrace();
            }
            return r;
          }

          public String json() {
            String r = null;
            try {
              ByteBuffer visit = future.get();
              r = null == visit ? null : UTF_8.decode(avoidStarvation(visit)).toString();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return r;
          }

          public Future<ByteBuffer> future() {
            return future;
          }

          public void continuousFeed() {
            throw new AbstractMethodError();
          }
        };
      }

      public JsonSendActionBuilder state(Rfc822HeaderState state) {
        return (JsonSendActionBuilder) super.state(state);
      }

      public JsonSendActionBuilder key(SelectionKey key) {
        return (JsonSendActionBuilder) super.key(key);
      }
    }
  }

  class BlobSend extends DbKeysBuilder {

    public BlobSendActionBuilder to() {
      assert 5 <= parms.size() : "required parameters are: [blob, db, docId, rev, attachname]";
      return new BlobSendActionBuilder();
    }

    public BlobSend blob(ByteBuffer bytebufferParam) {
      parms.put(CouchMetaDriver.etype.blob, bytebufferParam);
      return this;
    }

    public BlobSend db(String stringParam) {
      parms.put(CouchMetaDriver.etype.db, stringParam);
      return this;
    }

    public BlobSend docId(String stringParam) {
      parms.put(CouchMetaDriver.etype.docId, stringParam);
      return this;
    }

    public BlobSend rev(String stringParam) {
      parms.put(CouchMetaDriver.etype.rev, stringParam);
      return this;
    }

    public BlobSend attachname(String stringParam) {
      parms.put(CouchMetaDriver.etype.attachname, stringParam);
      return this;
    }

    public BlobSend mimetypeEnum(MimeType mimetypeParam) {
      parms.put(CouchMetaDriver.etype.mimetypeEnum, mimetypeParam);
      return this;
    }

    public BlobSend mimetype(String stringParam) {
      parms.put(CouchMetaDriver.etype.mimetype, stringParam);
      return this;
    }

    public interface BlobSendTerminalBuilder extends TerminalBuilder {
      CouchTx tx();

      Future<ByteBuffer> future();

      @Deprecated
      void oneWay();
    }

    public class BlobSendActionBuilder extends Tx {

      public BlobSendTerminalBuilder fire() {
        return new BlobSendTerminalBuilder() {
          Future<ByteBuffer> future = RpcHelper.EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
            final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();

            public ByteBuffer call() throws Exception {
              DbKeysBuilder.currentKeys.set(dbKeysBuilder);

              CouchMetaDriver.BlobSend.visit(dbKeysBuilder, BlobSendActionBuilder.this);
              return payload();
            }
          });

          public CouchTx tx() {
            CouchTx r = null;
            try {
              r =
                  CouchMetaDriver.gson().fromJson(UTF_8.decode(future.get()).toString(),
                      CouchTx.class);
            } catch (Exception e) {
              if (!RpcHelper.DEBUG_SENDJSON)
                return r;
              e.printStackTrace();
            }
            return r;
          }

          public Future<ByteBuffer> future() {
            return future;
          }

          @Deprecated
          public void oneWay() {
            final DbKeysBuilder dbKeysBuilder = DbKeysBuilder.get();

            RpcHelper.EXECUTOR_SERVICE.submit(new Runnable() {
              public void run() {
                try {
                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);

                  future.get();
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        };
      }

      public BlobSendActionBuilder state(Rfc822HeaderState state) {
        return (BlobSendActionBuilder) super.state(state);
      }

      public BlobSendActionBuilder key(SelectionKey key) {
        return (BlobSendActionBuilder) super.key(key);
      }
    }
  }
}
