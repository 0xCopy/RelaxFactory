package ro.server;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.concurrent.SynchronousQueue;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;

/**
 * User: jim
 * Date: 4/23/12
 * Time: 10:18 PM
 */
class FetchJsonByIdVisitor extends AsioVisitor.Impl {
  private final String path;
  private final SocketChannel channel;
  private final SynchronousQueue<String> returnTo;

  public FetchJsonByIdVisitor(String path, SocketChannel channel, SynchronousQueue<String> returnTo) throws ClosedChannelException {
    this.path = path;
    this.channel = channel;
    this.returnTo = returnTo;
    HttpMethod.enqueue(channel, OP_CONNECT, this);
  }

  @Override
  public void onConnect(SelectionKey key) {
    try {
      if (((SocketChannel) key.channel()).finishConnect()) {
        key.interestOps(OP_WRITE);
      }
    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }

  @Override
  public void onWrite(final SelectionKey selectionKey) {

    try {
      String format = (MessageFormat.format("GET /{0} HTTP/1.1\r\n\r\n", path.trim()));
      System.err.println("attempting connect: " + format.trim());
      channel.write(UTF8.encode(format));
      selectionKey.attach(new JsonResponseReader(returnTo));
      selectionKey.interestOps(OP_READ);
    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }

}
