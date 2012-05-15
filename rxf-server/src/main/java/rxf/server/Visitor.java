package rxf.server;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

/**
 * User: jim
 * Date: 4/15/12
 * Time: 9:57 PM
 */

public class Visitor {
  public static CouchLocator<Visitor> createLocator() {
    return new CouchLocator<Visitor>() {
      @Override
      public Class<Visitor> getDomainType() {
        return Visitor.class;
      }

      @Override
      public String getId(Visitor domainObject) {
        return domainObject.getId();
      }

      @Override
      public Object getVersion(Visitor domainObject) {
        return domainObject.getVersion();
      }
    };
  }

  @SerializedName("_id")
  private String id;

  @SerializedName("_rev")
  private String version;

  private Date creation;

  public static Visitor createSession() {
    Visitor roSession = new Visitor();
    roSession.setCreation(new Date());
    return roSession;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Date getCreation() {
    return creation;
  }

  public void setCreation(Date creation) {
    this.creation = creation;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return "Visitor{" +
        "id='" + id + '\'' +
        ", version='" + version + '\'' +
        ", creation=" + creation +
        '}';
  }
}
