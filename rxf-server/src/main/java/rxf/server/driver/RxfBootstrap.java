package rxf.server.driver;

public class RxfBootstrap {

  public static String getVar(String rxf_var, String... defaultVal) {
    String javapropname =
        "rxf.server."
            + rxf_var.toLowerCase().replaceAll("^rxf_(server_){0,1}", "").replace('_', '.');
    String var =
        null == System.getenv(rxf_var) ? System.getProperty(javapropname) : System.getenv(rxf_var);
    var = null == var && defaultVal.length > 0 ? defaultVal[0] : var;
    if (null != var) {
      System.setProperty(javapropname, var);
      System.err.println("// -D" + javapropname + "=" + "\"" + var + "\"");
    }
    return var;
  }
}
