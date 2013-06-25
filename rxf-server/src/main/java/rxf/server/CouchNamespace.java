package rxf.server;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpMethod;
import rxf.server.driver.RxfBootstrap;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * User: jim
 * Date: 6/25/12
 * Time: 1:24 PM
 */
public interface CouchNamespace {

  /**
   * a map of http methods each containing an ordered map of regexes tested in order of
   * map insertion.
   */
  Map<HttpMethod, Map<Pattern, Class<? extends Impl>>> NAMESPACE =
      new EnumMap<HttpMethod, Map<Pattern, Class<? extends Impl>>>(HttpMethod.class);

  /**
   * defines where 1xio/rxf finds static content root.
   */
  String COUCH_DEFAULT_FS_ROOT = RxfBootstrap.getVar("RXF_SERVER_CONTENT_ROOT", "./");

  /**
   * creates the orgname used in factories without localized namespaces
   */
  String COUCH_DEFAULT_ORGNAME = RxfBootstrap.getVar("RXF_ORGNAME", "rxf_");

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
    },
    entityName {
      @Override
      void setMe(CouchNamespace cl, String ns) {
        cl.setEntityName(ns);
      }
    };

    abstract void setMe(CouchNamespace cl, String ns);
  }
}
