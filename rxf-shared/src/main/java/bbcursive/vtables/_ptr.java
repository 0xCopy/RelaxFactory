package bbcursive.vtables;

import java.nio.ByteBuffer;

/**
 * pointer class -- approximation of c++ '*'
 * <p>
 * this class is not exactly a Pair, it is a ByteBuffer reference with a settable position() sensor designed only for
 * DirectByteBuffer work.
 * 
 * @author jim
 */
public class _ptr extends _edge<ByteBuffer, Integer> {
  @Override
  protected Integer at() {
    return r$();
  }

  /**
   * bb pos
   * 
   * @param integer
   * @return
   */
  @Override
  protected Integer goTo(Integer integer) {
    core().position(integer);
    return integer;
  }

  @Override
  protected Integer r$() {
    return core().position();
  }
}