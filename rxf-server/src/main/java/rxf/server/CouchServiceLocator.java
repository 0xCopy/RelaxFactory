package rxf.server;

import java.util.concurrent.ExecutionException;

import com.google.web.bindery.requestfactory.shared.ServiceLocator;

public class CouchServiceLocator implements ServiceLocator {

  @SuppressWarnings("unchecked")

  public Object getInstance(Class<?> arg0) {
    try {
      return CouchServiceFactory.get((Class<CouchService<?>>) arg0);
    } catch (InterruptedException e) {
      e.printStackTrace();  //todo: verify for a purpose
    } catch (ExecutionException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return null;
  }

}
