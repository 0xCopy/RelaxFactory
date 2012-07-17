package ds.shared.rf.request;

import java.util.List;

import com.google.web.bindery.requestfactory.shared.*;
import ds.model.NpoService;
import ds.shared.rf.proxy.NpoProxy;
import rxf.server.CouchServiceLocator;

@Service(value = NpoService.class, locator = CouchServiceLocator.class)
public interface NpoRequest extends RequestContext {

  Request<NpoProxy> find(String key);
  
  Request<List<NpoProxy>> findAll();
}
