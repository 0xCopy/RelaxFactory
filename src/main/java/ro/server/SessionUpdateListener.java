package ro.server;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

import one.xio.AsioVisitor;
import one.xio.HttpHeaders;

import static java.lang.Character.isWhitespace;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static ro.server.KernelImpl.EXECUTOR_SERVICE;

/**
 * User: jim
 * Date: 4/30/12
 * Time: 9:37 PM
 */
class SessionUpdateListener extends AsioVisitor.Impl {
  @Override
  public void onWrite(SelectionKey key) throws Exception {
    final SocketChannel channel = (SocketChannel) key.channel();
    ByteBuffer encode = UTF8.encode(MessageFormat.format("GET /rosession/_changes?include_docs=true&feed=continuous&heartbeat={0,number,#} HTTP/1.1\r\nAccept: */*\r\n\r\n", CouchChangesClient.POLL_HEARTBEAT_MS));
    int write = channel.write(encode);
    key.attach(new Impl() {
      @Override
      public void onRead(SelectionKey key) throws Exception {
        ByteBuffer dst = ByteBuffer.allocateDirect(channel.socket().getReceiveBufferSize());
        int read = channel.read(dst);
        int position1 = KernelImpl.moveCaretToDoubleEol((ByteBuffer) dst.flip()).position();
        ByteBuffer headers = (ByteBuffer) ByteBuffer.allocateDirect(position1).put((ByteBuffer) dst.duplicate().clear().limit(position1)).rewind();

        Map<String, int[]> map = HttpHeaders.getHeaders((ByteBuffer) headers.clear());
        boolean isChunked = false;
        if (map.containsKey("Transfer-Encoding")) {
          int[] ints = map.get("Transfer-Encoding");
          Buffer clear = headers.clear();
          Buffer position = clear.position(ints[0]);
          Buffer limit = position.limit(ints[1]);
          String v = UTF8.decode((ByteBuffer) limit).toString().trim();
          isChunked = v.contains("chunked");
        }
        headers.clear();
        while (!isWhitespace((int) headers.get())) {
        }
        ByteBuffer slice = headers.slice();
        while (!isWhitespace((int) slice.get())) {
        }

        String s = UTF8.decode((ByteBuffer) slice.flip()).toString().trim();
        int resCode = Integer.parseInt(s);
        switch (resCode) {
          case 200:
          case 201:

//       requires an event pump
            ByteBuffer slice1 = dst.slice();
            final BlockingDeque<ByteBuffer> slabs = new LinkedBlockingDeque<ByteBuffer>(555);
            final BlockingDeque<ByteBuffer> chunks = new LinkedBlockingDeque<ByteBuffer>(555);

            slabs.addLast(slice1);
            EXECUTOR_SERVICE.submit(new SlabDecoder(slabs, chunks));
            EXECUTOR_SERVICE.submit(new ChunkDecoder(chunks));
            key.attach(new Impl() {
              @Override
              public void onRead(SelectionKey key) throws Exception {
                final SocketChannel channel1 = (SocketChannel) key.channel();
                final ByteBuffer dst1 = ByteBuffer.allocateDirect(channel1.socket().getReceiveBufferSize());
                channel1.read(dst1);
                slabs.addLast((ByteBuffer) dst1.flip());
              }
            });
            key.interestOps(OP_READ);
            break;
          default:
            final Impl parent = this;
            key.attach(new Impl() {
              @Override
              public void onWrite(SelectionKey key) throws Exception {
                String str = "PUT /rosession/ HTTP/1.1\r\n\r\n";
                channel.write(UTF8.encode(str));
                key.attach(parent);
                key.interestOps(OP_READ);
              }
            });
            key.interestOps(OP_WRITE);
            break;
        }
      }
    });
    key.interestOps(OP_READ);


  }
}
