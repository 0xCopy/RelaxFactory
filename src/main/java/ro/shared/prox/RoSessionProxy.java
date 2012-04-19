// WARNING: THIS FILE IS MANAGED BY SPRING ROO.

package ro.shared.prox;

import java.util.Date;

import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.ProxyForName;

@ProxyForName(value = "ro.model.RoSession", locator = "ro.server.RoSessionLocator")
//@RooGwtProxy(value = "ro.model.RoSession", readOnly = { "version", "id" })
public interface RoSessionProxy extends EntityProxy {
 String getId();
 Date getCreation();
 void setCreation(Date creation);
 String getVersion();

}
