package bbcursive.vtables;

import java.util.function.Function;

/**
 * a documentation interface for a functional interface
 * 
 * this will reify a pojo
 * 
 * when the reification is complete, the value is returned. the implementation makes no guarantees about ByteBuffer
 * position before or after the call.
 * 
 * 
 * Created by jim on 1/5/2016.
 */
public interface _reifier<endPojo> extends Function<_ptr, endPojo> {

  /**
   * 
   * @param ptr bytebuffer and position sensor virtual pair
   * @return
   */
  @Override
  endPojo apply(_ptr ptr);
}
