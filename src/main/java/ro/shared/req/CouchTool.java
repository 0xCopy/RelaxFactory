package ro.shared.req;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;
import ro.server.CouchToolImpl;

/**
 * User: jim
 * Date: 4/20/12
 * Time: 5:05 PM
 */
@Service(value = CouchToolImpl.class)
public interface CouchTool extends RequestContext {
  Request<String> getProperty(String id, String key);

  Request<Void> setProperty(String id,String key ,String value);


}
