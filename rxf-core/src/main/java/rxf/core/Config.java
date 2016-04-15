package rxf.core;

import static bbcursive.lib.log.log;
import static java.lang.System.setProperty;

public class Config {

  public static String get(String rxf_var, String... defaultVal) {
    String javapropname =
        "rxf." + rxf_var.toLowerCase().replaceAll("^rxf[_.]", "").replace('_', '.');
    String rxfenv = System.getenv(rxf_var);
    String var = null == rxfenv ? System.getProperty(javapropname) : rxfenv;
    var = null == var && defaultVal.length > 0 ? defaultVal[0] : var;
    if (null != var) {
      setProperty(javapropname, var);
      log("// -D" + javapropname + "=" + "\"" + var + "\"");
    }
    return var;
  }
}
