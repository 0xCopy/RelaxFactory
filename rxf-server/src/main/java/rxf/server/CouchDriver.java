package rxf.server;import static rxf.server.DbKeys.*;import static rxf.server.DbKeys.etype.*;import java.util.*; public interface CouchDriver{rxf.server.CouchTx createDb( java.lang.String db, java.lang.String docId );

public class createDbBuilder extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    @Override
    public ActionBuilder<rxf.server.CouchTx> to() {
        if (parms.size() == parmsCount)
            return new ActionBuilder<rxf.server.CouchTx>() {
                @Override
                public TerminalBuilder<rxf.server.CouchTx> fire() {
                    return new TerminalBuilder<rxf.server.CouchTx>();
                }
            };
        throw new IllegalArgumentException("required paramters are: [db, docId]");
    }
    
static private final int parmsCount=2;
public createDbBuilder db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public createDbBuilder docId(java.lang.String string){parms.put(DbKeys.etype.docId,string);return this;}

}
rxf.server.CouchTx createDoc( java.lang.String db, java.lang.String docId, java.lang.String validjson );

public class createDocBuilder extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    @Override
    public ActionBuilder<rxf.server.CouchTx> to() {
        if (parms.size() == parmsCount)
            return new ActionBuilder<rxf.server.CouchTx>() {
                @Override
                public TerminalBuilder<rxf.server.CouchTx> fire() {
                    return new TerminalBuilder<rxf.server.CouchTx>();
                }
            };
        throw new IllegalArgumentException("required paramters are: [db, docId, validjson]");
    }
    
static private final int parmsCount=3;
public createDocBuilder db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public createDocBuilder docId(java.lang.String string){parms.put(DbKeys.etype.docId,string);return this;}
public createDocBuilder validjson(java.lang.String string){parms.put(DbKeys.etype.validjson,string);return this;}

}
java.lang.String getDoc( java.lang.String db, java.lang.String docId );

public class getDocBuilder extends DbKeysBuilder<java.lang.String> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    @Override
    public ActionBuilder<java.lang.String> to() {
        if (parms.size() == parmsCount)
            return new ActionBuilder<java.lang.String>() {
                @Override
                public TerminalBuilder<java.lang.String> fire() {
                    return new TerminalBuilder<java.lang.String>();
                }
            };
        throw new IllegalArgumentException("required paramters are: [db, docId]");
    }
    
static private final int parmsCount=2;
public getDocBuilder db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public getDocBuilder docId(java.lang.String string){parms.put(DbKeys.etype.docId,string);return this;}

}
java.lang.String getRevision( java.lang.String db, java.lang.String docId );

public class getRevisionBuilder extends DbKeysBuilder<java.lang.String> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    @Override
    public ActionBuilder<java.lang.String> to() {
        if (parms.size() == parmsCount)
            return new ActionBuilder<java.lang.String>() {
                @Override
                public TerminalBuilder<java.lang.String> fire() {
                    return new TerminalBuilder<java.lang.String>();
                }
            };
        throw new IllegalArgumentException("required paramters are: [db, docId]");
    }
    
static private final int parmsCount=2;
public getRevisionBuilder db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public getRevisionBuilder docId(java.lang.String string){parms.put(DbKeys.etype.docId,string);return this;}

}
rxf.server.CouchTx updateDoc( java.lang.String db, java.lang.String docId, java.lang.String rev, java.lang.String validjson );

public class updateDocBuilder extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    @Override
    public ActionBuilder<rxf.server.CouchTx> to() {
        if (parms.size() == parmsCount)
            return new ActionBuilder<rxf.server.CouchTx>() {
                @Override
                public TerminalBuilder<rxf.server.CouchTx> fire() {
                    return new TerminalBuilder<rxf.server.CouchTx>();
                }
            };
        throw new IllegalArgumentException("required paramters are: [db, docId, rev, validjson]");
    }
    
static private final int parmsCount=4;
public updateDocBuilder db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public updateDocBuilder docId(java.lang.String string){parms.put(DbKeys.etype.docId,string);return this;}
public updateDocBuilder rev(java.lang.String string){parms.put(DbKeys.etype.rev,string);return this;}
public updateDocBuilder validjson(java.lang.String string){parms.put(DbKeys.etype.validjson,string);return this;}

}
rxf.server.CouchTx createNewDesignDoc( java.lang.String db, java.lang.String designDocId, java.lang.String validjson );

public class createNewDesignDocBuilder extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    @Override
    public ActionBuilder<rxf.server.CouchTx> to() {
        if (parms.size() == parmsCount)
            return new ActionBuilder<rxf.server.CouchTx>() {
                @Override
                public TerminalBuilder<rxf.server.CouchTx> fire() {
                    return new TerminalBuilder<rxf.server.CouchTx>();
                }
            };
        throw new IllegalArgumentException("required paramters are: [db, designDocId, validjson]");
    }
    
static private final int parmsCount=3;
public createNewDesignDocBuilder db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public createNewDesignDocBuilder designDocId(java.lang.String string){parms.put(DbKeys.etype.designDocId,string);return this;}
public createNewDesignDocBuilder validjson(java.lang.String string){parms.put(DbKeys.etype.validjson,string);return this;}

}
java.lang.String getDesignDoc( java.lang.String db, java.lang.String designDocId );

public class getDesignDocBuilder extends DbKeysBuilder<java.lang.String> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    @Override
    public ActionBuilder<java.lang.String> to() {
        if (parms.size() == parmsCount)
            return new ActionBuilder<java.lang.String>() {
                @Override
                public TerminalBuilder<java.lang.String> fire() {
                    return new TerminalBuilder<java.lang.String>();
                }
            };
        throw new IllegalArgumentException("required paramters are: [db, designDocId]");
    }
    
static private final int parmsCount=2;
public getDesignDocBuilder db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public getDesignDocBuilder designDocId(java.lang.String string){parms.put(DbKeys.etype.designDocId,string);return this;}

}
rxf.server.CouchTx updateDesignDoc( java.lang.String db, java.lang.String designDocId, java.lang.String validjson );

public class updateDesignDocBuilder extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    @Override
    public ActionBuilder<rxf.server.CouchTx> to() {
        if (parms.size() == parmsCount)
            return new ActionBuilder<rxf.server.CouchTx>() {
                @Override
                public TerminalBuilder<rxf.server.CouchTx> fire() {
                    return new TerminalBuilder<rxf.server.CouchTx>();
                }
            };
        throw new IllegalArgumentException("required paramters are: [db, designDocId, validjson]");
    }
    
static private final int parmsCount=3;
public updateDesignDocBuilder db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public updateDesignDocBuilder designDocId(java.lang.String string){parms.put(DbKeys.etype.designDocId,string);return this;}
public updateDesignDocBuilder validjson(java.lang.String string){parms.put(DbKeys.etype.validjson,string);return this;}

}
rxf.server.CouchResultSet getView( java.lang.String db, java.lang.String view );

public class getViewBuilder extends DbKeysBuilder<rxf.server.CouchResultSet> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    @Override
    public ActionBuilder<rxf.server.CouchResultSet> to() {
        if (parms.size() == parmsCount)
            return new ActionBuilder<rxf.server.CouchResultSet>() {
                @Override
                public TerminalBuilder<rxf.server.CouchResultSet> fire() {
                    return new TerminalBuilder<rxf.server.CouchResultSet>();
                }
            };
        throw new IllegalArgumentException("required paramters are: [db, view]");
    }
    
static private final int parmsCount=2;
public getViewBuilder db(java.lang.String string){parms.put(DbKeys.etype.db,string);return this;}
public getViewBuilder view(java.lang.String string){parms.put(DbKeys.etype.view,string);return this;}

}
rxf.server.CouchTx sendJson( java.lang.String opaque, java.lang.String validjson );

public class sendJsonBuilder extends DbKeysBuilder<rxf.server.CouchTx> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    @Override
    public ActionBuilder<rxf.server.CouchTx> to() {
        if (parms.size() == parmsCount)
            return new ActionBuilder<rxf.server.CouchTx>() {
                @Override
                public TerminalBuilder<rxf.server.CouchTx> fire() {
                    return new TerminalBuilder<rxf.server.CouchTx>();
                }
            };
        throw new IllegalArgumentException("required paramters are: [opaque, validjson]");
    }
    
static private final int parmsCount=2;
public sendJsonBuilder opaque(java.lang.String string){parms.put(DbKeys.etype.opaque,string);return this;}
public sendJsonBuilder validjson(java.lang.String string){parms.put(DbKeys.etype.validjson,string);return this;}

}
java.util.Iterator getAsyncIterator( java.lang.String opaque );

public class getAsyncIteratorBuilder extends DbKeysBuilder<java.util.Iterator> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    @Override
    public ActionBuilder<java.util.Iterator> to() {
        if (parms.size() == parmsCount)
            return new ActionBuilder<java.util.Iterator>() {
                @Override
                public TerminalBuilder<java.util.Iterator> fire() {
                    return new TerminalBuilder<java.util.Iterator>();
                }
            };
        throw new IllegalArgumentException("required paramters are: [opaque]");
    }
    
static private final int parmsCount=1;
public getAsyncIteratorBuilder opaque(java.lang.String string){parms.put(DbKeys.etype.opaque,string);return this;}

}
rxf.server.Rfc822HeaderState sendBlob( java.lang.String opaque, one.xio.MimeType mimetype, java.nio.ByteBuffer blob );

public class sendBlobBuilder extends DbKeysBuilder<rxf.server.Rfc822HeaderState> {
    Rfc822HeaderState rfc822HeaderState;
    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);

    @Override
    public ActionBuilder<rxf.server.Rfc822HeaderState> to() {
        if (parms.size() == parmsCount)
            return new ActionBuilder<rxf.server.Rfc822HeaderState>() {
                @Override
                public TerminalBuilder<rxf.server.Rfc822HeaderState> fire() {
                    return new TerminalBuilder<rxf.server.Rfc822HeaderState>();
                }
            };
        throw new IllegalArgumentException("required paramters are: [opaque, mimetype, blob]");
    }
    
static private final int parmsCount=3;
public sendBlobBuilder opaque(java.lang.String string){parms.put(DbKeys.etype.opaque,string);return this;}
public sendBlobBuilder mimetype(one.xio.MimeType mimetype){parms.put(DbKeys.etype.mimetype,mimetype);return this;}
public sendBlobBuilder blob(java.nio.ByteBuffer bytebuffer){parms.put(DbKeys.etype.blob,bytebuffer);return this;}

}
}