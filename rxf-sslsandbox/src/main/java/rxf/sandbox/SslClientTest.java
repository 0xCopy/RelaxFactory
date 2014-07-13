package rxf.sandbox;

import one.xio.AsioVisitor.Helper.*;
import one.xio.AsioVisitor.SslVisitor.Minimal;
import one.xio.HttpMethod;
import rxf.core.Rfc822HeaderState;
import rxf.core.Server;
import rxf.core.TerminalBuilder;
import rxf.core.Tx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.concurrent.Executors;

import static one.xio.AsioVisitor.Helper.*;

/**
 * Created by jim on 7/12/14.
 */
public class SslClientTest {

  public static final String HTTPSDEST = "httpbin.org";

  public static void main(final String[] args) throws IOException {
/*

    Object content = new URL("https://" + HTTPSDEST).getContent();
    if(!Server.killswitch)System.exit(0 );
*/


    Minimal.setExecutorService(Executors.newCachedThreadPool());
    Minimal.getExecutorService().submit(new Runnable() {
      @Override
      public void run() {
        try {
          Server.init(null);

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    });
    SocketChannel open = SocketChannel.open();
    open.configureBlocking(false);
     open.connect(new InetSocketAddress(HTTPSDEST, 443));

    final ByteBuffer req = new Rfc822HeaderState().asRequest().path("/").method(HttpMethod.GET).headerStrings(new LinkedHashMap<String, String>() {{
      put("Host", HTTPSDEST);
    }}).asByteBuffer();
    final Tx tx = new Tx() {
      @Override
      public TerminalBuilder fire() {
        return null;
      }
    };
    Server.enqueue(open, SelectionKey.OP_CONNECT, toConnect(new F() {
      @Override
      public void apply(SelectionKey key) throws Exception {
        if (((SocketChannel) key.channel()).finishConnect()) {
          Minimal.client(HTTPSDEST).apply(key);

          finishWrite(key, new F() {
            @Override
            public void apply(SelectionKey key) throws Exception {
              toRead(new F() {
                @Override
                public void apply(SelectionKey key) throws Exception {
                  int bytesRead = tx.readHttpResponse(key);
                  if (0 == bytesRead) return;
                  if (null != tx.payload()) {
                    //done.
                    System.err.println(tx.state().asResponseHeaderString());
                  }
                }
              });
            }
          }, (ByteBuffer) req);

        }
      }
    }));
    synchronized (args) {
      try {
        args.wait();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

}
