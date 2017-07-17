package bbcursive.lib;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.function.UnaryOperator;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by jim on 1/17/16.
 */
public class strlit {

  public static UnaryOperator<ByteBuffer> strlit(CharSequence s) {
    return new ByteBufferUnaryOperator(s);
  }

  private static class ByteBufferUnaryOperator implements UnaryOperator<ByteBuffer> {
    private final CharSequence s;

    public ByteBufferUnaryOperator(CharSequence s) {
      this.s = s;
    }

    @Override
    public String toString() {
      return MessageFormat.format("u8\"{0}\"", s);
    }

    @Override
    public ByteBuffer apply(ByteBuffer buffer) {
      ByteBuffer encode = UTF_8.encode(String.valueOf(s));
      while (encode.hasRemaining() && buffer.hasRemaining() && encode.get() == buffer.get());
      return encode.hasRemaining() ? null : buffer;
    }
  }
}
