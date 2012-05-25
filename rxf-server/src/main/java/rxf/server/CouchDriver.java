package rxf.server;

import static rxf.server.DbKeys.etype;

public interface CouchDriver {
  rxf.server.CouchTx createDb(java.lang.String db, java.lang.String docId);


  public class createDbBuilder extends Rfc822HeaderState {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public createDbBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

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


  public class createDocBuilder extends Rfc822HeaderState {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public createDocBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

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


  public class getDocBuilder extends Rfc822HeaderState {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public getDocBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

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


  public class getRevisionBuilder extends Rfc822HeaderState {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public getRevisionBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

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


  public class updateDocBuilder extends Rfc822HeaderState {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public updateDocBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

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


  public class createNewDesignDocBuilder extends Rfc822HeaderState {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public createNewDesignDocBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

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


  public class getDesignDocBuilder extends Rfc822HeaderState {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public getDesignDocBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

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


  public class updateDesignDocBuilder extends Rfc822HeaderState {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public updateDesignDocBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

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


  public class getViewBuilder extends Rfc822HeaderState {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public getViewBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

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


  public class sendJsonBuilder extends Rfc822HeaderState {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public sendJsonBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    public sendJsonBuilder opaque(java.lang.String string) {
      parms.put(DbKeys.etype.opaque, string);
      return this;
    }

  }

  java.util.Iterator getAsyncIterator(java.lang.String opaque);


  public class getAsyncIteratorBuilder extends Rfc822HeaderState {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public getAsyncIteratorBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

    public getAsyncIteratorBuilder opaque(java.lang.String string) {
      parms.put(DbKeys.etype.opaque, string);
      return this;
    }

  }

  rxf.server.Rfc822HeaderState sendBlob(java.lang.String opaque, one.xio.MimeType mimetype, java.nio.ByteBuffer blob);


  public class sendBlobBuilder extends Rfc822HeaderState {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    public sendBlobBuilder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {
      for (Rfc822HeaderState rfc822HeaderState : opt) {
        this.rfc822HeaderState = rfc822HeaderState;
        break;
      }
    }

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
