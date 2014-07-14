package rxf.couch;

import com.google.web.bindery.requestfactory.shared.Locator;
import rxf.core.CouchNamespace;
import rxf.couch.gen.CouchDriver.DocFetch;
import rxf.couch.gen.CouchDriver.DocPersist;
import rxf.rpc.RelaxFactoryServerImpl;
import rxf.rpc.RpcHelper;
import rxf.shared.CouchTx;

import java.util.List;

import static rxf.couch.driver.CouchMetaDriver.gson;

public abstract class CouchLocator<T> extends Locator<T, String> implements CouchNamespace {

  public CouchLocator(String... nse) {
    for (int i = 0; i < nse.length; i++) {
      ns.values()[i].setMe(this, nse[i]);
    }
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
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
    throw new UnsupportedOperationException("no default ctor "
        + RelaxFactoryServerImpl.wheresWaldo(3));
  }

  @Override
  public T find(Class<? extends T> clazz, String id) {
    return gson().fromJson(new DocFetch().db(getEntityName()).docId(id).to().fire().json(),
        getDomainType());
  }

  /**
   * used by CouchAgent to create event channels per entities by sending it a locator
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

  public String getOrgName() {
    return null == orgname ? RpcHelper.getDefaultOrgName() : orgname;
  }

  public void setOrgname(String orgname) {
    this.orgname = orgname;
  }

  public CouchTx persist(T domainObject) {

    CouchTx ret;

    String pathPrefix = getEntityName();
    String id = getId(domainObject);
    final DocPersist.DocPersistTerminalBuilder fire =
        new DocPersist().db(pathPrefix).validjson(gson().toJson(domainObject)).to().fire();
    ret = fire.tx();

    return ret;
  }

  List<T> findAll() {
    return null; // To change body of created methods use File | Settings | File Templates.
  }

  List<T> search(String queryParm) {
    return null; // To change body of created methods use File | Settings | File Templates.
  }

  // /CouchNS boilerplate

  private String entityName;

  // threadlocals dont help much. rf is dispatched to new threads in a seperate executor.
  private String orgname = null;

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  /**
   * tbd -- longpolling feed rf token
   * 
   * @param queryParm
   * @return
   */
  String searchAsync(String queryParm) {
    return null;
  }

  public String getEntityName() {
    return entityName == null ? getDefaultEntityName() : entityName;
  }

  public String getDefaultEntityName() {
    return getOrgName() + getDomainType().getSimpleName().toLowerCase();
  }

}