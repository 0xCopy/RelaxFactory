package rxf.couch.daemon;

import rxf.core.Server;
import rxf.rpc.RpcHelper;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;

/**
 * this launches the main service thread and assigns the proxy port to socketservers. User: jnorthrup Date: 10/1/13
 * Time: 7:27 PM
 */
public class ProxyTask implements Runnable {
  public String prefix;
  public String[] proxyPorts;

  @Override
  public void run() {
    try {
      for (String proxyPort : proxyPorts) {
        Server.enqueue(ServerSocketChannel.open().bind(
            new InetSocketAddress(Integer.parseInt(proxyPort)), 4096).setOption(
            StandardSocketOptions.SO_REUSEADDR, Boolean.TRUE).configureBlocking(false),
            SelectionKey.OP_ACCEPT, new ProxyDaemon(this));
      }

    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public static void main(final String[] args) {
    // boilerplate HttpMethod.init() here
    RpcHelper.getEXECUTOR_SERVICE().submit(new ProxyTask() {
      {
        proxyPorts = args;
      }
    });
  }

}
