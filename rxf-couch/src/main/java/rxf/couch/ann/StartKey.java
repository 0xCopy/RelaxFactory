package rxf.couch.ann;

import java.lang.annotation.*;

/**
 * Marks a service method or parameter as being used per a couchdb view GET request as "startkey". In conjunction with
 * endkey and/or limit can be used to implement basic prefix search
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@CouchRequestParam("startkey")
@Documented
public @interface StartKey {
}
