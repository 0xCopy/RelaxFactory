package bbcursive;

import java.nio.ByteBuffer;

/**
 * Created by jim on 8/8/14.
 */
public interface WantsZeroCopy {
  ByteBuffer asByteBuffer();
}
