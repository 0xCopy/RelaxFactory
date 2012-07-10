package rxf.server;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;

/**
 * User: jim
 * Date: 6/25/12
 * Time: 1:24 PM
 */
public interface CouchNamespace<T> {

  /**
   * a map of http methods each containing an ordered map of regexes tested in order of
   * map insertion.
   */
  Map<HttpMethod, Map<Pattern, Class<? extends Impl>>> NAMESPACE = new EnumMap<HttpMethod, Map<Pattern, Class<? extends Impl>>>(HttpMethod.class);


  /**
   * a key for java -D xxxx to specify a deployment dir.
   */
  String RXF_SERVER_CONTENT_ROOT_SYSTEM_KEY = "rxf.server.content.root",
  /**
   * a key for java -D xxxx to specify a tenant name
   */
  RXF_ORGNAME_SYSTEM_KEY = "rxf.orgname";

  /**
   * a key to specify tenant name among otherwise identical apps on a common couch server
   * <p/>
   * overrides {@link #RXF_ORGNAME_SYSTEM_KEY}
   */
  String RXF_ORGNAME_ENV_KEY = "RXF_ORGNAME";

  /**
   * defines where 1xio finds static content root.
   */
  String COUCH_DEFAULT_FS_ROOT = System.getProperty(RXF_SERVER_CONTENT_ROOT_SYSTEM_KEY, "./");

  /**
   * creates the orgname used in factories without localized namespaces
   */
  String COUCH_DEFAULT_ORGNAME = System.getenv(RXF_ORGNAME_ENV_KEY) == null ? System.getProperty(RXF_ORGNAME_SYSTEM_KEY, "rxf_") : System.getenv(RXF_ORGNAME_ENV_KEY).toLowerCase().trim();

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
