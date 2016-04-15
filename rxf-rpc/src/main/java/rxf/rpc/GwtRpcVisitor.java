package rxf.rpc;

import bbcursive.Cursive;
import bbcursive.Cursive.pre;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.*;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpHeaders;
import one.xio.HttpStatus;
import rxf.core.CouchNamespace;
import rxf.core.Tx;
import rxf.web.inf.OpInterest;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.file.Paths;
import java.text.ParseException;

import static bbcursive.lib.log.log;
import static bbcursive.std.bb;
import static bbcursive.std.str;
import static one.xio.AsioVisitor.Helper.*;

// import static bbcursive.Cursive.std.asString;

// import static one.xio.AsioVisitor.Cursive.pre.*;

/**
 * assumes POST using Content-Length -- chunked encoding will drop on the floor User: jim Date: 6/3/12 Time: 7:42 PM
 */
@OpInterest(SelectionKey.OP_WRITE)
public class GwtRpcVisitor extends Impl implements SerializationPolicyProvider {

  private final Object delegate;

  public GwtRpcVisitor() {
    tx = Tx.current();
    delegate = null;
  }

  private Tx tx;

  public GwtRpcVisitor(Object delegate, Tx tx) {
    this.tx = tx;
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
    String fileName = null;
    SerializationPolicy serializationPolicy = null;
    try {
      String path = new URL(moduleBaseURL).getPath();
      fileName = SerializationPolicyLoader.getSerializationPolicyFileName(path + strongName);
      try (FileInputStream fileInputStream =
          new FileInputStream(String.valueOf(Paths.get(CouchNamespace.RXF_CONTENT_ROOT, fileName)))) {
        serializationPolicy = SerializationPolicyLoader.loadFromStream(fileInputStream, null);
      }
    } catch (ParseException e) {
      log("ERROR: Failed to parse the policy file " + Paths.get(fileName).toUri().toASCIIString());
    } catch (IOException e) {
      log("ERROR: Could not read the policy file " + Paths.get(fileName).toUri().toASCIIString());
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return serializationPolicy;
  }

  private class GwtRpcTask implements Runnable {

    public GwtRpcTask() {
    }

    public void run() {

      Class<?> aClass = delegate.getClass();
      log(aClass, "rpc for ");
      String payload = str(tx.payload(), pre.flip);
      log(payload, "rpc payload ");
      RPCRequest rpcRequest = RPC.decodeRequest((payload), aClass, GwtRpcVisitor.this);
      try {
        payload = RPC.invokeAndEncodeResponse(delegate, //
            rpcRequest.getMethod(), //
            rpcRequest.getParameters(), //
            rpcRequest.getSerializationPolicy(), //
            rpcRequest.getFlags());//

        log(payload, "RPC might've worked....");

      } catch (IncompatibleRemoteServiceException | SerializationException | RpcTokenException ex) {
        try {
          payload = RPC.encodeResponseForFailure(null, ex);
        } catch (SerializationException e) {
          e.printStackTrace();
        }
      }
      ByteBuffer bb = bb(payload, Cursive.pre.debug);
      tx.payload(bb);

      tx.hdr().asResponse().status(HttpStatus.$200).headerString(HttpHeaders.Content$2dType,
          "text/x-gwt-rpc; charset=UTF-8").headerString(HttpHeaders.Content$2dLength,
          "" + ((tx.payload()).limit()));

      finishWrite(tx.key(), new F() {
        @Override
        public void apply(SelectionKey key) throws Exception {
          bye(key);
        }
      }, (ByteBuffer) tx.hdr().asResponse().asByteBuffer().rewind(), (ByteBuffer) tx.payload()
          .rewind());
    }
  }
}