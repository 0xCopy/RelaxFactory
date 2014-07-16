package rxf.rpc;

import one.xio.AsioVisitor;
import rxf.core.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.ServerSocketChannel;

import static java.lang.StrictMath.min;
import static java.nio.channels.SelectionKey.OP_ACCEPT;

public class RelaxFactoryServerImpl implements RelaxFactoryServer {

  private int port = 8080;
  private AsioVisitor topLevel;
  private InetAddress hostname;

  private ServerSocketChannel serverSocketChannel;

  private volatile boolean isRunning;

  /**
   * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
   * 
   * @param channel the socketchanel
   * @param op int ChannelSelector.operator
   * @param s the payload: grammar {enum,data1,data..n}
   * @throws java.nio.channels.ClosedChannelException
   * 
   */
  public static void enqueue(SelectableChannel channel, int op, Object... s)
      throws ClosedChannelException {
    Server.enqueue(channel, op, s);
  }

  public static String wheresWaldo(int... depth) {
    int d = depth.length > 0 ? depth[0] : 2;
    Throwable throwable = new Throwable();
    Throwable throwable1 = throwable.fillInStackTrace();
    StackTraceElement[] stackTrace = throwable1.getStackTrace();
    String ret = "";
    for (int i = 2, end = min(stackTrace.length - 1, d); i <= end; i++) {
      StackTraceElement stackTraceElement = stackTrace[i];
      ret +=
          "\tat " + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName()
              + "(" + stackTraceElement.getFileName() + ":" + stackTraceElement.getLineNumber()
              + ")\n";

    }
    return ret;
  }

  public static void init(AsioVisitor protocoldecoder, String... a) throws IOException {
    Server.init(protocoldecoder);
  }

  public void setPort(int port) {
    this.port = port;
  }

  public void init(String hostname, int port, AsioVisitor topLevel) throws UnknownHostException {
    assert this.topLevel == null && this.serverSocketChannel == null : "Can't call init twice";
    this.topLevel = topLevel;
    this.setPort(port);
    this.hostname = InetAddress.getByName(hostname);
  }

  public void start() throws IOException {
    assert serverSocketChannel == null : "Can't start already started couch";

    isRunning = true;
    try {
      serverSocketChannel = ServerSocketChannel.open();
      InetSocketAddress addr = new InetSocketAddress(hostname, getPort());
      serverSocketChannel.socket().bind(addr);
      setPort(serverSocketChannel.socket().getLocalPort());
      System.out.println(hostname.getHostAddress() + ":" + getPort());
      serverSocketChannel.configureBlocking(false);

      enqueue(serverSocketChannel, OP_ACCEPT, topLevel);
      init(topLevel);
    } finally {
      isRunning = false;
    }
  }

  public int getPort() {
    return port;
  }

  public void stop() throws IOException {
    Server.killswitch = true;
    serverSocketChannel.close();
  }

  public boolean isRunning() {
    return isRunning;
  }

}
