package rxf.core;

import one.xio.AsioVisitor.Helper;
import one.xio.AsioVisitor.Helper.F;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import static one.xio.AsioVisitor.Helper.Do.post.grow;
import static one.xio.AsioVisitor.Helper.Do.pre.*;
import static one.xio.AsioVisitor.Helper.asString;
import static one.xio.AsioVisitor.Helper.on;
import static one.xio.HttpHeaders.*;

/**
 * when a transaction requires an inner call to couchdb or some other tier this abstraction holds a reversible header
 * state for outbound request and inbound response, and as of this comment a payload for the inbound results that a
 * service might provide, to take the place of more complex fire() grammars.
 * <p/>
 * User: jim Date: 5/29/12 Time: 1:58 PM
 */
public class Tx {

  public static final ByteBuffer NIL = ByteBuffer.allocate(0);

  /**
   * if the attachment is a tx, we resume filling headers and payload by keep. if the attachment is not Tx, it is set to
   * a fresh one.
   * <p/>
   * for TOP level default visitor root only!
   * 
   * @param key selectionKey
   * @return a tx
   */
  public static Tx acquireTx(SelectionKey key) {
    Object attachment = key.attachment();
    if (attachment instanceof Object[]) {
      Object[] objects = (Object[]) attachment;
      if (objects.length == 0)
        attachment = null;
      if (objects.length == 1)
        attachment = objects[0];
    }
    Tx tx;
    if (attachment instanceof Tx) {
      tx = current((Tx) attachment);
    } else
      tx = current(new Tx());
    key.attach(tx);
    return tx;
  }

  /**
   * @param key selectionKey
   * @return a tx
   */
  public static <T, C extends Class<T>> T queryAttachments(SelectionKey key, C c) {
    Object attachment = key.attachment();
    if (!(attachment instanceof Object[])) {
      attachment = new Object[] {attachment};
    }
    Object[] objects = (Object[]) attachment;
    switch (objects.length) {
      case 0:
        break;
      default:
        for (Object object : objects) {
          if (c.isAssignableFrom(object.getClass()))
            return (T) object;
        }
        break;
    }

    return null;
  }

  public static ByteBuffer getNextChunk(ByteBuffer in) {

    String lenString = asString(on(in, duplicate, slice, skipWs, toEol, rtrim, flip));
    int needs;
    try {
      needs = Integer.parseInt(lenString, 0x10);
      if (0 == needs)
        return NIL;
    } catch (NumberFormatException e) {
      e.printStackTrace();
      return null;// shhould never happen
    }
    if (in.remaining() < needs) {
      // returns the condition of chunk position !=0
      // in.Hasremaining==false
      return ByteBuffer.allocateDirect(needs).put(in);
    } else {
      // returns chunk position 0
      // in.hasRemaining=true.
      ByteBuffer result = (ByteBuffer) in.slice().limit(needs);
      in.position(in.position() + needs);
      return result;
    }
  }

  public String toString() {
    return "Tx{" + "key=" + key + ", headers=" + headers + ", payload=" + payload + '}';
  }

  /**
   * defaults.
   */
  public static final String[] HEADER_INTEREST = Rfc822HeaderState.staticHeaderStrings(ETag,
      Content$2dLength);
  public static final AbstractMethodError ABSTRACT_METHOD_ERROR = new AbstractMethodError(
      "no fire() here!");

  /**
   * not threadlocal headers
   */
  private SelectionKey key;

  /**
   * not threadlocal headers.
   * <p/>
   * this provides the REST details we wish to convey for a request and provides the headers per failure/success OOB.
   */
  private AtomicReference<Rfc822HeaderState> headers = new AtomicReference<>();

  /**
   * payload, took 2 years to deduce ByteBuffer is the most popular solution by far.
   * <p/>
   * //todo: may simplify the phaser/cyclical payloads being used and enable locking
   */
  private ByteBuffer payload;

  /**
   * threadlocal headers pumped in to outer thread to hold #headers and #key above
   */
  private static ThreadLocal<Tx> current = new InheritableThreadLocal<>();

  /**
   * there can be only one [per thread]
   */
  public Tx() {
    current.set(this);
  }

  /**
   * <ol>
   * <p/>
   * convenince method for http protocol which
   * <li>creates a buffer for {@link #headers} if current one is null.</li>
   * <li>grows buffer if current one is full</li>
   * <li>parses buffer for http results</li>
   * <li>reads contentlength if present and sizes {@link #payload} suitable for
   * {@link Helper#finishRead(java.nio.ByteBuffer, F)}</li>
   * <li>else throws remainder slice of buffer into {@link #payload} as a full buffer where hasRemaining() is false</li>
   * </ol>
   * <p/>
   * <p/>
   * if chunked encoding is indicated, the first chunk length is parsed and the payload is sized to the indicated chunk
   * size,
   * 
   * @param key
   * @return
   * @throws IOException
   */
  public Tx readHttpHeaders(SelectionKey key) throws Exception {
    Rfc822HeaderState state = state();
    ByteBuffer byteBuffer = state.headerBuf();
    if (null == byteBuffer)
      state.headerBuf(byteBuffer = ByteBuffer.allocateDirect(4 << 10));
    if (!byteBuffer.hasRemaining())
      state
          .headerBuf(byteBuffer =
              ByteBuffer.allocateDirect(byteBuffer.capacity() << 1).put(
                  (ByteBuffer) byteBuffer.flip()));
    int prior = byteBuffer.position(); // if the headers are extensive, this may be a buffer that has been extended
    int read = Helper.read(key, byteBuffer);

    switch (read) {
      case -1:
        key.cancel();
      case 0:
        return null;
      default:
        System.err.println("<?? "
            + StandardCharsets.UTF_8.decode((ByteBuffer) byteBuffer.duplicate().flip()));
        Buffer flip1 = byteBuffer.flip();
        Rfc822HeaderState rfc822HeaderState =
            state.addHeaderInterest(Content$2dLength).addHeaderInterest(Transfer$2dEncoding);
        boolean apply = rfc822HeaderState.apply((ByteBuffer) flip1);
        if (apply) {
          ByteBuffer slice = ((ByteBuffer) byteBuffer.duplicate().limit(prior + read)).slice();

          if ("POST".equals(state.asRequest().protocol())) {
            String anObject = state.headerString(Transfer$2dEncoding);
            if ("chunked".equals(anObject)) {
              payload(slice);
            }
          } else
            try {
              int remaining = Integer.parseInt(state.headerString(Content$2dLength.getHeader()));
              payload(ByteBuffer.allocateDirect(remaining).put(slice));
            } catch (NumberFormatException e) {
              payload(slice);
            }
        }
        break;
    }
    return this;
  }

  public TerminalBuilder fire() {
    throw new AbstractMethodError();
  }

  public Rfc822HeaderState state() {
    Rfc822HeaderState ret = headers.get();
    if (null == ret)
      headers.set(ret = new Rfc822HeaderState(HEADER_INTEREST));
    return ret;
  }

  public SelectionKey key() {
    return key;
  }

  public Tx state(Rfc822HeaderState state) {
    headers.set(state);
    return this;
  }

  public Tx key(SelectionKey key) {
    this.key = key;
    return this;
  }

  /**
   * @return a lazily created threadlocal Tx
   */
  public static Tx current() {
    Tx tx = current.get();
    if (null == tx)
      current(tx = new Tx() {

        public TerminalBuilder fire() {
          throw ABSTRACT_METHOD_ERROR;
        }
      });
    return tx;
  }

  public static Tx current(Tx tx) {
    current.set(tx);
    return tx;
  }

  public ByteBuffer payload() {
    return payload;
  }

  public Tx payload(ByteBuffer payload) {
    this.payload = payload;
    return this;
  }

  Tx clear() {
    return payload(null).state(null);
  }

  public ArrayList<ByteBuffer> decodeChunkedEncoding() throws InterruptedException,
      BrokenBarrierException, TimeoutException {
    // create a list of source buffers and then index them
    ArrayList<ByteBuffer> copies = new ArrayList<>();
    while (true) {
      boolean needsRead = false;
      try {
        ByteBuffer payload = payload();
        ByteBuffer nextChunk = getNextChunk(payload);
        if (null != nextChunk) {
          if (0 == nextChunk.limit()) {
            break;// 2 bytes remain in payload.
          }
          copies.add(on(nextChunk, debug));
        } else {
          needsRead = true;
        }

        if (!payload.hasRemaining())// payload has been fully consumed by nextChunk copy
        {
          if (nextChunk != null && nextChunk.hasRemaining()) {
            Helper.syncFill(key(), nextChunk, post.rewind);
          }
          needsRead = true;
        }

      } catch (BufferUnderflowException e) {
        payload(on(payload(), grow));
        needsRead = true;
      }
      if (needsRead) {
        if (!payload().hasRemaining()) {
          payload(ByteBuffer.allocateDirect(4 << 10));
        }
        final CyclicBarrier cyclicBarrier1 = new CyclicBarrier(2);
        Helper.toRead(key(), new F() {
          @Override
          public void apply(SelectionKey key) throws Exception {
            int read = Helper.read(key, payload());
            if (0 != read) {
              cyclicBarrier1.await();
            }
          }
        });
        cyclicBarrier1.await(1, TimeUnit.HOURS);
      }
    }
    return copies;
  }
}