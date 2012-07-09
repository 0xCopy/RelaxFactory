package rxf.server;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Pattern;

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
    Map<HttpMethod, Map<Pattern, AsioVisitor.Impl>> NAMESPACE = new EnumMap<HttpMethod, Map<Pattern, AsioVisitor.Impl>>(HttpMethod.class);
    String RXF_SERVER_CONTENT_ROOT_SYSTEM_KEY = "rxf.server.content.root";
    String COUCH_DEFAULT_FS_ROOT = System.getProperty(RXF_SERVER_CONTENT_ROOT_SYSTEM_KEY, "./");
    String RXF_ORGNAME_ENV_KEY = "RXF_ORGNAME";
    String COUCH_DEFAULT_ORGNAME = System.getenv("RXF_ORGNAME") == null ? System.getProperty(CouchLocator.class.getCanonicalName().toLowerCase() + ".orgname", "rxf_") : System.getenv(RXF_ORGNAME_ENV_KEY).toLowerCase().trim();

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
