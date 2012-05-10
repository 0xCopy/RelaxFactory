package rxf.server;

/**
 * User: jim
 * Date: 4/20/12
 * Time: 5:04 PM
 */
public class VisitorPropertiesAccess {


  public static final CouchPropertiesAccess<Visitor> VISITOR_COUCH_PROPERTIES_ACCESS = new CouchPropertiesAccess<Visitor>(BlobAntiPatternObject.VISITOR_LOCATOR);


 static   public String getSessionProperty( String key) {
      try {
        return VISITOR_COUCH_PROPERTIES_ACCESS.getSessionProperty(BlobAntiPatternObject.getSessionCookieId(), key);
      } catch (Exception e) {
        e.printStackTrace();  //todo: verify for a purpose
      }
      return key;
    }

    static public String setSessionProperty(String key, String value) {
      try {
        return VISITOR_COUCH_PROPERTIES_ACCESS.setSessionProperty( BlobAntiPatternObject.getSessionCookieId(), key, value);
      } catch (Exception e) {
        e.printStackTrace();  //todo: verify for a purpose
      }
      return key;
    }

}