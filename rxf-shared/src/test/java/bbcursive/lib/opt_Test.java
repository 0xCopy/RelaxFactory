package bbcursive.lib;

import org.junit.Test;

import java.nio.ByteBuffer;

import static bbcursive.lib.allOf_.allOf;
import static bbcursive.lib.chlit_.chlit;
import static bbcursive.lib.opt_.opt;
import static bbcursive.std.bb;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.TestCase.assertNull;

/**
 * Created by jim on 1/24/16.
 */
public class opt_Test {
  @Test
  public void testChlit() {
    ByteBuffer aa =
        bb("aba", chlit('a'), opt(chlit('a')), opt(chlit('c')), opt(chlit('b')), chlit('a'));
    assertNotNull(aa);
    aa =
        bb("aba", allOf(chlit('a'), opt(chlit('a')), opt(chlit('c')), opt(chlit('b')), chlit('z')));
    assertNull(aa);
    aa =
        bb("aba", allOf(chlit('a'), opt(chlit('a')), opt(chlit('b')), opt(chlit('z')), chlit('a'),
            opt(chlit('a'))));
    assertNotNull(aa);
  }
}