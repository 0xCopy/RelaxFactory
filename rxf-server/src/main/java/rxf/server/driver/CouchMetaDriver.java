package rxf.server.driver;

import java.io.IOException;
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
 * inserts them as the apropriate state for lower level visit(builder1...buildern) method
 * <p/>
 * <h2>{@link one.xio.AsioVisitor} visitor  sub-threads in threadpools must be one of:</h2><ol>
 * <li>inner classes using the <u>final</u> paramters passed in
 * via {@link #visit(rxf.server.DbKeysBuilder, rxf.server.ActionBuilder)}</li>
 * <li>fluent class interface agnostic(highly unlikely)</li>
 * <li>arduously carried in (same as first option but not as clean as inner class refs)</li>
 * </ol>
 * User: jim
 * Date: 5/24/12
 * Time: 3:09 PM
 */
@SuppressWarnings({"RedundantCast"})
public enum CouchMetaDriver {

  @DbTask({tx, oneWay}) @DbKeys({db})DbCreate {
    public <T> ByteBuffer visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
//      final Rfc822HeaderState state = actionBuilder.state();
//      final ByteBuffer header = state.$req().method(PUT).path("/" + dbKeysBuilder.get(db))
//          .headerString(Content$2dLength, "0")
//          .asRequestHeaderByteBuffer();
      final SocketChannel channel = createCouchConnection();
      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        // *******************************
        // *******************************
        // pathological buffersize traits
        // *******************************
        // *******************************


        String db = (String) dbKeysBuilder.get(etype.db);
        String id = (String) dbKeysBuilder.get(docId);
        HttpRequest request = actionBuilder.state().$req();
        private HttpResponse response;
        ByteBuffer header = (ByteBuffer) request
            .method(PUT).path("/" + (db))
//          .headerString(Content$2dLength, "0")
            .as(ByteBuffer.class);

        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write(header);
          assert !header.hasRemaining();
          header.clear();
          response = request.headerInterest(STATIC_JSON_SEND_HEADERS).$res();

          key.interestOps(OP_READ);/*WRITE-READ implicit turnaround in 1xio won't need .selector().wakeup()*/
        }

        ByteBuffer cursor;

        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            //geometric,  vulnerable to dev/null if not max'd here.
            header = (null == header) ? ByteBuffer.allocateDirect(getReceiveBufferSize()) : header.hasRemaining() ? header : ByteBuffer.allocateDirect(header.capacity() * 2).put((ByteBuffer) header.flip());

            int read = channel.read(header);
            ByteBuffer flip = (ByteBuffer) header.duplicate().flip();
            response.apply((ByteBuffer) flip);

            if (BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, response.headerBuf())) {
              cursor = (ByteBuffer) flip.slice();
              header = null;
            } else {
              return;
            }


            if (DEBUG_SENDJSON) {
              System.err.println(deepToString(response.statusEnum(), response, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }


            HttpStatus httpStatus = response.statusEnum();
            switch (httpStatus) {
              case $200:
              case $201:
                int remaining = Integer.parseInt(response.headerString(Content$2dLength));


                if (remaining == cursor.remaining()) {
                  deliver();
                } else {
                  cursor = ByteBuffer.allocate(remaining).put(cursor);
                }
                break;
              default: //error
                cyclicBarrier.reset();
            }
          } else {
            int read = channel.read(cursor);
            if (-1 == read) cyclicBarrier.reset();
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
    public <T> ByteBuffer visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);

      final SocketChannel channel = createCouchConnection();

      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        final HttpRequest request = actionBuilder
            .state()
            .$req();
        ByteBuffer header = (ByteBuffer) request
            .method(DELETE)
            .pathResCode("/" + dbKeysBuilder.get(db))
            .as(ByteBuffer.class);
        ByteBuffer cursor;
        public HttpResponse response;

        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write(header);
          assert !header.hasRemaining();
          header.clear();
          response = request.headerInterest(STATIC_JSON_SEND_HEADERS).$res();

          key.interestOps(OP_READ);/*WRITE-READ implicit turnaround in 1xio won't need .selector().wakeup()*/
        }


        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            //geometric,  vulnerable to dev/null if not max'd here.
            header = (null == header) ? ByteBuffer.allocateDirect(getReceiveBufferSize()) : header.hasRemaining() ? header : ByteBuffer.allocateDirect(header.capacity() * 2).put((ByteBuffer) header.flip());

            int read = channel.read(header);
            ByteBuffer flip = (ByteBuffer) header.duplicate().flip();
            response.apply((ByteBuffer) flip);

            if (BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, response.headerBuf())) {
              cursor = (ByteBuffer) flip.slice();
              header = null;
            } else {
              return;
            }


            if (DEBUG_SENDJSON) {
              System.err.println(deepToString(response.statusEnum(), response, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }


            HttpStatus httpStatus = response.statusEnum();
            switch (httpStatus) {
              case $200:
                int remaining = Integer.parseInt(response.headerString(Content$2dLength));


                if (remaining == cursor.remaining()) {
                  deliver();
                } else {
                  cursor = ByteBuffer.allocate(remaining).put(cursor);
                }
                break;
              default: //error
                cyclicBarrier.reset();
            }
          } else {
            int read = channel.read(cursor);
            if (-1 == read) cyclicBarrier.reset();
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
    public <T> ByteBuffer visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();

      final SocketChannel channel = createCouchConnection();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      enqueue(channel, OP_CONNECT | OP_WRITE, new Impl() {
        // *******************************
        // *******************************
        // pathological buffersize traits
        // *******************************
        // *******************************


        String db = (String) dbKeysBuilder.get(etype.db);
        String id = (String) dbKeysBuilder.get(docId);
        HttpRequest request = actionBuilder.state().$req();
        private HttpResponse response;
        ByteBuffer header = (ByteBuffer) request
            .path(scrub("/" + db + (null == id ? "" : "/" + id)))
            .method(GET)
            .addHeaderInterest(STATIC_CONTENT_LENGTH_ARR).as(ByteBuffer.class);

        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write(header);
          assert !header.hasRemaining();
          header.clear();
          response = request.headerInterest(STATIC_JSON_SEND_HEADERS).$res();

          key.interestOps(OP_READ);/*WRITE-READ implicit turnaround in 1xio won't need .selector().wakeup()*/
        }

        ByteBuffer cursor;

        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            //geometric,  vulnerable to dev/null if not max'd here.
            header = (null == header) ? ByteBuffer.allocateDirect(getReceiveBufferSize()) : header.hasRemaining() ? header : ByteBuffer.allocateDirect(header.capacity() * 2).put((ByteBuffer) header.flip());

            int read = channel.read(header);
            ByteBuffer flip = (ByteBuffer) header.duplicate().flip();
            response.apply((ByteBuffer) flip);

            if (BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, response.headerBuf())) {
              cursor = (ByteBuffer) flip.slice();
              header = null;
            } else {
              return;
            }


            if (DEBUG_SENDJSON) {
              System.err.println(deepToString(response.statusEnum(), response, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }


            HttpStatus httpStatus = response.statusEnum();
            switch (httpStatus) {
              case $200:
                int remaining = Integer.parseInt(response.headerString(Content$2dLength));


                if (remaining == cursor.remaining()) {
                  deliver();
                } else {
                  cursor = ByteBuffer.allocate(remaining).put(cursor);
                }
                break;
              default: //error
                cyclicBarrier.reset();
            }
          } else {
            int read = channel.read(cursor);
            if (-1 == read) cyclicBarrier.reset();
            if (!cursor.hasRemaining()) {
              cursor.flip();
              deliver();
            }
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
      final Rfc822HeaderState state = actionBuilder.state().headerInterest(HEADER)
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
        }                                       //V
      });                                       //V
      try {                                     //V
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
    public <T> ByteBuffer visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      final SocketChannel channel = createCouchConnection();
      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {


        // *******************************
        // *******************************
        // pathological buffersize traits
        // *******************************
        // *******************************

        final HttpRequest request = actionBuilder.state().$req();
        public LinkedList<ByteBuffer> list;
        private HttpResponse response;
        ByteBuffer header = (ByteBuffer) request
            .path(scrub("/" + dbKeysBuilder.get(db) + "/" + dbKeysBuilder.get(docId) + "?rev=" + dbKeysBuilder.get(rev)))
            .method(DELETE)
            .as(ByteBuffer.class);
        ByteBuffer cursor;

        public void onWrite(SelectionKey key) throws Exception {
          int write = channel.write(header);
          assert !header.hasRemaining();
          header.clear();
          response = request.headerInterest(STATIC_CONTENT_LENGTH_ARR).$res();

          key.interestOps(OP_READ);/*WRITE-READ implicit turnaround in 1xio won't need .selector().wakeup()*/
        }


        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            //geometric,  vulnerable to dev/null if not max'd here.
            header = (null == header) ? ByteBuffer.allocateDirect(getReceiveBufferSize()) : header.hasRemaining() ? header : ByteBuffer.allocateDirect(header.capacity() * 2).put((ByteBuffer) header.flip());

            int read = channel.read(header);
            if (-1 == read) cyclicBarrier.reset();
            ByteBuffer flip = (ByteBuffer) header.duplicate().flip();
            response.apply((ByteBuffer) flip);

            if (BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, response.headerBuf())) {
              cursor = (ByteBuffer) flip.slice();
              header = null;
            } else {
              return;
            }


            if (DEBUG_SENDJSON) {
              System.err.println(deepToString(response.statusEnum(), response, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }

            int remaining = Integer.parseInt(response.headerString(Content$2dLength));

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

        private LinkedList<ByteBuffer> getReadList() {
          return this.list == null ? new LinkedList<ByteBuffer>() : this.list;
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


  /**
   * a statistically imperfect chunked encoding reader which searches the end of current input for a token.
   * <p/>
   * <u> statistically imperfect </u>means that data containing said token {@link  #CE_TERMINAL} delivered on  a packet boundary or byte-at-a-time will false trigger the suffix.
   */
  @DbTask({rows, future, continuousFeed}) @DbKeys(value = {db, view}, optional = type)ViewFetch {
    public <T> ByteBuffer visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();
      final CyclicBarrier joinPoint = new CyclicBarrier(2);
      final String db = scrub('/' + (String) dbKeysBuilder.get(etype.db));
      Class<T> type = dbKeysBuilder.get(etype.type);
      final SocketChannel channel = createCouchConnection();
      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer cursor;
        /**
         * holds un-rewound raw buffers.  must potentially be backtracked to fulfill CE_TERMINAL.length token check under pathological fragmentation
         */

        List<ByteBuffer> list = new ArrayList<ByteBuffer>();
        final Impl prev = this;
        private HttpRequest request;
        private ByteBuffer header;
        private HttpResponse response;


        public void onWrite(SelectionKey key) throws Exception {


          request = actionBuilder.state().$req();

          header = (ByteBuffer)
              request
                  .method(GET)
                  .path(scrub('/' + db + '/' + dbKeysBuilder.get(view)))
                  .headerString(Accept, MimeType.json.contentType)
                  .as(ByteBuffer.class);
          int wrote = channel.write(header);
          assert !header.hasRemaining();
          key.interestOps(OP_READ);
        }


        public void onRead(SelectionKey key) throws IOException {
          if (null == cursor) {
            //geometric,  vulnerable to dev/null if not max'd here.
            header = (null == header) ? ByteBuffer.allocateDirect(getReceiveBufferSize()) : header.hasRemaining() ? header : ByteBuffer.allocateDirect(header.capacity() * 2).put((ByteBuffer) header.flip());

            int read = channel.read(header);
            ByteBuffer flip = (ByteBuffer) header.duplicate().flip();
            response = request.$res();
            response.apply((ByteBuffer) flip);

            if (BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, response.headerBuf())) {
              cursor = (ByteBuffer) flip.slice();
              header = null;
            } else {
              return;
            }


            if (DEBUG_SENDJSON) {
              System.err.println(deepToString(response.statusEnum(), response, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }


            HttpStatus httpStatus = response.statusEnum();
            switch (httpStatus) {
              case $200:

                if (response.headerStrings().containsKey(Content$2dLength.getHeader())) {  //rarity but for empty rowsets
                  final String remainingString = response.headerString(Content$2dLength);
                  int remaining = Integer.parseInt(remainingString);
                  if (cursor.remaining() == remaining) {
                    payload.set(cursor.slice());
                    EXECUTOR_SERVICE.submit(new Callable<Object>() {
                      public Object call() throws Exception {
                        joinPoint.await();
                        recycleChannel(channel);
                        return null;
                      }
                    });
                  } else {
                    cursor = ByteBuffer.allocateDirect(remaining).put(cursor);
                    key.attach(new Impl() {

                      public void onRead(SelectionKey key) throws Exception {
                        channel.read(cursor);
                        if (cursor.hasRemaining()) {
                          payload.set(cursor);
                          EXECUTOR_SERVICE.submit(new Callable<Object>() {
                            public Object call() throws Exception {
                              joinPoint.await();
                              recycleChannel(channel);
                              return null;
                            }
                          });
                        }
                      }
                    });
                  }
                }
                cursor = cursor.slice().compact();
//                key.attach(this);
//                key.selector().wakeup();
                break;
              default:
                joinPoint.reset();
                recycleChannel(channel);
                return;
            }
          } else
            try {
              final int read = channel.read(cursor);
              if (-1 == read) {
                if (cursor.position() > 0) list.add(cursor);
                deliver();
                recycleChannel(channel);
                return;
              }
            } catch (Throwable e) {
              e.printStackTrace();  //todo: verify for a purpose
            }
          //token suffix check
          boolean suffixMatches = BlobAntiPatternObject.suffixMatchChunks(CE_TERMINAL, cursor.duplicate(), list.toArray(new ByteBuffer[list.size()]));

          if (suffixMatches) {
            if (cursor.position() > 0)
              list.add(cursor);
            deliver();
            recycleChannel(channel);
            return;
          }
/*
          final ByteBuffer tmp = (ByteBuffer) cursor.duplicate();
          final int position = tmp.position();
          tmp.flip();
          String decode = UTF8.decode(((ByteBuffer) tmp.position(position - CE_TERMINAL.length()))).toString();
          if (CE_TERMINAL.equals(decode.toString())) {
            list.add(cursor);
            deliver();
            recycleChannel(channel);
            return;
          }
*/
          if (!cursor.hasRemaining()) {
            list.add(cursor);
            cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
          }
        }


        private void deliver() {

          EXECUTOR_SERVICE.submit(new Callable<Object>() {
            public Object call() throws Exception {
              int sum = 0;
              for (ByteBuffer byteBuffer : list) {
                sum += byteBuffer.flip().limit();
              }
              final ByteBuffer outbound = ByteBuffer.allocate(sum);
              for (ByteBuffer byteBuffer : list) {
                final ByteBuffer put = outbound.put(byteBuffer);
              }
              if (DEBUG_SENDJSON) {
                System.err.println(UTF8.decode((ByteBuffer) outbound.duplicate().flip()));
              }
              ByteBuffer src = ((ByteBuffer) outbound.rewind()).duplicate();
              int endl = 0;
              while (sum > 0 && src.hasRemaining()) {
                if (DEBUG_SENDJSON)
                  System.err.println("outbound:----\n" + UTF8.decode(outbound.duplicate()).toString() + "\n----");

                byte b = 0;
                boolean first = true;
                while (src.hasRemaining() && ('\n' != (b = src.get()) || first))
                  if (first && !Character.isWhitespace(b)) {
                    first = false;
                  }

                final int i = Integer.parseInt(UTF8.decode((ByteBuffer) src.duplicate().flip()).toString().trim(), 0x10);
                src = ((ByteBuffer) src.compact().position(i)).slice();
                endl += i;
                sum -= i;
                if (0 == i) break;
              }

              ByteBuffer retval = null;
              if (DEBUG_SENDJSON) {
                retval = (ByteBuffer) outbound.clear().limit(endl);
                System.err.println(UTF8.decode(retval));
              }

              payload.set(retval);       //V
              joinPoint.await();         //V
              return null;               //V
            }                            //V
          });                            //V
        }                                //V
      });                                //V                                    //V
      joinPoint.await(5L, getDefaultCollectorTimeUnit());//5 seconds query is enough.
      return payload.get();
    }
  },
  //training day for the Terminal rewrites

  @DbTask({tx, oneWay, rows, json, future, continuousFeed}) @DbKeys(value = {opaque, validjson}, optional = type)JsonSend {
    public <T> ByteBuffer visit(final DbKeysBuilder<T> dbKeysBuilder, final ActionBuilder<T> actionBuilder) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<ByteBuffer>();
      final CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
      String opaque = scrub('/' + (String) dbKeysBuilder.get(etype.opaque));


      int slashCounter = 0;
      int lastSlashIndex = 0;
      label:
      for (int i = 0; i < opaque.length(); i++) {
        char c1 = opaque.charAt(i);
        switch (c1) {
          case '?':
          case '#':
            break label;
          case '/':
            slashCounter++;
            lastSlashIndex = i;
          default:
            break;

        }
      }
      if (opaque.length() - 1 == lastSlashIndex) {
        opaque = opaque.substring(0, opaque.length() - 1);
      }
      String validjson = (String) dbKeysBuilder.get(etype.validjson);
      validjson = validjson == null ? "{}" : validjson;

      final Rfc822HeaderState state = actionBuilder.state();
      final byte[] outbound = validjson.getBytes(UTF8);


      final HttpMethod method = 1 == slashCounter || !(lastSlashIndex < opaque.lastIndexOf('?') && lastSlashIndex != opaque.indexOf('/')) ? POST : PUT;
      HttpRequest request = state.$req();
      final ByteBuffer header = (ByteBuffer) request.method(method)
          .path(opaque)
          .headerInterest(STATIC_JSON_SEND_HEADERS)
          .headerString(Content$2dLength, String.valueOf(outbound.length))
          .headerString(Accept, MimeType.json.contentType)
          .headerString(Content$2dType, MimeType.json.contentType)
          .as(ByteBuffer.class);
      if (DEBUG_SENDJSON) {
        System.err.println(deepToString(opaque, validjson, UTF8.decode(header.duplicate()), state));
      }
      final SocketChannel channel = createCouchConnection();
      final String finalOpaque = opaque;
      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
//        ByteBuffer cursor;


        // *******************************
        // *******************************
        // pathological buffersize traits
        // *******************************
        // *******************************


        String db = (String) dbKeysBuilder.get(etype.db);
        String id = (String) dbKeysBuilder.get(docId);
        HttpRequest request = actionBuilder.state().$req();
        private HttpResponse response;
        ByteBuffer header = (ByteBuffer) request
            .path(finalOpaque)
            .headerInterest(STATIC_JSON_SEND_HEADERS)
            .headerString(Content$2dLength, String.valueOf(outbound.length))
            .headerString(Accept, MimeType.json.contentType)
            .headerString(Content$2dType, MimeType.json.contentType)
            .as(ByteBuffer.class);

        public void onWrite(SelectionKey key) throws Exception {
          if (null == cursor) {
            int write = channel.write(header);
            cursor = ByteBuffer.wrap(outbound);
          }
          int write = channel.write(cursor);
          if (!cursor.hasRemaining()) {
            header.clear();
            response = request.$res();
            key.interestOps(OP_READ);
            cursor = null;
          }
        }

        ByteBuffer cursor;

        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            //geometric,  vulnerable to dev/null if not max'd here.
            header = (null == header) ? ByteBuffer.allocateDirect(getReceiveBufferSize()) : header.hasRemaining() ? header : ByteBuffer.allocateDirect(header.capacity() * 2).put((ByteBuffer) header.flip());

            int read = channel.read(header);
            ByteBuffer flip = (ByteBuffer) header.duplicate().flip();
            response.apply((ByteBuffer) flip);

            if (BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, response.headerBuf())) {
              cursor = (ByteBuffer) flip.slice();
              header = null;
            } else {
              return;
            }


            if (DEBUG_SENDJSON) {
              System.err.println(deepToString(response.statusEnum(), response, UTF8.decode((ByteBuffer) cursor.duplicate().rewind())));
            }


            HttpStatus httpStatus = response.statusEnum();
            switch (httpStatus) {
              case $200:
              case $201:
                int remaining = Integer.parseInt(response.headerString(Content$2dLength));


                if (remaining == cursor.remaining()) {
                  deliver();
                } else {
                  cursor = ByteBuffer.allocate(remaining).put(cursor);
                }
                break;
              default: //error
                cyclicBarrier.reset();
            }
          } else {
            int read = channel.read(cursor);
            if (-1 == read) cyclicBarrier.reset();
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
  public static final byte[] HEADER_TERMINATOR = "\r\n\r\n".getBytes(UTF8);
  public static final byte[] CE_TERMINAL = "\n0\r\n\r\n".getBytes(UTF8);

  //"premature optimization" s/mature/view/
  public static final String[] STATIC_VF_HEADERS = Rfc822HeaderState.staticHeaderStrings(new HttpHeaders[]{ETag, Content$2dLength, Transfer$2dEncoding});

  //"premature optimization" s/mature/view/
  public static final String[] STATIC_JSON_SEND_HEADERS = Rfc822HeaderState.staticHeaderStrings(new HttpHeaders[]{ETag, Content$2dLength, Content$2dEncoding});
  //"premature optimization" s/mature/view/
  public static final String[] STATIC_CONTENT_LENGTH_ARR = Rfc822HeaderState.staticHeaderStrings(new HttpHeaders[]{Content$2dLength});

  public static final String HEADER = ETag.getHeader();

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
  public static final String GENERATED_METHODS = "/*generated methods vsd78vs0fd078fv0sa78*/";
  public static final String IFACE_FIRE_TARGETS = "/*fire interface ijnoifnj453oijnfiojn h*/";
  public static final String FIRE_METHODS = "/*embedded fire terminals j63l4k56jn4k3jn5l63l456jn*/";

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

