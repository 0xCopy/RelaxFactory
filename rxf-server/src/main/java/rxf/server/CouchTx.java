package rxf.server;

public class CouchTx /*implements CouchTx*/ {
  private Boolean ok;
  private String id;
  private String rev;
  private String error;
  private String reason;

  public String toString() {
    return "CouchTx{" +
        "ok=" + getOk() +
        ", id='" + getId() + '\'' +
        ", rev='" + getRev() + '\'' +
        ", error='" + getError() + '\'' +
        ", reason='" + getReason() + '\'' +
        '}';
  }


  public Boolean getOk() {
    return ok;
  }


  public void setOk(Boolean ok) {
    this.ok = ok;
  }


  public String getId() {
    return id;
  }


  public void setId(String id) {
    this.id = id;
  }


  public String getRev() {
    return rev;
  }


  public void setRev(String rev) {
    this.rev = rev;
  }


  public String getError() {
    return error;
  }


  public void setError(String error) {
    this.error = error;
  }


  public String getReason() {
    return reason;
  }


  public void setReason(String reason) {
    this.reason = reason;
  }
}
