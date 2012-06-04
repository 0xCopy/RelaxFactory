package rxf.server;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import one.xio.AsioVisitor;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;
import org.intellij.lang.annotations.Language;
import rxf.server.DbKeys.etype;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.wheresWaldo;
import static rxf.server.BlobAntiPatternObject.COOKIE;
import static rxf.server.BlobAntiPatternObject.DEBUG_SENDJSON;
import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.GSON;
import static rxf.server.BlobAntiPatternObject.arrToString;
import static rxf.server.BlobAntiPatternObject.createCouchConnection;
import static rxf.server.BlobAntiPatternObject.deepToString;
import static rxf.server.BlobAntiPatternObject.dequote;
import static rxf.server.BlobAntiPatternObject.fetchJsonByPath;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;
import static rxf.server.BlobAntiPatternObject.recycleChannel;
import static rxf.server.BlobAntiPatternObject.sendJson;
import static rxf.server.DbKeys.etype.blob;
import static rxf.server.DbKeys.etype.db;
import static rxf.server.DbKeys.etype.designDocId;
import static rxf.server.DbKeys.etype.docId;
import static rxf.server.DbKeys.etype.mimetype;
import static rxf.server.DbKeys.etype.opaque;
import static rxf.server.DbKeys.etype.rev;
import static rxf.server.DbKeys.etype.validjson;
import static rxf.server.DbKeys.etype.view;
import static rxf.server.DbTerminal.continuousFeed;
import static rxf.server.DbTerminal.future;
import static rxf.server.DbTerminal.oneWay;
import static rxf.server.DbTerminal.pojo;
import static rxf.server.DbTerminal.rows;
import static rxf.server.DbTerminal.tx;


/**
 * confers traits on an oo platform...
 * <p/>
 * CouchDriver defines an interface and a method for each MetaCouchDriver enum attribute.  presently the generator does
 * not wire that interface up anywhere but the inner classes of the interface use this enum for slotted method dispatch.
 * <p/>
 * the fluent interface is carried in threadlocal variables from step to step.  the visit() method cracks these open and
 * inserts them as the apropriate state for lower level method calls.
 * <p/>
 * User: jim
 * Date: 5/24/12
 * Time: 3:09 PM
 */
public enum CouchMetaDriver {

  @DbTask({tx, oneWay}) @DbKeys({db, validjson})createDb {
    @Override
    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      return sendJson((String) dbKeysBuilder.parms().get(etype.validjson), (String) dbKeysBuilder.parms().get(etype.db));
    }
  },
  @DbTask({tx, oneWay}) @DbKeys({db, docId, validjson})createDoc {
    @Override
    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      EnumMap<etype, Object> parms = dbKeysBuilder.parms();
      String json = (String) parms.get(etype.validjson);
      String o = (String) parms.get(etype.db);
      String o1 = (String) parms.get(etype.docId);
      return sendJson(
          json,
          '/' + o + '/' + o1
      );

    }


  },
  @DbTask({pojo, future}) @DbResultUnit(String.class) @DbKeys({db, docId})getDoc {
    @Override
    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      String path = idpath(dbKeysBuilder, etype.docId);
      SocketChannel couchConnection = createCouchConnection();
      SynchronousQueue<Object> returnTo1 = ActionBuilder.get().sync();
      SynchronousQueue returnTo = returnTo1;
      AsioVisitor asioVisitor = fetchJsonByPath(couchConnection, returnTo, path);
      T take = (T) returnTo.poll(3, TimeUnit.SECONDS);
      recycleChannel(couchConnection);
      return take;
    }
  },
  @DbTask({tx, future}) @DbResultUnit(String.class) @DbKeys({db, docId})getRevision {
    @Override
    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      EnumMap<etype, Object> parms = dbKeysBuilder.parms();

      final String pathRescode = "/" + parms.get(db) + "/" + parms.get(etype.docId);
      final Rfc822HeaderState state = actionBuilder.state().headers(ETAG).methodProtocol("HEAD").pathResCode(pathRescode);
      return EXECUTOR_SERVICE.submit(new Callable<Object>() {
        public Object call() throws Exception {
          final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);

          HttpMethod.enqueue(createCouchConnection(), OP_WRITE | OP_CONNECT, new Impl() {
            @Override
            public void onWrite(SelectionKey key) throws Exception {
              SocketChannel channel = (SocketChannel) key.channel();
              int write = channel.write(state.asRequestHeaders());
              key.interestOps(OP_READ).selector().wakeup();

            }

            @Override
            public void onRead(final SelectionKey key) throws Exception {
              EXECUTOR_SERVICE.submit(
                  new Runnable() {
                    public void run() {
                      SocketChannel channel = null;
                      try {
                        channel = (SocketChannel) key.channel();
                        ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
                        int read = channel.read(dst);
                        state.apply((ByteBuffer) dst.flip());
                        cyclicBarrier.await(/*10, TimeUnit.MILLISECONDS*/);
                      } catch (Exception e) {
                        e.printStackTrace();
                      } finally {
                        recycleChannel(channel);

                      }
                    }
                  });
            }

          });
          if (BlobAntiPatternObject.DEBUG_SENDJSON) cyclicBarrier.await();
          else
            cyclicBarrier.await(3, TimeUnit.SECONDS);
          Map<String, String> headerStrings = state.getHeaderStrings();

          CouchTx ctx = new CouchTx().id(pathRescode);

          if (null == headerStrings || !headerStrings.containsKey(ETAG))
            return ctx.error(state.pathResCode()).reason(state.methodProtocol());
          String rev1 = state.dequotedHeader(ETAG);
          return ctx.ok(true).rev(rev1);

        }
      }).get();
    }
  },
  @DbTask({tx, oneWay, future}) @DbKeys({db, docId, rev, validjson})updateDoc {
    @Override
    <T> Object visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {

      return sendJson(
          (String) dbKeysBuilder.parms().get(etype.validjson),
          (String) dbKeysBuilder.parms().get(etype.db),
          (String) dbKeysBuilder.parms().get(etype.docId),
          (String) dbKeysBuilder.parms().get(etype.rev));

    }
  },
  @DbTask({tx, oneWay}) @DbKeys({db, designDocId, validjson})createNewDesignDoc {
    @Override
    <T> Object visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {

      return sendJson(
          (String) dbKeysBuilder.parms().get(etype.validjson),
          (String) dbKeysBuilder.parms().get(etype.db),
          (String) dbKeysBuilder.parms().get(etype.designDocId));

    }

  },
  @DbTask({tx}) @DbResultUnit(String.class) @DbKeys({db, designDocId})getDesignDoc {
    @Override
    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      String path = idpath(dbKeysBuilder, etype.designDocId);
      SocketChannel couchConnection = createCouchConnection();
      SynchronousQueue<Object> returnTo1 = ActionBuilder.get().sync();
      SynchronousQueue returnTo = returnTo1;
      AsioVisitor asioVisitor = fetchJsonByPath(couchConnection, returnTo, path);
      T take = (T) returnTo.poll(3, TimeUnit.SECONDS);
      recycleChannel(couchConnection);
      return take;
    }
  },
  @DbTask({tx, oneWay}) @DbKeys({db, designDocId, rev, validjson})updateDesignDoc {
    AtomicReference<CouchTx> payload = new AtomicReference<CouchTx>();

    @Override
    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {

//      return EXECUTOR_SERVICE.submit(new Callable() {
//
//        public Object call() throws Exception {
//          DbKeysBuilder.currentKeys.set(dbKeysBuilder);
//          ActionBuilder.currentAction.set(actionBuilder);
      final String json = (String) dbKeysBuilder.parms().get(etype.validjson);
      final byte[] bytes = json.getBytes(UTF8);

      String db = (String) dbKeysBuilder.parms().get(etype.db);
      String docId = (String) dbKeysBuilder.parms().get(etype.designDocId);
      String rev = (String) dbKeysBuilder.parms().get(etype.rev);
      assert null != rev;
      final String path = "/" + db +
          "/" + docId +
          "?rev=" + dequote(rev);
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);

      final Rfc822HeaderState state = actionBuilder.state();
      HttpMethod.enqueue(createCouchConnection(), OP_WRITE | OP_CONNECT, new Impl() {

        public ByteBuffer cursor;

        @Override
        public void onWrite(SelectionKey key) throws Exception {
          final SocketChannel channel = (SocketChannel) key.channel();
          state.methodProtocol("PUT").pathResCode(path);
          state.headerStrings().put(CONTENT_LENGTH, String.valueOf(json.length()));
          int write = channel.write(state.asRequestHeaders());
          key.selector().wakeup();
          key.attach(new Impl() {
            @Override
            public void onWrite(SelectionKey key) throws Exception {
              int write = channel.write(ByteBuffer.wrap(bytes));
              key.interestOps(OP_READ);
            }

            @Override
            public void onRead(SelectionKey key) throws Exception {
              ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
              int read = channel.read(dst);
              if (-1 == read) {
                System.err.println("unexpected bad stuff " + wheresWaldo());
                recycleChannel(channel);
                cyclicBarrier.reset();
              } else {
                String s3 = state.pathResCode();
                Rfc822HeaderState apply = state.headers(COOKIE, ETAG, CONTENT_LENGTH).apply((ByteBuffer) dst.flip());
                if (!apply.pathResCode().startsWith("20")) {
                  cyclicBarrier.reset();
                  channel.close();
                  throw new Error("!!! unexpected bad stuff: " + deepToString(s3, UTF8.decode((ByteBuffer) dst.rewind()), apply));
                } else {


                  String s = state.headerString(CONTENT_LENGTH);
                  long remaining = Long.parseLong(s);

                  final ByteBuffer cursor = ByteBuffer.allocateDirect((int) remaining).put(dst);
                  if (!cursor.hasRemaining()) {
                    deliver();
                  }
                  key.attach(new Impl() {
                    @Override
                    public void onRead(SelectionKey key) throws Exception {
                      if (!cursor.hasRemaining()) {
                        deliver();
                      }
                    }


                  });
                }
              }

            }

            private void deliver() {
              EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                  try {
                    cyclicBarrier.await(/*10, TimeUnit.MILLISECONDS*/);
                  } catch (RuntimeException e) {
                    e.printStackTrace();  //todo: verify for a purpose
                  } catch (Throwable e) {
                    e.printStackTrace();  //todo: verify for a purpose
                  } finally {

                    recycleChannel(channel);  // V
                  }
                }                           // V
              });                           // V
            }                               // V
          });                               // V
        }                                   // V
        //                                     V
      });                                   // V

      if (BlobAntiPatternObject.DEBUG_SENDJSON) {
        cyclicBarrier.await();
      } else {
        cyclicBarrier.await(3, TimeUnit.SECONDS);
      }

      return
          payload.get();
      //
      //        }
      //      }).get();
    }
  },
  @DbTask({rows, future, continuousFeed}) @DbResultUnit(CouchResultSet.class) @DbKeys({db, view})getView {
    @Override
    <T> Object visit(DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      SelectionKey key = actionBuilder.key();
      SocketChannel couchConnection = null;
      Object poll = null;
      T poll1 = null;
      try {
        couchConnection = null == key || key.channel().isOpen() ? createCouchConnection() : (SocketChannel) key.channel();

        String idpath = idpath(dbKeysBuilder, etype.view);
        final String format = MessageFormat.format("GET " + idpath + " HTTP/1.1\r\n\r\n", idpath.trim()).replace("//", "/");
        final SocketChannel cc = couchConnection;
        HttpMethod.enqueue(couchConnection, OP_WRITE | OP_CONNECT, new Impl() {
          @Override
          public void onWrite(SelectionKey key) throws Exception {
            actionBuilder.key(key);
            byte[] bytes = format.getBytes();
            int write = cc.write(ByteBuffer.wrap(bytes));
            key.interestOps(OP_READ).attach(new Impl() {
              @Override
              public void onRead(SelectionKey key) throws Exception {
                final ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
                int read = cc.read(dst);
                final Rfc822HeaderState state = new Rfc822HeaderState(COOKIE, ETAG, CONTENT_LENGTH, TRANSFER_ENCODING).apply((ByteBuffer) dst.flip());
                actionBuilder.state(state);


                if (state.getHeaderStrings().containsKey(TRANSFER_ENCODING)) {
                  //noinspection unchecked
                  ChunkedEncodingVisitor ob = new ChunkedEncodingVisitor(dst, getReceiveBufferSize(), actionBuilder.sync());
                  key.attach(ob);
                  ob.onRead(key);
                } else {
                  actionBuilder.key(null);
                  EXECUTOR_SERVICE.submit(new Runnable() {
                    public void run() {
                      try {
                        System.err.println("view fetch error: " + deepToString(state, UTF8.decode(dst.slice())));
                        actionBuilder.sync().put(UTF8.decode(dst.slice()));
                        cc.close();
                      } catch (Throwable e) {
                        e.printStackTrace();  //todo: verify for a purpose
                      }
                    }
                  });
                }

              }
            });


          }
        });

        poll1 = (T) actionBuilder.sync().poll(3, TimeUnit.SECONDS);
        System.err.println("view res: " + deepToString(poll1, actionBuilder));

      } catch (Exception e) {
        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
      } finally {
        recycleChannel(couchConnection);
      }

      return poll1;
    }
  },
  @DbTask({tx, oneWay, rows, future, continuousFeed}) @DbKeys({opaque, validjson})sendJson {
    @Override
    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<CouchTx> payload = new AtomicReference<CouchTx>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      String opaque = '/' + ((String) dbKeysBuilder.parms().get(etype.opaque)).replace("//", "/");
      String validjson = (String) dbKeysBuilder.parms().get(etype.validjson);
      int lastSlashIndex = opaque.lastIndexOf('/');
      final byte[] outbound = validjson.getBytes(UTF8);
      final Rfc822HeaderState state = actionBuilder.state();
      final ByteBuffer header = state.headers(ETAG, CONTENT_LENGTH, CONTENT_ENCODING)
          .methodProtocol(lastSlashIndex < opaque.lastIndexOf('?') || lastSlashIndex != opaque.indexOf('/') ? "PUT" : "POST")//works with or without _id [Version]set.
          .pathResCode(opaque)
          .headerString(CONTENT_LENGTH, String.valueOf(outbound.length))
          .headerString(ACCEPT, APPLICATION_JSON)
          .headerString(CONTENT_TYPE, APPLICATION_JSON)
          .asRequestHeaders();
      if (DEBUG_SENDJSON) System.err.println(deepToString(opaque, validjson, UTF8.decode(header.duplicate()), state));
      final SocketChannel channel = createCouchConnection();
      HttpMethod.enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor = null;

        @Override
        public void onWrite(SelectionKey key) throws Exception {
          if (cursor == null) {
            int write = channel.write((ByteBuffer) header);
            cursor = ByteBuffer.wrap(outbound);

          }
          int write = channel.write(cursor);
          if (!cursor.hasRemaining()) {
            key.interestOps(OP_READ).selector().wakeup();
            cursor = null;
          }
        }

        @Override
        public void onRead(SelectionKey key) throws Exception {
          if (cursor == null) {
            cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
            int read = channel.read(cursor);
            state.headerStrings().clear();
            state.apply((ByteBuffer) cursor.flip());
            if (BlobAntiPatternObject.DEBUG_SENDJSON) {
              System.err.println(deepToString(state.pathResCode(), state, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }

            int remaining = Integer.parseInt(state.apply(cursor).headerString(CONTENT_LENGTH));

            if (remaining == cursor.remaining()) {
              deliver();
            } else {
              cursor = ByteBuffer.allocate(remaining).put(cursor);
            }
          } else {
            int read = channel.read(cursor);
            if (!cursor.hasRemaining()) {
              cursor.flip();
              deliver();
            }
          }
        }

        void deliver() throws BrokenBarrierException, InterruptedException {
          CharBuffer decode = UTF8.decode(cursor);
          String json = decode.toString();
          payload.set(GSON.fromJson(json, CouchTx.class));
          int await = cyclicBarrier.await();
          recycleChannel(channel);
        }
      });
      if (DEBUG_SENDJSON) {
        cyclicBarrier.await(/*3, TimeUnit.SECONDS*/);
      } else {
        cyclicBarrier.await(3, TimeUnit.SECONDS);
      }

      CouchTx couchTx = payload.get();
      return couchTx;
    }
  },
  @DbTask({tx, future, oneWay}) @DbResultUnit(Rfc822HeaderState.class) @DbKeys({opaque, mimetype, blob})sendBlob {};
  private static final String APPLICATION_JSON = "application/json";
  public static final String ETAG = "ETag";
  public static final String CONTENT_LENGTH = "Content-Length";
  public static final String CONTENT_TYPE = "Content-Type";
  public static final String CONTENT_ENCODING = "Content-Encoding";
  public static final String ACCEPT = "Accept";
  public static final String ACCEPT_ENCODING = "Accept-Encoding";
  public static final String TRANSFER_ENCODING = "Transfer-Encoding";
  static final String SET_COOKIE = "Set-Cookie";

  static <T> String idpath(DbKeysBuilder<T> dbKeysBuilder, etype etype) {
    String db = (String) dbKeysBuilder.parms().get(etype.db);
    String id = (String) dbKeysBuilder.parms().get(etype);
    return '/' + db + '/' + id;
  }


  <T> Object visit() throws Exception {
    DbKeysBuilder<T> dbKeysBuilder = (DbKeysBuilder<T>) DbKeysBuilder.get();
    ActionBuilder<T> actionBuilder = (ActionBuilder<T>) ActionBuilder.get();

    if (dbKeysBuilder.validate())
      return visit(dbKeysBuilder, actionBuilder);
    throw new Error("validation error");
  }

  /*abstract */<T> Object visit(DbKeysBuilder<T> dbKeysBuilder,
                                ActionBuilder<T> actionBuilder) throws Exception {
    throw new AbstractMethodError();
  }

  public static final String XDEADBEEF_2 = "-0xdeadbeef.2";
  public static final String XXXXXXXXXXXXXXMETHODS = "/*XXXXXXXXXXXXXXMETHODS*/";
  public static final String IFACE_FIRE_TARGETS = "/*FIRE_IFACE*/";
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
      @Language("JAVA") String s2 = " \npublic class _ename_Builder<T> extends DbKeysBuilder<" +
          cn + "> {\n  private Rfc822HeaderState rfc822HeaderState;\n\n" +
          "\n  interface _ename_TerminalBuilder extends TerminalBuilder<" +
          cn + "> {\n    " + IFACE_FIRE_TARGETS + "\n  }\n\n  public class _ename_ActionBuilder extends ActionBuilder<" +
          cn + "> {\n    public _ename_ActionBuilder(SynchronousQueue/*<" +
          cn + ">*/... synchronousQueues) {\n      super(synchronousQueues);\n    }\n\n    @Override\n    public _ename_TerminalBuilder fire() {\n      return new _ename_TerminalBuilder() {\n        " + BAKED_IN_FIRE + "\n      };\n    }\n\n    @Override\n    public _ename_ActionBuilder state(Rfc822HeaderState state) {\n      return super.state(state);\n    }\n\n    @Override\n    public _ename_ActionBuilder key(java.nio.channels.SelectionKey key) {\n      return super.key(key);\n    }\n  }\n\n  @Override\n  public _ename_ActionBuilder to(SynchronousQueue/*<" +
          cn + ">*/... dest) {\n    if (parms.size() == parmsCount)\n      return new _ename_ActionBuilder(dest);\n\n    throw new IllegalArgumentException(\"required parameters are: " + arrToString(parms) + "\");\n  }\n  " +
          XXXXXXXXXXXXXXMETHODS + "\n" +
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
          String t = "", iface = "";
          for (DbTerminal terminal : terminals) {
            iface += terminal.builder(couchDriver, parms, rtype, false);
            t += terminal.builder(couchDriver, parms, rtype, true);

          }
          s = s.replace(BAKED_IN_FIRE, t).replace(IFACE_FIRE_TARGETS, iface);
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

}

