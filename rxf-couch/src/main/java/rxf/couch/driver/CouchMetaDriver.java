package rxf.couch.driver;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;
import one.xio.HttpStatus;
import one.xio.MimeType;
import rxf.core.Config;
import rxf.core.Rfc822HeaderState;
import rxf.core.Rfc822HeaderState.HttpRequest;
import rxf.core.Rfc822HeaderState.HttpResponse;
import rxf.core.Tx;
import rxf.couch.DbKeysBuilder;
import rxf.rpc.RpcHelper;
import rxf.web.inf.ProtocolMethodDispatch;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Phaser;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.nio.channels.SelectionKey.*;
import static one.xio.AsioVisitor.FSM;
import static one.xio.AsioVisitor.FSM.read;
import static one.xio.AsioVisitor.Helper.*;
import static one.xio.HttpHeaders.*;
import static one.xio.HttpMethod.*;
import static rxf.core.Server.enqueue;
import static rxf.couch.CouchConnectionFactory.createCouchConnection;
import static rxf.couch.CouchConnectionFactory.recycleChannel;
import static rxf.couch.driver.CouchMetaDriver.etype.*;

/**
 * confers traits on an oo platform...
 * <p/>
 * CouchDriver defines an interface and a method for each {@link CouchMetaDriver } enum attribute. presently the
 * generator does not wire that interface up anywhere but the inner classes of the interface use this enum for slotted
 * method dispatch.
 * <p/>
 * the fluent interface is carried in threadlocal variables from step to step. the visit() method cracks these open and
 * inserts them as the apropriate state for lower level visit(builder1...buildern) method
 * <p/>
 * <h2>{@link one.xio.AsioVisitor} visitor sub-threads in threadpools must be one of:</h2>
 * <ol>
 * <li>inner classes using the <u>final</u> paramters passed in via {@link #visit(rxf.couch.DbKeysBuilder, rxf.core.Tx)}
 * </li>
 * <li>fluent class interface agnostic(highly unlikely)</li>
 * <li>arduously carried in (same as first option but not as clean as inner class refs)</li>
 * </ol>
 * <p/>
 * <p/>
 * <ul>
 * DSEL addendum:
 * <li>to() - setup the request: provide early access to the header state to tweak whatever you want. This can then be
 * used after fire() to read out the header state from the response
 * <li>fire() - execution of the visitor, access to results, future, (errors?). If results is null, check error. If you
 * use future, its your own damn problem
 * </ul>
 * User: jim Date: 5/24/12 Time: 3:09 PM
 */
@SuppressWarnings({"RedundantCast"})
public enum CouchMetaDriver {

  // @DbTask( {tx, oneWay})
  // @DbKeys( {db})
  DbCreate {
    public void visit(final DbKeysBuilder dbKeysBuilder, final Tx tx) throws Exception {
      final Phaser phaser = new Phaser(2);
      final SocketChannel channel = createCouchConnection();
      enqueue(channel, OP_WRITE | OP_CONNECT, toWrite(new F() {
        @Override
        public void apply(SelectionKey key) throws Exception {
          String db = (String) dbKeysBuilder.get(etype.db);

          final ByteBuffer[] header =
              {(ByteBuffer) tx.state().$req().method(PUT).path("/" + db).asByteBuffer()};
          int write = FSM.write(key, header[0]);
          assert !header[0].hasRemaining();
          header[0].clear();
          final HttpResponse response =
              tx.state().$req().headerInterest(STATIC_JSON_SEND_HEADERS).$res();
          toRead(key, new F() {
            @Override
            public void apply(SelectionKey key) throws Exception {
              if (null == tx.payload()) {
                // geometric, vulnerable to dev/null if not max'd here.
                header[0] =
                    null == header[0] ? ByteBuffer.allocateDirect(4 << 10) : header[0]
                        .hasRemaining() ? header[0] : ByteBuffer.allocateDirect(
                        header[0].capacity() * 2).put((ByteBuffer) header[0].flip());
                int read = read(channel, header[0]);
                ByteBuffer flip = (ByteBuffer) header[0].duplicate().flip();
                response.read((ByteBuffer) flip);

                if (Rfc822HeaderState.suffixMatchChunks(ProtocolMethodDispatch.HEADER_TERMINATOR,
                    response.headerBuf())) {
                  tx.payload((ByteBuffer) flip.slice());
                  header[0] = null;

                  if (RpcHelper.DEBUG_SENDJSON) {
                    System.err.println(ProtocolMethodDispatch.deepToString(response.statusEnum(),
                        response, StandardCharsets.UTF_8.decode((ByteBuffer) tx.payload()
                            .duplicate().rewind())));
                  }

                  HttpStatus httpStatus = response.statusEnum();
                  switch (httpStatus) {
                    case $200:
                    case $201:
                      int remaining = Integer.parseInt(response.headerString(Content$2dLength));

                      if (remaining == tx.payload().remaining()) {
                        recycleChannel(channel);
                        phaser.arrive();
                      } else {
                        tx.payload(ByteBuffer.allocateDirect(remaining).put(tx.payload()));
                      }
                      break;
                    default: // error
                      phaser.forceTermination();
                      channel.close();
                  }
                }
              } else {
                int read = read(channel, tx.payload());
                switch (read) {
                  case -1:
                    phaser.forceTermination();

                    channel.close();
                    return;
                }
                if (!tx.payload().hasRemaining()) {
                  tx.payload().flip();
                  recycleChannel(channel);
                  phaser.arrive();
                }
              }
            }
          });
        }
      }));

      try {
        phaser.awaitAdvanceInterruptibly(phaser.arrive(), REALTIME_CUTOFF, REALTIME_UNIT);
      } catch (Exception e) {
        if (RpcHelper.DEBUG_SENDJSON) {
          System.err.println("!!! " + ProtocolMethodDispatch.deepToString(this, e) + "\n\tfrom");
          dbKeysBuilder.trace().printStackTrace();
        }
      }
    }

  },
  // @DbTask( {tx, oneWay})
  // @DbKeys( {db})
  DbDelete {
    public void visit(final DbKeysBuilder dbKeysBuilder, final Tx tx) throws Exception {
      final Phaser phaser = new Phaser(2);
      final SocketChannel channel = createCouchConnection();

      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {
        ByteBuffer header = (ByteBuffer) tx.state().$req().method(DELETE).pathResCode(
            "/" + dbKeysBuilder.get(db)).asByteBuffer();
        public HttpResponse response;

        public void onWrite(SelectionKey key) throws Exception {
          int write = FSM.write(key, header);
          assert !header.hasRemaining();
          header.clear();
          response = tx.state().$req().headerInterest(STATIC_JSON_SEND_HEADERS).$res();

          key.interestOps(OP_READ);/* WRITE-READ implicit turnaround in 1xio won't need .selector().wakeup() */
        }

        public void onRead(SelectionKey key) throws Exception {
          if (null == tx.payload()) {
            // geometric, vulnerable to dev/null if not max'd here.
            header =
                null == header ? ByteBuffer.allocateDirect(4 << 10) : header.hasRemaining()
                    ? header : ByteBuffer.allocateDirect(header.capacity() * 2).put(
                        (ByteBuffer) header.flip());

            int read = read(channel, header);
            ByteBuffer flip = (ByteBuffer) header.duplicate().flip();
            response.read((ByteBuffer) flip);

            if (Rfc822HeaderState.suffixMatchChunks(ProtocolMethodDispatch.HEADER_TERMINATOR,
                response.headerBuf())) {
              tx.payload(flip.slice());
              header = null;

              if (RpcHelper.DEBUG_SENDJSON) {
                System.err.println(ProtocolMethodDispatch.deepToString(response.statusEnum(),
                    response, StandardCharsets.UTF_8.decode((ByteBuffer) tx.payload().duplicate()
                        .rewind())));
              }

              HttpStatus httpStatus = response.statusEnum();
              switch (httpStatus) {
                case $200:
                  int remaining = Integer.parseInt(response.headerString(Content$2dLength));

                  if (remaining == tx.payload().remaining()) {
                    deliver();
                  } else {
                    tx.payload(ByteBuffer.allocateDirect(remaining).put(tx.payload()));
                  }
                  break;
                default: // error
                  phaser.forceTermination();
                  channel.close();
              }
            }
          } else {
            int read = read(channel, tx.payload());
            switch (read) {
              case -1:
                phaser.forceTermination();

                channel.close();
                return;
            }
            if (!tx.payload().hasRemaining()) {
              tx.payload().flip();
              deliver();
            }
          }
        }

        private void deliver() {
          recycleChannel(channel);
          tx.payload(tx.payload());
          phaser.arrive();
        }
      });
      try {
        phaser.awaitAdvanceInterruptibly(phaser.arrive(), REALTIME_CUTOFF, REALTIME_UNIT);
      } catch (Exception e) {
        if (RpcHelper.DEBUG_SENDJSON) {
          System.err.println("!!! " + ProtocolMethodDispatch.deepToString(this, e) + "\n\tfrom");
          dbKeysBuilder.trace().printStackTrace();
        }
      }
    }
  },

  // @DbTask( {pojo, future, json})
  // @DbKeys( {db, docId})
  DocFetch {
    public void visit(final DbKeysBuilder dbKeysBuilder, final Tx tx) throws Exception {
      final SocketChannel channel = createCouchConnection();
      final Phaser phaser = new Phaser(2);
      tx.payload(null);
      tx.state(null);
      enqueue(channel, OP_CONNECT | OP_WRITE, toWrite(new F() {
        final String db = (String) dbKeysBuilder.get(etype.db);
        final String id = (String) dbKeysBuilder.get(docId);
        final HttpRequest request = tx.state().$req();

        ByteBuffer header = (ByteBuffer) request.path(
            scrub("/" + db + (null == id ? "" : "/" + id))).method(GET).addHeaderInterest(
            STATIC_CONTENT_LENGTH_ARR).asByteBuffer();

        @Override
        public void apply(SelectionKey key) throws Exception {
          int write = FSM.write(key, header);
          assert !header.hasRemaining();
          header = null;

          toRead(key, new F() {
            @Override
            public void apply(SelectionKey key) throws Exception {
              if (null == tx.payload()) { // haven't started body yet
                // geometric, vulnerable to dev/null if not max'd here.
                if (null == header)
                  header = ByteBuffer.allocateDirect(4 << 10);
                else
                  header =
                      header.hasRemaining() ? header : ByteBuffer.allocateDirect(
                          header.capacity() * 2).put((ByteBuffer) header.flip());

                int read = read(channel, header);
                if (-1 == read) {// nothing else to read from the header, never started body, something is wrong
                  phaser.forceTermination();
                  key.cancel();
                  channel.close();
                  return;
                }
                ByteBuffer flip = (ByteBuffer) header.duplicate().flip();
                HttpResponse response = request.headerInterest(STATIC_JSON_SEND_HEADERS).$res();
                response.read((ByteBuffer) flip);

                if (Rfc822HeaderState.suffixMatchChunks(ProtocolMethodDispatch.HEADER_TERMINATOR,
                    response.headerBuf())) {
                  tx.payload((ByteBuffer) flip.slice());
                  header = null;

                  if (RpcHelper.DEBUG_SENDJSON) {
                    System.err.println(ProtocolMethodDispatch.deepToString(response.statusEnum(),
                        response, this, StandardCharsets.UTF_8.decode((ByteBuffer) tx.payload()
                            .duplicate().rewind())));
                  }

                  HttpStatus httpStatus = response.statusEnum();
                  switch (httpStatus) {
                    case $200:
                      int remaining = Integer.parseInt(response.headerString(Content$2dLength));

                      if (remaining == tx.payload().remaining()) {// we have all of the body already, just deliver
                        deliver();
                      } else { // we need more, allocate a buffer the size we need, and put what we already have
                        tx.payload(ByteBuffer.allocateDirect(remaining).put(tx.payload()));
                      }
                      break;
                    default: // error
                      phaser.forceTermination();
                      channel.close();
                  }
                }
              } else {// we've already begun the body, but didn't finish, and may do so now
                // read further of the body
                int read = read(channel, tx.payload());
                switch (read) {// if we didn't actually read, something is wrong
                  case -1:
                    phaser.forceTermination();
                    channel.close();
                    return;
                }
                if (!tx.payload().hasRemaining()) {// we've read to the end, flip to beginning and deliver
                  tx.payload().flip();
                  deliver();
                }
              }
            }

            private void deliver() {
              assert null != tx.payload();
              tx.payload((ByteBuffer) tx.payload().rewind());
              phaser.arrive();
              recycleChannel(channel);
            }
          });
        }
      }));

      try {
        phaser.awaitAdvanceInterruptibly(phaser.arrive(), REALTIME_CUTOFF, REALTIME_UNIT);
      } catch (TimeoutException | InterruptedException e) {
        if (RpcHelper.DEBUG_SENDJSON) {
          System.err.println("!!! " + ProtocolMethodDispatch.deepToString(this, e) + "\n\tfrom");
          dbKeysBuilder.trace().printStackTrace();
        }
      }
    }
  },

  // @DbTask( {json, future})
  // @DbKeys( {db, docId})
  RevisionFetch {
    public void visit(final DbKeysBuilder dbKeysBuilder, final Tx tx) throws Exception {

      final SocketChannel channel = createCouchConnection();
      final Phaser phaser = new Phaser(2);
      enqueue(channel, OP_CONNECT | OP_WRITE, new Impl() {
        // *******************************
        // *******************************
        // pathological buffersize traits
        // *******************************
        // *******************************

        String db = (String) dbKeysBuilder.get(etype.db);
        String id = (String) dbKeysBuilder.get(docId);
        HttpRequest request = tx.state().$req();
        final String scrub = scrub("/" + db + (null != id ? "/" + id : ""));
        ByteBuffer header = (ByteBuffer) request.path(scrub).method(HEAD).asByteBuffer();
        public HttpResponse response;
        public ByteBuffer cursor;

        public void onWrite(SelectionKey key) throws Exception {
          int write = FSM.write(key, header);
          assert !header.hasRemaining();
          header.clear();
          response = request.headerInterest(ETag).$res();

          key.interestOps(OP_READ);/* WRITE-READ implicit turnaround in 1xio won't need .selector().wakeup() */
        }

        public void onRead(SelectionKey key) throws Exception {

          if (null == cursor) {
            // geometric, vulnerable to dev/null if not max'd here.
            header =
                null == header ? ByteBuffer.allocateDirect(4 << 10) : header.hasRemaining()
                    ? header : ByteBuffer.allocateDirect(header.capacity() * 2).put(
                        (ByteBuffer) header.flip());

            int read = read(channel, header);
            if (-1 != read) {
              ByteBuffer flip = (ByteBuffer) header.duplicate().flip();
              response.read((ByteBuffer) flip);

              if (Rfc822HeaderState.suffixMatchChunks(ProtocolMethodDispatch.HEADER_TERMINATOR,
                  response.headerBuf())) {
                try {
                  if (RpcHelper.DEBUG_SENDJSON) {
                    System.err.println(ProtocolMethodDispatch.deepToString("??? ",
                        StandardCharsets.UTF_8.decode((ByteBuffer) flip.duplicate().rewind())));
                  }
                  if (HttpStatus.$200 == response.statusEnum()) {
                    tx.payload(StandardCharsets.UTF_8.encode(response.dequotedHeader(ETag
                        .getHeader())));
                  } else {// error message, pass null back to indicate no rev
                    tx.payload(null);
                  }
                } catch (Exception e) {
                  if (RpcHelper.DEBUG_SENDJSON) {
                    e.printStackTrace();
                    Throwable trace = dbKeysBuilder.trace();
                    if (null != trace) {
                      System.err.println("\tfrom:");
                      trace.printStackTrace();
                    }
                  }

                }
                recycleChannel(channel);
                // assumes quoted
                phaser.arrive();
              }
            } else {
              phaser.forceTermination();
              channel.close();
            }
          }
        }
      });
      try {
        phaser.awaitAdvanceInterruptibly(phaser.arrive(), REALTIME_CUTOFF, REALTIME_UNIT);
      } catch (Exception e) {
        if (RpcHelper.DEBUG_SENDJSON) {
          System.err.println("!!! " + ProtocolMethodDispatch.deepToString(this, e) + "\n\tfrom");
          dbKeysBuilder.trace().printStackTrace();
        }
      }
    }
  },
  // @DbTask( {tx, oneWay, future})
  // @DbKeys(value = {db, validjson}, optional = {docId, rev})
  DocPersist {
    public void visit(DbKeysBuilder dbKeysBuilder, Tx tx) throws Exception {

      String db = (String) dbKeysBuilder.get(etype.db);
      String docId = (String) dbKeysBuilder.get(etype.docId);
      String rev = (String) dbKeysBuilder.get(etype.rev);
      String sb =
          scrub('/' + db + (null == docId ? "" : '/' + docId + (null == rev ? "" : "?rev=" + rev)));
      dbKeysBuilder.put(opaque, sb);
      tx.state().$req().headerString(Content$2dType, MimeType.json.contentType);
      JsonSend.visit(dbKeysBuilder, tx);
    }
  },
  // @DbTask( {tx, oneWay, future})
  // @DbKeys(value = {db, docId, rev})
  DocDelete {
    public void visit(final DbKeysBuilder dbKeysBuilder, final Tx tx) throws Exception {
      final Phaser phaser = new Phaser(2);
      final SocketChannel channel = createCouchConnection();
      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {

        // *******************************
        // *******************************
        // pathological buffersize traits
        // *******************************
        // *******************************

        final HttpRequest request = tx.state().$req();
        public LinkedList<ByteBuffer> list;
        private HttpResponse response;
        ByteBuffer header = (ByteBuffer) request.path(
            scrub("/" + dbKeysBuilder.get(db) + "/" + dbKeysBuilder.get(docId) + "?rev="
                + dbKeysBuilder.get(rev))).method(DELETE).asByteBuffer();
        ByteBuffer cursor;

        public void onWrite(SelectionKey key) throws Exception {
          int write = FSM.write(key, header);
          assert !header.hasRemaining();
          header.clear();
          response = request.headerInterest(STATIC_CONTENT_LENGTH_ARR).$res();

          key.interestOps(OP_READ);/* WRITE-READ implicit turnaround in 1xio won't need .selector().wakeup() */
        }

        public void onRead(SelectionKey key) throws Exception {
          if (null == cursor) {
            // geometric, vulnerable to dev/null if not max'd here.
            header =
                null == header ? ByteBuffer.allocateDirect(4 << 10) : header.hasRemaining()
                    ? header : ByteBuffer.allocateDirect(header.capacity() * 2).put(
                        (ByteBuffer) header.flip());

            int read = read(channel, header);
            switch (read) {
              case -1:
                phaser.forceTermination();
                channel.close();
                break;
            }
            ByteBuffer flip = (ByteBuffer) header.duplicate().flip();
            response.read((ByteBuffer) flip);

            if (Rfc822HeaderState.suffixMatchChunks(ProtocolMethodDispatch.HEADER_TERMINATOR,
                response.headerBuf())) {
              cursor = (ByteBuffer) flip.slice();
              header = null;

              if (RpcHelper.DEBUG_SENDJSON) {
                System.err.println(ProtocolMethodDispatch.deepToString(response.statusEnum(),
                    response, StandardCharsets.UTF_8.decode((ByteBuffer) cursor.duplicate()
                        .rewind())));
              }

              int remaining = Integer.parseInt(response.headerString(Content$2dLength));

              if (remaining == cursor.remaining()) {
                deliver();
              } else {
                cursor = ByteBuffer.allocateDirect(remaining).put(cursor);
              }
            }
          } else {
            int read = read(channel, cursor);
            if (!cursor.hasRemaining()) {
              cursor.flip();
              deliver();
            }
          }
        }

        private void deliver() {
          tx.payload(cursor);
          recycleChannel(channel);
          phaser.arrive();
        }
      });
      try {
        phaser.awaitAdvanceInterruptibly(phaser.arrive(), REALTIME_CUTOFF, REALTIME_UNIT);
      } catch (Exception e) {
        if (RpcHelper.DEBUG_SENDJSON) {
          System.err.println("!!! " + ProtocolMethodDispatch.deepToString(this, e) + "\n\tfrom");
          dbKeysBuilder.trace().printStackTrace();
        }
      }

    }
  },
  // @DbTask( {pojo, future, json})
  // @DbKeys( {db, designDocId})
  DesignDocFetch {
    public void visit(DbKeysBuilder dbKeysBuilder, Tx tx) throws Exception {
      dbKeysBuilder.put(docId, dbKeysBuilder.remove(designDocId));
      DocFetch.visit(dbKeysBuilder, tx);
    }
  },

  /**
   * a statistically imperfect chunked encoding reader which searches the end of current input for a token.
   * <p/>
   * <u> statistically imperfect </u>means that data containing said token {@link #CE_TERMINAL} delivered on a packet
   * boundary or byte-at-a-time will false trigger the suffix.
   */
  // @DbTask( {rows, future, continuousFeed})
  // @DbKeys(value = {db, view}, optional = {type, keyType})
  ViewFetch {
    public void visit(final DbKeysBuilder dbKeysBuilder, final Tx tx) throws Exception {
      final AtomicReference<ByteBuffer> payload = new AtomicReference<>();
      final AtomicReference<List<ByteBuffer>> cePayload = new AtomicReference<>();
      final Phaser phaser = new Phaser(2);
      final String db = scrub('/' + (String) dbKeysBuilder.get(etype.db));

      final SocketChannel channel = createCouchConnection();
      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {

        /**
         * holds un-rewound raw buffers. must potentially be backtracked to fulfill CE_TERMINAL.length token check under
         * pathological fragmentation
         */

        List<ByteBuffer> list = new ArrayList<>();
        final Impl prev = this;

        private ByteBuffer header;
        private ByteBuffer cursor;

        private void simpleDeploy(ByteBuffer buffer) {
          payload.set((ByteBuffer) buffer);
          phaser.arrive();
          recycleChannel(channel);
        }

        public void onWrite(SelectionKey key) throws Exception {

          HttpRequest request = tx.state().$req();

          ByteBuffer header =
              (ByteBuffer) request.method(GET)
                  .path(scrub('/' + db + '/' + dbKeysBuilder.get(view))).headerString(Accept,
                      MimeType.json.contentType).asByteBuffer();
          int wrote = FSM.write(key, header);
          assert !header.hasRemaining() : "Failed to complete write in one pass, need to re-interest(READ)";
          key.interestOps(OP_READ);
        }

        public void onRead(SelectionKey key) throws IOException {
          if (null != cursor) {
            try {
              int read = read(channel, cursor);
              if (-1 == read) {
                // we were asked to read again, but no more content to read, just deliver what we already saw
                if (0 < cursor.position()) {
                  list.add(cursor);
                }
                cePayload.set(list);
                phaser.arrive();
                recycleChannel(channel);
                return;
              }
            } catch (Throwable e) {
              e.printStackTrace();
            }
            // token suffix check, see if we're at the end
            boolean suffixMatches =
                Rfc822HeaderState.suffixMatchChunks(CE_TERMINAL, cursor, list
                    .toArray(new ByteBuffer[list.size()]));

            if (suffixMatches) {
              if (0 < cursor.position()) {
                list.add(cursor);
              }
              cePayload.set(list);
              phaser.arrive();
              recycleChannel(channel);
              return;
            }
            if (!cursor.hasRemaining()) {
              list.add(cursor);
              cursor = ByteBuffer.allocateDirect(4 << 10);
            }
          } else {
            // geometric, vulnerable to dev/null if not max'd here.
            // can only happen if couch returns pathologically large headers
            if (null == header)
              header = ByteBuffer.allocateDirect(4 << 10);
            else if (!header.hasRemaining()) {
              header =
                  ByteBuffer.allocateDirect(header.capacity() * 2).put((ByteBuffer) header.flip());
            }

            int read = read(channel, header);
            ByteBuffer flip = (ByteBuffer) header.duplicate().flip();
            HttpResponse response =
                (HttpResponse) new Rfc822HeaderState().$res().headerInterest(STATIC_VF_HEADERS);
            response.read((ByteBuffer) flip);

            ByteBuffer currentBuff = response.headerBuf();
            if (!Rfc822HeaderState.suffixMatchChunks(ProtocolMethodDispatch.HEADER_TERMINATOR,
                currentBuff)) {
              // not enough content to finish loading headers, wait for more
              rxf.core.Server.getSelector().wakeup();
              return;
            }
            cursor = (ByteBuffer) flip.slice();
            tx.state(response);
            if (RpcHelper.DEBUG_SENDJSON) {
              System.err.println(ProtocolMethodDispatch
                  .deepToString(response.statusEnum(), response, StandardCharsets.UTF_8
                      .decode((ByteBuffer) cursor.duplicate().rewind())));
            }

            HttpStatus httpStatus = response.statusEnum();
            switch (httpStatus) {
              case $200:
                if (response.headerStrings().containsKey(Content$2dLength.getHeader())) { // rarity but for empty
                  // rowsets
                  String remainingString = response.headerString(Content$2dLength);
                  final int remaining = Integer.parseInt(remainingString);
                  if (cursor.remaining() == remaining) {
                    // No chunked encoding, all read in one pass, deploy the body without ce-parsing
                    simpleDeploy(cursor.slice());
                  } else {
                    // windows workaround?
                    key.attach(new Impl() {
                      private ByteBuffer cursor1 = cursor.capacity() > remaining
                          ? (ByteBuffer) cursor.limit(remaining) : ByteBuffer.allocateDirect(
                              remaining).put(cursor);

                      public void onRead(SelectionKey key) throws Exception {
                        int read1 = read(channel, cursor1);
                        switch (read1) {
                          case -1:
                            phaser.forceTermination();
                            channel.close();
                            break;
                        }
                        if (!cursor1.hasRemaining()) {
                          ByteBuffer flip1 = (ByteBuffer) cursor1.flip();
                          simpleDeploy(flip1);
                        }
                      }
                    });
                  }
                } else {
                  // if we're in this block it means that there was no content-length set, which means
                  // we're reading chunked data.

                  // since we sliced above to get the reference to cursor, we need to move the cursor to the end
                  boolean suffixMatches =
                      Rfc822HeaderState.suffixMatchChunks(CE_TERMINAL, (ByteBuffer) cursor
                          .duplicate().position(cursor.limit()));
                  if (suffixMatches) {
                    // 'fast forward' to the end of the cursor, since deliver will flip() which will end at current
                    // position, instead of just copying as is. A cleaner fix might be to change the first loop
                    // in deliver
                    cursor.position(cursor.limit());
                    list.add(cursor);
                    cePayload.set(list);
                    phaser.arrive();
                    recycleChannel(channel);
                  } else {
                    cursor.compact();
                  }
                }
                break;
              default:
                phaser.forceTermination();
                recycleChannel(channel);
            }
          }
        }
      });
      try {
        phaser.awaitAdvanceInterruptibly(phaser.arrive(), REALTIME_CUTOFF, REALTIME_UNIT);
      } catch (Exception e) {
        if (RpcHelper.DEBUG_SENDJSON) {
          System.err.println("!!! " + ProtocolMethodDispatch.deepToString(this, e) + "\n\tfrom");
          dbKeysBuilder.trace().printStackTrace();
        }
      }
      ByteBuffer simple = payload.get();
      if (null != simple) {
        tx.payload(simple);
        return;
      }
      List<ByteBuffer> list = cePayload.get();
      if (null == list) {
        tx.payload(null);
        return;
      }
      int sum = 0;
      for (ByteBuffer byteBuffer : list) {
        sum += byteBuffer.flip().limit();
      }
      ByteBuffer outbound = ByteBuffer.allocate(sum);
      for (ByteBuffer byteBuffer : list) {
        ByteBuffer put = outbound.put(byteBuffer);
      }
      if (RpcHelper.DEBUG_SENDJSON) {
        System.err.println(StandardCharsets.UTF_8.decode((ByteBuffer) outbound.duplicate().flip()));
      }
      ByteBuffer src = ((ByteBuffer) outbound.rewind()).duplicate();
      int endl = 0;
      while (0 < sum && src.hasRemaining()) {
        if (RpcHelper.DEBUG_SENDJSON)
          System.err.println("outbound:----\n"
              + StandardCharsets.UTF_8.decode(outbound.duplicate()).toString() + "\n----");

        byte b = 0;
        boolean first = true;
        while (src.hasRemaining() && ('\n' != (b = src.get()) || first))
          if (first && !Character.isWhitespace(b)) {
            first = false;
          }

        int i =
            Integer.parseInt(StandardCharsets.UTF_8.decode((ByteBuffer) src.duplicate().flip())
                .toString().trim(), 0x10);
        src = ((ByteBuffer) src.compact().position(i)).slice();
        endl += i;
        sum -= i;
        if (0 == i)
          break;
      }

      ByteBuffer retval = (ByteBuffer) outbound.clear().limit(endl);

      if (RpcHelper.DEBUG_SENDJSON) {
        System.err.println(StandardCharsets.UTF_8.decode(retval.duplicate()));
      }
      tx.payload(retval);
    }
  },
  // training day for the Terminal rewrites

  // @DbTask( {tx, oneWay, rows, json, future, continuousFeed})
  // @DbKeys(value = {opaque, validjson}, optional = {keyType, type})
  JsonSend {
    public void visit(final DbKeysBuilder dbKeysBuilder, final Tx tx) throws Exception {

      final Phaser phaser = new Phaser(2);
      String opaque = scrub('/' + (String) dbKeysBuilder.get(etype.opaque));

      int slashCounter = 0;
      int lastSlashIndex = 0;
      label : for (int i = 0; i < opaque.length(); i++) {
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
      validjson = null == validjson ? "{}" : validjson;

      final byte[] outbound = validjson.getBytes(StandardCharsets.UTF_8);

      HttpMethod method =
          1 == slashCounter
              || !(lastSlashIndex < opaque.lastIndexOf('?') && lastSlashIndex != opaque
                  .indexOf('/')) ? POST : PUT;
      HttpRequest request = tx.state().$req();
      if (null == request.headerString(Content$2dType)) {
        request.headerString(Content$2dType, MimeType.json.contentType);
      }
      ByteBuffer header =
          (ByteBuffer) request.method(method).path(opaque).headerInterest(STATIC_JSON_SEND_HEADERS)
              .headerString(Content$2dLength, String.valueOf(outbound.length)).headerString(Accept,
                  MimeType.json.contentType).asByteBuffer();
      if (RpcHelper.DEBUG_SENDJSON) {
        System.err.println(ProtocolMethodDispatch.deepToString(opaque, validjson,
            StandardCharsets.UTF_8.decode(header.duplicate()), tx.state()));
      }
      final SocketChannel channel = createCouchConnection();
      final String finalOpaque = opaque;
      enqueue(channel, OP_WRITE | OP_CONNECT, new Impl() {

        // *******************************
        // *******************************
        // pathological buffersize traits
        // *******************************
        // *******************************

        ByteBuffer header = (ByteBuffer) tx.state().$req().path(finalOpaque).headerInterest(
            STATIC_JSON_SEND_HEADERS).headerString(Content$2dLength,
            String.valueOf(outbound.length)).headerString(Accept, MimeType.json.contentType).as(
            ByteBuffer.class);

        public void onWrite(SelectionKey key) throws Exception {
          if (null == tx.payload()) {
            int write = FSM.write(key, header);
            tx.payload(ByteBuffer.wrap(outbound));
          }
          int write = channel.write(tx.payload());
          if (!tx.payload().hasRemaining()) {
            header.clear();
            final HttpResponse response = tx.state().$res();
            tx.payload(null);
            toRead(key, new F() {
              @Override
              public void apply(SelectionKey key) throws Exception {
                if (null == tx.payload()) {
                  // geometric, vulnerable to /dev/zero if not max'd here.
                  header =
                      null == header ? ByteBuffer.allocateDirect(4 << 10) : header.hasRemaining()
                          ? header : ByteBuffer.allocateDirect(header.capacity() * 2).put(
                              (ByteBuffer) header.flip());

                  try {
                    int read = read(channel, header);
                  } catch (IOException e) {
                    phaser.forceTermination();// .reset();
                    ProtocolMethodDispatch.deepToString(this, e);
                    channel.close();
                  }
                  ByteBuffer flip = (ByteBuffer) header.duplicate().flip();
                  response.read((ByteBuffer) flip);

                  if (Rfc822HeaderState.suffixMatchChunks(ProtocolMethodDispatch.HEADER_TERMINATOR,
                      response.headerBuf())) {
                    tx.payload(flip.slice());
                    header = null;

                    if (RpcHelper.DEBUG_SENDJSON) {
                      System.err.println(ProtocolMethodDispatch.deepToString(response.statusEnum(),
                          response, StandardCharsets.UTF_8.decode((ByteBuffer) tx.payload()
                              .duplicate().rewind())));
                    }

                    HttpStatus httpStatus = response.statusEnum();
                    switch (httpStatus) {
                      case $200:
                      case $201:
                        int remaining = Integer.parseInt(response.headerString(Content$2dLength));

                        if (remaining == tx.payload().remaining()) {
                          deliver();
                        } else {
                          tx.payload(ByteBuffer.allocateDirect(remaining).put(tx.payload()));
                        }
                        break;
                      default: // error
                        phaser.forceTermination();
                        channel.close();
                    }
                  }
                } else {
                  int read = read(channel, tx.payload());
                  if (-1 == read) {
                    phaser.forceTermination();

                    channel.close();
                    return;
                  }
                  if (!tx.payload().hasRemaining()) {
                    tx.payload().flip();
                    deliver();
                  }
                }
              }
            });
          }
        }

        void deliver() {
          tx.payload(tx.payload());
          phaser.arrive();
          recycleChannel(channel);
        }
      });
      try {
        phaser.awaitAdvanceInterruptibly(phaser.arrive(), REALTIME_CUTOFF, REALTIME_UNIT);
      } catch (TimeoutException | InterruptedException e) {
        if (RpcHelper.DEBUG_SENDJSON) {
          System.err.println("!!! " + ProtocolMethodDispatch.deepToString(this, e) + "\n\tfrom");
          dbKeysBuilder.trace().printStackTrace();
        }
      }
    }
  },

  BlobSend {
    public void visit(DbKeysBuilder dbKeysBuilder, final Tx tx) throws Exception {
      final Phaser phaser = new Phaser(2);
      final HttpRequest request = tx.state().$req();
      final ByteBuffer payload = (ByteBuffer) dbKeysBuilder.<ByteBuffer> get(etype.blob).rewind();
      String x = null;

      for (Object o : new Object[] {
          dbKeysBuilder.get(mimetypeEnum), dbKeysBuilder.get(mimetype), MimeType.bin}) {
        if (null != o) {
          if (o instanceof MimeType) {
            MimeType mimeType = (MimeType) o;
            x = mimeType.contentType;
          } else
            x = String.valueOf(o);
          break;
        }
      }

      String db = (String) dbKeysBuilder.get(etype.db);
      String docId = (String) dbKeysBuilder.get(etype.docId);
      String rev = (String) dbKeysBuilder.get(etype.rev);
      String attachname = dbKeysBuilder.get(etype.attachname);
      final String sb =
          scrub('/' + db + '/' + docId + '/'
              + URLEncoder.encode(attachname, StandardCharsets.UTF_8.displayName()) + "?rev=" + rev);

      final String ctype = x;
      final SocketChannel channel = createCouchConnection();

      enqueue(channel, OP_WRITE, new Impl() {
        @Override
        public void onWrite(SelectionKey key) throws Exception {

          int limit = payload.limit();
          ByteBuffer as =
              request.method(PUT).path(sb).headerString(Expect, "100-Continue").headerString(
                  Content$2dType, ctype).headerString(Accept, MimeType.json.contentType)
                  .headerString(Content$2dLength, String.valueOf(limit)).asByteBuffer();
          channel.write((ByteBuffer) as.rewind());
          key.interestOps(OP_READ);
        }

        @Override
        public void onRead(SelectionKey key) throws Exception {
          ByteBuffer[] byteBuffer = {ByteBuffer.allocateDirect(4 << 10)};
          int read = read(channel, byteBuffer[0]);
          HttpResponse httpResponse = request.$res();

          Rfc822HeaderState apply = httpResponse.read((ByteBuffer) byteBuffer[0].flip());
          HttpStatus httpStatus = httpResponse.statusEnum();
          switch (httpStatus) {
            case $100:
              key.interestOps(OP_WRITE).attach(new Impl() {
                public HttpResponse response = (HttpResponse) request.$res().headerInterest(
                    Content$2dLength);
                private ByteBuffer cursor;

                @Override
                public void onWrite(SelectionKey key) throws Exception {
                  FSM.write(key, payload);
                  if (!payload.hasRemaining()) {
                    key.interestOps(OP_READ);
                  }
                }

                boolean finish = false;

                void deliver() throws InterruptedException, BrokenBarrierException {

                  tx.payload().rewind();
                  phaser.arrive();
                  recycleChannel(channel);
                }

                public void onRead(SelectionKey key) throws Exception {

                  if (null == tx.payload())
                    tx.payload(ByteBuffer.allocateDirect(4 << 10));
                  int read = read(channel, tx.payload());
                  if (-1 == read) {
                    phaser.forceTermination();
                    key.cancel();
                    return;
                  }
                  if (finish) {
                    if (!tx.payload().hasRemaining()) {
                      deliver();
                    }
                    return;
                  }
                  ByteBuffer flip = (ByteBuffer) tx.payload().duplicate().flip();
                  response.read(flip);
                  finish =
                      Rfc822HeaderState.suffixMatchChunks(ProtocolMethodDispatch.HEADER_TERMINATOR,
                          response.headerBuf());
                  if (finish) {
                    switch (response.statusEnum()) {
                      case $201:
                      case $200:
                        int i = Integer.parseInt(response.headerString(Content$2dLength));
                        if (flip.remaining() == i) {
                          tx.payload(flip.slice());
                          deliver();
                        } else
                          tx.payload(ByteBuffer.allocateDirect(i).put(flip));
                        return;
                      default:
                        phaser.forceTermination();
                        key.cancel();
                    }
                  }
                }

              });
          }
        }
      });

      try {
        phaser.awaitAdvanceInterruptibly(phaser.arrive(), REALTIME_CUTOFF, REALTIME_UNIT);
      } catch (Exception e) {
        if (RpcHelper.DEBUG_SENDJSON) {
          System.err.println("!!! " + ProtocolMethodDispatch.deepToString(this, e) + "\n\tfrom");
          dbKeysBuilder.trace().printStackTrace();
        }
      }
    }

  };
  public static final byte[] CE_TERMINAL = "\n0\r\n\r\n".getBytes(StandardCharsets.UTF_8);
  // "premature optimization" s/mature/view/
  public static final String[] STATIC_VF_HEADERS = Rfc822HeaderState.staticHeaderStrings(ETag,
      Content$2dLength, Transfer$2dEncoding);
  public static final String[] STATIC_JSON_SEND_HEADERS = Rfc822HeaderState.staticHeaderStrings(
      ETag, Content$2dLength, Content$2dEncoding);
  public static final String[] STATIC_CONTENT_LENGTH_ARR = Rfc822HeaderState
      .staticHeaderStrings(Content$2dLength);
  public static final TimeUnit REALTIME_UNIT = TimeUnit.valueOf(Config.get("RXF_REALTIME_UNIT",
      RpcHelper.isDEBUG_SENDJSON() ? TimeUnit.HOURS.name() : TimeUnit.SECONDS.name()));
  public static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);
  public static final int REALTIME_CUTOFF = Integer
      .parseInt(Config.get("RXF_REALTIME_CUTOFF", "3"));
  public static final String PCOUNT = "-0xdeadbeef.2";
  public static final String GENERATED_METHODS = "/*generated methods vsd78vs0fd078fv0sa78*/";
  public static final String IFACE_FIRE_TARGETS = "/*fire interface ijnoifnj453oijnfiojn h*/";
  public static final String FIRE_METHODS = "/*embedded fire terminals j63l4k56jn4k3jn5l63l456jn*/";
  private static GsonBuilder BUILDER;

  static {
    GsonBuilder gsonBuilder =
        new GsonBuilder()
            .setDateFormat(Config.get("GSON_DATEFORMAT", "yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
            .setFieldNamingPolicy(
                FieldNamingPolicy.valueOf(Config.get("GSON_FIELDNAMINGPOLICY", "IDENTITY")));
    if ("true".equals(Config.get("GSON_PRETTY", "true")))
      gsonBuilder.setPrettyPrinting();
    if ("true".equals(Config.get("GSON_NULLS", "false")))
      gsonBuilder.serializeNulls();
    if ("true".equals(Config.get("GSON_NANS", "false")))
      gsonBuilder.serializeSpecialFloatingPointValues();

    builder(gsonBuilder);
  }

  /**
   * a lazy singleton that churns at an increment of 10k usages to free up potential threadlocals or other statics.
   * 
   * @return Gson object
   */
  public static Gson gson() {

    return (null == GSON || 0 == ATOMIC_INTEGER.incrementAndGet() % 10000) ? GSON =
        builder().create() : GSON;
  }

  /**
   * allow non-rxf code on registration of type adapters to null the gson used by the driver.
   * 
   * @param v null to rebuild with new TypeAdapters
   */

  public static void gson(Gson v) {

    GSON = v;
  }

  private static Gson GSON = builder().create();

  private static String s1 = "";

  public static String scrub(String scrubMe) {

    return null == scrubMe ? null : scrubMe.trim().replace("//", "/").replace("..", ".");
  }

  /**
   * the CouchDriver needs a builder that a client may access soas to register gson TypeAdapters.
   * 
   * @return the builder
   */
  public static GsonBuilder builder() {
    return BUILDER;
  }

  /**
   * sets the GsonBuilder for the driver and those depending on its gson marshalling.
   * 
   * @param BUILDER
   */
  public static void builder(GsonBuilder BUILDER) {
    CouchMetaDriver.BUILDER = BUILDER;
  }

  public void visit() throws Exception {
    DbKeysBuilder dbKeysBuilder = (DbKeysBuilder) DbKeysBuilder.get();
    Tx tx = Tx.current();

    if (!dbKeysBuilder.validate()) {

      throw new Error("validation error");
    }
    visit(dbKeysBuilder, tx);
  }

  /* abstract */
  public void visit(DbKeysBuilder dbKeysBuilder, Tx tx) throws Exception {
    throw new AbstractMethodError();
  }

  public static enum etype {

    opaque, db, docId, rev {
      /**
       * couchdb only returns a quoted etag for entities. this quoted etag breaks in queries sent back to couchdb as
       * rev="breakage"
       * 
       * @param data
       * @param <T>
       * @return
       */
      @Override
      public <T> boolean validate(T... data) {
        final String t = (String) data[0];
        return 0 < t.toString().length() && !t.startsWith("\"") && !t.endsWith("\"");
      }
    },
    attachname, designDocId, view, validjson, mimetype, mimetypeEnum {
      {
        clazz = MimeType.class;
      }
    },
    blob {
      {
        clazz = ByteBuffer.class;
      }
    },
    type {
      {
        clazz = Type.class;
      }
    },
    keyType {
      {
        clazz = Type.class;
      }
    };

    public <T> boolean validate(T... data) {
      return true;
    }

    public Class clazz = String.class;
  }
}
