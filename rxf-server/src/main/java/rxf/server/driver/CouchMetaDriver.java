package rxf.server.driver;

import java.io.IOException;
import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;
import one.xio.MimeType;
import org.intellij.lang.annotations.Language;
import rxf.server.*;
import rxf.server.an.*;
import rxf.server.an.DbKeys.etype;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.enqueue;
import static rxf.server.BlobAntiPatternObject.DEBUG_SENDJSON;
import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.GSON;
import static rxf.server.BlobAntiPatternObject.arrToString;
import static rxf.server.BlobAntiPatternObject.createCouchConnection;
import static rxf.server.BlobAntiPatternObject.deepToString;
import static rxf.server.BlobAntiPatternObject.getDefaultCollectorTimeUnit;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;
import static rxf.server.BlobAntiPatternObject.recycleChannel;
import static rxf.server.DbTerminal.continuousFeed;
import static rxf.server.DbTerminal.future;
import static rxf.server.DbTerminal.oneWay;
import static rxf.server.DbTerminal.pojo;
import static rxf.server.DbTerminal.rows;
import static rxf.server.DbTerminal.tx;
import static rxf.server.an.DbKeys.etype.blob;
import static rxf.server.an.DbKeys.etype.db;
import static rxf.server.an.DbKeys.etype.designDocId;
import static rxf.server.an.DbKeys.etype.docId;
import static rxf.server.an.DbKeys.etype.mimetype;
import static rxf.server.an.DbKeys.etype.opaque;
import static rxf.server.an.DbKeys.etype.rev;
import static rxf.server.an.DbKeys.etype.type;
import static rxf.server.an.DbKeys.etype.validjson;
import static rxf.server.an.DbKeys.etype.view;

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
    public <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<CouchTx> payload = new AtomicReference<CouchTx>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      final Rfc822HeaderState state = actionBuilder.state();
      final ByteBuffer header = state.methodProtocol("PUT").pathResCode("/" + dbKeysBuilder.get(etype.db))
          .protocolStatus("HTTP/1.1")
          .headerString("Content-Length", "0")
          .asRequestHeaderByteBuffer();
      final SocketChannel channel = createCouchConnection();
      HttpMethod.enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor = null;

        @Override
        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write(header);
          key.interestOps(OP_READ).selector().wakeup();
        }

        @Override
        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
            int read = channel.read(cursor);
            state.headerStrings().clear();
            state.addHeaderInterest(CONTENT_LENGTH);
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
          payload.set(GSON.fromJson(UTF8.decode(cursor).toString(), CouchTx.class));
          recycleChannel(channel);
          EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {

              try {
                cyclicBarrier.await();                //V
              } catch (Throwable e) {                 //V
                e.printStackTrace();                  //V
              }                                       //V
            }                                         //V
          });                                         //V
        }                                             //V
      });                                             //V
      int await = cyclicBarrier.await(3L, BlobAntiPatternObject.getDefaultCollectorTimeUnit());
      return payload.get();
    }
  },
  @DbTask({tx, oneWay}) @DbKeys({db})DbDelete {
    @Override
    public <T> Object visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<CouchTx> payload = new AtomicReference<CouchTx>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      final Rfc822HeaderState state = actionBuilder.state();
      final ByteBuffer header = state.methodProtocol("DELETE").pathResCode("/" + dbKeysBuilder.get(etype.db))
          .protocolStatus("HTTP/1.1")
          .headerString("Content-Length", "0")
          .asRequestHeaderByteBuffer();
      final SocketChannel channel = createCouchConnection();
      HttpMethod.enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor = null;

        @Override
        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write(header);
          key.interestOps(OP_READ).selector().wakeup();
        }

        @Override
        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
            int read = channel.read(cursor);
            state.headerStrings().clear();
            state.addHeaderInterest(CONTENT_LENGTH);
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
          payload.set(GSON.fromJson(UTF8.decode(cursor).toString(), CouchTx.class));
          EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
              try {
                cyclicBarrier.await();              //V
              } catch (Throwable e) {               //V
                e.printStackTrace();                //V
              }                                     //V
            }                                       //V
          });                                       //V
        }                                           //V
      });                                           //V
      int await = cyclicBarrier.await(3L, BlobAntiPatternObject.getDefaultCollectorTimeUnit());
      return payload.get();
    }
  },
  @DbTask({pojo, future}) @DbResultUnit(String.class) @DbKeys({db, docId})DocFetch {
    @Override
    public <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<Object> payload = new AtomicReference<Object>();

//      EnumMap<etype, Object> parms = dbKeysBuilder.parms();
      String db = (String) dbKeysBuilder.get(etype.db);
      String id = (String) dbKeysBuilder.get(etype.docId);
      final Rfc822HeaderState state = actionBuilder.state().addHeaderInterest(CONTENT_LENGTH).methodProtocol("GET").pathResCode("/" + db + (null == id ? "" : ("/" + id.trim())));
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
          assert null != cursor;
          payload.set(UTF8.decode((ByteBuffer) cursor.rewind()).toString());

          recycleChannel(channel);
          EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {

              try {
                cyclicBarrier.await();                     //V
              } catch (Throwable e) {                      //V
                e.printStackTrace();                       //V
              }                                            //V
            }                                              //V
          });                                              //V
        }                                                  //V
      });                                                  //V
      try {                                                //V
        if (BlobAntiPatternObject.DEBUG_SENDJSON) {        //V
          cyclicBarrier.await();                           //V
        } else {                                           //V
          cyclicBarrier.await(3L, BlobAntiPatternObject.getDefaultCollectorTimeUnit());
        }
      } catch (BrokenBarrierException ex) {
        // non-200 error code
      }

      return payload.get();
    }
  },
  @DbTask({tx, future}) @DbResultUnit(String.class) @DbKeys({db, docId})RevisionFetch {
    @Override
    public <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
//      EnumMap<etype, Object> parms = dbKeysBuilder.parms();

      Object id = dbKeysBuilder.get(etype.docId);
      final String pathRescode = "/" + dbKeysBuilder.get(db) + ((null != id) ? "/" + id : "");
      final Rfc822HeaderState state = actionBuilder.state().addHeaderInterest(ETAG).methodProtocol("HEAD").pathResCode(pathRescode);
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
                        cyclicBarrier.await(/*10, TimeUnit.MILLISECONDS*/);  //V
                      } catch (Exception e) {                                //V
                        e.printStackTrace();                                 //V
                      } finally {                                            //V
                        recycleChannel(channel);                             //V
                        //V
                      }                                                      //V
                    }                                                        //V
                  });                                                        //V
            }                                                                //V
            //V
          });                                                                //V
          cyclicBarrier.await(3L, BlobAntiPatternObject.getDefaultCollectorTimeUnit());
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
  @DbTask({tx, oneWay, future}) @DbKeys(value = {db, validjson}, optional = {docId, rev})DocPersist {
    @Override
    public <T> Object visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {

      String db = (String) dbKeysBuilder.get(etype.db);
      String docId = (String) dbKeysBuilder.get(etype.docId);
      String rev = (String) dbKeysBuilder.get(etype.rev);
      StringBuilder sb = new StringBuilder(db);
      if (null != docId) {
        sb.append("/").append(docId);
        if (null != rev) {
          sb.append("?rev=").append(rev);
        }
      }
      dbKeysBuilder.put(etype.opaque, sb.toString());
      return JsonSend.visit(dbKeysBuilder, actionBuilder);
    }
  },
  @DbTask({tx, oneWay, future}) @DbKeys(value = {db, docId, rev})DocDelete {
    @Override
    public <T> Object visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<CouchTx> payload = new AtomicReference<CouchTx>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      final Rfc822HeaderState state = actionBuilder.state();
      final ByteBuffer header = state.methodProtocol("DELETE").pathResCode("/" + dbKeysBuilder.get(db) + "/" + dbKeysBuilder.get(docId) + "?rev=" + dbKeysBuilder.get(rev))
          .protocolStatus("HTTP/1.1")
          .headerString("Content-Length", "0")
          .asRequestHeaderByteBuffer();
      final SocketChannel channel = createCouchConnection();
      HttpMethod.enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor = null;

        @Override
        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write(header);
          key.interestOps(OP_READ).selector().wakeup();
        }

        @Override
        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
            int read = channel.read(cursor);
            state.headerStrings().clear();
            state.addHeaderInterest(CONTENT_LENGTH);
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
          payload.set(GSON.fromJson(UTF8.decode(cursor).toString(), CouchTx.class));
          recycleChannel(channel);
          EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
              try {
                cyclicBarrier.await();                                    //V
              } catch (Throwable e) {                                     //V
                e.printStackTrace();                                      //V
              }                                                           //V
            }                                                             //V
          });                                                             //V
        }                                                                 //V
      });                                                                 //V
      int await = cyclicBarrier.await(3L, BlobAntiPatternObject.getDefaultCollectorTimeUnit());
      return payload.get();
    }
  },
  @DbTask({pojo, future}) @DbResultUnit(String.class) @DbKeys({db, designDocId})DesignDocFetch {
    @Override
    public <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      dbKeysBuilder.put(etype.docId, dbKeysBuilder.remove(etype.designDocId));
      return DocFetch.visit(dbKeysBuilder, actionBuilder);
    }
  },
  @DbTask({rows, future, continuousFeed}) @DbResultUnit(CouchResultSet.class) @DbKeys(value = {db, view}, optional = type)ViewFetch {
    @Override
    public <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<String> payload = new AtomicReference<String>();
      final CyclicBarrier joinPoint = new CyclicBarrier(2);
      final String db = '/' + ((String) dbKeysBuilder.get(etype.db)).replace("//", "/");
      Class<T> type = dbKeysBuilder.get(etype.type);
      final SocketChannel channel = createCouchConnection();
      HttpMethod.enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor;
        List<ByteBuffer> list = new ArrayList<ByteBuffer>();
        final Impl prev = this;

        @Override
        public void onWrite(SelectionKey key) throws Exception {

          ByteBuffer buffer =
              actionBuilder.state()
                  .headerInterest(ETAG, TRANSFER_ENCODING, CONTENT_LENGTH)
                  .methodProtocol("GET")
                  .pathResCode(('/' + db + '/' + dbKeysBuilder.get(etype.view)).replace("//", "/"))
                  .protocolStatus("HTTP/1.1")
                  .asRequestHeaderByteBuffer();
          int wrote = channel.write(buffer);
          assert (!buffer.hasRemaining());
          key.interestOps(OP_READ);//READ immmmmediately follows WRITE in httpmethod.init loop
        }


        @Override
        public void onRead(SelectionKey key) throws Exception {

          //this AsioVisitor does one of 2 things - parse the addHeaderInterest and then parses the chunk length.  cursor instance is _owned_ by this method.
          int remaining;
          if (null != cursor) {
            remaining = channel.read(cursor);
            if (-1 == remaining) {
              joinPoint.reset();
              return;
            }
          } else {
            ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
            remaining = channel.read(dst);
            Rfc822HeaderState state = actionBuilder.state();
            final String s = state.apply((ByteBuffer) dst.flip()).pathResCode();
            if (!s.equals("200")) {
              joinPoint.reset();
            }
            cursor = dst.slice();
          }


          while (cursor.hasRemaining()) {
            byte b;
            final int cend = cursor.limit();
            final int cbegin = cursor.position();
            while (cursor.hasRemaining() & '\n' != (b = cursor.get())) ;//
            if (!cursor.hasRemaining()) {
              //recvbuffer starvation
              //rewind the slice and reque with a shiny new recvbuffer

              cursor = ByteBuffer.allocateDirect(getReceiveBufferSize()).put((ByteBuffer) cursor.rewind());
              key.selector().wakeup();
              return;
            } else {

              int i = Integer.parseInt(UTF8.decode((ByteBuffer) cursor.duplicate().position(cbegin).limit(cursor.position())).toString().trim(), 0x10);
              cursor = cursor.slice();
              if (0 == i) {
                EXECUTOR_SERVICE.submit(new Runnable() {
                  public void run() {
                    //handle for termination
                    int sum = 0;
                    for (ByteBuffer buffer : list) {
                      sum += buffer.rewind().remaining();
                    }
                    ByteBuffer allocate = ByteBuffer.allocateDirect(sum);
                    for (ByteBuffer byteBuffer : list) {
                      allocate.put(byteBuffer);
                    }
                    payload.set(UTF8.decode((ByteBuffer) allocate.rewind()).toString());
                    try {
                      joinPoint.await(); //-------------------------------------->V
                    } catch (InterruptedException e) {                          //V
                      e.printStackTrace();                                      //V
                    } catch (BrokenBarrierException e) {                        //V
                      e.printStackTrace();                                      //V
                    }                                                           //V
                  }                                                             //V
                });                                                             //V
                recycleChannel(channel);                                        //V
                return;                                                         //V
              }                                                                 //V
              else if (cursor.remaining() >= i) {
                ByteBuffer chunk = ByteBuffer.allocateDirect(i);
                chunk.put((ByteBuffer) cursor.slice().limit(i));
                list.add(chunk);
                cursor.position(cursor.position() + i);
                b = 0;
                while (cursor.hasRemaining() && '\n' != (b = cursor.get())) ;

              } else {
                cursor = ByteBuffer.allocateDirect(i).put(cursor);
                key.attach(new Impl() {
                  @Override
                  public void onRead(SelectionKey key) throws Exception {
                    int read = channel.read(cursor);
                    if (-1 != read) {
                      if (!cursor.hasRemaining()) {
                        list.add(cursor);
                        cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
                        key.attach(prev);//goto
                        return;
                      }
                    } else {
                      joinPoint.reset();
                      recycleChannel(channel);
                      return;
                    }
                  }
                });
                return;
              }
            }
          }                                                                   //V
        }                                                                     //V
      });                                                                     //V
      joinPoint.await(5L, getDefaultCollectorTimeUnit());//5 seconds query is enough.
      //todo: redo type magic from Colin
      String s = payload.get();
//      if (null == type) {
      return s;
//      }
//      return GSON.fromJson(s, type);


    }
  },
  @DbTask({tx, oneWay, rows, future, continuousFeed}) @DbKeys(value = {opaque, validjson}, optional = type)JsonSend {
    @Override
    public <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<CouchTx> payload = new AtomicReference<CouchTx>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      String opaque = '/' + ((String) dbKeysBuilder.get(etype.opaque)).replace("//", "/");
      String validjson = (String) dbKeysBuilder.get(etype.validjson);
      int lastSlashIndex = opaque.lastIndexOf('/');
      final byte[] outbound = validjson.getBytes(UTF8);
      final Rfc822HeaderState state = actionBuilder.state();
      final ByteBuffer header = state.addHeaderInterest(ETAG, CONTENT_LENGTH, CONTENT_ENCODING)
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
          if (null == cursor) {
            int write = channel.write(header);
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
          if (null == cursor) {
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
          int await = cyclicBarrier.await();                 //V
          recycleChannel(channel);                           //V
        }                                                    //V
      });                                                    //V
      cyclicBarrier.await(3L, BlobAntiPatternObject.getDefaultCollectorTimeUnit());

      CouchTx couchTx = payload.get();
      return couchTx;
    }
  },
  @DbTask({tx, future, oneWay}) @DbResultUnit(Rfc822HeaderState.class) @DbKeys({db, docId, opaque, mimetype, blob})BlobSend {
    @Override
    public <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<String> payload = new AtomicReference<String>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      CouchTx visit = (CouchTx) RevisionFetch.visit(dbKeysBuilder, actionBuilder);
      String rev = null == visit ? null : visit.rev();

      final ByteBuffer byteBuffer = actionBuilder.state().methodProtocol("PUT")
          .pathResCode(null == "/" + dbKeysBuilder.get(db) + '/' + dbKeysBuilder.get(etype.docId) + '/' + dbKeysBuilder.get(etype.opaque) + rev ? "" : ("?rev=" + rev))
          .protocolStatus("HTTP/1.1")
          .headerStrings(new TreeMap<String, String>() {{
            put("Expect", "100-continue");
            put(CONTENT_TYPE, ((MimeType) dbKeysBuilder.get(etype.mimetype)).contentType);
            put(ACCEPT, "*/*");
          }}).asRequestHeaderByteBuffer();


      final SocketChannel channel = createCouchConnection();
      HttpMethod.enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        public ByteBuffer cursor;

        @Override
        public void onRead(SelectionKey key) throws Exception {

          ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
          int read = channel.read(dst);
          String continueString = actionBuilder.state().apply((ByteBuffer) dst.flip()).pathResCode();

          if (continueString.startsWith("100")) {
            cursor = (ByteBuffer) dbKeysBuilder.get(etype.blob);
            key.interestOps(OP_WRITE).selector().wakeup();
          }


        }

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
                  System.err.println(deepToString(this, dbKeysBuilder, actionBuilder));
                  EXECUTOR_SERVICE.submit(new Runnable() {
                    @Override
                    public void run() {
                      try {
                        payload.set(UTF8.decode(dst.slice()).toString()); //todo: ignoring content-length here..  not actually sane.  testcase with getReceiveBufersize()=64 needed
                        cyclicBarrier.await();                              //V
                        recycleChannel(channel);                            //V
                      } catch (Throwable e) {                               //V
                        try {                                               //V
                          channel.socket().close();                         //V
                        } catch (IOException e1) {                          //V
                        }                                                   //V
                        e.printStackTrace();                                //V
                      }                                                     //V
                    }                                                       //V
                  });                                                       //V
                }                                                           //V
              });                                                           //V
            }                                                               //V
          }                                                                 //V
          int write = channel.write((ByteBuffer) byteBuffer.rewind());      //V
          key.interestOps(OP_READ).selector().wakeup();                     //V
        }                                                                   //V
      });                                                                   //V
      cyclicBarrier.await(3L, getDefaultCollectorTimeUnit());                //V
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
  public static final String SET_COOKIE = "Set-Cookie";

  static <T> String idpath(DbKeysBuilder<T> dbKeysBuilder, etype etype) {
    String db = (String) dbKeysBuilder.get(DbKeys.etype.db);
    String id = (String) dbKeysBuilder.get(etype);
    return '/' + db + '/' + id;
  }


  public <T> Object visit() throws Exception {
    DbKeysBuilder<T> dbKeysBuilder = (DbKeysBuilder<T>) DbKeysBuilder.get();
    ActionBuilder<T> actionBuilder = ActionBuilder.get();

    if (dbKeysBuilder.validate())
      return visit(dbKeysBuilder, actionBuilder);
    throw new Error("validation error");
  }

  /*abstract */
  public <T> Object visit(DbKeysBuilder<T> dbKeysBuilder,
                          ActionBuilder<T> actionBuilder) throws Exception {
    throw new AbstractMethodError();
  }

  public static final String PCOUNT = "-0xdeadbeef.2";
  public static final String GENERATED_METHODS = "/*XXXXXXXXXXXXXXMETHODS*/";
  public static final String IFACE_FIRE_TARGETS = "/*FIRE_IFACE*/";
  public static final String FIRE_METHODS = "/*BAKED_IN_FIRE*/";


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
      if (0 != rtype.getTypeParameters().length) {
        StringBuilder params = new StringBuilder("<");
        StringBuilder bounds = new StringBuilder("<");
        for (TypeVariable<? extends GenericDeclaration> param : rtype.getTypeParameters()) {
          params.append(param.getName()).append(",");
          bounds.append(param.getName()).append(" extends ").append(((Class<?>) param.getBounds()[0]).getName()).append(",");
        }
        params.deleteCharAt(params.length() - 1).append(">");
        bounds.deleteCharAt(bounds.length() - 1).append(">");
        rtypeTypeParams = params.toString();
        rtypeBounds = bounds.toString();
      }
      String fqsn = rtype.getCanonicalName();
      String pfqsn = fqsn + rtypeTypeParams;
      @Language("JAVA") String s2 = "public class _ename_" +
          rtypeTypeParams + " extends DbKeysBuilder<" +
          pfqsn + "> {\n  private _ename_() {\n  }\n\n  static public " +
          rtypeBounds + " _ename_" +
          rtypeTypeParams + "\n\n  $() {\n    return new _ename_" + rtypeTypeParams + "();\n  }\n\n" +
          "  public interface _ename_TerminalBuilder" + rtypeTypeParams + " extends TerminalBuilder<" +
          pfqsn + "> {    " +
          IFACE_FIRE_TARGETS + "\n  }\n\n  public class _ename_ActionBuilder extends ActionBuilder<" +
          pfqsn + "> {\n    public _ename_ActionBuilder() {\n      super();\n    }\n\n    @Override\n    public _ename_TerminalBuilder" +
          rtypeTypeParams + " fire() {\n      return new _ename_TerminalBuilder" +
          rtypeTypeParams + "() {        " +
          FIRE_METHODS + "\n      };\n    }\n\n    @Override\n    " +
          "public _ename_ActionBuilder state(Rfc822HeaderState state) {\n      " +
          "return (_ename_ActionBuilder) super.state(state);\n    " +
          "}\n\n    @Override\n    public _ename_ActionBuilder key(java.nio.channels.SelectionKey key) " +
          "{\n      return (_ename_ActionBuilder) super.key(key);\n    }\n  }\n\n  @Override\n  public _ename_ActionBuilder to() " +
          "{\n    if (parms.size() >= parmsCount) return new _ename_ActionBuilder();\n    " +
          "throw new IllegalArgumentException(\"required parameters are: " +
          arrToString(parms) + "\");\n  }  " +
          GENERATED_METHODS + "\n" +
          "}";
      s = s2;
      int vl = parms.length;
      String s1 = "\nstatic private final int parmsCount=" + PCOUNT + ";\n";
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
          s = s.replace(FIRE_METHODS, t).replace(IFACE_FIRE_TARGETS, iface);
        }
      }

      s = s.replace(GENERATED_METHODS, s1).replace("_ename_", name()).replace(PCOUNT, String.valueOf(vl));
    }
    return s;
  }


  private String writeParameterSetter(String rtypeTypeParams, String s1, etype etype,
                                      Class<?> clazz) {
    @Language("JAVA") String y = "public _ename_" + rtypeTypeParams + "  _name_(_clazz_ _sclazz_){parms.put(DbKeys.etype." + etype.name() + ",_sclazz_);return this;}\n";
    s1 += y.replace("_name_", etype.name()).replace("_clazz_", clazz.getCanonicalName()).replace("_sclazz_", clazz.getSimpleName().toLowerCase() + "Param").replace("_ename_", name());
    return s1;
  }

  public static void main(String... args) throws NoSuchFieldException {
    Field[] fields = CouchMetaDriver.class.getFields();
    @Language("JAVA")
    String s = "package rxf.server.gen;\n//generated\n  \nimport java.lang.reflect.ParameterizedType;\nimport java.lang.reflect.Type;\nimport java.nio.ByteBuffer;\nimport java.nio.channels.SelectionKey;\nimport java.util.concurrent.Callable;\nimport java.util.concurrent.Future;\n\nimport rxf.server.*;\nimport rxf.server.an.*;\nimport rxf.server.driver.*;\n/**\n * generated drivers\n */\npublic interface CouchDriver{";
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


}

