package ds.shared.rf.request;

import java.util.List;

import ds.model.VendorService;
import ds.shared.rf.proxy.VendorProxy;
import rxf.server.CouchServiceLocator;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;

/**
 * User: jim
 * Date: 5/14/12
 * Time: 2:03 AM
 */
@Service(value = VendorService.class, locator = CouchServiceLocator.class)
public interface VendorRequest extends RequestContext {

  Request<VendorProxy> find(String key);
  
  Request<List<VendorProxy>> findAll();

}
