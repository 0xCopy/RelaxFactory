package rxf.couch.ann;

import java.lang.annotation.*;

/**
 * Marks a service method or parameter as being used per a couchdb view GET request as "endkey_docid". Allows for
 * pagination by id in case of duplicate 'endkey'.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@CouchRequestParam("endkey_docid")
@Documented
public @interface EndKeyDocId {
}
