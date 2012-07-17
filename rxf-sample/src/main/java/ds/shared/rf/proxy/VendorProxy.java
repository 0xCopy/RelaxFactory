package ds.shared.rf.proxy;

import java.util.Date;

import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import ds.model.Vendor;
import ds.server.SecurityImpl;

/**
 * User: jim
 * Date: 5/12/12
 * Time: 2:38 PM
 */
@ProxyFor(value = Vendor.class, locator = SecurityImpl.VendorCouchLocator.class)
public interface VendorProxy extends EntityProxy {
  String getId();

  void setId(String id);

  Date getCreation();

  void setCreation(Date creation);

  String getVersion();

  void setVersion(String version);

  String getName();

  void setName(String name);

  String getDescription();

  void setDescription(String description);

  String getPocName();

  void setPocName(String pocName);

  ContactProxy getContactInfo();

  void setContactInfo(ContactProxy contactInfo);
}
