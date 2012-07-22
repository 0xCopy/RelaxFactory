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
     * "keys".
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
    @Target({ElementType.PARAMETER/*, ElementType.METHOD*/})
    @CouchRequestParam("limit")
    @Documented
    public @interface Limit {
//    /**
//     * Value to use as the "limit" parameter. Annotations on the method override possible
//     * parameters.
//     */
//    int value() default -1;
    }

    /**
     * Marks a service method or parameter as being used on a couchdb view GET request
     * as "skip". A value need not be provided if used on a parameter, but the annotation
     * is useless on the method without a value.
     * <p/>
     * (Not yet supported on a method)
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.PARAMETER/*, ElementType.METHOD*/})
    @CouchRequestParam("skip")
    @Documented
    public @interface Skip {
//    /**
//     * Value to use as the "skip" parameter. Annotations on the method override possible
//     * parameters.
//     */
//    int value() default -1;
    }

    /**
     * Marks a service method or parameter as being used on a couchdb view GET request as
     * "startkey". In conjunection with endkey and/or limit can be used to implement basic
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
