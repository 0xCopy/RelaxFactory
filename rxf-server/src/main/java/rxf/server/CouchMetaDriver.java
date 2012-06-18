package rxf.server;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.enqueue;
import static rxf.server.BlobAntiPatternObject.COOKIE;
import static rxf.server.BlobAntiPatternObject.DEBUG_SENDJSON;
import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.GSON;
import static rxf.server.BlobAntiPatternObject.arrToString;
import static rxf.server.BlobAntiPatternObject.createCouchConnection;
import static rxf.server.BlobAntiPatternObject.deepToString;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;
import static rxf.server.BlobAntiPatternObject.recycleChannel;
import static rxf.server.DbKeys.etype.blob;
import static rxf.server.DbKeys.etype.db;
import static rxf.server.DbKeys.etype.designDocId;
import static rxf.server.DbKeys.etype.docId;
import static rxf.server.DbKeys.etype.mimetype;
import static rxf.server.DbKeys.etype.opaque;
import static rxf.server.DbKeys.etype.validjson;
import static rxf.server.DbKeys.etype.view;
import static rxf.server.DbKeys.etype.type;
import static rxf.server.DbKeys.etype.rev;
import static rxf.server.DbTerminal.continuousFeed;
import static rxf.server.DbTerminal.future;
import static rxf.server.DbTerminal.oneWay;
import static rxf.server.DbTerminal.pojo;
import static rxf.server.DbTerminal.rows;
import static rxf.server.DbTerminal.tx;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.TypeVariable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;
import one.xio.MimeType;

import org.intellij.lang.annotations.Language;

import rxf.server.DbKeys.etype;

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

  @DbTask({tx, oneWay}) @DbKeys({db})DbCreate {
    @Override
    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<CouchTx> payload = new AtomicReference<CouchTx>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      final Rfc822HeaderState state = actionBuilder.state();
      final ByteBuffer header = state.methodProtocol("PUT").pathResCode("/" + dbKeysBuilder.parms().get(etype.db))
          .protocolStatus("HTTP/1.1")
          .headerString("Content-Length", "0")
          .asRequestHeaderByteBuffer();
      final SocketChannel channel = createCouchConnection();
      HttpMethod.enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor = null;
        @Override
        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write((ByteBuffer) header);
          key.interestOps(OP_READ).selector().wakeup();
        }
        @Override
        public void onRead(SelectionKey key) throws Exception {
          if (cursor == null) {
            cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
            int read = channel.read(cursor);
            state.headerStrings().clear();
            state.headers(CONTENT_LENGTH);
            state.apply((ByteBuffer) cursor.flip());
            if (BlobAntiPatternObject.DEBUG_SENDJSON) {
              System.err.println(deepToString(state.pathResCode(), state, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }

            int remaining = Integer.parseInt(state.headerString(CONTENT_LENGTH));

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
        private void deliver() {
          payload.set(GSON.fromJson(UTF8.decode((ByteBuffer) cursor).toString(), CouchTx.class));
          recycleChannel(channel);
          EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {

              try {
                cyclicBarrier.await();
              } catch (Throwable e) {
                e.printStackTrace();  //todo: verify for a purpose
              }
            }
          });
        }
      });
      int await = cyclicBarrier.await(3, TimeUnit.SECONDS);
      return payload.get();
    }
  },
  @DbTask({tx, oneWay}) @DbKeys({db}) DbDelete {
    @Override
    <T> Object visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<CouchTx> payload = new AtomicReference<CouchTx>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      final Rfc822HeaderState state = actionBuilder.state();
      final ByteBuffer header = state.methodProtocol("DELETE").pathResCode("/" + dbKeysBuilder.parms().get(etype.db))
          .protocolStatus("HTTP/1.1")
          .headerString("Content-Length", "0")
          .asRequestHeaderByteBuffer();
      final SocketChannel channel = createCouchConnection();
      HttpMethod.enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor = null;
        @Override
        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write((ByteBuffer) header);
          key.interestOps(OP_READ).selector().wakeup();
        }
        @Override
        public void onRead(SelectionKey key) throws Exception {
          if (cursor == null) {
            cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
            int read = channel.read(cursor);
            state.headerStrings().clear();
            state.headers(CONTENT_LENGTH);
            state.apply((ByteBuffer) cursor.flip());
            if (BlobAntiPatternObject.DEBUG_SENDJSON) {
              System.err.println(deepToString(state.pathResCode(), state, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }

            int remaining = Integer.parseInt(state.headerString(CONTENT_LENGTH));

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
        private void deliver() {
          recycleChannel(channel);
          payload.set(GSON.fromJson(UTF8.decode((ByteBuffer) cursor).toString(), CouchTx.class));
          EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
              try {
                cyclicBarrier.await();
              } catch (Throwable e) {
                e.printStackTrace();  //todo: verify for a purpose
              }
            }
          });
        }
      });
      int await = cyclicBarrier.await(3, TimeUnit.SECONDS);
      return payload.get();
    }
  },
  //  @DbTask({tx, oneWay}) @DbKeys({db, docId, validjson})createDoc {
//    @Override
//    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
//      EnumMap<etype, Object> parms = dbKeysBuilder.parms();
//      String json = (String) parms.get(etype.validjson);
//      String o = (String) parms.get(etype.db);
//      String o1 = (String) parms.get(etype.docId);
//      return sendJson(
//          json,
//          '/' + o + '/' + o1
//      );
//
//    }
//
//
//  },
  @DbTask({pojo, future}) @DbResultUnit(String.class) @DbKeys({db, docId})DocFetch {

    @Override
    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<Object> payload = new AtomicReference<Object>();

      EnumMap<etype, Object> parms = dbKeysBuilder.parms();
      String db = (String) parms.get(etype.db);
      String id = (String) parms.get(etype.docId);
      final Rfc822HeaderState state = actionBuilder.state().headers(CONTENT_LENGTH).methodProtocol("GET").pathResCode("/" + db + (null == id ? "" : ("/" + id.trim())));
      state.protocolStatus("HTTP/1.1");
      final SocketChannel channel = createCouchConnection();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      enqueue(channel, OP_CONNECT | OP_WRITE, new Impl() {
        public ByteBuffer cursor;

        @Override
        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write(state.asRequestHeaderByteBuffer());
          key.interestOps(OP_READ).selector().wakeup();
        }

        @Override
        public void onRead(SelectionKey key) throws Exception {
          if (null != cursor) {
            int read = channel.read(cursor);
            if (-1 == read) {
              channel.socket().close();
            }
            if (!cursor.hasRemaining()) {
              System.err.println("*********" + UTF8.decode((ByteBuffer) cursor.duplicate().rewind()));
              deliver();
            }

          }
          ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
          int read = channel.read(dst);
          String s = state.apply((ByteBuffer) dst.flip()).pathResCode();
          if (s.startsWith("20")) {
            int remaining = Integer.parseInt(state.headerString(CONTENT_LENGTH));
            if (remaining == dst.remaining()) {
              cursor = dst.slice();
              deliver();
              return;
            }
            cursor = ByteBuffer.allocate(remaining).put(dst);
          } else {//error
            cyclicBarrier.reset();
          }
        }

        private void deliver() {
          assert cursor != null;
          payload.set(UTF8.decode((ByteBuffer) cursor.rewind()).toString());
          
          recycleChannel(channel);
          EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {

              try {
                cyclicBarrier.await();
              } catch (Throwable e) {
                e.printStackTrace();  //todo: verify for a purpose
              }
            }
          });
        }
      });
      try {
      if (BlobAntiPatternObject.DEBUG_SENDJSON) {
        cyclicBarrier.await();
      } else {
        cyclicBarrier.await(3, TimeUnit.SECONDS);
      }
      } catch (BrokenBarrierException ex) {
        // non-200 error code
      }
      
      return payload.get();
    }
  },
  @DbTask({tx, future}) @DbResultUnit(String.class) @DbKeys({db, docId})RevisionFetch {
    @Override
    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      EnumMap<etype, Object> parms = dbKeysBuilder.parms();

      Object id = parms.get(etype.docId);
      final String pathRescode = "/" + parms.get(db) + ((id != null) ? "/" + id : "");
      final Rfc822HeaderState state = actionBuilder.state().headers(ETAG).methodProtocol("HEAD").pathResCode(pathRescode);
      state.protocolStatus("HTTP/1.1");
      return EXECUTOR_SERVICE.submit(new Callable<Object>() {
        public Object call() throws Exception {
          final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);

          HttpMethod.enqueue(createCouchConnection(), OP_WRITE | OP_CONNECT, new Impl() {
            @Override
            public void onWrite(SelectionKey key) throws Exception {
              SocketChannel channel = (SocketChannel) key.channel();
              int write = channel.write(state.asRequestHeaderByteBuffer());
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
  @DbTask({tx, oneWay, future}) @DbKeys(value={db, validjson}, optional={docId, rev})DocPersist {
    @Override
    <T> Object visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {
//
//      return sendJson(
//          (String) dbKeysBuilder.parms().get(etype.db),
//          (String) dbKeysBuilder.parms().get(etype.validjson)
////          (String) dbKeysBuilder.parms().get(etype.docId),
////          (String) dbKeysBuilder.parms().get(etype.rev)
//      );
      String db = (String) dbKeysBuilder.parms().get(etype.db);
      String docId = (String) dbKeysBuilder.parms().get(etype.docId);
      String rev = (String) dbKeysBuilder.parms().get(etype.rev);
      StringBuilder sb = new StringBuilder(db);
      if (docId != null) {
        sb.append("/").append(docId);
      }
      if (rev != null) {
        sb.append("?rev=").append(rev);
      }
      dbKeysBuilder.parms().put(etype.opaque, sb.toString());
      return JsonSend.visit(dbKeysBuilder, actionBuilder);
    }
  },
  @DbTask({tx, oneWay, future}) @DbKeys(value={db, docId, rev}) DocDelete {
    @Override
    <T> Object visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<CouchTx> payload = new AtomicReference<CouchTx>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      final Rfc822HeaderState state = actionBuilder.state();
      final ByteBuffer header = state.methodProtocol("DELETE").pathResCode("/" + dbKeysBuilder.parms().get(db) + "/" + dbKeysBuilder.parms().get(docId) + "?rev=" + dbKeysBuilder.parms().get(rev))
          .protocolStatus("HTTP/1.1")
          .headerString("Content-Length", "0")
          .asRequestHeaderByteBuffer();
      final SocketChannel channel = createCouchConnection();
      HttpMethod.enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor = null;
        @Override
        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write((ByteBuffer) header);
          key.interestOps(OP_READ).selector().wakeup();
        }
        @Override
        public void onRead(SelectionKey key) throws Exception {
          if (cursor == null) {
            cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
            int read = channel.read(cursor);
            state.headerStrings().clear();
            state.headers(CONTENT_LENGTH);
            state.apply((ByteBuffer) cursor.flip());
            if (BlobAntiPatternObject.DEBUG_SENDJSON) {
              System.err.println(deepToString(state.pathResCode(), state, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }

            int remaining = Integer.parseInt(state.headerString(CONTENT_LENGTH));

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
        private void deliver() {
          payload.set(GSON.fromJson(UTF8.decode((ByteBuffer) cursor).toString(), CouchTx.class));
          recycleChannel(channel);
          EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
              try {
                cyclicBarrier.await();
              } catch (Throwable e) {
                e.printStackTrace();  //todo: verify for a purpose
              }
            }
          });
        }
      });
      int await = cyclicBarrier.await(3, TimeUnit.SECONDS);
      return payload.get();
    }
  },
  @DbTask({pojo, future}) @DbResultUnit(String.class) @DbKeys({db, designDocId}) DesignDocFetch {
    @Override
    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      dbKeysBuilder.parms().put(etype.docId, dbKeysBuilder.parms().remove(etype.designDocId));
      return DocFetch.visit(dbKeysBuilder, actionBuilder);
    }
  },
  @DbTask({rows, future, continuousFeed}) @DbResultUnit(CouchResultSet.class) @DbKeys(value={db, view}, optional = type)ViewFetch {
    @Override
    <T> Object visit(DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      SelectionKey key = actionBuilder.key();
      SocketChannel couchConnection = null;
      Object poll = null;
      T poll1 = null;
      try {
        couchConnection = null == key || key.channel().isOpen() ? createCouchConnection() : (SocketChannel) key.channel();

        String idpath = idpath(dbKeysBuilder, etype.view);
        final String format = MessageFormat.format("GET " + idpath + " HTTP/1.1\r\nAccept: application/json\r\n\r\n", idpath.trim()).replace("//", "/");
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
                        actionBuilder.sync().put(UTF8.decode(dst.slice()).toString());
                        cc.close();
                        recycleChannel(cc);
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
      }

      return poll1;
    }
  },
  @DbTask({tx, oneWay, rows, future, continuousFeed}) @DbKeys(value = {opaque, validjson}, optional = type)JsonSend {
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
          .protocolStatus("HTTP/1.1")
          .headerString(CONTENT_LENGTH, String.valueOf(outbound.length))
          .headerString(ACCEPT, APPLICATION_JSON)
          .headerString(CONTENT_TYPE, APPLICATION_JSON)
          .asRequestHeaderByteBuffer();
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

            int remaining = Integer.parseInt(state.headerString(CONTENT_LENGTH));

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
  @DbTask({tx, future, oneWay}) @DbResultUnit(Rfc822HeaderState.class) @DbKeys({db, docId, opaque, mimetype, blob})BlobSend {
    @Override
    <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
       final AtomicReference<String> payload = new AtomicReference<String>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      final EnumMap<etype, Object> parms = dbKeysBuilder.parms();
      CouchTx visit = (CouchTx) RevisionFetch.visit(dbKeysBuilder, actionBuilder);
      String rev = visit == null ? null : visit.rev();

      final ByteBuffer byteBuffer = actionBuilder.state().methodProtocol("PUT")
          .pathResCode("/" + parms.get(db) + '/' + parms.get(etype.docId) + '/' + parms.get(etype.opaque) + rev == null ? "" : ("?rev=" + rev))
          .protocolStatus("HTTP/1.1")
          .headerStrings(new TreeMap<String, String>() {{
            put("Expect", "100-continue");
            put(CONTENT_TYPE, ((MimeType) parms.get(etype.mimetype)).contentType);
            put(ACCEPT, "*/*");
          }}).asRequestHeaderByteBuffer();


      final SocketChannel channel = createCouchConnection();
      HttpMethod.enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        public ByteBuffer cursor;

        @Override
        public void onWrite(SelectionKey key) throws Exception {
          if (null != cursor) {
            int write = channel.write(cursor);
            if (-1 == write || !cursor.hasRemaining()) {
              key.interestOps(OP_READ).selector().wakeup();
              cyclicBarrier.reset();
              key.attach(new Impl() {
                @Override
                public void onRead(final SelectionKey key) throws Exception {

                  final ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
                  int read = channel.read(dst);
                  actionBuilder.state().apply((ByteBuffer) dst.flip());
                  System.err.println(deepToString(this, parms, actionBuilder));
                  EXECUTOR_SERVICE.submit(new Runnable() {
                    @Override
                    public void run() {
                      try {
                        payload.set(UTF8.decode(dst.slice()).toString()); //ignoring content-length here..  not actually sane.
                        cyclicBarrier.await();
                        recycleChannel(channel);
                      } catch (Throwable e) {
                        try {
                          channel.socket().close();
                        } catch (IOException e1) {

                        }
                        e.printStackTrace();  //todo: verify for a purpose
                      }

                    }
                  })                          ;

                }
              });
            }

          }
          int write = channel.write((ByteBuffer) byteBuffer.rewind());
          key.interestOps(OP_READ).selector().wakeup();
        }

        @Override
        public void onRead(SelectionKey key) throws Exception {

          ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
          int read = channel.read(dst);
          String continueString = actionBuilder.state().apply((ByteBuffer) dst.flip()).pathResCode();

          if (continueString.startsWith("100")) {
            cursor = (ByteBuffer) parms.get(etype.blob);
            key.interestOps(OP_WRITE).selector().wakeup();
          }


        }
      });
      cyclicBarrier.await(6, TimeUnit.MINUTES);
      return payload;
    }



  };
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
    String db = (String) dbKeysBuilder.parms().get(DbKeys.etype.db);
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
      etype[] optionalParams = field.getAnnotation(DbKeys.class).optional();
      Class<?> rtype = CouchTx.class;
      try {
        rtype = field.getAnnotation(DbResultUnit.class).value();
      } catch (Exception e) {
      }
      String rtypeTypeParams = "";
      String rtypeBounds = "";
      if (rtype.getTypeParameters().length != 0) {
        StringBuilder params = new StringBuilder("<");
        StringBuilder bounds = new StringBuilder("<");
        for (TypeVariable<? extends GenericDeclaration> param : rtype.getTypeParameters()) {
          params.append(param.getName()).append(",");
          bounds.append(param.getName()).append(" extends ").append(((Class<?>)param.getBounds()[0]).getName()).append(",");
        }
        params.deleteCharAt(params.length() - 1).append(">");
        bounds.deleteCharAt(bounds.length() - 1).append(">");
        rtypeTypeParams = params.toString();
        rtypeBounds = bounds.toString();
      }
      String fqsn = rtype.getCanonicalName();
      String pfqsn = fqsn + rtypeTypeParams;
      @Language("JAVA") String s2 = " \n\npublic cla" +
          "ss _ename_"+rtypeTypeParams+" extends DbKeysBuilder<" +
          pfqsn + "> {\n  private _ename_() {\n  }\n\n  static public "+rtypeBounds+" _ename_"+rtypeTypeParams+" $() {\n    return new _ename_"+rtypeTypeParams+"();\n  }" +
          "\n\n  public interface _ename_TerminalBuilder"+rtypeTypeParams+" extends TerminalBuilder<" +
          pfqsn + "> {\n    " + IFACE_FIRE_TARGETS + "\n  }\n\n  public class _ename_ActionBuilder extends ActionBuilder<" +
          pfqsn + "> {\n    public _ename_ActionBuilder( /*<" +
          pfqsn + ">*/ ) {\n      super(/*synchronousQueues*/);\n    }\n\n    @Override\n    public _ename_TerminalBuilder"+rtypeTypeParams+" fire() {\n      return new _ename_TerminalBuilder"+rtypeTypeParams+"() {\n        " + BAKED_IN_FIRE + "\n      };\n    }\n\n    @Override\n    public _ename_ActionBuilder state(Rfc822HeaderState state) {\n      return (_ename_ActionBuilder)super.state(state);\n    }\n\n    @Override\n    public _ename_ActionBuilder key(java.nio.channels.SelectionKey key) {\n      return (_ename_ActionBuilder)super.key(key);\n    }\n  }\n\n  @Override\n  public _ename_ActionBuilder to( /*<" +
          pfqsn + ">*/ ) {\n    if (parms.size() >= parmsCount)\n      return new _ename_ActionBuilder(/*dest*/);\n\n    throw new IllegalArgumentException(\"required parameters are: " + arrToString(parms) + "\");\n  }\n  " +
          XXXXXXXXXXXXXXMETHODS + "\n" +
          "}\n";
      s = s2;
      int vl = parms.length;
      String s1 = "\nstatic private final int parmsCount=" + XDEADBEEF_2 + ";\n";
      for (etype etype : parms) {
        s1 = writeParameterSetter(rtypeTypeParams, s1, etype, etype.clazz);
      }
      for (etype etype : optionalParams) {
        s1 = writeParameterSetter(rtypeTypeParams, s1, etype, etype.clazz);
      }
      {

        DbTask annotation = field.getAnnotation(DbTask.class);
        if (null != annotation) {
          DbTerminal[] terminals = annotation.value();
          String t = "", iface = "";
          for (DbTerminal terminal : terminals) {
            iface += terminal.builder(couchDriver, parms, pfqsn, false);
            t += terminal.builder(couchDriver, parms, pfqsn, true);

          }
          s = s.replace(BAKED_IN_FIRE, t).replace(IFACE_FIRE_TARGETS, iface);
        }
      }

      s = s.replace(XXXXXXXXXXXXXXMETHODS, s1).replace("_ename_", name()).replace(XDEADBEEF_2, String.valueOf(vl));
    }
    return s;
  }


  private String writeParameterSetter(String rtypeTypeParams, String s1, etype etype,
      Class<?> clazz) {
    @Language("JAVA") String y = "public _ename_"+rtypeTypeParams+"  _name_(_clazz_ _sclazz_){parms.put(DbKeys.etype." + etype.name() + ",_sclazz_);return this;}\n";
    s1 += y.replace("_name_", etype.name()).replace("_clazz_", clazz.getCanonicalName()).replace("_sclazz_", clazz.getSimpleName().toLowerCase() + "Param").replace("_ename_", name());
    return s1;
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

