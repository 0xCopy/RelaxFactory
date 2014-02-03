package rxf.server.gen;

import com.google.gson.JsonSyntaxException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rxf.server.BlobAntiPatternObject;
import rxf.shared.CouchTx;
import rxf.server.driver.CouchMetaDriver;
import rxf.server.gen.CouchDriver.DbCreate;
import rxf.server.gen.CouchDriver.DbDelete;
import rxf.server.gen.CouchDriver.DocFetch;
import rxf.server.gen.CouchDriver.DocPersist;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static one.xio.HttpMethod.*;
import static org.junit.Assert.*;
import static rxf.server.BlobAntiPatternObject.setDEBUG_SENDJSON;

/**
 * Tests out the db, cleaning up after itself. These must be run in order to work correctly and clean up.
 */
public class PathologicalBufferSizeTest {

  public static final String SOMEDBPREFIX = "test_somedb_";
  public static final String SOMEDB = SOMEDBPREFIX + System.currentTimeMillis(); //ordered names of testdbs for failure postmortem....
  public static final String DESIGN_SAMPLE = "_design/sample";
  public static final String[] A = new String[] {};
  public static ScheduledExecutorService exec;

  static void nukeTestDbs() {
    //REALLY NUKE THE OLD TESTS

    try {
      String json = DocFetch.$().db("").docId("_all_dbs").to().fire().json();
      String[] strings = CouchMetaDriver.gson().fromJson(json, String[].class);
      for (String s : strings) {
        if (s.startsWith(SOMEDBPREFIX))
          DbDelete.$().db(s).to().fire().tx();
      }
    } catch (JsonSyntaxException e) {
      e.printStackTrace();
      fail();
    }
  }

  @BeforeClass
  static public void setUp() throws Exception {
    setDEBUG_SENDJSON(true);
    setKillswitch(false);
    exec = Executors.newScheduledThreadPool(2);
    exec.submit(new Runnable() {
      public void run() {
        try {
          init(null/*, 1000*/);
        } catch (Exception e) {
          fail();
        }
      }
    });
    nukeTestDbs();
    BlobAntiPatternObject.setReceiveBufferSize(4);
    BlobAntiPatternObject.setSendBufferSize(67);
    {
      CouchTx tx = DbCreate.$().db(SOMEDB).to().fire().tx();
      assertNotNull(tx);
      assertTrue(tx.ok());
      assertNull(tx.getError());
    }

  }

  @AfterClass
  static public void tearDown() throws Exception {

    try {
      setKillswitch(true);
      getSelector().close();
      exec.shutdown();
    } catch (Exception ignore) {
    }
  }

  @Test(timeout = 1000)
  public void testLowLevelUpdateDoc() {
    CouchTx tx = DocPersist.$().db(SOMEDB).validjson("{}").to().fire().tx();

    String data = DocFetch.$().db(SOMEDB).docId(tx.id()).to().fire().json();
    Map<String, Object> obj =
        CouchMetaDriver.gson().<Map<String, Object>> fromJson(data, Map.class);
    obj.put("abc", "123");
    data = CouchMetaDriver.gson().toJson(obj);
    CouchTx updateTx = DocPersist.$().db(SOMEDB).validjson(data).to().fire().tx();
    assertNotNull(updateTx);
  }

  @Test(timeout = 1000)
  public void testLowLevelFetch() {
    CouchTx tx = DocPersist.$().db(SOMEDB).validjson("{\"created\":true}").to().fire().tx();

    String data = DocFetch.$().db(SOMEDB).docId(tx.id()).to().fire().json();
    assertTrue(data.contains("created"));
  }

  @Test
  public void testMissingDocLowLevelFailure() {
    CouchTx tx = DocPersist.$().db("dne_dne").validjson("{}").to().fire().tx();
    //new contract for non-20x results is find the nearest window and get to ground.
    assertNull(tx);
  }

  @Test(timeout = 1000)
  public void testDocPersist() {
    CouchTx tx = DocPersist.$().db(SOMEDB).validjson("{\"_id\":\"t\"}").to().fire().tx();
    assertNotNull(tx);
    assertTrue(tx.ok());
    assertNotNull(tx.getId());
    assertNull(tx.getError());
  }
  /*

   @Test(timeout = 1000)
   public void testManualViewFetch() {
   String doc =
   "{" + "  \"_id\" : \"" + DESIGN_SAMPLE + "\"," + "  \"views\" : {" + "    \"foo\" : {"
   + "      \"map\" : \"function(doc){ emit(doc.name, doc); }\"" + "    }" + "  }" + "}";
   //TODO inconsistent with DesignDocFetch
   CouchTx tx = JsonSend.$().opaque(SOMEDB).validjson(doc).to().fire().tx();
   assertNotNull(tx);
   assertTrue(tx.ok());
   assertEquals(tx.id(), DESIGN_SAMPLE);

   DocPersist.$().db(SOMEDB).validjson("{\"name\":\"a\",\"brand\":\"c\"}").to().fire().tx();
   String space =
   "hal kjfljdskjahkjsdfkajhdf halkjsdf kgasdkjfh hwroeuvbdfhjvb nv ihdfousbkvjlsdfkvbdkjfvpghblkjfgbldkgf,xjbxdl kfjbhxv,vdlkgfhbfkljdflkjh dfjgh bsjdhfg hlhgdvkjhgksdfglhs";
   DocPersist.$().db(SOMEDB).validjson(
   "{\"name\":\"b\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to().fire().tx();
   DocPersist.$().db(SOMEDB).validjson(
   "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
   .fire().tx();
   DocPersist.$().db(SOMEDB).validjson(
   "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
   .fire().tx();
   DocPersist.$().db(SOMEDB).validjson(
   "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
   .fire().tx();
   DocPersist.$().db(SOMEDB).validjson(
   "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
   .fire().tx();
   DocPersist.$().db(SOMEDB).validjson(
   "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
   .fire().tx();
   DocPersist.$().db(SOMEDB).validjson(
   "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
   .fire().tx();
   DocPersist.$().db(SOMEDB).validjson(
   "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
   .fire().tx();

   //running view
   final ViewFetchTerminalBuilder fire =
   ViewFetch.$().db(SOMEDB).type(Map.class).view(DESIGN_SAMPLE + "/_view/foo?key=\"a\"").to()
   .fire();
   CouchResultSet<Map<String, String>> data = fire.rows();
   assertNotNull(data);
   assertEquals(1, data.rows.size());
   assertEquals("a", data.rows.get(0).value.get("name")); //TODO no consistent way to write designdoc
   String designDoc = DesignDocFetch.$().db(SOMEDB).designDocId(DESIGN_SAMPLE).to().fire().json();
   assertNotNull(designDoc);
   Map<String, Object> obj =
   CouchMetaDriver.gson().<Map<String, Object>> fromJson(designDoc, Map.class);

   Map<String, String> foo = (Map<String, String>) ((Map<String, ?>) obj.get("views")).get("foo");
   foo.put("map", "function(doc){ emit(doc.brand, doc); }");

   designDoc = CouchMetaDriver.gson().toJson(obj);
   tx = JsonSend.$().opaque(SOMEDB).validjson(designDoc).to().fire().tx();

   assertNotNull(tx);
   assertTrue(tx.ok());
   assertFalse(obj.get("_rev").equals(tx.getRev()));
   assertEquals(obj.get("_id"), tx.id());

   data =
   ViewFetch.$().db(SOMEDB).type(Map.class).view(DESIGN_SAMPLE + "/_view/foo?key=\"d\"").to()
   .fire().rows();
   assertNotNull(data);
   assertEquals(8, data.rows.size());
   assertEquals("b", data.rows.get(0).value.get("name"));

   String rev = null;
   try {
   rev = RevisionFetch.$().db(SOMEDB).docId(DESIGN_SAMPLE).to().fire().json();
   tx = DocDelete.$().db(SOMEDB).docId(DESIGN_SAMPLE).rev(rev).to().fire().tx();
   assert tx.ok();
   } catch (Exception e) {
   e.printStackTrace();
   fail(rev);
   }
   designDoc = DesignDocFetch.$().db(SOMEDB).designDocId(DESIGN_SAMPLE).to().fire().json();
   assertNull(designDoc);
   }

   public static class CSFTest {
   public String _id, _rev;
   public String model;
   public String brand;
   }
   */

}
