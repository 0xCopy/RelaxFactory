package bbcursive.lib;

import bbcursive.std;

import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;

import static java.util.Arrays.deepToString;

/**
 * Created by jim on 1/17/16.
 */
public interface allOf_ {
  ;

  /**
   * bbcursive.lib.allOf_ of, in sequence, without failures
   * 
   * @param allOf
   * @return null if not bbcursive.lib.allOf_ match in sequence
   */
  static UnaryOperator<ByteBuffer> allOf(UnaryOperator<ByteBuffer>... allOf) {
        return new UnaryOperator<ByteBuffer>() {
            @Override
            public String toString() {
                return "all"+ deepToString(allOf);
            }

            @Override
            public ByteBuffer apply(ByteBuffer target) {
                return   std.bb(target, allOf);
            }
        };
    }
}
