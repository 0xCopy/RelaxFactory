package rxf.server;

import java.lang.reflect.Field;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.Iterator;

import one.xio.AsioVisitor;
import org.intellij.lang.annotations.Language;
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
  sendBlob {
    @Override
    public void onWrite(SelectionKey key) throws Exception {
      super.onWrite(key);    //To change body of overridden methods use File | Settings | File Templates.
    }
  };
  public static final String XXXXXXXXXXXXXXMETHODS = "/*XXXXXXXXXXXXXXMETHODS*/";

  //  public static final String XDEADBEEF_2 = "-0xdeadbeef.2";
  public <T> String builder() throws NoSuchFieldException {
    final Field field = CouchMetaDriver.class.getField(name());
    @Language("JAVA")
    String s = "\n\npublic class _ename_Builder extends Rfc822HeaderState {\n  Rfc822HeaderState rfc822HeaderState;\n  java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);\n\n  public _ename_Builder(java.nio.channels.SelectionKey key, Rfc822HeaderState... opt) {\n    for (Rfc822HeaderState rfc822HeaderState : opt) {\n      this.rfc822HeaderState = rfc822HeaderState;\n      break;\n    }                    }\n    " + XXXXXXXXXXXXXXMETHODS + " \n}\n";
    String s1 = "";
    if (field.getType().isAssignableFrom(CouchMetaDriver.class)) {
      CouchMetaDriver couchDriver = CouchMetaDriver.valueOf(field.getName());
      DbKeys annotation = field.getAnnotation(DbKeys.class);
      etype[] value = annotation.value();


      final int vl = value.length;
      for (int i = 0; i < vl; i++) {
        etype etype = value[i];
        final String name = etype.name();
        final Class clazz = etype.clazz;
        @Language("JAVA")
        String y = "public _ename_Builder _name_(_clazz_ _sclazz_){ parms.put(DbKeys.etype." + etype.name() + ",_sclazz_); return this;}\n";
        s1 += y.replace("_name_", etype.name()).replace("_clazz_", clazz.getCanonicalName()).replace("_sclazz_", clazz.getSimpleName().toLowerCase()).replace("_ename_", name());
      }

      s = s./*replace(XDEADBEEF_2, String.valueOf(vl)).*/replace(XXXXXXXXXXXXXXMETHODS, s1).replace("_ename_", name());
    }
    return s;
  }

  public
  static void main(String... args) throws NoSuchFieldException {


    Field[] fields = CouchMetaDriver.class.getFields();
    String s = "package rxf.server;import static rxf.server.DbKeys.*;import static rxf.server.DbKeys.etype.*;import java.util.*;import java.util.*;public interface CouchDriver{";
    for (Field field : fields) {
      if (field.getType().isAssignableFrom(CouchMetaDriver.class)) {
        CouchMetaDriver couchDriver = CouchMetaDriver.valueOf(field.getName());
        DbKeys dbKeys = field.getAnnotation(DbKeys.class);
        etype[] value = dbKeys.value();
        {

          DbResultUnit annotation = field.getAnnotation(DbResultUnit.class);
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
          final String builder = couchDriver.builder();
          s += "\n" + builder;
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
