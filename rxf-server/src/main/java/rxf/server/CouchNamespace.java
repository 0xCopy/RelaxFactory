package rxf.server;

import one.xio.AsioVisitor.Impl;
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
	Map<HttpMethod, Map<Pattern, Class<? extends Impl>>> NAMESPACE = new EnumMap<HttpMethod, Map<Pattern, Class<? extends Impl>>>(
			HttpMethod.class);

	/**
	 * specify web/gwt content root
	 * <p/>
	 * a key for java -Drxf.server.content.root=target/my[roject-1.0 (if maven)
	 */
	String RXF_SERVER_CONTENT_ROOT_SYSTEM_KEY = "rxf.server.content.root";

	/**
	 * a key for java -Drxf.orgname=xxxx to specify a tenant name
	 */
	String RXF_ORGNAME_SYSTEM_KEY = "rxf.orgname";

	/**
	 * a key to specify tenant name among otherwise identical apps on a common couch server
	 * <p/>
	 * overrides {@link #RXF_ORGNAME_SYSTEM_KEY}
	 */
	String RXF_ORGNAME_ENV_KEY = "RXF_ORGNAME";
	/**
	 * content root env key
	 */
	String RXF_SERVER_CONTENT_ROOT_ENV_KEY = "RXF_SERVER_CONTENT_ROOT";
	/**
	 * defines where 1xio/rxf finds static content root.
	 */
	String COUCH_DEFAULT_FS_ROOT = null == System
			.getenv(RXF_SERVER_CONTENT_ROOT_ENV_KEY) ? System.getProperty(
			RXF_SERVER_CONTENT_ROOT_SYSTEM_KEY, "./") : System
			.getenv(RXF_SERVER_CONTENT_ROOT_ENV_KEY);

	/**
	 * creates the orgname used in factories without localized namespaces
	 */
	String COUCH_DEFAULT_ORGNAME = null == System.getenv(RXF_ORGNAME_ENV_KEY)
			? System.getProperty(RXF_ORGNAME_SYSTEM_KEY, "rxf_")
			: System.getenv(RXF_ORGNAME_ENV_KEY).toLowerCase().trim();

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
