package rxf.server;

import java.util.Map;

import rxf.server.CouchDriver.getDocBuilder;
import rxf.server.CouchDriver.updateDocBuilder;

import static rxf.server.BlobAntiPatternObject.GSON;

/**
 * User: jim
 * Date: 4/20/12
 * Time: 5:04 PM
 */
public class VisitorPropertiesAccess {

  static public String getSessionProperty(String key) throws Exception {
    String doc = new getDocBuilder().db(Visitor.createLocator().getPathPrefix()).docId(BlobAntiPatternObject.getSessionCookieId()).to().fire().pojo();
    return String.valueOf(GSON.fromJson(doc, Map.class).get(key));
  }

  static public String setSessionProperty(String key, String value) throws Exception {

    String eid = BlobAntiPatternObject.getSessionCookieId();
    String doc = new getDocBuilder().db(Visitor.createLocator().getPathPrefix()).docId(eid).to().fire().pojo();
    Map map = GSON.fromJson(doc, Map.class);
    map.put(key, value);
    String s = GSON.toJson(map);
    CouchTx tx = new updateDocBuilder().db(doc).docId(eid).rev(ActionBuilder.get().state().dequotedHeader(CouchMetaDriver.ETAG)).to().fire().tx();
    return GSON.toJson(tx);

  }
}