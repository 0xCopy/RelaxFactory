package bbcursive.lib;

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;

/**
 * char literal
 */
public class chlit_ {
  public static UnaryOperator<ByteBuffer> chlit(char c) {
    return new ByteBufferUnaryOperator(c);
  }

  public static UnaryOperator<ByteBuffer> chlit(CharSequence s) {
    return chlit(s.charAt(0));
  }

  private static class ByteBufferUnaryOperator implements UnaryOperator<ByteBuffer> {
    private final char c;

    public ByteBufferUnaryOperator(char c) {
      this.c = c;
    }

    @Override
    public String toString() {
      return "c8'" + c + "'";
    }

    @Nullable
    @Override
    public ByteBuffer apply(ByteBuffer buf) {
      if (null == buf) {
        return null;
      }
      if (buf.hasRemaining()) {
        byte b = buf.get();
        return (c & 0xff) == (b & 0xff) ? buf : null;
      }
      return null;

    }
  }
}
