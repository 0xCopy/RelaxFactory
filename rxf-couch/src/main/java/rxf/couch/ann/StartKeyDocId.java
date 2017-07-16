package rxf.couch.ann;

import java.lang.annotation.*;

/**
 * Marks a service method or parameter as being used per a couchdb view GET request as "startkey_docid". Allows for
 * pagination by id in case of duplicate 'startkey', and without the performance issues of 'skip'.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
@CouchRequestParam("startkey_docid")
@Documented
public @interface StartKeyDocId {
}
