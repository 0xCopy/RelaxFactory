package rxf.server;

/**
 * User: jim
 * Date: 4/20/12
 * Time: 5:04 PM
 */
public class VisitorPropertiesAccess {

  private static CouchPropertiesAccess<Visitor> visitorCouchPropertiesAccess = new CouchPropertiesAccess<Visitor>(BlobAntiPatternObject.VISITOR_LOCATOR);

  static public String getSessionProperty(String key) {
    try {
      return visitorCouchPropertiesAccess.getSessionProperty(BlobAntiPatternObject.getSessionCookieId(), key);
    } catch (Exception e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return key;
  }

  static public String setSessionProperty(String key, String value) {
    try {
      return visitorCouchPropertiesAccess.setSessionProperty(BlobAntiPatternObject.getSessionCookieId(), key, value);
    } catch (Exception e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return key;
  }
}