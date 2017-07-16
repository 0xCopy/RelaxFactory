package rxf.couch.ann;

import java.lang.annotation.*;

/**
 * The group_level argument. See http://wiki.apache.org/couchdb/HTTP_view_API#Querying_Options for additional details.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
@CouchRequestParam("group")
@Documented
public @interface GroupLevel {
  int value() default 0;
}
