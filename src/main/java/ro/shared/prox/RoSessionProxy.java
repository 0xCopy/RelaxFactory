// WARNING: THIS FILE IS MANAGED BY SPRING ROO.

package ro.shared.prox;

import java.util.Date;

import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import ro.model.RoSession;
import ro.server.RoSessionLocator;

@ProxyFor(value = RoSession.class, locator = RoSessionLocator.class)
//@RooGwtProxy(value = "ro.model.RoSession", readOnly = { "version", "id" })
public interface RoSessionProxy extends EntityProxy {
 String getId();
 Date getCreation();
 void setCreation(Date creation);
 String getVersion();
void setVersion(String response);
}
