package rxf.rpc;

import com.colinalworth.rpq.server.BatchInvoker;
import com.colinalworth.rpq.server.BatchServiceLocator;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.server.rpc.*;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpHeaders;
import one.xio.HttpStatus;
import one.xio.MimeType;
import rxf.core.Rfc822HeaderState;
import rxf.core.Rfc822HeaderState.HttpRequest;
import rxf.shared.PreRead;
import rxf.web.inf.ProtocolMethodDispatch;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

/**
 * User: jim Date: 6/3/12 Time: 7:42 PM
 */
@PreRead
public class RequestQueueVisitor extends Impl implements SerializationPolicyProvider {

  private HttpRequest req;
  private ByteBuffer cursor = null;
  private SocketChannel channel;
  private String payload;

  private final BatchInvoker invoker;

  public RequestQueueVisitor() {
    this(new BatchServiceLocator());
  }

  public RequestQueueVisitor(BatchServiceLocator locator) {
    this(new BatchInvoker(locator));
  }

  public RequestQueueVisitor(BatchInvoker invoker) {
    this.invoker = invoker;
  }

  @Override
  public void onRead(SelectionKey key) throws Exception {
    channel = (SocketChannel) key.channel();
    if (cursor == null) {
      if (key.attachment() instanceof Object[]) {
        Object[] ar = (Object[]) key.attachment();
        for (Object o : ar) {
          if (o instanceof ByteBuffer) {
            cursor = (ByteBuffer) o;
            continue;
          }
          if (o instanceof Rfc822HeaderState) {
            req = ((Rfc822HeaderState) o).$req();
          }
        }
      }
      key.attach(this);
    }
    cursor =
        null == cursor ? ByteBuffer.allocateDirect(4 << 10) : cursor.hasRemaining() ? cursor
            : ByteBuffer.allocateDirect(cursor.capacity() << 1).put((ByteBuffer) cursor.rewind());
    int read = channel.read(cursor);
    if (read == -1)
      key.cancel();
    Buffer flip = cursor.duplicate().flip();
    req = (HttpRequest) req.headerInterest(HttpHeaders.Content$2dLength).read((ByteBuffer) flip);
    if (!Rfc822HeaderState.suffixMatchChunks(ProtocolMethodDispatch.HEADER_TERMINATOR, req
        .headerBuf())) {
      return;
    }
    cursor = cursor.slice();
    int remaining = Integer.parseInt(req.headerString(HttpHeaders.Content$2dLength));
    final RequestQueueVisitor prev = this;
    if (cursor.remaining() != remaining) {
      key.attach(new Impl() {
        @Override
        public void onRead(SelectionKey key) throws Exception {
          int read1 = channel.read(cursor);
          if (read1 == -1) {
            key.cancel();
          }
          if (!cursor.hasRemaining()) {
            key.interestOps(SelectionKey.OP_WRITE).attach(prev);
          }
        }
      });
    } else {
      key.interestOps(SelectionKey.OP_WRITE);
    }
  }

  @Override
  public void onWrite(final SelectionKey key) throws Exception {
    if (payload == null) {
      key.interestOps(0);
      RpcHelper.EXECUTOR_SERVICE.submit(new Runnable() {
        @Override
        public void run() {
          try {
            String reqPayload =
                StandardCharsets.UTF_8.decode((ByteBuffer) cursor.rewind()).toString();

            RPCRequest rpcRequest =
                BatchInvoker.decodeRequest(reqPayload, null, RequestQueueVisitor.this);

            try {
              payload =
                  RPC.invokeAndEncodeResponse(invoker, rpcRequest.getMethod(), rpcRequest
                      .getParameters(), rpcRequest.getSerializationPolicy(), rpcRequest.getFlags());
            } catch (IncompatibleRemoteServiceException ex) {
              payload = RPC.encodeResponseForFailure(null, ex);
            } catch (RpcTokenException ex) {
              payload = RPC.encodeResponseForFailure(null, ex);
            }
            ByteBuffer pbuf = (ByteBuffer) StandardCharsets.UTF_8.encode(payload).rewind();
            final int limit = pbuf.rewind().limit();
            Rfc822HeaderState.HttpResponse res = req.$res();
            res.status(HttpStatus.$200);
            ByteBuffer as =
                res.headerString(HttpHeaders.Content$2dType, MimeType.json.contentType)
                    .headerString(HttpHeaders.Content$2dLength, String.valueOf(limit)).as(
                        ByteBuffer.class);
            int needed = as.rewind().limit() + limit;

            cursor =
                (ByteBuffer) ((ByteBuffer) (cursor.capacity() >= needed ? cursor.clear().limit(
                    needed) : ByteBuffer.allocateDirect(needed))).put(as).put(pbuf).rewind();

            key.interestOps(SelectionKey.OP_WRITE);
          } catch (Exception e) {
            key.cancel();
            e.printStackTrace(); // todo: verify for a purpose
          } finally {
          }
        }
      });
      return;
    }
    int write = channel.write(cursor);
    if (!cursor.hasRemaining()) {
      /*
       * Socket socket = channel.socket(); socket.getOutputStream().flush(); socket.close();
       */
      key.interestOps(SelectionKey.OP_READ).attach(null);
    }

  }

  public final SerializationPolicy getSerializationPolicy(String moduleBaseURL, String strongName) {
    // TODO cache policies in weakrefmap? cleaner than reading from fs?

    // Translate the module path to a path on the filesystem, and grab a stream
    InputStream is = null;
    String fileName;
    try {
      String path = new URL(moduleBaseURL).getPath();
      fileName = SerializationPolicyLoader.getSerializationPolicyFileName(path + strongName);
      is = new File("./" + fileName).toURI().toURL().openStream();
    } catch (MalformedURLException e1) {
      System.out.println("ERROR: malformed moduleBaseURL: " + moduleBaseURL);
      return null;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }

    SerializationPolicy serializationPolicy = null;
    try {
      serializationPolicy = SerializationPolicyLoader.loadFromStream(is, null);
    } catch (ParseException e) {
      System.out.println("ERROR: Failed to parse the policy file '" + fileName + "'");
    } catch (IOException e) {
      System.out.println("ERROR: Could not read the policy file '" + fileName + "'");
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }

    return serializationPolicy;
  }
}