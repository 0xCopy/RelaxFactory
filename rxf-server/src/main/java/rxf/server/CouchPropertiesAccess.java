package rxf.server;

import static rxf.server.BlobAntiPatternObject.setGenericDocumentProperty;

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

  public String getSessionProperty(String eid, String key) {
    try {
      String path = locator.getPathPrefix() + '/' + eid;
      return BlobAntiPatternObject.getGenericDocumentProperty(path, key);
    } catch (Exception e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return null;
  }

  /**
   * @return new version string
   */
  public String setSessionProperty(String eid, String key, String value) {

    String path = locator.getPathPrefix() + '/' + eid;
    CouchTx tx = null;
    try {
      tx = setGenericDocumentProperty(path, key, value);
      System.err.println("tx: " + tx.toString());
      return tx.rev();
    } catch (Exception e) {
      e.printStackTrace();  //todo: verify for a purpose
    }


    return eid;
  }
}
