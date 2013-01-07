package ds.shared.rf.request;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;
import ds.model.Login;
import ds.shared.rf.proxy.LoginProxy;
import rxf.server.guice.InjectingServiceLocator;

/**
 * User: jim
 * Date: 5/14/12
 * Time: 2:03 AM
 */
@Service(value = Login.LoginService.class, locator = InjectingServiceLocator.class)

public interface LoginRequest extends RequestContext {


    Request<LoginProxy> find(String key);

//  /**
//   * at first this i  a single key.  later it will expand to include range key{start,stop}
//   *
//   * @param q
//   * @return
//   */
//  Request<List<DealProxy>> findByProduct(String q);
}
