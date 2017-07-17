package bbcursive.lib;

import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;

/**
 * Created by jim on 1/17/16.
 */
public class lim implements UnaryOperator<ByteBuffer> {
  private final int position;

  public lim(int position) {
    this.position = position;
  }

  /**
   * reposition
   * 
   * @param position
   * @return
   */
  public static UnaryOperator<ByteBuffer> lim(int position) {
    return new lim(position);

  }

  @Override
  public ByteBuffer apply(ByteBuffer target) {
    return (ByteBuffer) target.limit(position);
  }
}
