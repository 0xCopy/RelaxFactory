package rxf.shared;

public class CouchTx implements KouchTx {
  private Boolean ok;
  private String id;
  private String rev;
  private String error;
  private String reason;

  public String toString() {
    return "CouchTx{" + "ok=" + ok + ", key='" + id + '\'' + ", rev='" + rev + '\'' + ", error='"
        + error + '\'' + ", reason='" + reason + '\'' + '}';
  }

  public Boolean ok() {
    return null != ok && ok;
  }

  public String id() {
    return id;
  }

  public String rev() {
    return rev;
  }

  public String error() {
    return error;
  }

  public String reason() {
    return reason;
  }

  public KouchTx ok(Boolean ok) {
    this.ok = ok;
    return this;
  }

  public KouchTx id(String id) {
    this.id = id;
    return this;
  }

  public KouchTx rev(String rev) {
    this.rev = rev;
    return this;
  }

  public KouchTx error(String error) {
    this.error = error;
    return this;
  }

  public KouchTx reason(String reason) {
    this.reason = reason;
    return this;
  }

  // //////////////////////////// RF hideousity below

  @Override
  public Boolean getOk() {
    return ok;
  }

  @Override
  public void setOk(Boolean ok) {
    this.ok = ok;
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String getRev() {
    return rev;
  }

  @Override
  public void setRev(String rev) {
    this.rev = rev;
  }

  @Override
  public String getError() {
    return error;
  }

  @Override
  public void setError(String error) {
    this.error = error;
  }

  @Override
  public String getReason() {
    return reason;
  }

  @Override
  public void setReason(String reason) {
    this.reason = reason;
  }
}
