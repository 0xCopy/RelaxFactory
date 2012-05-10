package rxf.server;

import java.util.LinkedHashMap;

import com.google.web.bindery.requestfactory.shared.Locator;

/**
* User: jim
* Date: 5/10/12
* Time: 7:24 AM
*/
public class SessionCouchAgent<T,C extends Class<?extends T>>extends CouchChangesClient {

  public SessionCouchAgent(Locator locator) {
    final Class SessionPojo = locator.getDomainType();
    feedname = SessionPojo.getSimpleName().toLowerCase();
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
