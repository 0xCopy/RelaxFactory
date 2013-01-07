package ds.shared.rf.proxy;

import com.google.web.bindery.requestfactory.shared.ProxyFor;
import com.google.web.bindery.requestfactory.shared.ValueProxy;
import ds.model.Contact;

/**
 * User: jim
 * Date: 5/12/12
 * Time: 2:40 PM
 */
@ProxyFor(Contact.class)
public interface ContactProxy extends ValueProxy {
  public String getEmail();

  public void setEmail(String email);

  public String getPhone();

  public void setPhone(String phone);

  public String getAddr1();

  public void setAddr1(String addr1);

  public String getAddr2();

  public void setAddr2(String addr2);

  public String getAddr3();

  public void setAddr3(String addr3);

  public String getCity();

  public void setCity(String city);

  public String getState();

  public void setState(String state);

  public String getZip();

  public void setZip(String zip);

  public String getCountry();

  public void setCountry(String country);
}
