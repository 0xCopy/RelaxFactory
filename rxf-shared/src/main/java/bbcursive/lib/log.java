package bbcursive.lib;

import bbcursive.WantsZeroCopy;
import bbcursive.std;

import java.nio.ByteBuffer;

import static bbcursive.Cursive.pre.debug;

/**
 * Created by jim on 1/17/16.
 */
public class log {
  /**
   * conditional debug output assert log(Object,[prefix[,suffix]])
   * 
   * @param ob
   * @param prefixSuffix
   * @return
   */
  public static void log(Object ob, String... prefixSuffix) {
    assert log$(ob, prefixSuffix);
  }

  /**
   * conditional debug output assert log(Object,[prefix[,suffix]])
   * 
   * @param ob
   * @param prefixSuffix
   * @return
   */
  public static boolean log$(Object ob, String... prefixSuffix) {
    boolean hasSuffix = 1 < prefixSuffix.length;
    if (0 < prefixSuffix.length)
      System.err.print(prefixSuffix[0] + "\t");
    if (!(ob instanceof ByteBuffer)) {
      if (ob instanceof WantsZeroCopy) {
        WantsZeroCopy wantsZeroCopy = (WantsZeroCopy) ob;
        std.bb(wantsZeroCopy.asByteBuffer(), debug);
      } else {
        std.bb(String.valueOf(ob), debug);
      }
    } else {
      std.bb((ByteBuffer) ob, debug);
    }
    if (hasSuffix) {
      System.err.println(prefixSuffix[1] + "\t");
    }
    return true;
  }
}
