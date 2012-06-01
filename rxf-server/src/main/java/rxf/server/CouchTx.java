package rxf.server;

public class CouchTx /*implements CouchTx*/ {
  private Boolean ok;
  private String id;
  private String rev;
  private String error;
  private String reason;

  @Override
  public String toString() {
    return "CouchTx{" +
        "ok=" + ok +
        ", id='" + id + '\'' +
        ", rev='" + rev + '\'' +
        ", error='" + error + '\'' +
        ", reason='" + reason + '\'' +
        '}';
  }

  public Boolean ok() {
    return ok;
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

  public CouchTx ok(Boolean ok) {
    this.ok = ok;
    return this;
  }

  public CouchTx id(String id) {
    this.id = id;
    return this;
  }


  public CouchTx rev(String rev) {
    this.rev = rev;
    return this;
  }

  public CouchTx error(String error) {
    this.error = error;
    return this;
  }

  public CouchTx reason(String reason) {
    this.reason = reason;
    return this;
  }
}
