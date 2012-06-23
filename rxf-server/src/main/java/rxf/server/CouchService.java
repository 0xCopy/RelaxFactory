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
  public @interface View {
    String map();

    String reduce() default "";
  }

  /**
   * Marks a service call parameter as being used on a couchdb view GET $req as "key":
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  public @interface Key {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.PARAMETER)
  public @interface Keys {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD})
  public @interface Limit {
    int value() default -1;
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.PARAMETER, ElementType.METHOD})
  public @interface Skip {
    int value() default -1;
  }
}
