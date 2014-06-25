package rxf.rpc;

import rxf.core.Config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static rxf.core.CouchNamespace.COUCH_DEFAULT_ORGNAME;

/**
 * Created by jim on 6/7/14.
 */
public class RpcHelper {
  public static final boolean RXF_CACHED_THREADPOOL = "true".equals(Config.get(
      "RXF_CACHED_THREADPOOL", "false"));
  public static ExecutorService EXECUTOR_SERVICE = RXF_CACHED_THREADPOOL ? Executors
      .newCachedThreadPool() : Executors.newFixedThreadPool(Runtime.getRuntime()
      .availableProcessors() + 3);
  public static boolean DEBUG_SENDJSON = System.getenv().containsKey("DEBUG_SENDJSON");

  public static String getDefaultOrgName() {
    return COUCH_DEFAULT_ORGNAME;
  }

  public static boolean isDEBUG_SENDJSON() {
    return DEBUG_SENDJSON;
  }

  public static void setDEBUG_SENDJSON(boolean DEBUG_SENDJSON) {
    RpcHelper.DEBUG_SENDJSON = DEBUG_SENDJSON;
  }

  public static ExecutorService getEXECUTOR_SERVICE() {
    return EXECUTOR_SERVICE;
  }
}
