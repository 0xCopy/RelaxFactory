package rxf.server;

public class CouchTx {
  public Boolean ok;
  public String id, rev, error, reason;

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
}
