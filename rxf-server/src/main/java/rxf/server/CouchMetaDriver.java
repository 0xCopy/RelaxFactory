package rxf.server;

import java.lang.reflect.Field;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
  @DbKeys({db, docId})
  getDoc,
  @DbKeys({db, docId, rev})
  updateDoc,
  @DbKeys({db, designDocId})
  createNewDesignDoc,
  @DbResultUnit(String.class) @DbKeys({db, designDocId})
  getDesignDoc,
  @DbKeys({db, designDocId})
  updateDesignDoc,
  @DbKeys({db, designDocId})
  defineQuery,
  @DbResultUnit(List.class) @DbKeys({db, designDocId})
  getView,
  @DbKeys({opaque}) @DbResultUnit(Iterator.class)
  getAsyncIterator,

  @DbResultUnit(Rfc822HeaderState.class)
  @DbKeys({opaque, mimetype, blob})
  sendBlob;


  public
  static void main(String... args) {


    Field[] fields = CouchMetaDriver.class.getFields();
    String s = "" + "public interface CouchDriver{";
    for (Field field : fields) {
      if (field.getType().isAssignableFrom(CouchMetaDriver.class)) {
        CouchMetaDriver couchDriver = CouchMetaDriver.valueOf(field.getName());
        DbKeys dbKeys = field.getAnnotation(DbKeys.class);
        etype[] value = dbKeys.value();
        {

          final DbResultUnit annotation = field.getAnnotation(DbResultUnit.class);
          if (null == annotation)

            s += "CouchTx ";
          else s += annotation.value().getCanonicalName() + ' ';
          s += couchDriver.name() + '(';
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
//  verb	format	use case
//PUT	/{db}/	create db, useful to avoid manual setup for new objects, tenants	db' is database	first use of each entity
//
//POST	/{db}/	create doc, generates new id		entity first persist
//GET	/{db}/{id}?rev={rev}	retreive doc, rev might be optional? do we need another way to ask 'is this the latest'?	id' is key, 'rev' is revision	locator.find
//PUT	/{db}/{id}?rev={rev}	update doc based on last known rev - this will give us an error if we want to make sure we aren't clobbering other changes. Do we want a way to allow clobbering?		entity persist
//
//PUT	/{db}/{dd}	Create a new design doc	dd (design doc) is like id, except not generated	first use of each finder class
//GET	/{db}/{dd}	Retrieve current design doc (to check rev, or check that is up to date)		first use of each finder class
//GET	/{db}/{dd}/{view}/...	Run a view within the design doc, read results	 '...' could be any number of things	running a finder method
//PUT	/{db}/{dd}?rev={rev}	update an existing design doc (as above)		first use of each finder class


  @Override
  public void onRead(SelectionKey key) throws Exception {
    //todo: verify for a purpose
  }

  @Override
  public void onConnect(SelectionKey key) throws Exception {
    //todo: verify for a purpose
  }

  @Override
  public void onWrite(SelectionKey key) throws Exception {
    //todo: verify for a purpose
  }

  @Override
  public void onAccept(SelectionKey key) throws Exception {
    //todo: verify for a purpose
  }

}
