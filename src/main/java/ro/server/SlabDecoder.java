package ro.server;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;

import static java.lang.Character.isWhitespace;
import static one.xio.HttpMethod.UTF8;

/**
 * pushes a blocking queue of slab bytebuffers
 */
class SlabDecoder implements Callable<Object> {
  private final BlockingDeque<ByteBuffer> slabs;
  private final BlockingDeque<ByteBuffer> chunks;

  public SlabDecoder(BlockingDeque<ByteBuffer> slabs, BlockingDeque<ByteBuffer> chunks) {
    this.slabs = slabs;
    this.chunks = chunks;
  }

  @Override
  public Object call() throws Exception {
    ByteBuffer curChunk = null;
    ByteBuffer buffer;
    while (null != (buffer = slabs.takeFirst())) {
      while (buffer.hasRemaining()) {
        if (null != curChunk) {
          while (curChunk.hasRemaining() && buffer.hasRemaining()) {
            curChunk.put(buffer.get());//suboptimal
          }
          if (!curChunk.hasRemaining()) {
            chunks.addLast((ByteBuffer) curChunk.rewind());
            curChunk = null;
            byte b = 0;
            if (buffer.hasRemaining()) {
              b = buffer.get();
            }

            if (buffer.hasRemaining()) {
              b = buffer.get();
            }
            b++;
          }
          continue;
        }
        System.err.println("remaining " + buffer.remaining() + " : " + UTF8.decode((ByteBuffer) buffer.slice().limit(Math.min(40, buffer.remaining()))));
        while (isWhitespace(buffer.get())) ;
        int p = buffer.position() - 1;
        while (!isWhitespace(buffer.get())) ;
        final String trim = UTF8.decode((ByteBuffer) buffer.duplicate().position(p).limit(buffer.position())).toString().trim();
        int chunkSize = Integer.parseInt(trim, 0x10);
        curChunk = ByteBuffer.allocateDirect(chunkSize);

      }

    }

    return null;
  }
}
