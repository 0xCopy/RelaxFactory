package rxf.server;

import java.lang.annotation.*;

/**
 * Declares a generated service that can be implemented automatically by CouchServiceFactory.
 * Specific to a single entity backed by CouchDB, E. Comes with two built-in methods: to find an
 * object by key, and to persist a given object.
 * <p/>
 * The views will all be created in a design document in the specific database for E.
 *
 * @param <E> the entity this service is concerned with.
 */
public interface CouchService<E> {
  E find(String key);

  CouchTx persist(E entity);

  /**
   * Describes the JavaScript view to run in CouchDB when this method is invoked. The
   * map function is required, but the reduce function is optional.
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

  /**
   * Marks a service call parameter as being used on a couchdb view GET request as "key".
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @CouchRequestParam("key")
  @Documented
  public @interface Key {
  }

  /**
   * Marks a service method or parameter as being used on a couchdb view GET request as
   * "keys". Typically used on a parameter that accepts an array or collection of keys.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @CouchRequestParam("keys")
  @Documented
  public @interface Keys {
  }

  /**
   * Marks a service method or parameter as being used on a couchdb view GET request as
   * "limit". A value need not be provided if used on a parameter, but the annotation is
   * useless on the method without a value.
   * <p/>
   * (Not yet supported on a method)
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target( {ElementType.PARAMETER, ElementType.METHOD})
  @CouchRequestParam("limit")
  @Documented
  public @interface Limit {
    /**
     * Value to use as the "limit" parameter. Annotations on the method override possible
     * parameters.
     */
    int value() default -1;
  }

  /**
   * Marks a service method or parameter as being used on a couchdb view GET request
   * as "skip". A value need not be provided if used on a parameter, but the annotation
   * is useless on the method without a value.
   * <p/>
   * From http://wiki.apache.org/couchdb/HTTP_view_API#Querying_Options:
   * <blockquote>"The skip option should only be used with small values, as skipping a large range of documents this way is
   * inefficient (it scans the index from the startkey and then skips N elements, but still needs to read all the index
   * values to do that). For efficient paging you'll need to use startkey and limit. If you expect to have multiple
   * documents emit identical keys, you'll need to use startkey_docid in addition to startkey to paginate correctly. The
   * reason is that startkey alone will no longer be sufficient to uniquely identify a row."</blockquote>
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target( {ElementType.PARAMETER, ElementType.METHOD})
  @CouchRequestParam("skip")
  @Documented
  public @interface Skip {
    /**
     * Value to use as the "skip" parameter. Annotations on the method override possible
     * parameters.
     */
    int value() default -1;
  }

  /**
   * Marks a service method or parameter as being used on a couchdb view GET request as
   * "startkey". In conjunction with endkey and/or limit can be used to implement basic
   * prefix search
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @CouchRequestParam("startkey")
  @Documented
  public @interface StartKey {
  }

  /**
   * Marks a service method or parameter as being used on a couchdb view GET request as
   * "endkey".
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  @CouchRequestParam("endkey")
  @Documented
  public @interface EndKey {
  }
}
