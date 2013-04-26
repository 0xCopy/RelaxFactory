package ds.model;

import com.google.gson.annotations.SerializedName;
import rxf.server.CouchService;
import rxf.server.CouchTx;
import rxf.server.gen.CouchDriver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static rxf.server.driver.CouchMetaDriver.gson;

/**
 * Visitors can authenticate to commit to a transaction.  this new state becomes tracked in Login entities.
 * <p/>
 * User: jim
 * Date: 5/12/12
 * Time: 2:56 PM
 */
public class Login {

    public Date getLastModified() {
        return lastModified;
    }

    public Date getExpires() {
        return expires;
    }

    public String getAuthUrl() {
        return authUrl;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public String getService() {
        return service;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public class LoginService implements CouchService<Login> {
        public Login find(String key) {
            return null;  //To change body of created methods use File | Settings | File Templates.
        }

        public CouchTx persist(Login entity) {


            ArrayList<String> l = new ArrayList<String>();
            l.add("rxf_login");
            String id1 = entity.getId();
            if (null != id1) {
                l.add(id1);
                String version1 = entity.getVersion();
                if (null != version1) l.add(version1);
            }
            try {
                return CouchDriver.DocPersist.$().validjson(gson().toJson(entity)).db(l.get(0)).to().fire().tx();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    public String toString() {
        return "Login{" +
                "id='" + id + '\'' +
                ", version='" + version + '\'' +
                ", token='" + getToken() + '\'' +
                ", visitorId='" + visitorId + '\'' +
                ", md5='" + md5 + '\'' +
                ", lastModified=" + lastModified +
                ", expires=" + expires +
                ", authUrl='" + authUrl + '\'' +
                ", scopes=" + scopes +
                ", service='" + service + '\'' +
                ", dirty=" + dirty +
                '}';
    }

    @SerializedName("_id")
    private String id;

    @SerializedName("_rev")
    private String version;
    private String token;
    private String visitorId;
    private String md5;
    private Date lastModified;
    private Date expires;
    private String authUrl;
    private List<String> scopes;
    private String service;


    public String getId() {
        return id;
    }


    public void setId(String id) {
        this.id = id;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    private transient boolean dirty = true;


    public String getVisitorId() {
        return visitorId;
    }

    public void setVisitorId(String visitorId) {
        this.visitorId = visitorId;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public void setService(String service) {
        this.service = service;
    }
}
