package rxf.server;

public class Config {

  public static String get(String rxf_var, String... defaultVal) {
    String javapropname =
        "rxf.server." + rxf_var.toLowerCase().replaceAll("^rxf_(server_)?", "").replace('_', '.');
    String rxfenv = System.getenv(rxf_var);
    String var = null == rxfenv ? System.getProperty(javapropname) : rxfenv;
    var = null == var && defaultVal.length > 0 ? defaultVal[0] : var;
    if (null != var) {
      System.setProperty(javapropname, var);
      System.err.println("// -D" + javapropname + "=" + "\"" + var + "\"");
    }
    return var;
  }
}
