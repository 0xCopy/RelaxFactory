package rxf.couch.ann;

import java.lang.annotation.*;

/**
 * The group option controls whether the reduce function reduces to a set of distinct keys or to a single result row.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
@CouchRequestParam("group")
@Documented
public @interface Group {
  boolean value() default false;
}
