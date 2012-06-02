package rxf.server;
//generated

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.*;

import one.xio.MimeType;

/**
 * generated drivers
 */
public interface CouchDriver {
  CouchTx createDb(String db, String validjson);


  public class createDbBuilder<T> extends DbKeysBuilder<CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;


    interface createDbTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();

      void oneWay();
    }

    public class createDbActionBuilder extends ActionBuilder<CouchTx> {
      public createDbActionBuilder(SynchronousQueue/*<rxf.server.CouchTx>*/... synchronousQueues) {
        super(synchronousQueues);
      }

      @Override
      public createDbTerminalBuilder fire() {
        return new createDbTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.createDb.visit();
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
                  CouchMetaDriver.createDb.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        };
      }

      @Override
      public createDbActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public createDbActionBuilder key(SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public createDbActionBuilder to(SynchronousQueue/*<rxf.server.CouchTx>*/... dest) {
      if (parms.size() == parmsCount)
        return new createDbActionBuilder(dest);

      throw new IllegalArgumentException("required parameters are: [db, validjson]");
    }

    static private final int parmsCount = 2;

    public createDbBuilder db(String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public createDbBuilder validjson(String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  CouchTx createDoc(String db, String docId, String validjson);


  public class createDocBuilder<T> extends DbKeysBuilder<CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;


    interface createDocTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();

      void oneWay();
    }

    public class createDocActionBuilder extends ActionBuilder<CouchTx> {
      public createDocActionBuilder(SynchronousQueue/*<rxf.server.CouchTx>*/... synchronousQueues) {
        super(synchronousQueues);
      }

      @Override
      public createDocTerminalBuilder fire() {
        return new createDocTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.createDoc.visit();
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
                  CouchMetaDriver.createDoc.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        };
      }

      @Override
      public createDocActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public createDocActionBuilder key(SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public createDocActionBuilder to(SynchronousQueue/*<rxf.server.CouchTx>*/... dest) {
      if (parms.size() == parmsCount)
        return new createDocActionBuilder(dest);

      throw new IllegalArgumentException("required parameters are: [db, docId, validjson]");
    }

    static private final int parmsCount = 3;

    public createDocBuilder db(String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public createDocBuilder docId(String string) {
      parms.put(DbKeys.etype.docId, string);
      return this;
    }

    public createDocBuilder validjson(String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  String getDoc(String db, String docId);


  public class getDocBuilder<T> extends DbKeysBuilder<String> {
    private Rfc822HeaderState rfc822HeaderState;


    interface getDocTerminalBuilder extends TerminalBuilder<String> {
      String pojo();

      Future<String> future();
    }

    public class getDocActionBuilder extends ActionBuilder<String> {
      public getDocActionBuilder(SynchronousQueue/*<java.lang.String>*/... synchronousQueues) {
        super(synchronousQueues);
      }

      @Override
      public getDocTerminalBuilder fire() {
        return new getDocTerminalBuilder() {
          public String pojo() {
            try {
              return (String) CouchMetaDriver.getDoc.visit();
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
                  return (String) CouchMetaDriver.getDoc.visit(dbKeysBuilder, actionBuilder);
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
      public getDocActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public getDocActionBuilder key(SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public getDocActionBuilder to(SynchronousQueue/*<java.lang.String>*/... dest) {
      if (parms.size() == parmsCount)
        return new getDocActionBuilder(dest);

      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }

    static private final int parmsCount = 2;

    public getDocBuilder db(String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public getDocBuilder docId(String string) {
      parms.put(DbKeys.etype.docId, string);
      return this;
    }

  }

  String getRevision(String db, String docId);


  public class getRevisionBuilder<T> extends DbKeysBuilder<String> {
    private Rfc822HeaderState rfc822HeaderState;


    interface getRevisionTerminalBuilder extends TerminalBuilder<String> {
      CouchTx tx();

      Future<String> future();
    }

    public class getRevisionActionBuilder extends ActionBuilder<String> {
      public getRevisionActionBuilder(SynchronousQueue/*<java.lang.String>*/... synchronousQueues) {
        super(synchronousQueues);
      }

      @Override
      public getRevisionTerminalBuilder fire() {
        return new getRevisionTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.getRevision.visit();
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
                  return (String) CouchMetaDriver.getRevision.visit(dbKeysBuilder, actionBuilder);
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
      public getRevisionActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public getRevisionActionBuilder key(SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public getRevisionActionBuilder to(SynchronousQueue/*<java.lang.String>*/... dest) {
      if (parms.size() == parmsCount)
        return new getRevisionActionBuilder(dest);

      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }

    static private final int parmsCount = 2;

    public getRevisionBuilder db(String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public getRevisionBuilder docId(String string) {
      parms.put(DbKeys.etype.docId, string);
      return this;
    }

  }

  CouchTx updateDoc(String db, String docId, String rev, String validjson);


  public class updateDocBuilder<T> extends DbKeysBuilder<CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;


    interface updateDocTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();

      void oneWay();

      Future<CouchTx> future();
    }

    public class updateDocActionBuilder extends ActionBuilder<CouchTx> {
      public updateDocActionBuilder(SynchronousQueue/*<rxf.server.CouchTx>*/... synchronousQueues) {
        super(synchronousQueues);
      }

      @Override
      public updateDocTerminalBuilder fire() {
        return new updateDocTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.updateDoc.visit();
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
                  CouchMetaDriver.updateDoc.visit(/*dbKeysBuilder,actionBuilder*/);
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
                  return (CouchTx) CouchMetaDriver.updateDoc.visit(dbKeysBuilder, actionBuilder);
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
      public updateDocActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public updateDocActionBuilder key(SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public updateDocActionBuilder to(SynchronousQueue/*<rxf.server.CouchTx>*/... dest) {
      if (parms.size() == parmsCount)
        return new updateDocActionBuilder(dest);

      throw new IllegalArgumentException("required parameters are: [db, docId, rev, validjson]");
    }

    static private final int parmsCount = 4;

    public updateDocBuilder db(String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public updateDocBuilder docId(String string) {
      parms.put(DbKeys.etype.docId, string);
      return this;
    }

    public updateDocBuilder rev(String string) {
      parms.put(DbKeys.etype.rev, string);
      return this;
    }

    public updateDocBuilder validjson(String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  CouchTx createNewDesignDoc(String db, String designDocId, String validjson);


  public class createNewDesignDocBuilder<T> extends DbKeysBuilder<CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;


    interface createNewDesignDocTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();

      void oneWay();
    }

    public class createNewDesignDocActionBuilder extends ActionBuilder<CouchTx> {
      public createNewDesignDocActionBuilder(SynchronousQueue/*<rxf.server.CouchTx>*/... synchronousQueues) {
        super(synchronousQueues);
      }

      @Override
      public createNewDesignDocTerminalBuilder fire() {
        return new createNewDesignDocTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.createNewDesignDoc.visit();
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
                  CouchMetaDriver.createNewDesignDoc.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        };
      }

      @Override
      public createNewDesignDocActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public createNewDesignDocActionBuilder key(SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public createNewDesignDocActionBuilder to(SynchronousQueue/*<rxf.server.CouchTx>*/... dest) {
      if (parms.size() == parmsCount)
        return new createNewDesignDocActionBuilder(dest);

      throw new IllegalArgumentException("required parameters are: [db, designDocId, validjson]");
    }

    static private final int parmsCount = 3;

    public createNewDesignDocBuilder db(String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public createNewDesignDocBuilder designDocId(String string) {
      parms.put(DbKeys.etype.designDocId, string);
      return this;
    }

    public createNewDesignDocBuilder validjson(String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  String getDesignDoc(String db, String designDocId);


  public class getDesignDocBuilder<T> extends DbKeysBuilder<String> {
    private Rfc822HeaderState rfc822HeaderState;


    interface getDesignDocTerminalBuilder extends TerminalBuilder<String> {
      CouchTx tx();
    }

    public class getDesignDocActionBuilder extends ActionBuilder<String> {
      public getDesignDocActionBuilder(SynchronousQueue/*<java.lang.String>*/... synchronousQueues) {
        super(synchronousQueues);
      }

      @Override
      public getDesignDocTerminalBuilder fire() {
        return new getDesignDocTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.getDesignDoc.visit();
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }
        };
      }

      @Override
      public getDesignDocActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public getDesignDocActionBuilder key(SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public getDesignDocActionBuilder to(SynchronousQueue/*<java.lang.String>*/... dest) {
      if (parms.size() == parmsCount)
        return new getDesignDocActionBuilder(dest);

      throw new IllegalArgumentException("required parameters are: [db, designDocId]");
    }

    static private final int parmsCount = 2;

    public getDesignDocBuilder db(String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public getDesignDocBuilder designDocId(String string) {
      parms.put(DbKeys.etype.designDocId, string);
      return this;
    }

  }

  CouchTx updateDesignDoc(String db, String designDocId, String rev, String validjson);


  public class updateDesignDocBuilder<T> extends DbKeysBuilder<CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;


    interface updateDesignDocTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();

      void oneWay();
    }

    public class updateDesignDocActionBuilder extends ActionBuilder<CouchTx> {
      public updateDesignDocActionBuilder(SynchronousQueue/*<rxf.server.CouchTx>*/... synchronousQueues) {
        super(synchronousQueues);
      }

      @Override
      public updateDesignDocTerminalBuilder fire() {
        return new updateDesignDocTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.updateDesignDoc.visit();
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
                  CouchMetaDriver.updateDesignDoc.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        };
      }

      @Override
      public updateDesignDocActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public updateDesignDocActionBuilder key(SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public updateDesignDocActionBuilder to(SynchronousQueue/*<rxf.server.CouchTx>*/... dest) {
      if (parms.size() == parmsCount)
        return new updateDesignDocActionBuilder(dest);

      throw new IllegalArgumentException("required parameters are: [db, designDocId, rev, validjson]");
    }

    static private final int parmsCount = 4;

    public updateDesignDocBuilder db(String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public updateDesignDocBuilder designDocId(String string) {
      parms.put(DbKeys.etype.designDocId, string);
      return this;
    }

    public updateDesignDocBuilder rev(String string) {
      parms.put(DbKeys.etype.rev, string);
      return this;
    }

    public updateDesignDocBuilder validjson(String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  CouchResultSet getView(String db, String view);


  public class getViewBuilder<T> extends DbKeysBuilder<CouchResultSet> {
    private Rfc822HeaderState rfc822HeaderState;


    interface getViewTerminalBuilder extends TerminalBuilder<CouchResultSet> {
      CouchResultSet/*<rxf.server.CouchResultSet>*/ rows();

      Future<CouchResultSet> future();

      void continuousFeed();

    }

    public class getViewActionBuilder extends ActionBuilder<CouchResultSet> {
      public getViewActionBuilder(SynchronousQueue/*<rxf.server.CouchResultSet>*/... synchronousQueues) {
        super(synchronousQueues);
      }

      @Override
      public getViewTerminalBuilder fire() {
        return new getViewTerminalBuilder() {
          public CouchResultSet/*<rxf.server.CouchResultSet>*/ rows() {
            try {
              return (CouchResultSet/*<rxf.server.CouchResultSet>*/) BlobAntiPatternObject.GSON.fromJson((String) CouchMetaDriver.getView.visit(), CouchResultSet/*<rxf.server.CouchResultSet>*/.class);
            } catch (Exception e) {
              e.printStackTrace();
            }
            return null;
          }

          public Future<CouchResultSet> future() {
            try {
              BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<CouchResultSet>() {


                final DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
                final ActionBuilder actionBuilder = (ActionBuilder) ActionBuilder.get();

                public CouchResultSet call() throws Exception {

                  DbKeysBuilder.currentKeys.set(dbKeysBuilder);
                  ActionBuilder.currentAction.set(actionBuilder);
                  return (CouchResultSet) CouchMetaDriver.getView.visit(dbKeysBuilder, actionBuilder);
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
      public getViewActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public getViewActionBuilder key(SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public getViewActionBuilder to(SynchronousQueue/*<rxf.server.CouchResultSet>*/... dest) {
      if (parms.size() == parmsCount)
        return new getViewActionBuilder(dest);

      throw new IllegalArgumentException("required parameters are: [db, view]");
    }

    static private final int parmsCount = 2;

    public getViewBuilder db(String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public getViewBuilder view(String string) {
      parms.put(DbKeys.etype.view, string);
      return this;
    }

  }

  CouchTx sendJson(String opaque, String validjson);


  public class sendJsonBuilder<T> extends DbKeysBuilder<CouchTx> {
    private Rfc822HeaderState rfc822HeaderState;


    interface sendJsonTerminalBuilder extends TerminalBuilder<CouchTx> {
      CouchTx tx();

      void oneWay();

      CouchResultSet/*<rxf.server.CouchTx>*/ rows();

      Future<CouchTx> future();

      void continuousFeed();

    }

    public class sendJsonActionBuilder extends ActionBuilder<CouchTx> {
      public sendJsonActionBuilder(SynchronousQueue/*<rxf.server.CouchTx>*/... synchronousQueues) {
        super(synchronousQueues);
      }

      @Override
      public sendJsonTerminalBuilder fire() {
        return new sendJsonTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.sendJson.visit();
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
                  CouchMetaDriver.sendJson.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }

          public CouchResultSet/*<rxf.server.CouchTx>*/ rows() {
            try {
              return (CouchResultSet/*<rxf.server.CouchTx>*/) BlobAntiPatternObject.GSON.fromJson((String) CouchMetaDriver.sendJson.visit(), CouchResultSet/*<rxf.server.CouchTx>*/.class);
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
                  return (CouchTx) CouchMetaDriver.sendJson.visit(dbKeysBuilder, actionBuilder);
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
      public sendJsonActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public sendJsonActionBuilder key(SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public sendJsonActionBuilder to(SynchronousQueue/*<rxf.server.CouchTx>*/... dest) {
      if (parms.size() == parmsCount)
        return new sendJsonActionBuilder(dest);

      throw new IllegalArgumentException("required parameters are: [opaque, validjson]");
    }

    static private final int parmsCount = 2;

    public sendJsonBuilder opaque(String string) {
      parms.put(DbKeys.etype.opaque, string);
      return this;
    }

    public sendJsonBuilder validjson(String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  Rfc822HeaderState sendBlob(String opaque, MimeType mimetype, ByteBuffer blob);


  public class sendBlobBuilder<T> extends DbKeysBuilder<Rfc822HeaderState> {
    private Rfc822HeaderState rfc822HeaderState;


    interface sendBlobTerminalBuilder extends TerminalBuilder<Rfc822HeaderState> {
      CouchTx tx();

      Future<Rfc822HeaderState> future();

      void oneWay();
    }

    public class sendBlobActionBuilder extends ActionBuilder<Rfc822HeaderState> {
      public sendBlobActionBuilder(SynchronousQueue/*<rxf.server.Rfc822HeaderState>*/... synchronousQueues) {
        super(synchronousQueues);
      }

      @Override
      public sendBlobTerminalBuilder fire() {
        return new sendBlobTerminalBuilder() {
          public CouchTx tx() {
            try {
              return (CouchTx) CouchMetaDriver.sendBlob.visit();
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
                  return (Rfc822HeaderState) CouchMetaDriver.sendBlob.visit(dbKeysBuilder, actionBuilder);
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
                  CouchMetaDriver.sendBlob.visit(/*dbKeysBuilder,actionBuilder*/);
                } catch (Exception e) {
                  e.printStackTrace();
                }
              }
            });
          }
        };
      }

      @Override
      public sendBlobActionBuilder state(Rfc822HeaderState state) {
        return super.state(state);
      }

      @Override
      public sendBlobActionBuilder key(SelectionKey key) {
        return super.key(key);
      }
    }

    @Override
    public sendBlobActionBuilder to(SynchronousQueue/*<rxf.server.Rfc822HeaderState>*/... dest) {
      if (parms.size() == parmsCount)
        return new sendBlobActionBuilder(dest);

      throw new IllegalArgumentException("required parameters are: [opaque, mimetype, blob]");
    }

    static private final int parmsCount = 3;

    public sendBlobBuilder opaque(String string) {
      parms.put(DbKeys.etype.opaque, string);
      return this;
    }

    public sendBlobBuilder mimetype(MimeType mimetype) {
      parms.put(DbKeys.etype.mimetype, mimetype);
      return this;
    }

    public sendBlobBuilder blob(ByteBuffer bytebuffer) {
      parms.put(DbKeys.etype.blob, bytebuffer);
      return this;
    }

  }
}
