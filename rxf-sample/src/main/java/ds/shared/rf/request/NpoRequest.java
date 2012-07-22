package ds.shared.rf.request;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;
import ds.model.NpoService;
import ds.shared.rf.proxy.NpoProxy;
import rxf.server.guice.InjectingServiceLocator;

import java.util.List;

@Service(value = NpoService.class, locator = InjectingServiceLocator.class)
public interface NpoRequest extends RequestContext {

    Request<NpoProxy> find(String key);

    Request<List<NpoProxy>> findAll();
}
