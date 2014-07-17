package rxf.couch.gen;

import com.google.gson.JsonSyntaxException;
import one.xio.AsioVisitor;
import one.xio.AsyncSingletonServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rxf.couch.CouchResultSet;
import rxf.rpc.RpcHelper;
import rxf.shared.CouchTx;
import rxf.couch.driver.CouchMetaDriver;
import rxf.couch.gen.CouchDriver.*;
import rxf.couch.gen.CouchDriver.ViewFetch.ViewFetchTerminalBuilder;
import rxf.web.inf.ProtocolMethodDispatch;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static junit.framework.TestCase.*;

/**
 * Tests out the db, cleaning up after itself. These must be run in order to work correctly and clean up.
 */
public class CouchDriverTest {

  public static final String SOMEDBPREFIX = "test_somedb_";
  public static final String SOMEDB = SOMEDBPREFIX + System.currentTimeMillis(); // ordered names of testdbs for failure
                                                                                 // postmortem....
  public static final String DESIGN_SAMPLE = "_design/sample";
  public static ScheduledExecutorService exec;

  @BeforeClass
  static public void setUp() throws Exception {
    RpcHelper.DEBUG_SENDJSON = true;
    AsyncSingletonServer.killswitch.set(false);
    exec = Executors.newScheduledThreadPool(2);
    exec.submit(new Runnable() {
      public void run() {
        AsioVisitor topLevel = new ProtocolMethodDispatch();
        try {
          AsyncSingletonServer.SingleThreadSingletonServer.init(topLevel);
        } catch (Exception e) {
          fail();
        }
      }
    });
    nukeTestDbs();

    {
      CouchTx tx = new DbCreate().db(SOMEDB).to().fire().tx();
      assertNotNull(tx);
      assertTrue(tx.ok());
      assertNull(tx.getError());
    }

  }

  static void nukeTestDbs() {
    // REALLY NUKE THE OLD TESTS

    try {
      String json = new DocFetch().db("").docId("_all_dbs").to().fire().json();
      String[] strings = CouchMetaDriver.gson().fromJson(json, String[].class);
      for (String s : strings) {
        if (s.startsWith(SOMEDBPREFIX))
          new DbDelete().db(s).to().fire().tx();
      }
    } catch (JsonSyntaxException e) {
      e.printStackTrace();
      fail();
    }
  }

  @AfterClass
  static public void tearDown() throws Exception {

    // DbDelete.$().db(SOMEDB).to().fire().oneWay();

    try {
      AsyncSingletonServer.killswitch.set(true);
      AsioVisitor.Helper.getSelector().close();
      // HttpMethod.broke = null;
      exec.shutdown();
      // Thread.sleep(4000);//more than 3 seconds, standard timeout
    } catch (Exception ignore) {
    }
  }

  /*
   * public static final String DB = "rxf_csftest";
   */

  public static class CSFTest {
    public String _id, _rev;

    public String model;
    public String brand;
  }

  @Test(timeout = 1000)
  public void testLowLevelUpdateDoc() {
    CouchTx tx = new DocPersist().db(SOMEDB).validjson("{}").to().fire().tx();

    String data = new DocFetch().db(SOMEDB).docId(tx.id()).to().fire().json();
    Map<String, Object> obj =
        CouchMetaDriver.gson().<Map<String, Object>> fromJson(data, Map.class);
    obj.put("abc", "123");
    data = CouchMetaDriver.gson().toJson(obj);
    CouchTx updateTx = new DocPersist().db(SOMEDB).validjson(data).to().fire().tx();
    assertNotNull(updateTx);
  }

  @Test(timeout = 1000)
  public void testLowLevelFetch() {
    CouchTx tx = new DocPersist().db(SOMEDB).validjson("{\"created\":true}").to().fire().tx();

    String data = new DocFetch().db(SOMEDB).docId(tx.id()).to().fire().json();
    assertTrue(data.contains("created"));
  }

  @Test
  public void testMissingDocLowLevelFailure() {

    CouchTx tx = new DocPersist().db("dne_dne").validjson("{}").to().fire().tx();
    // new contract for non-20x results is find the nearest window and get to ground.
    assertNull(tx);
  }

  @Test(timeout = 1000)
  public void testDocPersist() {
    CouchTx tx = new DocPersist().db(SOMEDB).validjson("{\"_id\":\"t\"}").to().fire().tx();
    assertNotNull(tx);
    assertTrue(tx.ok());
    assertNotNull(tx.getId());
    assertNull(tx.getError());
  }

  @Test(timeout = 10000)
  public void testManualViewFetch() {
    String doc =
        "{" + "  \"_id\" : \"" + DESIGN_SAMPLE + "\"," + "  \"views\" : {" + "    \"foo\" : {"
            + "      \"map\" : \"function(doc){ emit(doc.name, doc); }\"" + "    }" + "  }" + "}";
    // TODO inconsistent with DesignDocFetch
    CouchTx tx = new JsonSend().opaque(SOMEDB).validjson(doc).to().fire().tx();
    assertNotNull(tx);
    assertTrue(tx.ok());
    assertEquals(tx.id(), DESIGN_SAMPLE);

    new DocPersist().db(SOMEDB).validjson("{\"name\":\"a\",\"brand\":\"c\"}").to().fire().tx();
    new DocPersist().db(SOMEDB).validjson("{\"name\":\"b\",\"brand\":\"d\"}").to().fire().tx();
    String space =
        "hal kjfljdskjahkjsdfkajhdf halkjsdf kgasdkjfh hwroeuvbdfhjvb nv ihdfousbkvjlsdfkvbdkjfvpghblkjfgbldkgf,xjbxdl kfjbhxv,vdlkgfhbfkljdflkjh dfjgh bsjdhfg hlhgdvkjhgksdfglhs";
    new DocPersist().db(SOMEDB).validjson(
        "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
        .fire().tx();
    new DocPersist().db(SOMEDB).validjson(
        "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
        .fire().tx();
    new DocPersist().db(SOMEDB).validjson(
        "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
        .fire().tx();
    new DocPersist().db(SOMEDB).validjson(
        "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
        .fire().tx();
    new DocPersist().db(SOMEDB).validjson(
        "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
        .fire().tx();
    new DocPersist().db(SOMEDB).validjson(
        "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
        .fire().tx();
    new DocPersist().db(SOMEDB).validjson(
        "{\"name\":\"" + System.nanoTime() + "\",\"brand\":\"d\",\"crap\":\"" + space + "\"}").to()
        .fire().tx();
    // running view
    final ViewFetchTerminalBuilder fire =
        new ViewFetch().db(SOMEDB).type(Map.class).view(DESIGN_SAMPLE + "/_view/foo?key=\"a\"")
            .to().fire();
    CouchResultSet<?, Map<String, String>> data = fire.rows();
    assertNotNull(data);
    assertEquals(1, data.rows.size());
    assertEquals("a", data.rows.get(0).value.get("name")); // TODO no consistent way to write designdoc
    String designDoc =
        new DesignDocFetch().db(SOMEDB).designDocId(DESIGN_SAMPLE).to().fire().json();
    assertNotNull(designDoc);
    Map<String, Object> obj =
        CouchMetaDriver.gson().<Map<String, Object>> fromJson(designDoc, Map.class);

    Map<String, String> foo = (Map<String, String>) ((Map<String, ?>) obj.get("views")).get("foo");
    foo.put("map", "function(doc){ emit(doc.brand, doc); }");

    designDoc = CouchMetaDriver.gson().toJson(obj);
    tx = new JsonSend().opaque(SOMEDB).validjson(designDoc).to().fire().tx();

    assertNotNull(tx);
    assertTrue(tx.ok());
    assertFalse(obj.get("_rev").equals(tx.getRev()));
    assertEquals(obj.get("_id"), tx.id());
    try {
      TimeUnit.MILLISECONDS.sleep(500);
    } catch (InterruptedException e) {
      fail(e.toString());
    }
    data =
        new ViewFetch().db(SOMEDB).type(Map.class).view(DESIGN_SAMPLE + "/_view/foo?key=\"d\"")
            .to().fire().rows();
    assertNotNull(data);
    assertEquals(8, data.rows.size());
    assertEquals("b", data.rows.get(0).value.get("name"));

    String rev = null;
    try {
      rev = new RevisionFetch().db(SOMEDB).docId(DESIGN_SAMPLE).to().fire().json();
      tx = new DocDelete().db(SOMEDB).docId(DESIGN_SAMPLE).rev(rev).to().fire().tx();
      assert tx.ok();
    } catch (Exception e) {
      e.printStackTrace();
      fail(rev);
    }
    designDoc = new DesignDocFetch().db(SOMEDB).designDocId(DESIGN_SAMPLE).to().fire().json();
    assertNull(designDoc);
  }

}
