package rxf.server;

import java.util.Date;

import com.google.gson.annotations.SerializedName;

/**
 * User: jim
 * Date: 4/15/12
 * Time: 9:57 PM
 */

public class Visitor {

  @SerializedName("_id")
  private String id;

  private Date creation;

  @SerializedName("_rev")
  private String version;

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
}
