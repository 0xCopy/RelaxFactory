package rxf.server;

/**
 * User: jim
 * Date: 4/20/12
 * Time: 5:04 PM
 */
public class VisitorPropertiesAccess {

  private static CouchPropertiesAccess<Visitor> visitorCouchPropertiesAccess = new CouchPropertiesAccess<Visitor>() {
    @Override
    public CouchLocator<Visitor> getLocator() {
      return new CouchLocator<Visitor>() {
        @Override
        public Class<Visitor> getDomainType() {
          return Visitor.class;

        }

        @Override
        public String getId(Visitor domainObject) {
          return domainObject.getId();
        }

        @Override
        public Object getVersion(Visitor domainObject) {
          return domainObject.getVersion();
        }
      };
    }
  };

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
      return visitorCouchPropertiesAccess.setSessionProperty(key, value);
    } catch (Exception e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return key;
  }
}