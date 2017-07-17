package bbcursive.lib;

import bbcursive.ann.Backtracking;
import bbcursive.std;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.UnaryOperator;

import static bbcursive.std.bb;

@Backtracking
public enum backtrack_ {
  ;

  @Backtracking
  public static UnaryOperator<ByteBuffer> backtracker(UnaryOperator<ByteBuffer>... allOf) {
    return new backTracker(allOf);

  }

  @Backtracking
  private static class backTracker implements UnaryOperator<ByteBuffer> {
    private final UnaryOperator<ByteBuffer>[] allOf;

    public backTracker(UnaryOperator<ByteBuffer>... allOf) {
      this.allOf = allOf;
    }

    @Override
    public String toString() {
      return "backtracker" + Arrays.deepToString(allOf);
    }

    @Override
    public ByteBuffer apply(ByteBuffer buffer) {
      std.flags.get().add(std.traits.skipper);

      return bb(buffer, allOf);
    }
  }
}
