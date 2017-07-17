package bbcursive.lib;

import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;

/**
 * Created by jim on 1/17/16.
 */
public class pos implements UnaryOperator<ByteBuffer> {
  private final int position;

  public pos(int position) {
    this.position = position;
  }

  /**
   * reposition
   * 
   * @param position
   * @return
   */
  public static UnaryOperator<ByteBuffer> pos(int position) {
    return new pos(position) {
      @Override
      public String toString() {
        return "pos(" + position + ")";
      }
    };
  }

  @Override
  public ByteBuffer apply(ByteBuffer t) {
    return null == t ? t : (ByteBuffer) t.position(position);
  }
}
