package rxf.server.driver;

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 6/25/13
 * Time: 11:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class RxfBootstrap {

  public static String getVar(String rxf_var, String... defaultVal) {
    String javapropname =
        "rxf.server." + rxf_var.toLowerCase().replaceAll("^rxf_(server_){0,1}", "");
    String var =
        null == System.getenv(rxf_var) ? System.getProperty(javapropname) : System.getenv(rxf_var);
    var = null == var && defaultVal.length > 0 ? defaultVal[0] : var;
    if (null != var)
      System.setProperty(javapropname, var);
    return var;
  }
}
