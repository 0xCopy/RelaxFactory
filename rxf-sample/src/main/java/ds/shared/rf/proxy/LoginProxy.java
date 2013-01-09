package ds.shared.rf.proxy;

import com.google.web.bindery.requestfactory.shared.EntityProxy;
import com.google.web.bindery.requestfactory.shared.ProxyFor;
import ds.model.Login;
import ds.server.SecurityImpl;

import java.util.Date;
import java.util.List;

/**
 * User: jim
 * Date: 5/14/12
 * Time: 1:40 AM
 */
@ProxyFor(value = Login.class, locator = SecurityImpl.LoginCouchLocator.class)
public interface LoginProxy extends EntityProxy {
    String getId();

    void setId(String id);

    String getVersion();

    void setVersion(String version);

//  Visitor getVisitor();
//
//  void setVisitor(Visitor visitor);
//
//  String getVisitorId();
//
//  void setVisitorId(String visitorId);

    String getMd5();

    //  void setMd5(String md5);
    Date getLastModified();

    Date getExpires();

    String getAuthUrl();

    List<String> getScopes();

    String getService();

    void setLastModified(Date s);

    void setExpires(Date date);

    void setAuthUrl(String authUrl);

    void setScopes(List<String> strings);

    void setService(String service);
}
