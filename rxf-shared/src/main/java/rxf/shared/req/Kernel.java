// WARNING: THIS FILE IS MANAGED BY SPRING ROO.

package rxf.shared.req;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.ServiceName;
import rxf.shared.prox.VisitorProxy;


@ServiceName("rxf.server.BlobAntiPatternObject")
public interface Kernel extends RequestContext {
  Request<VisitorProxy> getCurrentSession();
}
