package rxf.shared.prox;

import com.google.web.bindery.requestfactory.shared.ProxyForName;
import com.google.web.bindery.requestfactory.shared.ValueProxy;

/**
 * User: jim
 * Date: 5/14/12
 * Time: 7:21 PM
 */
@ProxyForName(value = "rxf.couch.CouchTx")
public interface CouchTxProxy extends ValueProxy {
  Boolean getOk();

  void setOk(Boolean ok);

  String getId();

  void setId(String id);

  String getRev();

  void setRev(String rev);

  String getError();

  void setError(String error);

  String getReason();

  void setReason(String reason);
}
