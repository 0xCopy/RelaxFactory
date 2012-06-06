package rxf.shared.req;

import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.Request;
import rxf.shared.prox.CouchTxProxy;

public interface PersistsCouch<T extends EntityProxy> {
  Request<CouchTxProxy> persist(T t);
}