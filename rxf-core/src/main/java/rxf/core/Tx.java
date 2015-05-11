package rxf.core;

import bbcursive.WantsZeroCopy;
import bbcursive.std;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.io.CharStreams;
import one.xio.AsioVisitor.Helper;
import one.xio.AsioVisitor.Helper.*;
import one.xio.HttpHeaders;
import rxf.core.Rfc822HeaderState.HttpRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.Buffer;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import static bbcursive.Cursive.pre.*;
import static bbcursive.std.*;
import static one.xio.AsioVisitor.Helper.*;
import static one.xio.HttpHeaders.*;

/**
 * when a transaction requires an inner call to couchdb or some other tier this abstraction holds a reversible header
 * state for outbound request and inbound response, and as of this comment a payload for the inbound results that a
 * service might provide, to take the place of more complex fire() grammars.
 * <p/>
 * User: jim Date: 5/29/12 Time: 1:58 PM
 */
public class Tx implements WantsZeroCopy {

  /**
   * always an empty readonly buffer singleton
   */
  public static final ByteBuffer NIL = ByteBuffer.allocate(0).asReadOnlyBuffer();
  public static final char SEPARATOR = '&';
  public static final String ASSIGNMENT_OPERATOR = "=";
  private boolean chunked;
  private boolean noPayload;

  public Tx(SelectionKey selectionKey) {
    key(selectionKey);
  }

  /**
   * if the attachment is a tx, we resume filling headers and payload. if the attachment is not Tx, we return a fresh
   * one.
   * <p/>
   * for TOP level default visitor root only!
   * 
   * @param entryPoint selectionKey which is not part of a visitor presently
   * @param interest
   * @return a tx
   */
  public static Tx acquireTx(SelectionKey entryPoint, HttpHeaders... interest) {
    Object attachment = entryPoint.attachment();
    if (attachment instanceof Object[]) {
      Object[] objects = (Object[]) attachment;
      if (objects.length == 0)
        attachment = null;
      if (objects.length == 1)
        attachment = objects[0];
    }
    Tx tx = null;
    if (attachment instanceof Tx) {
      tx = current((Tx) attachment);
    } else {
      tx = current().clear();
      Rfc822HeaderState rfc822HeaderState = tx.hdr().addHeaderInterest(interest);
    }
    entryPoint.attach(tx);
    tx.key(entryPoint);
    return tx;
  }

  /**
   * @return a tx
   */
  public <T, C extends Class<T>> T queryAttachments(C c) {
    SelectionKey key = key();
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
   * @param headerInterest
   * @return true if sane. chunked()==true when the response is chunked.
   *         {@link #decodeChunkedEncoding(java.util.List, one.xio.AsioVisitor.Helper.F)} is an optional for the client
   *         caller, it may be desirable to get a chunk at a time.
   * @throws IOException
   */
  public boolean readHttpHeaders(HttpHeaders... headerInterest) throws Exception {
    assert null != key();
    Rfc822HeaderState state = hdr();
    if (headerInterest.length > 0)
      state.addHeaderInterest(headerInterest);
    ByteBuffer byteBuffer = state.headerBuf();
    if (null == byteBuffer)
      state.headerBuf(byteBuffer = alloc(4 << 10));
    if (!byteBuffer.hasRemaining())
      state
          .headerBuf(byteBuffer =
              alloc(byteBuffer.capacity() << 1).put(
                  (ByteBuffer) byteBuffer.flip()));
    int prior = byteBuffer.position(); // if the headers are extensive, this may be a buffer that has been extended
    assert key != null;
    int read = read(key, byteBuffer);

    switch (read) {
      case -1:
        key.cancel();
      case 0:
        return false;
      default:
        /*
         * System.err.println("<?? " + StandardCharsets.UTF_8.decode((ByteBuffer) byteBuffer.duplicate().flip()));
         */
        Buffer flip1 = byteBuffer.flip();
        Rfc822HeaderState rfc822HeaderState =
            noPayload() ? state : state.addHeaderInterest(Content$2dLength).addHeaderInterest(
                Transfer$2dEncoding);
        boolean apply = rfc822HeaderState.apply((ByteBuffer) flip1);
        if (apply) {
          ByteBuffer slice = ((ByteBuffer) byteBuffer.duplicate().limit(prior + read)).slice();

          String anObject = state.headerString(Transfer$2dEncoding);

          if (!"chunked".equals(anObject)) try {
            if (state.headerStrings().containsKey(Content$2dLength.getHeader())
                    ) {
              int remaining = parseInt(state.headerString(Content$2dLength.getHeader()));
              payload(alloc(remaining).put(slice));
            }
          } catch (Exception e) {
            payload(NIL);
          }
          else {
            payload(slice);
            chunked(true);
          }
          break;
        }
        break;
    }
    return true;
  }



  public boolean noPayload() {
    return noPayload;
  }

  /**
   * methods like HEAD may contain Content-Length and we dont want to attempt to fill the cursor with that.
   * 
   * @param t
   */
  public Tx noPayload(boolean t) {
    noPayload = t;
    return this;
  }

  /**
   * signals that this is transfer encoding chunked.
   * 
   * @param b
   */
  public void chunked(boolean b) {

    chunked = b;
  }

  public boolean chunked() {
    return chunked;
  }

  public static String formUrlEncode(Map<String, String> theMap) {
    return Joiner.on(SEPARATOR).withKeyValueSeparator(ASSIGNMENT_OPERATOR).join(
        FluentIterable.from(theMap.entrySet()).transform(new Function() {
          public Object apply(Object o) {
            Entry e = (Entry) o;
            e.setValue(URLEncoder.encode((String) e.getValue()));
            return e;
          }
        }));
  }

  public TerminalBuilder fire() {
    throw new AbstractMethodError();
  }

  public Rfc822HeaderState hdr() {
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

  public Tx key(SelectionKey key, HttpHeaders... headerInterest) {
    this.key = key;
    if (headerInterest.length > 0)
      hdr().addHeaderInterest(headerInterest);
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

  public Tx clear() {
    return payload(null).state(null);
  }

  /**
   * make payload integral regardless of chunked or content-length
   * 
   * @param success
   */
  public void finishPayload(final F success) {
    log(success, "finishPaylaod");
    if (chunked()) {
      final List<ByteBuffer> res = new ArrayList<>();
      try {
        payload().compact();
        decodeChunkedEncoding(res, new F() {
          @Override
          public void apply(SelectionKey key) throws Exception {
            ByteBuffer byteBuffer = cat(res);
            payload(byteBuffer);
            success.apply(key());
          }
        });
      } catch (Exception e) {
        key().cancel();
      }
    } else {
      finishRead(key(), payload(), success);
    }
  }

  public void decodeChunkedEncoding(final List<ByteBuffer> res, final F success) {
    assert key() != null;
    try {
      while (true)
        try {
          final ByteBuffer chunk = getNextChunk();
          if (NIL == chunk) {
            log(key, "decodeChunkSuccess", success.toString());
            success.apply(key());
            break;
          }
          F advance = new F() {
            @Override
            public void apply(SelectionKey key) throws Exception {
              res.add(bb(chunk, flip));
              decodeChunkedEncoding(res, success);
            }
          };
          if (!chunk.hasRemaining()) {
            advance.apply(key());
            continue;
          }
          finishRead(key(), chunk, advance);
          break;
        } catch (BufferUnderflowException e) {

          /**
           * due to sslengine permitting pre-fetch backlogs, the 1xio event depModel is screwed. however we can access
           * that backlog data anytime by calling read(), we just need to be immediate about it, and register normally
           * after an initial grab
           */

          toRead(key(), new F() {
            @Override
            public void apply(SelectionKey key) throws Exception {
              int read = read(key, payload().compact());
              if (-1 == read) {
                bye(key);
              } else {
                /* assert null != on(payload, debug); */
                park(key, new F() {
                  @Override
                  public void apply(SelectionKey key) throws Exception {
                    decodeChunkedEncoding(res, success);
                  }
                });
              }
            }
          });
          break;
        }
    } catch (Throwable e) {
      // e.printStackTrace();
    }
  }

  public ByteBuffer getNextChunk() throws BufferUnderflowException {
    bb(payload(), flip);
    int needs;
    try {

      needs = parseInt(bb(payload, mark, skipWs, slice, forceToEol, rtrim, flip));
    } catch (Exception e) {
      payload.reset();
      throw new BufferUnderflowException();
    }

    bb(payload, toEol);
    if (needs != 0) {
      ByteBuffer chunk;
      chunk = alloc(needs);
      std.push(payload, chunk);
      bb(payload, post.compact, debug);
      return chunk;
    }
    return NIL;
  }

  @Override
  public ByteBuffer asByteBuffer() {
    return payload();
  }

  public static void fetchGetContent(URL url, HttpRequest httpRequest,
      AtomicReference<String> payload, F onSuccess) throws Exception {
    final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

    try {

      urlConnection.setRequestMethod("GET");

      for (Entry<String, String> stringStringEntry : httpRequest.headerStrings().entrySet()) {
        urlConnection.setRequestProperty(stringStringEntry.getKey(), stringStringEntry.getValue());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    Object content = null;
    try {
      log(urlConnection.getRequestProperties().toString());
      content = urlConnection.getContent();
    } catch (IOException e) {
      String s = CharStreams.toString(new InputStreamReader(urlConnection.getErrorStream()));
      log(s, "!!!! fetchGetContent failure", url.toURI().toASCIIString());
      log(httpRequest.asRequestHeaderString(), url.toURI().toASCIIString());
    }
    String r = stringFromContent(content);

    log(r, "fetchGetContent", String.valueOf(url));
    payload.set(r);
    onSuccess.apply(null);

  }

  private static String stringFromContent(Object content) throws IOException {
    String r = null;
    if (content instanceof InputStream) {
      InputStream inputStream = (InputStream) content;
      r = CharStreams.toString(new InputStreamReader(inputStream));
    }
    if (content instanceof String) {
      r = (String) content;

    }
    return str(r);
  }

  public static void fetchPostContent(ByteBuffer reqPayload, HttpRequest httpRequest, URL url,
      F onSuccess, AtomicReference<String> payload) throws Exception {
    final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

    try {

      urlConnection.setRequestMethod("GET");

      for (Entry<String, String> stringStringEntry : httpRequest.headerStrings().entrySet()) {
        urlConnection.setRequestProperty(stringStringEntry.getKey(), stringStringEntry.getValue());
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    urlConnection.setRequestMethod("POST");
    urlConnection.setDoOutput(true);
    urlConnection.getOutputStream().write(
        push(reqPayload, ByteBuffer.wrap(new byte[reqPayload.limit()])).array());
    Object content = urlConnection.getContent();
    String r = stringFromContent(content);
    log(r, "fetchPostContent", String.valueOf(url));
    payload.set(r);
    onSuccess.apply(null);

  }
}