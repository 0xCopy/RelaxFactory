package ro.server;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Callable;

import static one.xio.HttpMethod.UTF8;

/**
 * User: jim
 * Date: 4/30/12
 * Time: 9:38 PM
 */
class ChunkDecoder implements Callable {
  private final BlockingDeque<ByteBuffer> chunks;

  public ChunkDecoder(BlockingDeque<ByteBuffer> chunks) {
    this.chunks = chunks;
  }

  @Override
  public Object call() throws Exception {
    while (true) {

      final ByteBuffer event = chunks.takeFirst();
      KernelImpl.adjustProxyCache(KernelImpl.GSON.fromJson(UTF8.decode((ByteBuffer) event.clear()).toString().trim(), Map.class));

    }

  }
}
