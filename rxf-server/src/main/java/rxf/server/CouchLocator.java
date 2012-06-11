package rxf.server;

import java.util.List;

import com.google.web.bindery.requestfactory.shared.Locator;
import one.xio.HttpMethod;
import rxf.server.CouchDriver.DocFetch;
import rxf.server.CouchDriver.DocPersist;

import static rxf.server.BlobAntiPatternObject.GSON;

/**
 * User: jim
 * Date: 5/10/12
 * Time: 7:37 AM
 */
public abstract class CouchLocator<T> extends Locator<T, String> {

  private String orgname = "rxf_";//default

  public String getPathPrefix() {
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
    final String pojo = DocFetch.<T>$().db(getPathPrefix()).docId(id).to().fire().pojo() ;

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
    return orgname;
  }

  public CouchTx persist(T domainObject) throws Exception {
    String pathPrefix = getPathPrefix();
    String id = getId(domainObject);

    return DocPersist.<T>$().db(pathPrefix).validjson(GSON.toJson(domainObject)).to().fire().tx();
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
}