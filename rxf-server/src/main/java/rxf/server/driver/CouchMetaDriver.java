package rxf.server.driver;

import java.lang.reflect.*;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

import one.xio.AsioVisitor.Impl;
import one.xio.*;
import org.intellij.lang.annotations.Language;
import rxf.server.*;
import rxf.server.Rfc822HeaderState.HttpRequest;
import rxf.server.Rfc822HeaderState.HttpResponse;
import rxf.server.an.DbKeys;
import rxf.server.an.DbKeys.etype;
import rxf.server.an.DbTask;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpHeaders.Accept;
import static one.xio.HttpHeaders.Content$2dEncoding;
import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpHeaders.Content$2dType;
import static one.xio.HttpHeaders.ETag;
import static one.xio.HttpHeaders.Transfer$2dEncoding;
import static one.xio.HttpMethod.DELETE;
import static one.xio.HttpMethod.GET;
import static one.xio.HttpMethod.HEAD;
import static one.xio.HttpMethod.POST;
import static one.xio.HttpMethod.PUT;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.enqueue;
import static rxf.server.BlobAntiPatternObject.DEBUG_SENDJSON;
import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.arrToString;
import static rxf.server.BlobAntiPatternObject.createCouchConnection;
import static rxf.server.BlobAntiPatternObject.deepToString;
import static rxf.server.BlobAntiPatternObject.getDefaultCollectorTimeUnit;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;
import static rxf.server.BlobAntiPatternObject.recycleChannel;
import static rxf.server.DbTerminal.continuousFeed;
import static rxf.server.DbTerminal.future;
import static rxf.server.DbTerminal.json;
import static rxf.server.DbTerminal.oneWay;
import static rxf.server.DbTerminal.pojo;
import static rxf.server.DbTerminal.rows;
import static rxf.server.DbTerminal.tx;
import static rxf.server.an.DbKeys.etype.db;
import static rxf.server.an.DbKeys.etype.designDocId;
import static rxf.server.an.DbKeys.etype.docId;
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
    public <T> ByteBuffer visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      final Rfc822HeaderState state = actionBuilder.state();
      final ByteBuffer header = state.$req().method(PUT).path("/" + dbKeysBuilder.get(db))
          .headerString(Content$2dLength, "0")
          .asRequestHeaderByteBuffer();
      final SocketChannel channel = createCouchConnection();
      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor;


        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write(header);
          key.interestOps(OP_READ).selector().wakeup();
        }


        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
            int read = channel.read(cursor);
            Rfc822HeaderState httpResponse = state.$res();
            httpResponse
                .headerInterest(Content$2dLength.getHeader())
                .apply((ByteBuffer) cursor.flip());
            if (DEBUG_SENDJSON) {
              System.err.println(deepToString(state.pathResCode(), state, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }

            int remaining = Integer.parseInt(state.headerString(Content$2dLength));

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
          payload.set(cursor);
          recycleChannel(channel);
          EXECUTOR_SERVICE.submit(new Runnable() {

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
      int await = cyclicBarrier.await(3L, getDefaultCollectorTimeUnit());
      return payload.get();
    }
  },
  @DbTask({tx, oneWay}) @DbKeys({db})DbDelete {
    public <T> ByteBuffer visit(DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      final ByteBuffer header = (ByteBuffer) actionBuilder
          .state()
          .$req()
          .method(DELETE)
          .pathResCode("/" + dbKeysBuilder.get(db))
          .as(ByteBuffer.class);
      final SocketChannel channel = createCouchConnection();
      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor;


        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write(header);
          key.interestOps(OP_READ).selector().wakeup();
        }


        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
            int read = channel.read(cursor);

            Rfc822HeaderState state = actionBuilder.state();
            state.headerInterest(Content$2dLength.getHeader()).apply((ByteBuffer) cursor.flip());
            if (DEBUG_SENDJSON) {
              System.err.println(deepToString(state.pathResCode(), state, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }

            int remaining = Integer.parseInt(state.headerString(Content$2dLength));

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
          payload.set(cursor);
          EXECUTOR_SERVICE.submit(new Runnable() {

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
      int await = cyclicBarrier.await(3L, getDefaultCollectorTimeUnit());
      return payload.get();
    }
  },

  @DbTask({pojo, future, json}) @DbKeys({db, docId})DocFetch {
    public <T> ByteBuffer visit(DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();


      String db = (String) dbKeysBuilder.get(etype.db);
      String id = (String) dbKeysBuilder.get(docId);
      /*final Rfc822HeaderState state = */
      HttpRequest request = actionBuilder.state().$req();
      request
          .path(scrub("/" + db + (null == id ? "" : "/" + id)))
          .method(GET)
          .addHeaderInterest(Content$2dLength.getHeader());
      final SocketChannel channel = createCouchConnection();
      final ByteBuffer as = (ByteBuffer) request.$req().as(ByteBuffer.class);
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      enqueue(channel, OP_CONNECT | OP_WRITE, new Impl() {
        public ByteBuffer cursor;


        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write(as);
          key.interestOps(OP_READ).selector().wakeup();
        }


        public void onRead(SelectionKey key) throws Exception {
          if (null != cursor) {
            int read = channel.read(cursor);
            if (-1 == read) {
              channel.socket().close();
            }
            if (!cursor.hasRemaining()) {
              System.err.println("*********  " + UTF8.decode((ByteBuffer) cursor.duplicate().rewind()));
              deliver();
            }

          }
          ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
          int read = channel.read(dst);

          HttpResponse state = actionBuilder.state().apply((ByteBuffer) dst.flip()).$res();
          HttpStatus httpStatus = state.statusEnum();
          switch (httpStatus) {
            case $200:
//            case $201:
              int remaining = Integer.parseInt(state.headerString(Content$2dLength));
              if (remaining == dst.remaining()) {
                cursor = dst.slice();
                deliver();
                return;
              }
              cursor = ByteBuffer.allocate(remaining).put(dst);
            default: //error
              cyclicBarrier.reset();
          }
        }

        private void deliver() {
          assert null != cursor;
          payload.set((ByteBuffer) cursor.rewind());

          recycleChannel(channel);
          EXECUTOR_SERVICE.submit(new Runnable() {

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
        cyclicBarrier.await(3L, getDefaultCollectorTimeUnit());
      } catch (BrokenBarrierException ex) {
        // non-200 error code
      }

      return payload.get();
    }
  },


  @DbTask({json, future}) @DbKeys({db, docId})RevisionFetch {
    public <T> ByteBuffer visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {

      Object id = dbKeysBuilder.get(docId);
      String scrubMe = "/" + dbKeysBuilder.get(db) + (null != id ? "/" + id : "");
      String path = scrub(scrubMe);
      final Rfc822HeaderState state = actionBuilder.state().headerInterest(ETag.getHeader())
          .$req().method(HEAD)
          .path(path);
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      final SocketChannel channel = createCouchConnection();
      HttpMethod.enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {

        public void onWrite(SelectionKey key) throws Exception {
          ByteBuffer as = (ByteBuffer) state.$req().as(ByteBuffer.class);
          int write = channel.write(as);
          assert !as.hasRemaining();
          key.interestOps(OP_READ);
        }


        public void onRead(SelectionKey key) throws Exception {
          final ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
          int read = channel.read(dst);
          final int[] ints = HttpHeaders.getHeaders((ByteBuffer) dst.flip()).get(ETag.getHeader());
          assert -1 != read;
          EXECUTOR_SERVICE.submit(new Callable<Object>() {
            public Object call() throws Exception {
              try {
                //assumes quoted
                payload.set(((ByteBuffer) dst.duplicate().limit(ints[1] - 3).position(ints[0] + 2)).slice());
                cyclicBarrier.await();          //V
                return null;                    //V
              } catch (Exception e) {           //V
                cyclicBarrier.reset();          //V
              } finally {                       //V
                recycleChannel(channel);        //V
              }                                 //V
              return null;                      //V
            }                                   //V
          });                                   //V
        }                                         //V
      });
      try {
        cyclicBarrier.await(3, getDefaultCollectorTimeUnit());
      } catch (Exception e) {

      }
      return payload.get();
    }
  },
  @DbTask({tx, oneWay, future}) @DbKeys(value = {db, validjson}, optional = {docId, rev})DocPersist {
    public <T> ByteBuffer visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {

      String db = (String) dbKeysBuilder.get(etype.db);
      String docId = (String) dbKeysBuilder.get(etype.docId);
      String rev = (String) dbKeysBuilder.get(etype.rev);
      String sb = scrub('/' + db + (null == docId ? "" : '/' + docId + (null == rev ? "" : "?rev=" + rev)));
      dbKeysBuilder.put(opaque, sb);
      actionBuilder.state().$req().headerString(HttpHeaders.Content$2dType, MimeType.json.contentType);
      return JsonSend.visit(dbKeysBuilder, actionBuilder);
    }
  },
  @DbTask({tx, oneWay, future}) @DbKeys(value = {db, docId, rev})DocDelete {
    public <T> ByteBuffer visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      final HttpRequest request = actionBuilder.state().$req();
      final ByteBuffer header = (ByteBuffer) request
          .path(scrub("/" + dbKeysBuilder.get(db) + "/" + dbKeysBuilder.get(docId) + "?rev=" + dbKeysBuilder.get(rev)))
          .method(DELETE)
          .as(ByteBuffer.class);
      final SocketChannel channel = createCouchConnection();
      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor;


        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write(header);
          key.interestOps(OP_READ).selector().wakeup();
        }


        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
            int read = channel.read(cursor);
            request.headerInterest(Content$2dLength.getHeader()).apply((ByteBuffer) cursor.flip());
            if (DEBUG_SENDJSON) {
              System.err.println(deepToString(request.pathResCode(), request, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }

            int remaining = Integer.parseInt(request.headerString(Content$2dLength));

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
          payload.set(cursor);
          recycleChannel(channel);
          EXECUTOR_SERVICE.submit(new Runnable() {

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
      int await = cyclicBarrier.await(3L, getDefaultCollectorTimeUnit());
      return payload.get();
    }
  },
  @DbTask({pojo, future, json}) @DbKeys({db, designDocId})DesignDocFetch {
    public <T> ByteBuffer visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {
      dbKeysBuilder.put(docId, dbKeysBuilder.remove(designDocId));
      return DocFetch.visit(dbKeysBuilder, actionBuilder);
    }
  },
  @DbTask({rows, future, continuousFeed}) @DbKeys(value = {db, view}, optional = type)ViewFetch {
    public <T> ByteBuffer visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();
      final CyclicBarrier joinPoint = new CyclicBarrier(2);
      final String db = scrub('/' + (String) dbKeysBuilder.get(etype.db));
      Class<T> type = dbKeysBuilder.get(etype.type);
      final SocketChannel channel = createCouchConnection();
      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor;
        List<ByteBuffer> list = new ArrayList<ByteBuffer>();
        final Impl prev = this;
        private String attempt;


        public void onWrite(SelectionKey key) throws Exception {


          HttpRequest request = actionBuilder.state().$req();
          ByteBuffer buffer =
              (ByteBuffer) request
                  .method(GET)
                  .path(scrub('/' + db + '/' + dbKeysBuilder.get(view)))
                  .headerString(Accept, MimeType.json.contentType)
                  .headerInterest(
                      ETag.getHeader(),
                      Transfer$2dEncoding.getHeader(),
                      Content$2dLength.getHeader())
                  .as(ByteBuffer.class);
          attempt = request.toString();
          int wrote = channel.write(buffer);
          assert !buffer.hasRemaining();
          key.interestOps(OP_READ);//READ immediately follows WRITE in httpmethod.init loop
        }


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

            Rfc822HeaderState httpResponse = actionBuilder.state()
                .$res();

            HttpStatus httpStatus = httpResponse
                .apply((ByteBuffer) dst.flip())
                .$res().statusEnum();
            switch (httpStatus) {
              case $200:
                break;
              default:
                joinPoint.reset();
            }
            cursor = dst.slice();
          }


          while (cursor.hasRemaining()) {
            int cbegin = cursor.position();
            while (cursor.hasRemaining() && '\n' != cursor.get()) ;//
            if (cursor.hasRemaining()) {

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
                    payload.set((ByteBuffer) allocate.rewind());
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
                while (cursor.hasRemaining() && '\n' != cursor.get()) ;

              } else {
                cursor = ByteBuffer.allocateDirect(i).put(cursor);
                key.attach(new Impl() {

                  public void onRead(SelectionKey key) throws Exception {
                    int read = channel.read(cursor);
                    if (-1 == read) {
                      joinPoint.reset();
                      recycleChannel(channel);
                    } else {
                      if (!cursor.hasRemaining()) {
                        list.add(cursor);
                        cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
                        key.attach(prev);//goto
                      }
                    }
                  }
                });
                return;
              }
            } else {
              //recvbuffer starvation
              //rewind the slice and reque with a shiny new recvbuffer

              cursor = ByteBuffer.allocateDirect(getReceiveBufferSize()).put((ByteBuffer) cursor.rewind());
              key.selector().wakeup();
              return;
            }
          }                                                                   //V
        }                                                                     //V
      });                                                                     //V
      try {
        joinPoint.await(5L, getDefaultCollectorTimeUnit());//5 seconds query is enough.
      } catch (Exception e) {
        e.printStackTrace();  //todo: verify for a purpose
        System.err.println("\tfrom:");
        dbKeysBuilder.trace().printStackTrace();
      }
      return payload.get();
    }
  },
  //training day for the Terminal rewrites

  @DbTask({tx, oneWay, rows, json, future, continuousFeed}) @DbKeys(value = {opaque, validjson}, optional = type)JsonSend {
    public <T> ByteBuffer visit(DbKeysBuilder<T> dbKeysBuilder, ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      String opaque = scrub('/' + (String) dbKeysBuilder.get(etype.opaque));
      int lastSlashIndex = opaque.lastIndexOf('/');
      if (opaque.length() - 1 == lastSlashIndex) {
        opaque = opaque.substring(0, opaque.length() - 1);
      }
      int c = 0;
      label:
      for (int i = 0; i < opaque.length(); i++) {
        char c1 = opaque.charAt(i);
        switch (c1) {
          case '?':
          case '#':
            break label;
          case '/':
            c++;
          default:
            break;

        }
      }
      String validjson = (String) dbKeysBuilder.get(etype.validjson);
      validjson = validjson == null ? "{}" : validjson;

      final Rfc822HeaderState state = actionBuilder.state();
      HttpRequest httpRequest = state.$req();
      final byte[] outbound = validjson.getBytes(UTF8);


      final HttpMethod method = 1 == c || !(lastSlashIndex < opaque.lastIndexOf('?') && lastSlashIndex != opaque.indexOf('/')) ? POST : PUT;
      final ByteBuffer header = (ByteBuffer) httpRequest.method(method)
          .path(opaque)
          .headerInterest(ETag.getHeader(), Content$2dLength.getHeader(), Content$2dEncoding.getHeader())
          .headerString(Content$2dLength, String.valueOf(outbound.length))
          .headerString(Accept, MimeType.json.contentType)
          .headerString(Content$2dType, MimeType.json.contentType)
          .as(ByteBuffer.class);
      if (DEBUG_SENDJSON) {
        System.err.println(deepToString(opaque, validjson, UTF8.decode(header.duplicate()), state));
      }
      final SocketChannel channel = createCouchConnection();
      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor;


        public void onWrite(SelectionKey key) throws Exception {
          if (null == cursor) {
            int write = channel.write(header);
            cursor = ByteBuffer.wrap(outbound);
          }
          int write = channel.write(cursor);
          if (!cursor.hasRemaining()) {
            key.interestOps(OP_READ);
            cursor = null;
          }
        }


        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
            int read = channel.read(cursor);
            state.headerStrings().clear();
            state.apply((ByteBuffer) cursor.flip());
            if (DEBUG_SENDJSON) {
              System.err.println(deepToString(state.pathResCode(), state, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }

            int remaining = Integer.parseInt(state.headerString(Content$2dLength));

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
          payload.set(cursor);
          EXECUTOR_SERVICE.submit(new Callable<Object>() {
            public Object call() throws Exception {
              cyclicBarrier.await();                 //V
              return null;
            }
          });
          recycleChannel(channel);                           //V
        }                                                    //V
      });                                                    //V
      cyclicBarrier.await(3L, getDefaultCollectorTimeUnit());

      return payload.get();
    }
  },

// TODO:
// @DbTask({tx, future, oneWay})  @DbKeys({db, docId, opaque, mimetype, blob})BlobSend {
//    
//    public <T> Object visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
//      final AtomicReference<String> payload = new AtomicReference<String>();
//      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
//      CouchTx visit = (CouchTx) RevisionFetch.visit(dbKeysBuilder, actionBuilder);
//      String rev = null == visit ? null : visit.rev();
//
//      final ByteBuffer byteBuffer = actionBuilder.state().methodProtocol("PUT")
//          .pathResCode(null == "/" + dbKeysBuilder.get(db) + '/' + dbKeysBuilder.get(etype.docId) + '/' + dbKeysBuilder.get(etype.opaque) + rev ? "" : "?rev=" + rev)
//          .protocolStatus("HTTP/1.1")
//          .headerStrings(new TreeMap<String, String>() {{
//            put("Expect", "100-continue");
//            put(CONTENT_TYPE, ((MimeType) dbKeysBuilder.get(etype.mimetype)).contentType);
//            put(ACCEPT, "*/*");
//          }}).asRequestHeaderByteBuffer();
//
//
//      final SocketChannel channel = createCouchConnection();
//      HttpMethod.enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
//        public ByteBuffer cursor;
//
//        
//        public void onRead(SelectionKey key) throws Exception {
//
//          ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
//          int read = channel.read(dst);
//          String continueString = actionBuilder.state().apply((ByteBuffer) dst.flip()).pathResCode();
//
//          if (continueString.startsWith("100")) {
//            cursor = (ByteBuffer) dbKeysBuilder.get(etype.blob);
//            key.interestOps(OP_WRITE).selector().wakeup();
//          }
//
//
//        }
//
//        
//        public void onWrite(SelectionKey key) throws Exception {
//          if (null != cursor) {
//            int write = channel.write(cursor);
//            if (-1 == write || !cursor.hasRemaining()) {
//              key.interestOps(OP_READ).selector().wakeup();
//              cyclicBarrier.reset();
//              key.attach(new Impl() {
//                
//                public void onRead(final SelectionKey key) throws Exception {
//
//                  final ByteBuffer dst = ByteBuffer.allocateDirect(getReceiveBufferSize());
//                  int read = channel.read(dst);
//                  actionBuilder.state().apply((ByteBuffer) dst.flip());
//                  System.err.println(deepToString(this, dbKeysBuilder, actionBuilder));
//                  EXECUTOR_SERVICE.submit(new Runnable() {
//                    
//                    public void run() {
//                      try {
//                        payload.set(UTF8.decode(dst.slice()).toString()); //todo: ignoring content-length here..  not actually sane.  testcase with getReceiveBufersize()=64 needed
//                        cyclicBarrier.await();                              //V
//                        recycleChannel(channel);                            //V
//                      } catch (Throwable e) {                               //V
//                        try {                                               //V
//                          channel.socket().close();                         //V
//                        } catch (IOException e1) {                          //V
//                        }                                                   //V
//                        e.printStackTrace();                                //V
//                      }                                                     //V
//                    }                                                       //V
//                  });                                                       //V
//                }                                                           //V
//              });                                                           //V
//            }                                                               //V
//          }                                                                 //V
//          int write = channel.write((ByteBuffer) byteBuffer.rewind());      //V
//          key.interestOps(OP_READ).selector().wakeup();                     //V
//        }                                                                   //V
//      });                                                                   //V
//      cyclicBarrier.await(3L, getDefaultCollectorTimeUnit());                //V
//      return payload;
//    }
//
//
//  }
  ;

  public static String scrub(String scrubMe) {
    return null == scrubMe ? null : scrubMe.trim().replace("//", "/").replace("..", ".");
  }

  public static final String[] EMPTY = new String[0];


  public <T> ByteBuffer visit() throws Exception {
    DbKeysBuilder<T> dbKeysBuilder = (DbKeysBuilder<T>) DbKeysBuilder.get();
    ActionBuilder<T> actionBuilder = ActionBuilder.get();

    if (!dbKeysBuilder.validate()) {

      throw new Error("validation error");
    }
    return visit(dbKeysBuilder, actionBuilder);
  }

  /*abstract */
  public <T> ByteBuffer visit(DbKeysBuilder<T> dbKeysBuilder,
                              ActionBuilder<T> actionBuilder) throws Exception {
    throw new AbstractMethodError();
  }

  public static final String PCOUNT = "-0xdeadbeef.2";
  public static final String GENERATED_METHODS = "/*generated ethods kadfsljhk*/";
  public static final String IFACE_FIRE_TARGETS = "/*fire intereface lasjhfkjas h*/";
  public static final String FIRE_METHODS = "/*embedded fire terminals alkjdssssf hasdlh*/";


  public <T> String builder() throws NoSuchFieldException {
    Field field = CouchMetaDriver.class.getField(name());


    String s = null;
    if (field.getType().isAssignableFrom(CouchMetaDriver.class)) {
      CouchMetaDriver couchDriver = CouchMetaDriver.valueOf(field.getName());

      etype[] parms = field.getAnnotation(DbKeys.class).value();
      etype[] optionalParams = field.getAnnotation(DbKeys.class).optional();
      Class<?> rtype = CouchTx.class;
      try {
        rtype = ByteBuffer.class;
      } catch (Exception e) {
      }
      String rtypeTypeParams = "";
      String rtypeBounds = "";
      if (0 != rtype.getTypeParameters().length) {

        boolean first = true;

        String s0 = "";
        String s1 = "";
        for (int i = 0; i < rtype.getTypeParameters().length; i++) {
          TypeVariable<? extends GenericDeclaration> classTypeVariable = rtype.getTypeParameters()[i];

          Type[] bounds = classTypeVariable.getBounds();
          if (0 != i || !Object.class.equals(bounds[0])) {
            if (first) {
              first = false;
              s0 = "<";
              s1 = "<";
            } else {
              s0 += ',';
              s1 += ',';
            }
            s0 += bounds[0];
            s1 += classTypeVariable.getName();
          }

        }
        if (!first) {
          s0 += '>';
          s1 += '>';
          rtypeTypeParams = s0;
          rtypeBounds = s1;
        }
      }
      String fqsn = rtype.getCanonicalName();
      String pfqsn = fqsn + rtypeTypeParams;
      s = "public class _ename_" +
          rtypeTypeParams + " extends DbKeysBuilder<" + pfqsn + "> {\n  private _ename_() {\n  }\n\n  static public " +
          rtypeBounds + " _ename_" + rtypeTypeParams + "\n\n  $() {\n    return new _ename_" + rtypeTypeParams +
          "();\n  }\n\n  public interface _ename_TerminalBuilder" +
          rtypeTypeParams + " extends TerminalBuilder<" + pfqsn + "> {    " + IFACE_FIRE_TARGETS +
          "\n  }\n\n  public class _ename_ActionBuilder extends ActionBuilder<" + pfqsn
          + "> {\n    public _ename_ActionBuilder() {\n      super();\n    }\n\n    \n    public _ename_TerminalBuilder" +
          rtypeTypeParams + " fire() {\n      return new _ename_TerminalBuilder" +
          rtypeTypeParams + "() {        \n      " + FIRE_METHODS + "\n      };\n    }\n\n    \n    " +
          "public _ename_ActionBuilder state(Rfc822HeaderState state) {\n      " +
          "return (_ename_ActionBuilder) super.state(state);\n    " +
          "}\n\n    \n    public _ename_ActionBuilder key(java.nio.channels.SelectionKey key) " +
          "{\n      return (_ename_ActionBuilder) super.key(key);\n    }\n  }\n\n  \n  public _ename_ActionBuilder to() " +
          "{\n    if (parms.size() >= parmsCount) return new _ename_ActionBuilder();\n    " +
          "throw new IllegalArgumentException(\"required parameters are: " + arrToString(parms) +
          "\");\n  } \n   \n   " +
          GENERATED_METHODS + "\n" +
          "}";
      int vl = parms.length;
      String s1 = "\nstatic private final int parmsCount=" + PCOUNT + ";\n";
      for (etype etype : parms) {
        s1 = writeParameterSetter(rtypeTypeParams, s1, etype, etype.clazz);
      }
      for (etype etype : optionalParams) {
        s1 = writeParameterSetter(rtypeTypeParams, s1, etype, etype.clazz);
      }

      DbTask annotation = field.getAnnotation(DbTask.class);
      if (null != annotation) {
        DbTerminal[] terminals = annotation.value();
        String t = "", iface = "";
        for (DbTerminal terminal : terminals) {
          iface += terminal.builder(couchDriver, parms, false);
          t += terminal.builder(couchDriver, parms, true);

        }
        s = s.replace(FIRE_METHODS, t).replace(IFACE_FIRE_TARGETS, iface);
      }

      s = s.replace(GENERATED_METHODS, s1).replace("_ename_", name()).replace(PCOUNT, String.valueOf(vl));
    }
    return s;
  }


  private String writeParameterSetter(String rtypeTypeParams, String s1, etype etype,
                                      Class<?> clazz) {
    @Language("JAVA") String y = "public _ename_" + rtypeTypeParams +
        "  _name_(_clazz_ _sclazz_){parms.put(DbKeys.etype." + etype.name() + ",_sclazz_);return this;}\n";
    s1 += y.replace("_name_", etype.name()).replace("_clazz_", clazz.getCanonicalName()).replace("_sclazz_", clazz.getSimpleName().toLowerCase() + "Param").replace("_ename_", name());
    return s1;
  }

  public static void main(String... args) throws NoSuchFieldException {
    Field[] fields = CouchMetaDriver.class.getFields();
    @Language("JAVA")
    String s = "package rxf.server.gen;\n//generated\n  \n\nimport rxf.server.*;\nimport rxf.server.an.*;\nimport rxf.server.driver.*;\nimport java.lang.reflect.ParameterizedType;\nimport java.lang.reflect.Type;\nimport java.nio.ByteBuffer;\nimport java.nio.channels.SelectionKey;\nimport java.util.concurrent.Callable;\nimport java.util.concurrent.Future;\n\n\nimport static rxf.server.BlobAntiPatternObject.*;\n/**\n * generated drivers\n */\npublic interface CouchDriver{";
    for (Field field : fields)
      if (field.getType().isAssignableFrom(CouchMetaDriver.class)) {
        CouchMetaDriver couchDriver = CouchMetaDriver.valueOf(field.getName());
        DbKeys dbKeys = field.getAnnotation(DbKeys.class);
        etype[] value = dbKeys.value();

        s += ByteBuffer.class.getCanonicalName();
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
    s += "}";
    System.out.println(s);
  }


}

