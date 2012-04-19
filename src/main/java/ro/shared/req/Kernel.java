// WARNING: THIS FILE IS MANAGED BY SPRING ROO.

package ro.shared.req;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;
import ro.server.KernelImpl;
import ro.shared.prox.RoSessionProxy;


@Service(value = KernelImpl.class)
public interface Kernel extends RequestContext {
  Request<RoSessionProxy> getCurrentSession();
}
