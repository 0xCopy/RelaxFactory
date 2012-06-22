package rxf.server;

import java.util.List;

import com.google.web.bindery.requestfactory.shared.Locator;
import one.xio.HttpMethod;
import rxf.server.gen.CouchDriver.DocFetch;
import rxf.server.gen.CouchDriver.DocPersist;

import static rxf.server.BlobAntiPatternObject.GSON;

public abstract class CouchLocator<T> extends Locator<T, String> {

  public enum ns {
    orgname {
      @Override
      void setMe(CouchLocator cl, String ns) {
        cl.setOrgname(ns);

      }
    }, entityName {
      @Override
      void setMe(CouchLocator cl, String ns) {
        cl.setEntityName(ns);
      }
    };

    abstract void setMe(CouchLocator cl, String ns);
  }

  /**
   * User: jim
   * Date: 5/10/12
   * Time: 7:37 AM
   */


  private String entityName;

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  //threadlocals dont help much.  rf is dispatched to new threads in a seperate executor.
  private String orgname = null;

  public CouchLocator(String... nse) {
    for (int i = 0; i < nse.length; i++) {
      ns.values()[i].setMe(this,
          nse[i]);

    }
  }

  private static String getDefaultOrgName() {
    return System.getenv("RXF_ORGNAME") == null ? System.getProperty(CouchLocator.class.getCanonicalName().toLowerCase() + ".orgname", "rxf_") : System.getenv("RXF_ORGNAME").toLowerCase().trim();
  }

  public String getEntityName() {
    return entityName == null ? getDefaultEntityName() : entityName;
  }

  private String getDefaultEntityName() {
    return getOrgname() + getDomainType().getSimpleName().toLowerCase();
  }

  /**
   * <pre>
   * POST /rosession HTTP/1.1
   * Content-Type: application/json
   * Content-Length: 133
   *
   * [data not shown]
   * HTTP/1.1 201 Created
   *
   * [data not shown]
   * </pre>
   *
   * @param clazz
   * @return
   */
  @Override
  public T create(Class<? extends T> clazz) {
    try {
      return clazz.newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();  //todo: verify for a purpose
    } catch (IllegalAccessException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    throw new UnsupportedOperationException("no default ctor " + HttpMethod.wheresWaldo(3));
  }

  @Override
  public T find(Class<? extends T> clazz, String id) {
    final String pojo = DocFetch.$().db(getEntityName()).docId(id).to().fire().pojo();

    return GSON.fromJson(pojo, getDomainType());
  }

  /**
   * used by CouchAgent to create event channels on entities by sending it a locator
   *
   * @return
   */
  @Override
  abstract public Class<T> getDomainType();

  @Override
  abstract public String getId(T domainObject);

  @Override
  public Class<String> getIdType() {
    return String.class;
  }

  @Override
  abstract public Object getVersion(T domainObject);

  public String getOrgname() {
    return null == orgname ? getDefaultOrgName() : orgname;
  }

  public CouchTx persist(T domainObject) throws Exception {
    String pathPrefix = getEntityName();
    String id = getId(domainObject);

    return DocPersist.$().db(pathPrefix).validjson(GSON.toJson(domainObject)).to().fire().tx();
  }

  List<T> findAll() {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }

  List<T> search(String queryParm) {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }

  /**
   * tbd -- longpolling feed rf token
   *
   * @param queryParm
   * @return
   */
  String searchAsync(String queryParm) {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }

  public void setOrgname(String orgname) {
    this.orgname = orgname;
  }
}