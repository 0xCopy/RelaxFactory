package ds.server;

import one.xio.HttpMethod;
import ds.model.*;
import rxf.server.CouchLocator;
import rxf.server.CouchTx;

/**
 * User: jim
 * Date: 5/18/12
 * Time: 6:51 PM
 */
public class SecurityImpl {

  public static final CouchLocator<Deal> DEAL_COUCH_LOCATOR = new DealCouchLocator();
  public static final CouchLocator<Login> LOGIN_COUCH_LOCATOR = new LoginCouchLocator();
  public static final CouchLocator<Npo> NPO_COUCH_LOCATOR = new NpoCouchLocator();
  public static final CouchLocator<Vendor> VENDOR_COUCH_LOCATOR = new VendorCouchLocator();

  public static CouchTx deal(Deal deal) throws Exception {
    return DEAL_COUCH_LOCATOR.persist(deal);
  }

  public static CouchTx login(Login login) throws Exception {
    CouchTx persist = LOGIN_COUCH_LOCATOR.persist(login);
    System.err.println("returning " + persist.toString() + HttpMethod.wheresWaldo());
    return persist;
  }

  public static CouchTx npo(Npo npo) throws Exception {
    return NPO_COUCH_LOCATOR.persist(npo);
  }

  public static CouchTx vendor(Vendor vendor) throws Exception {
    return VENDOR_COUCH_LOCATOR.persist(vendor);
  }


  public static class DealCouchLocator extends CouchLocator<Deal> {
    @Override
    public Class<Deal> getDomainType() {
      return Deal.class;
    }

    @Override
    public String getId(Deal domainObject) {
      return domainObject.getId();
    }

    @Override
    public Object getVersion(Deal domainObject) {
      return domainObject.getVersion();
    }
  }

  public static class VendorCouchLocator extends CouchLocator<Vendor> {
    @Override
    public Class<Vendor> getDomainType() {
      return Vendor.class;
    }

    @Override
    public String getId(Vendor domainObject) {
      return domainObject.getId();
    }

    @Override
    public Object getVersion(Vendor domainObject) {
      return domainObject.getVersion();
    }


    static public Vendor find(String id) {
      return VENDOR_COUCH_LOCATOR.find(Vendor.class, id);
    }
  }

  public static class LoginCouchLocator extends CouchLocator<Login> {
    @Override
    public Class<Login> getDomainType() {
      return Login.class;
    }

    @Override
    public String getId(Login domainObject) {
      return domainObject.getId();
    }

    @Override
    public Object getVersion(Login domainObject) {
      return domainObject.getVersion();
    }

  }

  public static class NpoCouchLocator extends CouchLocator<Npo> {
    @Override
    public Class<Npo> getDomainType() {
      return Npo.class;
    }

    @Override
    public String getId(Npo domainObject) {
      return domainObject.getId();
    }

    @Override
    public Object getVersion(Npo domainObject) {
      return domainObject.getVersion();
    }
  }
}
