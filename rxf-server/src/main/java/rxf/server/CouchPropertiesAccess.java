package rxf.server;

/**
 * User: jim
 * Date: 5/10/12
 * Time: 7:59 AM
 */
public class CouchPropertiesAccess<T> {

  private CouchLocator<T> locator;

  public CouchPropertiesAccess(CouchLocator<T> locator) {
    this.locator = locator;
  }

}
