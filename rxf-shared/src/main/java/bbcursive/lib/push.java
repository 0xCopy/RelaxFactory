package bbcursive.lib;

import java.nio.ByteBuffer;

/**
 * Created by jim on 1/17/16.
 */
public class push {
  /**
   * @param src
   * @param dest
   * @return
   */

  public static ByteBuffer push(ByteBuffer src, ByteBuffer dest) {
    int need = src.remaining(), have = dest.remaining();
    if (have > need) {
      return dest.put(src);
    }
    dest.put((ByteBuffer) src.slice().limit(have));
    src.position(src.position() + have);
    return dest;
  }
}
