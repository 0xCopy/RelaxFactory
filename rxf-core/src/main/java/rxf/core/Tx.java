package rxf.core;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.concurrent.atomic.AtomicReference;

import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpHeaders.ETag;

/**
 * 
 * when a transaction requires an inner call to couchdb or some other tier this abstraction holds a reversible header
 * state for outbound request and inbound response, and as of this comment a payload for the inbound results that a
 * service might provide, to take the place of more complex fire() grammars.
 * 
 * 
 * User: jim Date: 5/29/12 Time: 1:58 PM
 */
public abstract class Tx {
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
   * this provides the REST details we wish to convey for a request and provides the headers on failure/success OOB.
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
   * 
   */
  public Tx() {
    current.set(this);
  }

  public abstract TerminalBuilder fire();

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
        @Override
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
    return this.payload(null).state(null);
  };
}
