package rxf.server;

import java.nio.ByteBuffer;
import java.util.Iterator;

import one.xio.MimeType;

public interface CouchDriver {
  CouchTx createDb(String db, String docId);

  CouchTx createDoc(String db, String docId);

  String getDoc(String db, String docId);

  String getRevision(String db, String docId);

  CouchTx updateDoc(String db, String docId, String rev);

  CouchTx createNewDesignDoc(String db, String designDocId);

  String getDesignDoc(String db, String designDocId);

  CouchTx updateDesignDoc(String db, String designDocId);

  CouchResultSet getView(String db, String view);

  CouchTx sendJson(String opaque);

  Iterator getAsyncIterator(String opaque);

  Rfc822HeaderState sendBlob(String opaque, MimeType mimetype, ByteBuffer blob);
}
