package rxf.couch.ann;

import java.lang.annotation.*;

/**
 * Marks a service method or parameter as being used per a couchdb view GET request as "skip". A value need not be
 * provided if used per a parameter, but the annotation is useless per the method without a value.
 * <p/>
 * From http://wiki.apache.org/couchdb/HTTP_view_API#Querying_Options: <blockquote>"The skip option should only be used
 * with small values, as skipping a large range of documents this way is inefficient (it scans the index from the
 * startkey and then skips N elements, but still needs to read all the index values to do that). For efficient paging
 * you'll need to use startkey and limit. If you expect to have multiple documents emit identical keys, you'll need to
 * use startkey_docid in addition to startkey to paginate correctly. The reason is that startkey alone will no longer be
 * sufficient to uniquely identify a row."</blockquote>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
@CouchRequestParam("skip")
@Documented
public @interface Skip {
  /**
   * Value to use as the "skip" parameter. Annotations per the method override possible parameters.
   */
  int value() default -1;
}
