package rxf.server;

import java.util.LinkedHashMap;

/**
 * User: jim
 * Date: 5/10/12
 * Time: 7:24 AM
 */
public class SessionCouchAgent<T> extends CouchChangesClient {

  public SessionCouchAgent(CouchLocator<T> locator) {
    feedname = (locator).getPathPrefix();
  }

  @Override
  public Runnable getDocUpdateHandler(final LinkedHashMap couchChange) {
    return new Runnable() {
      @Override
      public void run() {
        String id = String.valueOf(couchChange.get("id"));
        System.err.println("session details changed for the id: " + id);
      }
    };
  }
}
