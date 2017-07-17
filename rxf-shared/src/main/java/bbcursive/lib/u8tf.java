package bbcursive.lib;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * unique code completion for utf8
 */
public enum u8tf {
  ;

  /**
   * utf8 encoder macro
   * 
   * @param charseq
   * @return
   */
  public static ByteBuffer c2b(String charseq) {
    return StandardCharsets.UTF_8.encode(charseq);
  }

  /**
   * UTF8 decoder macro
   * 
   * @param buffer
   * @return defered string translation decision
   */
  public static CharSequence b2c(ByteBuffer buffer) {
    return StandardCharsets.UTF_8.decode(buffer);
  }
}
