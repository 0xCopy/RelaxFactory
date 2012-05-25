package rxf.server;

import static rxf.server.DbKeys.ActionBuilder;
import static rxf.server.DbKeys.DbKeysBuilder;
import static rxf.server.DbKeys.etype;

public interface CouchDriver {
  rxf.server.CouchTx createDb(java.lang.String db, java.lang.String docId);


  public class createDbBuilder extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public createDbBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    @Override
    public ActionBuilder<rxf.server.CouchTx> to() {
      return new ActionBuilder<rxf.server.CouchTx>() {
        @Override
        public ResultAction<rxf.server.CouchTx> fire() {
          return new ResultAction<rxf.server.CouchTx>();
        }
      };
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

  rxf.server.CouchTx createDoc(java.lang.String db, java.lang.String docId);


  public class createDocBuilder extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public createDocBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    @Override
    public ActionBuilder<rxf.server.CouchTx> to() {
      return new ActionBuilder<rxf.server.CouchTx>() {
        @Override
        public ResultAction<rxf.server.CouchTx> fire() {
          return new ResultAction<rxf.server.CouchTx>();
        }
      };
    }

    static private final int parmsCount = 2;

    public createDocBuilder db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public createDocBuilder docId(java.lang.String string) {
      parms.put(DbKeys.etype.docId, string);
      return this;
    }

  }

  java.lang.String getDoc(java.lang.String db, java.lang.String docId);


  public class getDocBuilder extends DbKeysBuilder<java.lang.String> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public getDocBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    @Override
    public ActionBuilder<java.lang.String> to() {
      return new ActionBuilder<java.lang.String>() {
        @Override
        public ResultAction<java.lang.String> fire() {
          return new ResultAction<java.lang.String>();
        }
      };
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


  public class getRevisionBuilder extends DbKeysBuilder<java.lang.String> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public getRevisionBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    @Override
    public ActionBuilder<java.lang.String> to() {
      return new ActionBuilder<java.lang.String>() {
        @Override
        public ResultAction<java.lang.String> fire() {
          return new ResultAction<java.lang.String>();
        }
      };
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

  rxf.server.CouchTx updateDoc(java.lang.String db, java.lang.String docId, java.lang.String rev);


  public class updateDocBuilder extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public updateDocBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    @Override
    public ActionBuilder<rxf.server.CouchTx> to() {
      return new ActionBuilder<rxf.server.CouchTx>() {
        @Override
        public ResultAction<rxf.server.CouchTx> fire() {
          return new ResultAction<rxf.server.CouchTx>();
        }
      };
    }

    static private final int parmsCount = 3;

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

  }

  rxf.server.CouchTx createNewDesignDoc(java.lang.String db, java.lang.String designDocId);


  public class createNewDesignDocBuilder extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public createNewDesignDocBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    @Override
    public ActionBuilder<rxf.server.CouchTx> to() {
      return new ActionBuilder<rxf.server.CouchTx>() {
        @Override
        public ResultAction<rxf.server.CouchTx> fire() {
          return new ResultAction<rxf.server.CouchTx>();
        }
      };
    }

    static private final int parmsCount = 2;

    public createNewDesignDocBuilder db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public createNewDesignDocBuilder designDocId(java.lang.String string) {
      parms.put(DbKeys.etype.designDocId, string);
      return this;
    }

  }

  java.lang.String getDesignDoc(java.lang.String db, java.lang.String designDocId);


  public class getDesignDocBuilder extends DbKeysBuilder<java.lang.String> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public getDesignDocBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    @Override
    public ActionBuilder<java.lang.String> to() {
      return new ActionBuilder<java.lang.String>() {
        @Override
        public ResultAction<java.lang.String> fire() {
          return new ResultAction<java.lang.String>();
        }
      };
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

  rxf.server.CouchTx updateDesignDoc(java.lang.String db, java.lang.String designDocId);


  public class updateDesignDocBuilder extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public updateDesignDocBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    @Override
    public ActionBuilder<rxf.server.CouchTx> to() {
      return new ActionBuilder<rxf.server.CouchTx>() {
        @Override
        public ResultAction<rxf.server.CouchTx> fire() {
          return new ResultAction<rxf.server.CouchTx>();
        }
      };
    }

    static private final int parmsCount = 2;

    public updateDesignDocBuilder db(java.lang.String string) {
      parms.put(DbKeys.etype.db, string);
      return this;
    }

    public updateDesignDocBuilder designDocId(java.lang.String string) {
      parms.put(DbKeys.etype.designDocId, string);
      return this;
    }

  }

  rxf.server.CouchResultSet getView(java.lang.String db, java.lang.String view);


  public class getViewBuilder extends DbKeysBuilder<rxf.server.CouchResultSet> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public getViewBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    @Override
    public ActionBuilder<rxf.server.CouchResultSet> to() {
      return new ActionBuilder<rxf.server.CouchResultSet>() {
        @Override
        public ResultAction<rxf.server.CouchResultSet> fire() {
          return new ResultAction<rxf.server.CouchResultSet>();
        }
      };
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

  rxf.server.CouchTx sendJson(java.lang.String opaque);


  public class sendJsonBuilder extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public sendJsonBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    @Override
    public ActionBuilder<rxf.server.CouchTx> to() {
      return new ActionBuilder<rxf.server.CouchTx>() {
        @Override
        public ResultAction<rxf.server.CouchTx> fire() {
          return new ResultAction<rxf.server.CouchTx>();
        }
      };
    }

    static private final int parmsCount = 1;

    public sendJsonBuilder opaque(java.lang.String string) {
      parms.put(DbKeys.etype.opaque, string);
      return this;
    }

  }

  java.util.Iterator getAsyncIterator(java.lang.String opaque);


  public class getAsyncIteratorBuilder extends DbKeysBuilder<java.util.Iterator> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public getAsyncIteratorBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    @Override
    public ActionBuilder<java.util.Iterator> to() {
      return new ActionBuilder<java.util.Iterator>() {
        @Override
        public ResultAction<java.util.Iterator> fire() {
          return new ResultAction<java.util.Iterator>();
        }
      };
    }

    static private final int parmsCount = 1;

    public getAsyncIteratorBuilder opaque(java.lang.String string) {
      parms.put(DbKeys.etype.opaque, string);
      return this;
    }

  }

  rxf.server.Rfc822HeaderState sendBlob(java.lang.String opaque, one.xio.MimeType mimetype, java.nio.ByteBuffer blob);


  public class sendBlobBuilder extends DbKeysBuilder<rxf.server.Rfc822HeaderState> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public sendBlobBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    @Override
    public ActionBuilder<rxf.server.Rfc822HeaderState> to() {
      return new ActionBuilder<rxf.server.Rfc822HeaderState>() {
        @Override
        public ResultAction<rxf.server.Rfc822HeaderState> fire() {
          return new ResultAction<rxf.server.Rfc822HeaderState>();
        }
      };
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