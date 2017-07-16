package rxf.couch.ann;

import java.lang.annotation.*;

/**
 * Marks a service method or parameter as being used per a couchdb view GET request as "limit". A value need not be
 * provided if used per a parameter, but the annotation is useless per the method without a value.
 * <p/>
 * (Not yet supported per a method)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
@CouchRequestParam("limit")
@Documented
public @interface Limit {
  /**
   * Value to use as the "limit" parameter. Annotations per the method override possible parameters.
   */
  int value() default -1;
}
