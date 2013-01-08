package ds.shared.rf;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.RequestFactory;
import com.google.web.bindery.requestfactory.shared.Service;
import ds.server.SecurityImpl;
import ds.shared.rf.proxy.*;
import ds.shared.rf.request.*;
import rxf.shared.prox.CouchTxProxy;

/**
 * User: jim
 * Date: 5/14/12
 * Time: 2:50 AM
 */
public interface DealRequestFactory extends RequestFactory {

    DealRequest dealReq();

    LoginRequest loginReq();

    VendorRequest vendorReq();

    NpoRequest npoReq();

    @Service(SecurityImpl.class)
    interface SendRequest extends RequestContext {
        Request<CouchTxProxy> deal(DealProxy deal);

        Request<CouchTxProxy> login(LoginProxy login);

        Request<CouchTxProxy> vendor(VendorProxy vendor);
    }

    SendRequest send();

}
