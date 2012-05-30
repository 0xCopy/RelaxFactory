package rxf.server;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import org.intellij.lang.annotations.Language;
import rxf.server.DbKeys.etype;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import static java.nio.channels.SelectionKey.*;
import static rxf.server.BlobAntiPatternObject.*;
import static rxf.server.DbKeys.etype.*;
import static rxf.server.DbTerminal.*;


/**
 * confers traits on an oo platform...
 * <p/>
 * User: jim
 * Date: 5/24/12
 * Time: 3:09 PM
 */
public enum CouchMetaDriver {

    @DbTask({tx, oneWay}) @DbKeys({db, validjson})createDb {
        @Override
        <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
            return sendJson((String) dbKeysBuilder.getParms().get(etype.validjson), (String) dbKeysBuilder.getParms().get(etype.db));
        }
    },
    @DbTask({tx, oneWay}) @DbKeys({db, docId, validjson})createDoc {
        @Override
        <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
            return sendJson(
                    (String) dbKeysBuilder.getParms().get(etype.validjson),
                    (String) dbKeysBuilder.getParms().get(etype.db),
                    (String) dbKeysBuilder.getParms().get(etype.docId)
            );

        }


    },
    @DbTask({pojo, future}) @DbResultUnit(String.class) @DbKeys({db, docId})getDoc {
        @Override
        <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
            String path = idpath(dbKeysBuilder);
            SocketChannel couchConnection = createCouchConnection();
            SynchronousQueue returnTo = getQ();
            AsioVisitor asioVisitor = fetchJsonByPath(couchConnection, returnTo, path);
            T take = (T) returnTo.poll(3, TimeUnit.SECONDS);
            recycleChannel(couchConnection);
            return take;
        }
    },
    @DbTask({tx, future}) @DbResultUnit(String.class) @DbKeys({db, docId})getRevision {
        @Override
        <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {

            final SocketChannel couchConnection = createCouchConnection();
            String db = (String) dbKeysBuilder.getParms().get(etype.db);
            final String id = (String) dbKeysBuilder.getParms().get(etype.docId);
            final SynchronousQueue q = getQ();
            HttpMethod.enqueue(couchConnection, OP_WRITE | OP_CONNECT, new AsioVisitor.Impl() {
                @Override
                public void onWrite(SelectionKey key) throws Exception {
                    String r = "HEAD " + idpath(dbKeysBuilder) + " HTTP/1.1\r\n\r\n";
                    int write = couchConnection.write(ByteBuffer.wrap(r.getBytes()));
                    key.selector().wakeup();
                    key.interestOps(OP_READ).attach(new Impl() {
                        @Override
                        public void onRead(SelectionKey key) throws Exception {
                            final ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
                            int read = couchConnection.read(dst);
                            if (-1 == read) {
                                return;
                            }

                            final String ver = actionBuilder.getState().headers("ETag").apply((ByteBuffer) dst.flip()).getHeaderStrings().get("ETag");
                            q.put(ver);
                            recycleChannel(couchConnection);
                        }
                    });

                }

            });
            final String poll = (String) q.poll(3, TimeUnit.SECONDS);

            return new CouchTx() {{
                setRev(poll);
                setId(id);
                setOk(Boolean.TRUE);
            }};
        }
    },
    @DbTask({tx, oneWay, future}) @DbKeys({db, docId, rev, validjson})updateDoc,
    @DbTask({tx, oneWay}) @DbKeys({db, designDocId, validjson})createNewDesignDoc,
    @DbTask({tx}) @DbResultUnit(String.class) @DbKeys({db, designDocId})getDesignDoc,
    @DbTask({tx, oneWay}) @DbKeys({db, designDocId, validjson})updateDesignDoc,
    @DbTask({rows, future, continuousFeed}) @DbResultUnit(CouchResultSet.class) @DbKeys({db, view})getView,
    @DbTask({tx, oneWay, rows, future, continuousFeed}) @DbKeys({opaque, validjson})sendJson {
        @Override
        <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
            String opaque = (String) dbKeysBuilder.getParms().get(etype.opaque);
            String validjson = (String) dbKeysBuilder.getParms().get(etype.validjson);
            return sendJson(opaque, validjson);
        }
    },
    @DbTask({tx, future, oneWay}) @DbResultUnit(Rfc822HeaderState.class) @DbKeys({opaque, mimetype, blob})sendBlob {};

    static <T> SynchronousQueue getQ() {
        SynchronousQueue[] sync = ActionBuilder.currentAction.get().sync();
        SynchronousQueue returnTo = null;
        for (SynchronousQueue synchronousQueue : sync) {
            returnTo = synchronousQueue;
        }
        if (null == returnTo) returnTo = new SynchronousQueue<T>();
        return returnTo;
    }

    static <T> String idpath(DbKeysBuilder<T> dbKeysBuilder) {
        String db = (String) dbKeysBuilder.getParms().get(etype.db);
        String id = (String) dbKeysBuilder.getParms().get(etype.docId);
        return '/' + db + '/' + id;
    }


    <T> Object visit() throws Exception {
        DbKeysBuilder<T> dbKeysBuilder = (DbKeysBuilder<T>) DbKeysBuilder.currentKeys.get();
        ActionBuilder<T> actionBuilder = (ActionBuilder<T>) ActionBuilder.currentAction.get();
        return visit(dbKeysBuilder, actionBuilder);
    }

    /*abstract */<T> Object visit(DbKeysBuilder<T> dbKeysBuilder,
                                  ActionBuilder<T> actionBuilder) throws Exception {
        throw new AbstractMethodError();
    }

    public static final String XDEADBEEF_2 = "-0xdeadbeef.2";
    public static final String XXXXXXXXXXXXXXMETHODS = "/*XXXXXXXXXXXXXXMETHODS*/";
    public static final String BAKED_IN_FIRE = "/*BAKED_IN_FIRE*/";


    public <T> String builder() throws NoSuchFieldException {
        Field field = CouchMetaDriver.class.getField(name());


        String s = null;
        if (field.getType().isAssignableFrom(CouchMetaDriver.class)) {
            CouchMetaDriver couchDriver = CouchMetaDriver.valueOf(field.getName());

            etype[] parms = field.getAnnotation(DbKeys.class).value();
            Class rtype = CouchTx.class;
            try {
                rtype = field.getAnnotation(DbResultUnit.class).value();
            } catch (Exception e) {
            }
            String cn = rtype.getCanonicalName();
            @Language("JAVA") String s2 = "\n\npublic class _ename_Builder <T>extends DbKeysBuilder<" + cn + "> {\n" +
                    "    Rfc822HeaderState rfc822HeaderState;\n" +
                    "    java.util.EnumMap<etype, Object> parms = new java.util.EnumMap<etype, Object>(etype.class);\n" +
                    "  private SynchronousQueue<" +
                    cn + ">[] dest;\n\n" +
                    "  @Override\n" +
                    "    public ActionBuilder<" + cn + "> to(SynchronousQueue<" +
                    cn + ">...dest) {\n    this.dest = dest;\n    if (parms.size() == parmsCount)\n            return new ActionBuilder<" + cn + ">() {\n                @Override\n" +
                    "                public AbstractTerminalBuilder<" + cn + "> fire() {\n" +
                    "                    return new AbstractTerminalBuilder <" + cn + ">(){" +
                    BAKED_IN_FIRE + "};\n" +
                    "                }\n" +
                    "            };\n" +
                    "        throw new IllegalArgumentException(\"required parameters are: " + arrToString(parms) + "\");\n" +
                    "    }\n" +
                    "    " + XXXXXXXXXXXXXXMETHODS + "\n" +
                    "}\n";
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
            {

                DbTask annotation = field.getAnnotation(DbTask.class);
                if (null != annotation) {
                    DbTerminal[] terminals = annotation.value();
                    String t = "";
                    for (DbTerminal terminal : terminals) {
                        t += terminal.builder(couchDriver, parms, rtype
                        );


                    }
                    s = s.replace(BAKED_IN_FIRE, t);
                }
            }

            s = s.replace(XXXXXXXXXXXXXXMETHODS, s1).replace("_ename_", name()).replace(XDEADBEEF_2, String.valueOf(vl));
        }
        return s;
    }

    public static void main(String... args) throws NoSuchFieldException {
        Field[] fields = CouchMetaDriver.class.getFields();
        @Language("JAVA")
        String s = "package rxf.server;\n//generated\nimport java.util.concurrent.*;\nimport java.util.*;\nimport static rxf.server.DbKeys.*;\nimport static rxf.server.DbKeys.etype.*;\nimport static rxf.server.CouchMetaDriver.*;\nimport java.util.*;\n\n/**\n * generated drivers\n */\npublic interface CouchDriver{";
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

    public static ThreadLocal<SynchronousQueue[]> currentSync = new ThreadLocal<SynchronousQueue[]>();

    /**
     * try to bind in common couchDriver stuff
     */
    public void sanityCheck() {
        new DbKeysBuilder() {
            @Override
            public ActionBuilder to(SynchronousQueue... clients) {
                return new ActionBuilder<CouchTx>() {
                    @Override
                    public TerminalBuilder<CouchTx> fire() {
                        return new AbstractTerminalBuilder<CouchTx>() {
                            @Override
                            void toVoid() {
                                EXECUTOR_SERVICE.submit(new Runnable() {


                                    public void run() {
                                        try {
                                            sendJson("foo", "");
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }

                                    }
                                });
                            }

                            @Override
                            CouchTx tx() throws Exception {
                                return sendJson("foo", "");
                            }
                        };
                    }
                };
            }
        };
    }
}

