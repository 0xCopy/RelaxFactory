package ro.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;

import static one.xio.HttpMethod.UTF8;

/**
 * a POST interception wrapper for http protocol cracking
 * User: jim
 * Date: 4/18/12
 * Time: 12:37 PM
 */
class RfPostWrapper implements AsioVisitor {


  public static final ExecutorService EXECUTOR_SERVICE = KernelImpl.EXECUTOR_SERVICE;

  @Override
  public void onRead(final SelectionKey key) {
    try {
      SocketChannel channel = (SocketChannel) key.channel();
      int receiveBufferSize = channel.socket().getReceiveBufferSize();
      ByteBuffer dst = ByteBuffer.allocateDirect(receiveBufferSize);
      int read = channel.read(dst);
      if (-1 == read) {
        key.cancel();
      } else {
        dst.flip();
        try {
          ByteBuffer duplicate = dst.duplicate();
          while (duplicate.hasRemaining() && !Character.isWhitespace(duplicate.get())) ;
          String trim = UTF8.decode((ByteBuffer) duplicate.flip()).toString().trim();
          HttpMethod method = HttpMethod.valueOf(trim);
          dst.limit(read).position(0);
          key.attach(dst);
          switch (method) {
            case POST:
              EXECUTOR_SERVICE.submit(new RfTxTask(key));
              break;
            default:
              method.onRead(key);
              break;
          }

        } catch (Exception e) {
          CharBuffer methodName = UTF8.decode((ByteBuffer) dst.clear().limit(read));
          System.err.println("BOOM " + methodName);
          e.printStackTrace();  //todo: verify for a purpose
        }
      }


    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }

  public static void startServer(String... args) throws IOException {
    ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
    serverSocketChannel.socket().bind(new InetSocketAddress(8080));
    serverSocketChannel.configureBlocking(false);
    AsioVisitor topLevel = new RfPostWrapper();
    HttpMethod.enqueue(serverSocketChannel, SelectionKey.OP_ACCEPT, topLevel);
    HttpMethod.init(args, topLevel);
  }


  public void onConnect(SelectionKey key) {
    HttpMethod.$.onConnect(key);
  }

  @Override
  public void onWrite(SelectionKey key) {
  }

  @Override
  public void onAccept(SelectionKey key) {
    try {
      ServerSocketChannel channel = (ServerSocketChannel) key.channel();
      SocketChannel accept = channel.accept();
      accept.configureBlocking(false);
      HttpMethod.enqueue(accept, SelectionKey.OP_READ, this);
    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }

  }

}
