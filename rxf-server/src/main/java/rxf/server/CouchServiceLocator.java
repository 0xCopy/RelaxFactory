package rxf.server;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.google.web.bindery.requestfactory.shared.ServiceLocator;

public class CouchServiceLocator implements ServiceLocator {

  @SuppressWarnings("unchecked")

  public Object getInstance(Class<?> arg0) {
    try {
      return CouchServiceFactory.get((Class<CouchService<?>>) arg0);
    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    } catch (TimeoutException e) {
      e.printStackTrace();  //todo: verify for a purpose
    } catch (InterruptedException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return null;
  }

}
