package bbcursive.lib;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.UnaryOperator;

import static bbcursive.std.bb;

/**
 * Created by jim on 1/17/16.
 */
public enum opt_ {
  ;

  public static UnaryOperator<ByteBuffer> opt(UnaryOperator<ByteBuffer>... unaryOperators) {
    return new ByteBufferUnaryOperator(unaryOperators);
  }

  public static class ByteBufferUnaryOperator implements UnaryOperator<ByteBuffer> {
    private UnaryOperator<ByteBuffer>[] allOrPrevious;

    @Override
    public String toString() {
      return "opt:" + Arrays.deepToString(allOrPrevious);
    }

    public ByteBufferUnaryOperator(UnaryOperator<ByteBuffer>[] allOrPrevious) {

      this.allOrPrevious = allOrPrevious;
    }

    @Override
    public ByteBuffer apply(ByteBuffer buffer) {
      int position = buffer.position();
      ByteBuffer r = bb(buffer, allOrPrevious);
      if (null == r) {
        buffer.position(position);
      }
      return buffer;
    }
  }
}
