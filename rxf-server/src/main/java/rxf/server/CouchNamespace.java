package rxf.server;

/**
 * User: jim
 * Date: 6/25/12
 * Time: 1:24 PM
 */
public interface CouchNamespace<T> {

  String getOrgName();

  void setOrgname(String orgname);

  void setEntityName(String entityName);

  String getEntityName();

  String getDefaultEntityName();


  public enum ns {
    orgname {
      @Override
      void setMe(CouchNamespace cl, String ns) {
        cl.setOrgname(ns);

      }
    }, entityName {
      @Override
      void setMe(CouchNamespace cl, String ns) {
        cl.setEntityName(ns);
      }
    };

    abstract void setMe(CouchNamespace cl, String ns);
  }
}
