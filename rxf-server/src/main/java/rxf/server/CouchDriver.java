package rxf.server;
//generated

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;

import static rxf.server.DbKeys.etype;

/**
 * generated drivers
 */
public interface CouchDriver {
  rxf.server.CouchTx createDb(java.lang.String db, java.lang.String docId);


  public class createDbBuilder<T> extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
    private SynchronousQueue<T>[] dest;

    @Override
    public ActionBuilder<rxf.server.CouchTx> to(SynchronousQueue<rxf.server.CouchTx>... dest) {
      this.dest = dest;
      if (parms.size() == parmsCount)
        return new ActionBuilder<rxf.server.CouchTx>() {
          @Override
          public AbstractTerminalBuilder<rxf.server.CouchTx> fire() {
            return new AbstractTerminalBuilder<rxf.server.CouchTx>() {
              public CouchTx tx() {
                try {
                  return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.createDb.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }

              void oneWay() {
                BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                  @Override
                  public void run() {
                    try {
                      rxf.server.CouchMetaDriver.createDb.visit();
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                  }
                });
              }
            };
          }
        };
      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }

    static private final int parmsCount = 2;

    public createDbBuilder db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public createDbBuilder docId(java.lang.String string) {
      parms.put(DbKeys.etype.docId, string);
      return this;
    }

  }

  rxf.server.CouchTx createDoc(java.lang.String db, java.lang.String docId, java.lang.String validjson);


  public class createDocBuilder<T> extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
    private SynchronousQueue<T>[] dest;

    @Override
    public ActionBuilder<rxf.server.CouchTx> to(SynchronousQueue<rxf.server.CouchTx>... dest) {
      this.dest = dest;
      if (parms.size() == parmsCount)
        return new ActionBuilder<rxf.server.CouchTx>() {
          @Override
          public AbstractTerminalBuilder<rxf.server.CouchTx> fire() {
            return new AbstractTerminalBuilder<rxf.server.CouchTx>() {
              public CouchTx tx() {
                try {
                  return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.createDoc.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }

              void oneWay() {
                BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                  @Override
                  public void run() {
                    try {
                      rxf.server.CouchMetaDriver.createDoc.visit();
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                  }
                });
              }
            };
          }
        };
      throw new IllegalArgumentException("required parameters are: [db, docId, validjson]");
    }

    static private final int parmsCount = 3;

    public createDocBuilder db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public createDocBuilder docId(java.lang.String string) {
      parms.put(DbKeys.etype.docId, string);
      return this;
    }

    public createDocBuilder validjson(java.lang.String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  java.lang.String getDoc(java.lang.String db, java.lang.String docId);


  public class getDocBuilder<T> extends DbKeysBuilder<java.lang.String> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
    private SynchronousQueue<T>[] dest;

    @Override
    public ActionBuilder<java.lang.String> to(SynchronousQueue<java.lang.String>... dest) {
      this.dest = dest;
      if (parms.size() == parmsCount)
        return new ActionBuilder<java.lang.String>() {
          @Override
          public AbstractTerminalBuilder<java.lang.String> fire() {
            return new AbstractTerminalBuilder<java.lang.String>() {
              public java.lang.String pojo() {
                try {
                  return (java.lang.String) rxf.server.CouchMetaDriver.getDoc.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }

              public Future<java.lang.String> future() {
                try {

                  BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<java.lang.String>() {
                    @Override
                    public java.lang.String call() throws Exception {
                      return (java.lang.String) rxf.server.CouchMetaDriver.getDoc.visit();
                    }
                  });
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }
            };
          }
        };
      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }

    static private final int parmsCount = 2;

    public getDocBuilder db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public getDocBuilder docId(java.lang.String string) {
      parms.put(DbKeys.etype.docId, string);
      return this;
    }

  }

  java.lang.String getRevision(java.lang.String db, java.lang.String docId);


  public class getRevisionBuilder<T> extends DbKeysBuilder<java.lang.String> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
    private SynchronousQueue<T>[] dest;

    @Override
    public ActionBuilder<java.lang.String> to(SynchronousQueue<java.lang.String>... dest) {
      this.dest = dest;
      if (parms.size() == parmsCount)
        return new ActionBuilder<java.lang.String>() {
          @Override
          public AbstractTerminalBuilder<java.lang.String> fire() {
            return new AbstractTerminalBuilder<java.lang.String>() {
              public CouchTx tx() {
                try {
                  return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.getRevision.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }

              public Future<java.lang.String> future() {
                try {

                  BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<java.lang.String>() {
                    @Override
                    public java.lang.String call() throws Exception {
                      return (java.lang.String) rxf.server.CouchMetaDriver.getRevision.visit();
                    }
                  });
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }
            };
          }
        };
      throw new IllegalArgumentException("required parameters are: [db, docId]");
    }

    static private final int parmsCount = 2;

    public getRevisionBuilder db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public getRevisionBuilder docId(java.lang.String string) {
      parms.put(DbKeys.etype.docId, string);
      return this;
    }

  }

  rxf.server.CouchTx updateDoc(java.lang.String db, java.lang.String docId, java.lang.String rev, java.lang.String validjson);


  public class updateDocBuilder<T> extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
    private SynchronousQueue<T>[] dest;

    @Override
    public ActionBuilder<rxf.server.CouchTx> to(SynchronousQueue<rxf.server.CouchTx>... dest) {
      this.dest = dest;
      if (parms.size() == parmsCount)
        return new ActionBuilder<rxf.server.CouchTx>() {
          @Override
          public AbstractTerminalBuilder<rxf.server.CouchTx> fire() {
            return new AbstractTerminalBuilder<rxf.server.CouchTx>() {
              public CouchTx tx() {
                try {
                  return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.updateDoc.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }

              void oneWay() {
                BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                  @Override
                  public void run() {
                    try {
                      rxf.server.CouchMetaDriver.updateDoc.visit();
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                  }
                });
              }

              public Future<rxf.server.CouchTx> future() {
                try {

                  BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.CouchTx>() {
                    @Override
                    public rxf.server.CouchTx call() throws Exception {
                      return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.updateDoc.visit();
                    }
                  });
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }
            };
          }
        };
      throw new IllegalArgumentException("required parameters are: [db, docId, rev, validjson]");
    }

    static private final int parmsCount = 4;

    public updateDocBuilder db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public updateDocBuilder docId(java.lang.String string) {
      parms.put(DbKeys.etype.docId, string);
      return this;
    }

    public updateDocBuilder rev(java.lang.String string) {
      parms.put(DbKeys.etype.rev, string);
      return this;
    }

    public updateDocBuilder validjson(java.lang.String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  rxf.server.CouchTx createNewDesignDoc(java.lang.String db, java.lang.String designDocId, java.lang.String validjson);


  public class createNewDesignDocBuilder<T> extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
    private SynchronousQueue<T>[] dest;

    @Override
    public ActionBuilder<rxf.server.CouchTx> to(SynchronousQueue<rxf.server.CouchTx>... dest) {
      this.dest = dest;
      if (parms.size() == parmsCount)
        return new ActionBuilder<rxf.server.CouchTx>() {
          @Override
          public AbstractTerminalBuilder<rxf.server.CouchTx> fire() {
            return new AbstractTerminalBuilder<rxf.server.CouchTx>() {
              public CouchTx tx() {
                try {
                  return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.createNewDesignDoc.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }

              void oneWay() {
                BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                  @Override
                  public void run() {
                    try {
                      rxf.server.CouchMetaDriver.createNewDesignDoc.visit();
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                  }
                });
              }
            };
          }
        };
      throw new IllegalArgumentException("required parameters are: [db, designDocId, validjson]");
    }

    static private final int parmsCount = 3;

    public createNewDesignDocBuilder db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public createNewDesignDocBuilder designDocId(java.lang.String string) {
      parms.put(DbKeys.etype.designDocId, string);
      return this;
    }

    public createNewDesignDocBuilder validjson(java.lang.String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  java.lang.String getDesignDoc(java.lang.String db, java.lang.String designDocId);


  public class getDesignDocBuilder<T> extends DbKeysBuilder<java.lang.String> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
    private SynchronousQueue<T>[] dest;

    @Override
    public ActionBuilder<java.lang.String> to(SynchronousQueue<java.lang.String>... dest) {
      this.dest = dest;
      if (parms.size() == parmsCount)
        return new ActionBuilder<java.lang.String>() {
          @Override
          public AbstractTerminalBuilder<java.lang.String> fire() {
            return new AbstractTerminalBuilder<java.lang.String>() {
              public CouchTx tx() {
                try {
                  return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.getDesignDoc.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }
            };
          }
        };
      throw new IllegalArgumentException("required parameters are: [db, designDocId]");
    }

    static private final int parmsCount = 2;

    public getDesignDocBuilder db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public getDesignDocBuilder designDocId(java.lang.String string) {
      parms.put(DbKeys.etype.designDocId, string);
      return this;
    }

  }

  rxf.server.CouchTx updateDesignDoc(java.lang.String db, java.lang.String designDocId, java.lang.String validjson);


  public class updateDesignDocBuilder<T> extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
    private SynchronousQueue<T>[] dest;

    @Override
    public ActionBuilder<rxf.server.CouchTx> to(SynchronousQueue<rxf.server.CouchTx>... dest) {
      this.dest = dest;
      if (parms.size() == parmsCount)
        return new ActionBuilder<rxf.server.CouchTx>() {
          @Override
          public AbstractTerminalBuilder<rxf.server.CouchTx> fire() {
            return new AbstractTerminalBuilder<rxf.server.CouchTx>() {
              public CouchTx tx() {
                try {
                  return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.updateDesignDoc.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }

              void oneWay() {
                BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                  @Override
                  public void run() {
                    try {
                      rxf.server.CouchMetaDriver.updateDesignDoc.visit();
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                  }
                });
              }
            };
          }
        };
      throw new IllegalArgumentException("required parameters are: [db, designDocId, validjson]");
    }

    static private final int parmsCount = 3;

    public updateDesignDocBuilder db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public updateDesignDocBuilder designDocId(java.lang.String string) {
      parms.put(DbKeys.etype.designDocId, string);
      return this;
    }

    public updateDesignDocBuilder validjson(java.lang.String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  rxf.server.CouchResultSet getView(java.lang.String db, java.lang.String view);


  public class getViewBuilder<T> extends DbKeysBuilder<rxf.server.CouchResultSet> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
    private SynchronousQueue<T>[] dest;

    @Override
    public ActionBuilder<rxf.server.CouchResultSet> to(SynchronousQueue<rxf.server.CouchResultSet>... dest) {
      this.dest = dest;
      if (parms.size() == parmsCount)
        return new ActionBuilder<rxf.server.CouchResultSet>() {
          @Override
          public AbstractTerminalBuilder<rxf.server.CouchResultSet> fire() {
            return new AbstractTerminalBuilder<rxf.server.CouchResultSet>() {
              public CouchResultSet<rxf.server.CouchResultSet> rows() {
                try {
                  return (CouchResultSet<rxf.server.CouchResultSet>) rxf.server.CouchMetaDriver.getView.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }

              public Future<rxf.server.CouchResultSet> future() {
                try {

                  BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.CouchResultSet>() {
                    @Override
                    public rxf.server.CouchResultSet call() throws Exception {
                      return (rxf.server.CouchResultSet) rxf.server.CouchMetaDriver.getView.visit();
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

              ;
            };
          }
        };
      throw new IllegalArgumentException("required parameters are: [db, view]");
    }

    static private final int parmsCount = 2;

    public getViewBuilder db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public getViewBuilder view(java.lang.String string) {
      parms.put(DbKeys.etype.view, string);
      return this;
    }

  }

  rxf.server.CouchTx sendJson(java.lang.String opaque, java.lang.String validjson);


  public class sendJsonBuilder<T> extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
    private SynchronousQueue<T>[] dest;

    @Override
    public ActionBuilder<rxf.server.CouchTx> to(SynchronousQueue<rxf.server.CouchTx>... dest) {
      this.dest = dest;
      if (parms.size() == parmsCount)
        return new ActionBuilder<rxf.server.CouchTx>() {
          @Override
          public AbstractTerminalBuilder<rxf.server.CouchTx> fire() {
            return new AbstractTerminalBuilder<rxf.server.CouchTx>() {
              public CouchTx tx() {
                try {
                  return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.sendJson.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }

              void oneWay() {
                BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                  @Override
                  public void run() {
                    try {
                      rxf.server.CouchMetaDriver.sendJson.visit();
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                  }
                });
              }

              public CouchResultSet<rxf.server.CouchTx> rows() {
                try {
                  return (CouchResultSet<rxf.server.CouchTx>) rxf.server.CouchMetaDriver.sendJson.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }

              public Future<rxf.server.CouchTx> future() {
                try {

                  BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.CouchTx>() {
                    @Override
                    public rxf.server.CouchTx call() throws Exception {
                      return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.sendJson.visit();
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

              ;
            };
          }
        };
      throw new IllegalArgumentException("required parameters are: [opaque, validjson]");
    }

    static private final int parmsCount = 2;

    public sendJsonBuilder opaque(java.lang.String string) {
      parms.put(DbKeys.etype.opaque, string);
      return this;
    }

    public sendJsonBuilder validjson(java.lang.String string) {
      parms.put(DbKeys.etype.validjson, string);
      return this;
    }

  }

  rxf.server.Rfc822HeaderState sendBlob(java.lang.String opaque, one.xio.MimeType mimetype, java.nio.ByteBuffer blob);


  public class sendBlobBuilder<T> extends DbKeysBuilder<rxf.server.Rfc822HeaderState> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);
    private SynchronousQueue<T>[] dest;

    @Override
    public ActionBuilder<rxf.server.Rfc822HeaderState> to(SynchronousQueue<rxf.server.Rfc822HeaderState>... dest) {
      this.dest = dest;
      if (parms.size() == parmsCount)
        return new ActionBuilder<rxf.server.Rfc822HeaderState>() {
          @Override
          public AbstractTerminalBuilder<rxf.server.Rfc822HeaderState> fire() {
            return new AbstractTerminalBuilder<rxf.server.Rfc822HeaderState>() {
              public CouchTx tx() {
                try {
                  return (rxf.server.CouchTx) rxf.server.CouchMetaDriver.sendBlob.visit();
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }

              public Future<rxf.server.Rfc822HeaderState> future() {
                try {

                  BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<rxf.server.Rfc822HeaderState>() {
                    @Override
                    public rxf.server.Rfc822HeaderState call() throws Exception {
                      return (rxf.server.Rfc822HeaderState) rxf.server.CouchMetaDriver.sendBlob.visit();
                    }
                  });
                } catch (Exception e) {
                  e.printStackTrace();
                }
                return null;
              }

              void oneWay() {
                BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
                  @Override
                  public void run() {
                    try {
                      rxf.server.CouchMetaDriver.sendBlob.visit();
                    } catch (Exception e) {
                      e.printStackTrace();
                    }
                  }
                });
              }
            };
          }
        };
      throw new IllegalArgumentException("required parameters are: [opaque, mimetype, blob]");
    }

    static private final int parmsCount = 3;

    public sendBlobBuilder opaque(java.lang.String string) {
      parms.put(DbKeys.etype.opaque, string);
      return this;
    }

    public sendBlobBuilder mimetype(one.xio.MimeType mimetype) {
      parms.put(DbKeys.etype.mimetype, mimetype);
      return this;
    }

    public sendBlobBuilder blob(java.nio.ByteBuffer bytebuffer) {
      parms.put(DbKeys.etype.blob, bytebuffer);
      return this;
    }

  }
}
