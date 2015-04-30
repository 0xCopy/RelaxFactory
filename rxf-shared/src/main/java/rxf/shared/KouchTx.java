package rxf.shared;

/**
 * Created by jim on 8/8/14.
 */
public interface KouchTx {
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
