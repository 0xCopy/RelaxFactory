package rxf.couch.ann;

import java.lang.annotation.*;

/**
 * Marks a service call parameter as being used per a couchdb view GET request as "key".
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@CouchRequestParam("key")
@Documented
public @interface Key {
}
