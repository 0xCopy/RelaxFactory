package rxf.server;

import com.google.web.bindery.requestfactory.shared.ServiceLocator;

import java.util.concurrent.ExecutionException;

/**
 * User: jim
 * Date: 7/2/12
 * Time: 7:19 PM
 */
public class CouchServiceLocator implements ServiceLocator {

    public Object getInstance(Class<?> aClass) {
        CouchService<?> ret = null;
        try {
            ret = CouchServiceFactory.get((Class<CouchService<?>>) aClass,
                    CouchNamespace.COUCH_DEFAULT_ORGNAME);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return ret;
    }
}
