package bbcursive.lib;

import bbcursive.ann.ForwardOnly;
import bbcursive.ann.Infix;

import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;

import static bbcursive.lib.chlit_.chlit;
import static bbcursive.lib.infix_.infix;
import static bbcursive.lib.opt_.opt;
import static bbcursive.lib.repeat_.repeat;

/**
 * Created by jim on 1/21/16.
 */
@Infix
@ForwardOnly
public class value_ implements UnaryOperator<ByteBuffer> {

  public static final value_ VALUE_ = new value_();

  private value_() {
  }

  public static value_ value() {
    return VALUE_;
  }

  @Override
  public ByteBuffer apply(ByteBuffer buffer) {
    return (ByteBuffer) infix(opt(chlit("0")), anyOf_.anyIn("1.0"), opt(repeat(anyOf_
        .anyIn("1029384756"))));
  }
}
