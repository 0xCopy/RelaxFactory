package ro.server;

import java.util.LinkedHashMap;

/**
 * User: jim
 * Date: 4/20/12
 * Time: 5:04 PM
 */
public class VisitorPropertiesAccess {
  static public String INSTANCE = Visitor.class.getSimpleName().toLowerCase();//default

  public static String getSessionProperty(String key) {
    try {
      final String sessionCookieId = KernelImpl.getSessionCookieId();
      return KernelImpl.getSessionProperty(key);
    } catch (Exception e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return null;
  }

  /**
   * @return new version string
   */
  public static void setSessionProperty(String key, String value) {
    try {
      String id = KernelImpl.getSessionCookieId();
      LinkedHashMap linkedHashMap = KernelImpl.fetchMapById(id);
      linkedHashMap.put(key, value);
      CouchTx tx = KernelImpl.sendJson(KernelImpl.GSON.toJson(linkedHashMap), Visitor.class.getSimpleName().toLowerCase() + "/" + id, String.valueOf(linkedHashMap.get("_rev")));

    } catch (Throwable ignored) {

    }
  }

}