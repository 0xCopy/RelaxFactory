package rxf.server;

import java.util.LinkedHashMap;

/**
 * User: jim
 * Date: 5/10/12
 * Time: 7:24 AM
 */
public class SessionCouchAgent<T> extends CouchChangesClient {

  public SessionCouchAgent(CouchNamespace<T> locator) {
    feedname = locator.getEntityName();
  }

  @Override
  public Runnable getDocUpdateHandler(final LinkedHashMap couchChange) {
    return new Runnable() {

      public void run() {
        String id = String.valueOf(couchChange.get("id"));
        System.err.println("session details changed for the id: " + id);
      }
    };
  }
}
