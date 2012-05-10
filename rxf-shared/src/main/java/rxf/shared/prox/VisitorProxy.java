// WARNING: THIS FILE IS MANAGED BY SPRING ROO.

package rxf.shared.prox;

import java.util.Date;

import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.ProxyForName;

@ProxyForName(value = "rxf.server.Visitor", locator = "rxf.server.VisitorLocator")
public interface VisitorProxy extends EntityProxy {
  String getId();

  Date getCreation();

  void setCreation(Date creation);

  String getVersion();

  void setVersion(String response);
}
