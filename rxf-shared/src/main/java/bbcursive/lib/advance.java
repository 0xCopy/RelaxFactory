package bbcursive.lib;

import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;

/**
 * Created by jim on 1/17/16.
 */
public class advance {
  /**
   * consumes a token from the current ByteBuffer position. null signals fail and should reset.
   * 
   * @param exemplar ussually name().getBytes(), but might be other value also.
   * @return null if no match -- rollback not done here use Narsive.$ for whitespace and rollback
   */
  public static UnaryOperator<ByteBuffer> genericAdvance(byte... exemplar) {

    return new UnaryOperator<ByteBuffer>() {

      byte[] bytes = exemplar;

      @Override
      public String toString() {
        return asString();
      }

      public String asString() {
        bytes = exemplar;
        return "advance->" + new String(bytes);
      }

      @Override
      public ByteBuffer apply(ByteBuffer target) {
        int c = 0;
        while (null != exemplar && null != target && target.hasRemaining() && c < exemplar.length
            && exemplar[c] == target.get())
          c++;
        return !(null != target && c == exemplar.length) ? null : target;
      }
    };
  }
}
