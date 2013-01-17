package rxf.server;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import one.xio.HttpStatus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.StrictMath.min;
import static java.nio.channels.SelectionKey.OP_ACCEPT;

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 1/2/13
 * Time: 8:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class RelaxFactoryServerImpl implements RelaxFactoryServer {

  private int port = 8080;
  private AsioVisitor topLevel;
  private InetAddress hostname;

  private ServerSocketChannel serverSocketChannel;

  /**
   * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
   *
   * @param channel the socketchanel
   * @param op      int ChannelSelector.operator
   * @param s       the payload: grammar {enum,data1,data..n}
   * @throws java.nio.channels.ClosedChannelException
   *
   */
  public static void enqueue(SelectableChannel channel, int op, Object... s)
      throws ClosedChannelException {
    HttpMethod.enqueue(channel, op, s);
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

  public static void response(SelectionKey key, HttpStatus httpStatus) throws IOException {
    try {
      SocketChannel channel = (SocketChannel) key.channel();
      ByteBuffer buffer = ByteBuffer.allocateDirect(channel.socket().getSendBufferSize());
      CharBuffer charBuffer =
          (CharBuffer) buffer.asCharBuffer().append("HTTP/1.1 ").append(
              httpStatus.name().substring(1)).append(' ').append(httpStatus.caption).append("\r\n")
              .flip();
      ByteBuffer out = HttpMethod.UTF8.encode(charBuffer);
      ((SocketChannel) key.channel()).write(out);
    } catch (Exception ignored) {
    }

  }

  public static void init(AsioVisitor protocoldecoder, String... a) throws IOException {
    HttpMethod.init(protocoldecoder, a);
  }

  static AsioVisitor inferAsioVisitor(AsioVisitor default$, SelectionKey key) {
    Object attachment = key.attachment();
    AsioVisitor m;
    if (null == attachment)
      m = default$;
    if (attachment instanceof Object[]) {
      for (Object o : ((Object[]) attachment)) {
        attachment = o;
        break;
      }
    }
    if (attachment instanceof Iterable) {
      Iterable iterable = (Iterable) attachment;
      for (Object o : iterable) {
        attachment = o;
        break;
      }
    }
    if (attachment instanceof AsioVisitor) {
      m = (AsioVisitor) attachment;

    } else {

      m = default$;
    }
    return m;
  }

  public void setPort(int port) {
    this.port = port;
  }

  @Override
  public void init(String hostname, int port, AsioVisitor topLevel) throws UnknownHostException {
    assert topLevel == null && serverSocketChannel == null : "Can't call init twice";
    this.topLevel = topLevel;
    this.setPort(port);
    this.hostname = InetAddress.getByName(hostname);
  }

  @Override
  public void start() throws IOException {
    assert serverSocketChannel == null : "Can't start already started server";
    serverSocketChannel = ServerSocketChannel.open();
    InetSocketAddress addr = new InetSocketAddress(hostname, getPort());
    serverSocketChannel.socket().bind(addr);
    setPort(serverSocketChannel.socket().getLocalPort());
    System.out.println(hostname.getHostAddress() + ":" + getPort());
    serverSocketChannel.configureBlocking(false);

    enqueue(serverSocketChannel, OP_ACCEPT, topLevel);
    init(topLevel);
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public void stop() throws IOException {
    HttpMethod.killswitch = true;
    serverSocketChannel.close();
  }

}
