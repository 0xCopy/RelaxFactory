package rxf.server;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import one.xio.AsioVisitor.Impl;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static rxf.server.CouchMetaDriver.CONTENT_LENGTH;

/**
 * User: jim
 * Date: 6/3/12
 * Time: 7:42 PM
 */
class GwtRequestFactoryVisitor extends Impl {
  @Override
  public Impl preRead(Object... env) {
    final AtomicReference<Rfc822HeaderState> state = new AtomicReference<Rfc822HeaderState>();
    for (Object o : env) {
      if (o instanceof Rfc822HeaderState) {
        state.set((Rfc822HeaderState) o);
        break;
      }
    }
    final AtomicReference<ByteBuffer> cursor = new AtomicReference<ByteBuffer>();
    for (Object o : env) {
      if (o instanceof ByteBuffer) {
        cursor.set((ByteBuffer) o);
        break
            ;
      }
    }
    int remaining = Integer.parseInt(state.get().headerString(CONTENT_LENGTH));
    if (remaining == cursor.get().remaining()) {
      ByteBuffer byteBuffer = cursor.get();
      deliver(byteBuffer, state.get());
      return null;

    }

    cursor.set(ByteBuffer.allocateDirect(remaining).put(cursor.get()));

    return new Impl() {
      @Override
      public void onRead(SelectionKey key) throws Exception {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer dst = cursor.get();
        int read = channel.read(dst);
        if (!dst.hasRemaining()) {
          deliver((ByteBuffer) dst.rewind(), state.get());
        }
      }
    };

  }

  void deliver(final ByteBuffer buffer, final Rfc822HeaderState state) {
    BlobAntiPatternObject.EXECUTOR_SERVICE.submit(new Callable<String>() {
      public String call() throws Exception {
        try {
          String decode = UTF8.decode(buffer).toString().trim();
          String json = RfPostWrapper.SIMPLE_REQUEST_PROCESSOR.process(decode);


          final ByteBuffer payload = ByteBuffer.wrap(json.getBytes(UTF8));
          state./*cookieStrings(null).*/sourceKey().interestOps(OP_WRITE).attach(new Impl() {
            @Override
            public void onWrite(SelectionKey key) throws Exception {
              ByteBuffer headersBuf = state.headerString(CONTENT_LENGTH, String.valueOf(payload.limit())).asResponseHeaders();
              final SocketChannel channel = (SocketChannel) key.channel();
              channel.write((ByteBuffer) headersBuf);
              int write = channel.write((ByteBuffer) payload.rewind());

              if (!payload.hasRemaining()) {
                key.interestOps(OP_READ).attach(null);
                return;
              }
              key.attach(new Impl() {
                @Override
                public void onWrite(SelectionKey key) throws Exception {
                  int write1 = channel.write(payload);
                  if (!payload.hasRemaining()) {
                    key.interestOps(OP_READ).attach(null);

                  }

                }
              });

            }
          });

        } catch (Exception e) {
          e.printStackTrace();
        }
        return null;
      }
    });
  }
}
