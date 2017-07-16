package rxf.couch.ann;

import java.lang.annotation.*;

/**
 * Describes the JavaScript view to run in CouchDB when this method is invoked. The map function is required, but the
 * reduce function is optional.
 * <p/>
 * Methods decorated with this should return List or CouchResultSet
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface View {
  String map();

  /**
   * Typically empty (the default, meaning "don't reduce"), "_sum", "_count", or "_stats".
   */
  String reduce() default "";
}
