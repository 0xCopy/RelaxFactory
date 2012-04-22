package ro.shared.req;

import com.google.web.bindery.requestfactory.shared.Request;
import com.google.web.bindery.requestfactory.shared.RequestContext;
import com.google.web.bindery.requestfactory.shared.Service;
import ro.server.SessionToolImpl;

/**
 * User: jim
 * Date: 4/20/12
 * Time: 5:05 PM
 */
@Service(value = SessionToolImpl.class)
public interface SessionTool extends RequestContext {
  Request<String> getSessionProperty(String id, String key);

  /**
   * @param id
   * @param key
   * @param value
   * @return new version string
   */
  Request<String> setSessionProperty(String id, String key, String value);


}
