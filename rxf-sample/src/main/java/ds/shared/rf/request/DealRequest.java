package ds.shared.rf.request;

import java.util.List;

import rxf.server.guice.InjectingServiceLocator;
import rxf.shared.req.PersistsCouch;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;

import ds.model.DealService;
import ds.shared.rf.proxy.DealProxy;

/**
 * User: jim
 * Date: 5/14/12
 * Time: 1:01 AM
 */
@Service(value = DealService.class, locator = InjectingServiceLocator.class)
public interface DealRequest extends RequestContext, PersistsCouch<DealProxy> {
    Request<DealProxy> find(String key);

    /**
     * at first this i  a single key.  later it will expand to include range key{start,stop}
     *
     * @param q
     * @return
     */
    Request<List<DealProxy>> findByProduct(String q);

    Request<List<DealProxy>> findAll();

}
