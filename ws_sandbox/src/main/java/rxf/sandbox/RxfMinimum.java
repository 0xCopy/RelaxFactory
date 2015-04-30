package rxf.sandbox;

import one.xio.AsioVisitor;
import one.xio.AsyncSingletonServer;
import rxf.core.Config;
import rxf.core.Rfc822HeaderState;
import rxf.core.Tx;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static bbcursive.std.log;
import static java.nio.channels.SelectionKey.OP_READ;
import static one.xio.AsioVisitor.Helper.F;
import static one.xio.AsyncSingletonServer.SingleThreadSingletonServer;
import static one.xio.AsyncSingletonServer.SingleThreadSingletonServer.enqueue;
import static one.xio.HttpHeaders.*;

public class RxfMinimum {

  private static int c = 0;

  public static void main(String... args) throws IOException {
    String host = args.length > 0 ? args[0] : "localhost";
    Integer port = args.length > 1 ? Integer.parseInt(args[1]) : 8888;

    final ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.configureBlocking(false);
    serverSocketChannel.bind(new InetSocketAddress(port));

    AsioVisitor.Impl root = new AsioVisitor.Impl() {
      @Override
      public void onAccept(SelectionKey key) throws Exception {
        SocketChannel accept = serverSocketChannel.accept();
        accept.configureBlocking(false);
        enqueue(accept, OP_READ);
      }

      @Override
      public void onRead(SelectionKey browserKey) throws Exception {
        final Tx tx =
            Tx.acquireTx(browserKey, Content$2dLength, Transfer$2dEncoding, Location, Cookie, Host);
        if (tx.readHttpHeaders())
          tx.finishPayload(new F() {
            @Override
            public void apply(SelectionKey browserKey) throws Exception {
              Rfc822HeaderState.HttpRequest httpRequest = tx.hdr().asRequest();
              log(httpRequest.asRequestHeaderString(), "Request " + c++);
              String pathx = httpRequest.path();

              if (pathx.matches("/wsinit")) {
                log("do something!");
                browserKey.cancel();
              }
            }
          });
      }

    };
    while (!AsyncSingletonServer.killswitch.get())
      try {
        SingleThreadSingletonServer.init(root);
      } catch (Throwable e) {
        e.printStackTrace();
      }
  }
}