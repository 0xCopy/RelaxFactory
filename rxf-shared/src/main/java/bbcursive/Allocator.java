package bbcursive;

import java.nio.ByteBuffer;

import static bbcursive.lib.log.log;

/**
 * User: jim Date: Oct 6, 2007 Time: 3:10:32 AM
 */
public class Allocator {

  ByteBuffer DIRECT_HEAP;
  public static int MEG = (1 << 10) << 10, BLOCKSIZE = MEG * 2;

  private int initialCapacity = Runtime.getRuntime().availableProcessors() * 20 * 2;

  public final ByteBuffer EMPTY_SET = ByteBuffer.allocate(0).asReadOnlyBuffer();

  int size = initialCapacity;

  public Allocator(int... bytes) {
    if (bytes.length > 0)
      initialCapacity = bytes[0];

    ByteBuffer buffer = null;
    while (buffer == null)
      try {

        if (isDirect())
          buffer = (ByteBuffer) ByteBuffer.allocateDirect(size).limit(0);
        else
          buffer = (ByteBuffer) ByteBuffer.allocate(size).limit(0);

        DIRECT_HEAP = buffer;
        log("Heap allocated at " + size / MEG + " megs");
        size *= 2;

      } catch (IllegalArgumentException e) {
        size = Math.max(16 * MEG, size / 2);
        System.gc();
      } catch (OutOfMemoryError e) {
        size = Math.max(16 * MEG, size / 2);
        System.gc();
      }
  }

  private void init() {

    ByteBuffer buffer = null;
    while (buffer == null)
      try {

        if (isDirect())
          buffer = (ByteBuffer) ByteBuffer.allocateDirect(size).limit(0);
        else
          buffer = (ByteBuffer) ByteBuffer.allocate(size).limit(0);

        DIRECT_HEAP = buffer;
        log("Heap allocated at " + size / MEG + " megs");
        size *= 2;

      } catch (IllegalArgumentException e) {
        size = Math.max(16 * MEG, size / 2);
        System.gc();
      } catch (OutOfMemoryError e) {
        size = Math.max(16 * MEG, size / 2);
        System.gc();
      }
  }

  ByteBuffer allocate(int size) {
    if (size == 0)
      return EMPTY_SET;
    try {
      DIRECT_HEAP.limit(DIRECT_HEAP.limit() + size);
    } catch (IllegalArgumentException e) {
      init();
      return allocate(size);
    }
    ByteBuffer ret = (ByteBuffer) DIRECT_HEAP.slice().limit(size).mark();
    DIRECT_HEAP.position(DIRECT_HEAP.limit());
    return ret;
  }

  public boolean isDirect() {
    return false;
  }

}
