package rxf.rpc;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.*;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpHeaders;
import one.xio.HttpStatus;
import one.xio.MimeType;
import rxf.core.CouchNamespace;
import rxf.core.Tx;
import rxf.web.inf.OpInterest;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.file.Paths;
import java.text.ParseException;

import static bbcursive.Cursive.post.rewind;
import static bbcursive.Cursive.pre.debug;
import static bbcursive.std.str;
import static java.nio.charset.StandardCharsets.UTF_8;
import static one.xio.AsioVisitor.Helper.F;
import static one.xio.AsioVisitor.Helper.finishWrite;
import static one.xio.AsioVisitor.Helper.park;

// import static bbcursive.Cursive.std.asString;

// import static one.xio.AsioVisitor.Cursive.pre.*;

/**
 * assumes POST using Content-Length -- chunked encoding will drop on the floor User: jim Date: 6/3/12 Time: 7:42 PM
 */
@OpInterest(SelectionKey.OP_WRITE)
public class GwtRpcVisitor extends Impl implements SerializationPolicyProvider {

  private final Object delegate;
  private Tx tx = Tx.current();

  public GwtRpcVisitor() {
    this(null);
  }

  public GwtRpcVisitor(Object delegate) {

    if (delegate == null) {
      delegate = this;
    }
    this.delegate = delegate;
  }

  @Override
  public void onWrite(SelectionKey key) throws Exception {
    park(key, new F() {
      @Override
      public void apply(SelectionKey key) throws Exception {
        RpcHelper.getEXECUTOR_SERVICE().submit(new GwtRpcTask());

      }
    });
  }

  public SerializationPolicy getSerializationPolicy(String moduleBaseURL, String strongName) {
    // TODO cache policies in weakrefmap? cleaner than reading from fs?

    // Translate the module path to a path per the filesystem, and grab a stream
    InputStream is;
    String fileName;
    try {
      String path = new URL(moduleBaseURL).getPath();
      fileName = SerializationPolicyLoader.getSerializationPolicyFileName(path + strongName);
      is =
          new FileInputStream(String.valueOf(Paths.get(CouchNamespace.RXF_CONTENT_ROOT, fileName)));
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
    }
    return serializationPolicy;
  }

  private class GwtRpcTask implements Runnable {

    public GwtRpcTask() {
    }

    public void run() {

      Class<?> aClass = delegate.getClass();
      ByteBuffer payload1 = tx.payload();
      RPCRequest rpcRequest =
          RPC.decodeRequest(str(payload1, rewind, debug), aClass, GwtRpcVisitor.this);
      String payload = null;
      try {
        Tx.current(tx);
        payload =
            RPC.invokeAndEncodeResponse(delegate, rpcRequest.getMethod(), rpcRequest
                .getParameters(), rpcRequest.getSerializationPolicy(), rpcRequest.getFlags());
      } catch (IncompatibleRemoteServiceException | SerializationException | RpcTokenException ex) {
        try {
          payload = RPC.encodeResponseForFailure(null, ex);
        } catch (SerializationException e) {
          e.printStackTrace();
        }
      }
      tx.payload(UTF_8.encode(payload));

      tx.hdr().$res().status(HttpStatus.$200).headerString(HttpHeaders.Content$2dType,
          MimeType.json.contentType).headerString(HttpHeaders.Content$2dLength,
          String.valueOf(UTF_8.encode(payload).limit()));

      finishWrite(tx.key(), new F() {
        @Override
        public void apply(SelectionKey key) throws Exception {
          key.interestOps(SelectionKey.OP_READ).attach(null);
        }
      }, (ByteBuffer) tx.hdr().asResponse().asByteBuffer().rewind(), (ByteBuffer) tx.payload()
          .rewind());

    }
  }
}