package rxf.server;

import java.lang.reflect.Field;
import java.nio.channels.SelectionKey;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.SynchronousQueue;

import one.xio.AsioVisitor;
import org.intellij.lang.annotations.Language;
import rxf.server.DbKeys.DbResultUnit;
import rxf.server.DbKeys.etype;

import static rxf.server.DbKeys.etype.blob;
import static rxf.server.DbKeys.etype.db;
import static rxf.server.DbKeys.etype.designDocId;
import static rxf.server.DbKeys.etype.docId;
import static rxf.server.DbKeys.etype.mimetype;
import static rxf.server.DbKeys.etype.opaque;
import static rxf.server.DbKeys.etype.rev;
import static rxf.server.DbKeys.etype.validjson;
import static rxf.server.DbKeys.etype.view;

/**
 * confers traits on an oo platform...
 * <p/>
 * User: jim
 * Date: 5/24/12
 * Time: 3:09 PM
 */
public enum CouchMetaDriver {

  @DbKeys({db, docId})
  createDb {},
  @DbKeys({db, docId, validjson})
//    @DbInputUnit(String.class)
      createDoc,

  @DbResultUnit(String.class) @DbKeys({db, docId})
  getDoc,


  @DbResultUnit(String.class) @DbKeys({db, docId})

  getRevision,
  @DbKeys({db, docId, rev, validjson})
  updateDoc,
  @DbKeys({db, designDocId, validjson})
  createNewDesignDoc,
  @DbResultUnit(String.class) @DbKeys({db, designDocId})
  getDesignDoc,
  @DbKeys({db, designDocId, validjson})
  updateDesignDoc,
  @DbResultUnit(CouchResultSet.class) @DbKeys({db, view})
  getView,
  @DbKeys({opaque, validjson})sendJson {
    @Override
    <T> Object visit(final DbKeys.DbKeysBuilder<T> dbKeysBuilder, final DbKeys.ActionBuilder<T> actionBuilder, DbKeys.ReturnAction<T> terminalBuilder) throws Exception {
//            SelectionKey key = actionBuilder.key();
//            key.interestOps(SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT).selector().wakeup();
      String opaque = (String) dbKeysBuilder.getParms().get(etype.opaque);
      String validjson = (String) dbKeysBuilder.getParms().get(etype.validjson);
      return BlobAntiPatternObject.sendJson(opaque, validjson);


    }
  },
  @DbKeys({opaque}) @DbResultUnit(Iterator.class)
  getAsyncIterator,

  @DbResultUnit(Rfc822HeaderState.class)
  @DbKeys({opaque, mimetype, blob})
  sendBlob {
  };


  public static final String XXXXXXXXXXXXXXMETHODS = "/*XXXXXXXXXXXXXXMETHODS*/";

  <T> Object visit() throws Exception {
    DbKeys.DbKeysBuilder<T> dbKeysBuilder = (DbKeys.DbKeysBuilder<T>) DbKeys.DbKeysBuilder.currentKeys.get();
    DbKeys.ActionBuilder<T> actionBuilder = (DbKeys.ActionBuilder<T>) DbKeys.ActionBuilder.currentAction.get();
    DbKeys.ReturnAction<T> returnAction = (DbKeys.ReturnAction<T>) DbKeys.ReturnAction.currentResults.get();
    return visit(dbKeysBuilder, actionBuilder, returnAction);
  }

  /*abstract */<T> Object visit(DbKeys.DbKeysBuilder<T> dbKeysBuilder,
                                DbKeys.ActionBuilder<T> actionBuilder,
                                DbKeys.ReturnAction<T> terminalBuilder) throws Exception {
    throw new AbstractMethodError();
  }

  ;

  public static final String XDEADBEEF_2 = "-0xdeadbeef.2";


  public <T> String builder() throws NoSuchFieldException {
    Field field = CouchMetaDriver.class.getField(name());


    String s = null;
    if (field.getType().isAssignableFrom(CouchMetaDriver.class)) {
      CouchMetaDriver couchDriver = CouchMetaDriver.valueOf(field.getName());
      DbKeys fieldAnnotation = field.getAnnotation(DbKeys.class);
      etype[] parms = fieldAnnotation.value();
      Class rtype = CouchTx.class;
      try {
        rtype = field.getAnnotation(DbResultUnit.class).value();
      } catch (Exception e) {
      }
      final String cn = rtype.getCanonicalName();
      @Language("JAVA")
      final String s2 = "import java.util.concurrent.SynchronousQueue;\n\npublic class _ename_Builder extends DbKeysBuilder<" + cn + "> {\n    Rfc822HeaderState rfc822HeaderState;\n    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);\n  private SynchronousQueue<T>[] dest;\n\n  @Override\n    public ActionBuilder<" + cn + "> to(SynchronousQueue<T>...dest) {\n    this.dest = dest;\n    if (parms.size() == parmsCount)\n            return new ActionBuilder<" + cn + ">() {\n                @Override\n                public TerminalBuilder<" + cn + "> fire() {\n                    return new TerminalBuilder<" + cn + ">();\n                }\n            };\n        throw new IllegalArgumentException(\"required paramters are: " + BlobAntiPatternObject.arrToString(parms) + "\");\n    }\n    " + XXXXXXXXXXXXXXMETHODS + "\n}\n";
      s = s2;
      int vl = parms.length;
      String s1 = "\nstatic private final int parmsCount=" + XDEADBEEF_2 + ";\n";
      for (int i = 0; i < vl; i++) {
        etype etype = parms[i];
        String name = etype.name();
        Class<? extends Object> clazz = etype.clazz;
        @Language("JAVA") String y = "public _ename_Builder _name_(_clazz_ _sclazz_){parms.put(DbKeys.etype." + etype.name() + ",_sclazz_);return this;}\n";
        s1 += y.replace("_name_", etype.name()).replace("_clazz_", clazz.getCanonicalName()).replace("_sclazz_", clazz.getSimpleName().toLowerCase()).replace("_ename_", name());
      }
      s = s.replace(XXXXXXXXXXXXXXMETHODS, s1).replace("_ename_", name()).replace(XDEADBEEF_2, String.valueOf(vl));
    }
    return s;
  }

  public static void main(String... args) throws NoSuchFieldException {
    Field[] fields = CouchMetaDriver.class.getFields();
    String s = "package rxf.server;import static rxf.server.DbKeys.*;import static rxf.server.DbKeys.etype.*;import java.util.*; public interface CouchDriver{";
    for (Field field : fields)
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
          String builder = couchDriver.builder();
          s += "\n" + builder;
        }
      }
    s += "}";
    System.out.println(s);
  }

  public static ThreadLocal<SynchronousQueue[]> currentSync =new ThreadLocal<SynchronousQueue[]>();

}
class ThreadedSelectorDelegate<T> extends AsioVisitor.Impl {
  private AsioVisitor surrogate;

  public ThreadedSelectorDelegate(AsioVisitor surrogate) {


    this.surrogate = surrogate;
  }

  @Override
  public void onRead(final SelectionKey key) throws Exception {
    CouchMetaDriver.currentSync.set(DbKeys.ActionBuilder.currentAction.get().sync());
    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
      public void run() {
        try {

          surrogate.onRead(key);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }


  @Override
  public void onWrite(final SelectionKey key) throws Exception {
    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Runnable() {
      public void run() {
        try {
          surrogate.onWrite(key);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });
  }
}