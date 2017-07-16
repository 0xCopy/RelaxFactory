package rxf.couch.ann;

import java.lang.annotation.*;

/**
 * Changes the direction of a search.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
@CouchRequestParam("descending")
@Documented
public @interface Descending {
  boolean value() default false;
}
