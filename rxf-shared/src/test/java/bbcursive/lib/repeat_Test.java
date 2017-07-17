package bbcursive.lib;

import bbcursive.std;
import org.junit.Test;

import java.nio.ByteBuffer;

import static bbcursive.lib.chlit_.chlit;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Created by jim on 1/23/16.
 */
public class repeat_Test {

  @Test
  public void testRepeat() {

    ByteBuffer aaa = std.bb("aaa", repeat_.repeat(chlit('a')));
    assertNotNull(aaa);
    aaa = std.bb("aba", repeat_.repeat(chlit('a')));
    assertNotNull(aaa);
    aaa = std.bb("baa", repeat_.repeat(chlit('a')));
    assertNull(aaa);
    std.flags.get().add(std.traits.skipper);
    aaa = std.bb("a a a", repeat_.repeat(chlit('a')));
    assertNotNull(aaa);
    std.flags.get().remove(std.traits.skipper);
    aaa = std.bb(" a a a", repeat_.repeat(chlit('a')));
    assertNull(aaa);

  }

}