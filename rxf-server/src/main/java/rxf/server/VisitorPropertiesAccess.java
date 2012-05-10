package rxf.server;

/**
 * User: jim
 * Date: 4/20/12
 * Time: 5:04 PM
 */
public class VisitorPropertiesAccess {
  static public String INSTANCE = Visitor.class.getSimpleName().toLowerCase();//default

  public static String getSessionProperty(String key) {
    try {
      String sessionCookieId1 = BlobAntiPatternObject.getSessionCookieId();
      return BlobAntiPatternObject.getGenericMapProperty(sessionCookieId1, key);
    } catch (Exception e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return null;
  }

  /**
   * @return new version string
   */
  public static void setSessionProperty(String key, String value) {
    try {
      String id = BlobAntiPatternObject.getSessionCookieId();
      final String instance = INSTANCE;
      String ret;
      String path = instance + '/' + id;
      CouchTx tx = BlobAntiPatternObject.setGenericDocumentProperty(path, key, value);
      System.err.println("tx: " + tx.toString());
    } catch (Throwable ignored) {

    }
  }

}