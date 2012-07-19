package ds.shared.rf.request;

import java.util.List;

import rxf.server.guice.InjectingServiceLocator;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;

import ds.model.VendorService;
import ds.shared.rf.proxy.VendorProxy;

/**
 * User: jim
 * Date: 5/14/12
 * Time: 2:03 AM
 */
@Service(value = VendorService.class, locator = InjectingServiceLocator.class)
public interface VendorRequest extends RequestContext {

  Request<VendorProxy> find(String key);
  
  Request<List<VendorProxy>> findAll();

}
