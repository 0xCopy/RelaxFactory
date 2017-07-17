package bbcursive.lib;

import bbcursive.std;

import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;

/**
 * Created by jim on 1/17/16.
 */
public class abort {
  public static UnaryOperator<ByteBuffer> abort(int rollbackPosition) {
        return b -> null == b ? null : std.bb(b, pos.pos(rollbackPosition), null);
    }
}
