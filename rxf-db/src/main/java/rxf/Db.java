package rxf;

import rxf.server.ActionBuilder;
import rxf.server.RelaxFactoryServer;
import rxf.server.Rfc822HeaderState;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import static java.nio.channels.SelectionKey.OP_READ;
import static one.xio.AsioVisitor.Impl;
import static rxf.server.BlobAntiPatternRelic.getReceiveBufferSize;
import static rxf.server.BlobAntiPatternRelic.suffixMatchChunks;
import static rxf.server.driver.CouchMetaDriver.HEADER_TERMINATOR;

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 1/7/13
 * Time: 2:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class Db {
  public static void main(String... args) throws IOException, InterruptedException {
    Db db = new Db();
    synchronized (args) {
      args.wait();
    }
  }

  public Db() throws IOException {
    RelaxFactoryServer.App.get().launchVhost("127.0.0.1", 5984, new Impl() {
      public void onAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
        final SocketChannel channel = serverSocketChannel.accept();
        channel.configureBlocking(false);
        RelaxFactoryServer.App.get().enqueue(channel, OP_READ, new Impl() {

          public ByteBuffer cursor;
          public ByteBuffer header;
          public Rfc822HeaderState req = ActionBuilder.get().state().$req();

          public void onRead(SelectionKey key) throws Exception {
            if (null == cursor) {
              //geometric,  vulnerable to dev/null if not max'd here.
              header =
                  null == header ? ByteBuffer.allocateDirect(getReceiveBufferSize()) : header
                      .hasRemaining() ? header : ByteBuffer.allocateDirect(header.capacity() * 2)
                      .put((ByteBuffer) header.flip());

              int read = channel.read(header);
              ByteBuffer flip = (ByteBuffer) header.duplicate().flip();

              req.apply(flip);

              ByteBuffer currentBuff = req.headerBuf();
              if (suffixMatchChunks(HEADER_TERMINATOR, currentBuff)) {
                cursor = flip.slice();
                header = null;
              }
              System.err.println("received header: " + req.as(String.class));
            }
          }
        });
      }
    });
  }
}
