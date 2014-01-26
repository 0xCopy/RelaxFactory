package rxf.server.daemon;

import one.xio.AsioVisitor;
import rxf.server.driver.RxfBootstrap;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;

/**
 * this visitor shovels data from the outward selector to the inward selector, and vice versa.  once the headers are
 * sent inward the only state monitored is when one side of the connections close.
 */
public class HttpPipeVisitor extends AsioVisitor.Impl {
  private static final boolean PROXY_DEBUG =
      "true".equals(RxfBootstrap.getVar("PROXY_DEBUG", String.valueOf(false)));
  final private ByteBuffer[] b;
  protected String name;
  protected AtomicInteger remaining;
  SelectionKey otherKey;
  private boolean limit;

  HttpPipeVisitor(String name, SelectionKey otherKey, ByteBuffer... b) {
    this.name = name;
    this.otherKey = otherKey;
    this.b = b;

  }

  @Override
  public void onRead(SelectionKey key) throws Exception {
    SocketChannel channel = (SocketChannel) key.channel();
    if (otherKey.isValid()) {
      int read = channel.read(getInBuffer());
      if (read == -1) /*key.cancel();*/
        key.interestOps(OP_WRITE);
      else {
        //if buffer fills up, stop the read option for a bit
        otherKey.interestOps(OP_READ | OP_WRITE);
      }
    } else {
      key.cancel();
    }
  }

  @Override
  public void onWrite(SelectionKey key) throws Exception {
    SocketChannel channel = (SocketChannel) key.channel();
    ByteBuffer flip = (ByteBuffer) getOutBuffer().flip();
    if (PROXY_DEBUG) {
      CharBuffer decode = UTF8.decode(flip.duplicate());
      System.err.println("writing to " + name + ": " + decode + "-");
    }
    int write = channel.write(flip);

    if (-1 == write || isLimit() && null != remaining && 0 == remaining.get()) {
      key.cancel();
    } else {
      if (isLimit() && null != remaining) {
        this.remaining.getAndAdd(-write);
        if (1 > remaining.get()) {
          key.channel().close();
          otherKey.channel().close();
          return;
        }
      }
      key.interestOps(OP_READ | (getOutBuffer().hasRemaining() ? OP_WRITE : 0));
      getOutBuffer().compact();
    }
  }

  public ByteBuffer getInBuffer() {
    return b[0];
  }

  public ByteBuffer getOutBuffer() {
    return b[1];
  }

  public boolean isLimit() {
    return limit;
  }

  public void setLimit(boolean limit) {
    this.limit = limit;
  }
}
