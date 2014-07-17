package rxf.core;

import one.xio.AsioVisitor.Helper;
import one.xio.AsioVisitor.Helper.F;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpHeaders.ETag;

/**
 * when a transaction requires an inner call to couchdb or some other tier this abstraction holds a reversible header
 * state for outbound request and inbound response, and as of this comment a payload for the inbound results that a
 * service might provide, to take the place of more complex fire() grammars.
 * 
 * User: jim Date: 5/29/12 Time: 1:58 PM
 */
public class Tx {

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
   * 
   * this provides the REST details we wish to convey for a request and provides the headers per failure/success OOB.
   */
  private AtomicReference<Rfc822HeaderState> headers = new AtomicReference<>();

  /**
   * payload, took 2 years to deduce ByteBuffer is the most popular solution by far.
   * 
   * //todo: may simplify the phaser/cyclical payloads being used and enable locking
   * 
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
   * 
   * convenince method for http protocol which
   * <li>creates a buffer for {@link #headers} if current one is null.</li>
   * <li>grows buffer if current one is full</li>
   * <li>parses buffer for http results</li>
   * <li>reads contentlength if present and sizes {@link #payload} suitable for
   * {@link Helper#finishRead(java.nio.ByteBuffer, F)}</li>
   * <li>else throws remainder slice of buffer into {@link #payload} as a full buffer</li>
   * </ol>
   * 
   * @param key
   * @return
   * @throws IOException
   */
  public int readHttpResponse(SelectionKey key) throws IOException {
    ByteBuffer byteBuffer = state().headerBuf();
    if (null == byteBuffer)
      state().headerBuf(byteBuffer = ByteBuffer.allocateDirect(4 << 10));
    if (!byteBuffer.hasRemaining())
      state().headerBuf(
          byteBuffer =
              ByteBuffer.allocateDirect(byteBuffer.capacity() << 1).put(
                  (ByteBuffer) byteBuffer.flip()));
    int prior = byteBuffer.position(); // if the headers are extensive, this may be a buffer that has been extended
    int read = Helper.read(key, byteBuffer);

    if (0 != read) // 0 per read is quite likely ssl intervention. just let this bounce through.
    {
      System.err.println("<?? " + StandardCharsets.UTF_8.decode((ByteBuffer) byteBuffer.duplicate().flip()));
      if (state().asResponse().apply(byteBuffer)) {
        ByteBuffer slice = ((ByteBuffer) byteBuffer.duplicate().limit(prior + read)).slice();
        try {
          int remaining = Integer.parseInt(state().headerString(Content$2dLength.getHeader()));
          payload(ByteBuffer.allocateDirect(remaining).put(slice));
        } catch (NumberFormatException e) {
          payload(slice);
        }
      }
    }
    return read;
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
}