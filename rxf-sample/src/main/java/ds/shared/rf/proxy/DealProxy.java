package ds.shared.rf.proxy;

import java.util.Date;

import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import ds.model.Deal;
import ds.server.SecurityImpl;

/**
 * User: jim
 * Date: 5/14/12
 * Time: 1:36 AM
 */
@ProxyFor(value = Deal.class, locator = SecurityImpl.DealCouchLocator.class)
public interface DealProxy extends EntityProxy {
  String getId();

  void setId(String id);

  Date getCreation();

  void setCreation(Date creation);

  String getVersion();

  void setVersion(String version);

  String getProduct();

  void setProduct(String product);

  /**
   * @return trusted html to be drawn on the page
   */
  String getProductDescription();

  void setProductDescription(String productDescription);

  LoginProxy getCreator();

  String getCreatorId();

  void setCreatorId(String creatorId);

  void setCreator(LoginProxy creator);

  VendorProxy getVendor();

  String getVendorId();

  void setVendorId(String vendorId);

  void setVendor(VendorProxy vendor);

  NpoProxy getNpo();

  String getNpoId();

  void setNpoId(String npoId);

  void setNpo(NpoProxy npo);

  String getRoAdmin();

  void setRoAdmin(String roAdmin);

  String getJson();

  void setJson(String json);

  Float getDiscount();

  void setDiscount(Float discount);

  Float getAmount();

  void setAmount(Float amount);

  Date getExpire();

  void setExpire(Date expire);

  Integer getMinimum();

  void setMinimum(Integer minimum);

  Integer getLimit();

  void setLimit(Integer limit);
}
