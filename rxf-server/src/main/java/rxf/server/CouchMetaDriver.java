package rxf.server;

import java.lang.reflect.Field;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.Iterator;

import one.xio.AsioVisitor;
import rxf.server.DbKeys.DbInputUnit;
import rxf.server.DbKeys.DbResultUnit;
import rxf.server.DbKeys.etype;

import static rxf.server.DbKeys.etype.blob;
import static rxf.server.DbKeys.etype.db;
import static rxf.server.DbKeys.etype.designDocId;
import static rxf.server.DbKeys.etype.docId;
import static rxf.server.DbKeys.etype.mimetype;
import static rxf.server.DbKeys.etype.opaque;
import static rxf.server.DbKeys.etype.rev;
import static rxf.server.DbKeys.etype.view;

/**
 * confers traits on an oo platform...
 * <p/>
 * User: jim
 * Date: 5/24/12
 * Time: 3:09 PM
 */
public enum CouchMetaDriver implements AsioVisitor {

  @DbKeys({db, docId})
  createDb,
  @DbKeys({db, docId}) @DbInputUnit(String.class)
  createDoc,

  @DbResultUnit(String.class) @DbKeys({db, docId})
  getDoc,
  @DbResultUnit(String.class) @DbKeys({db, docId})

  getRevision,
  @DbKeys({db, docId, rev})
  updateDoc,
  @DbKeys({db, designDocId})
  createNewDesignDoc,
  @DbResultUnit(String.class) @DbKeys({db, designDocId})
  getDesignDoc,
  @DbKeys({db, designDocId})
  updateDesignDoc,
  @DbResultUnit(CouchResultSet.class) @DbKeys({db, view})
  getView,
  @DbKeys({opaque})sendJson,
  @DbKeys({opaque}) @DbResultUnit(Iterator.class)
  getAsyncIterator,

  @DbResultUnit(Rfc822HeaderState.class)
  @DbKeys({opaque, mimetype, blob})
  sendBlob;


  public
  static void main(String... args) {


    Field[] fields = CouchMetaDriver.class.getFields();
    String s = "" + "package rxf.server;public interface CouchDriver{";
    for (Field field : fields) {
      if (field.getType().isAssignableFrom(CouchMetaDriver.class)) {
        CouchMetaDriver couchDriver = CouchMetaDriver.valueOf(field.getName());
        DbKeys dbKeys = field.getAnnotation(DbKeys.class);
        etype[] value = dbKeys.value();
        {

          final DbResultUnit annotation = field.getAnnotation(DbResultUnit.class);
          s += null != annotation ? annotation.value().getCanonicalName() : CouchTx.class.getCanonicalName();
          s += ' ' + couchDriver.name() + '(';
          Iterator<etype> iterator = Arrays.asList(value).iterator();
          while (iterator.hasNext()) {
            etype etype = iterator.next();
            s += " " +
                etype.clazz.getCanonicalName() + " " + etype.name();
            if (iterator.hasNext())
              s += ',';
          }
          s += " );\n";
        }
      }
    }
    s += "}";
    System.out.println(s);
  }

  AsioVisitor delegate = new Impl();

  @Override
  public void onRead(SelectionKey key) throws Exception {
    delegate.onRead(key);
  }

  @Override
  public void onConnect(SelectionKey key) throws Exception {
    delegate.onConnect(key);
  }

  @Override
  public void onWrite(SelectionKey key) throws Exception {
    delegate.onWrite(key);
  }

  @Override
  public void onAccept(SelectionKey key) throws Exception {
    delegate.onAccept(key);
  }
}
