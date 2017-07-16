package rxf.couch.ann;

import java.lang.annotation.*;

/**
 * Marks a service method or parameter as being used per a couchdb view GET request as "keys". Typically used per a
 * parameter that accepts an array or collection of keys.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@CouchRequestParam("keys")
@Documented
public @interface Keys {
}
