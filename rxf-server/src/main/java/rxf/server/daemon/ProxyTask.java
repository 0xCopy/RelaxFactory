package rxf.server.daemon;

import one.xio.HttpMethod;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

/**
 * this launches the main service thread and assigns the proxy port and countdown ports to socketservers.
 * User: jnorthrup
 * Date: 10/1/13
 * Time: 7:27 PM
 */
public class ProxyTask implements Runnable {
  public String prefix;
  private String[] proxyPorts;

  @Override
  public void run() {
    try {
      InetSocketAddress socketAddress;
      ServerSocketChannel serverSocketChannel;
      for (String proxyPort : proxyPorts) {
        HttpMethod.enqueue(ServerSocketChannel
            .open()
            .bind(new InetSocketAddress(Integer.parseInt(proxyPort)), 4096)
            .setOption(StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE)
                    /*.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE)*/
            .configureBlocking(false), SelectionKey.OP_ACCEPT, new ProxyDaemon(this));
      }

    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

}
