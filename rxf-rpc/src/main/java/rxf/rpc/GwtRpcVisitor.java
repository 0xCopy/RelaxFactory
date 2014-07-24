package rxf.rpc;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.server.rpc.*;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpHeaders;
import one.xio.HttpStatus;
import one.xio.MimeType;
import rxf.core.Tx;
import rxf.shared.PreRead;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import static one.xio.AsioVisitor.Helper.*;

/**
 * User: jim Date: 6/3/12 Time: 7:42 PM
 */
@PreRead
public class GwtRpcVisitor extends Impl implements SerializationPolicyProvider {

  private final Object delegate;

  public GwtRpcVisitor() {
    this(null);
  }

  public GwtRpcVisitor(Object delegate) {
    if (delegate == null) {
      delegate = this;
    }
    this.delegate = delegate;
  }

  Tx tx = Tx.current(new Tx());

  public void onRead(final SelectionKey key) throws Exception {
    tx.readHttpHeaders(key);
    if (null != tx.payload())
      finishRead(key, tx.payload(), new Runnable() {
        @Override
        public void run() {
          toWrite(key, new Helper.F() {
            @Override
            public void apply(final SelectionKey key) throws Exception {
              key.interestOps(0);
              RpcHelper.EXECUTOR_SERVICE.submit(new Runnable() {
                public void run() {
                  try {

                    RPCRequest rpcRequest =
                        RPC.decodeRequest(StandardCharsets.UTF_8.decode(
                            (ByteBuffer) tx.payload().rewind()).toString(), delegate.getClass(),
                            GwtRpcVisitor.this);
                    String payload;
                    try {
                      payload =
                          RPC.invokeAndEncodeResponse(delegate, rpcRequest.getMethod(), rpcRequest
                              .getParameters(), rpcRequest.getSerializationPolicy(), rpcRequest
                              .getFlags());
                    } catch (IncompatibleRemoteServiceException | RpcTokenException ex) {
                      payload = RPC.encodeResponseForFailure(null, ex);
                    }
                    ByteBuffer pbuf = StandardCharsets.UTF_8.encode(payload);
                    tx.payload(pbuf);

                    tx.state().$res().status(HttpStatus.$200).headerString(
                        HttpHeaders.Content$2dType, MimeType.json.contentType).headerString(
                        HttpHeaders.Content$2dLength, String.valueOf(pbuf.limit()));

                    finishWrite(key, new F() {
                      @Override
                      public void apply(SelectionKey key) throws Exception {
                        key.interestOps(SelectionKey.OP_READ).attach(null);
                      }
                    }, (ByteBuffer) tx.state().asResponse().asByteBuffer().rewind(),
                        (ByteBuffer) tx.payload().rewind());

                  } catch (Exception e) {
                    key.cancel();
                    e.printStackTrace(); // todo: verify for a purpose
                  } finally {
                  }
                }
              });

            }
          });
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
    }

    return serializationPolicy;
  }
}