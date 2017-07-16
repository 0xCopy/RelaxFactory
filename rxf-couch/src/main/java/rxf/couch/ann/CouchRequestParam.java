package rxf.couch.ann;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the decorated annotation is a couch request param.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface CouchRequestParam {
  /**
   * The parameter name to use in the request
   */
  String value();

  /**
   * True if the value should be encoded as json. If false, the parameter value will be toString'd to send it over the
   * wire. Defaults to true.
   */
  boolean isJson() default true;
}
