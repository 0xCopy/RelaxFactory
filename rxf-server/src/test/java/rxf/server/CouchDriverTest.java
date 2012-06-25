package rxf.server;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import com.google.gson.JsonSyntaxException;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import org.junit.*;
import rxf.server.gen.CouchDriver.*;
import rxf.server.web.inf.ProtocolMethodDispatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static rxf.server.BlobAntiPatternObject.GSON;

/**
 * Tests out the db, cleaning up after itself. These must be run in order to work correctly and clean up.
 */
public class CouchDriverTest {

  public static final String SOMEDBPREFIX = "test_somedb_";
  public static final String SOMEDB = SOMEDBPREFIX + System.currentTimeMillis();   //ordered names of testdbs for failure postmortem....
  public static final String DESIGN_SAMPLE = "_design/sample";
  private ScheduledExecutorService exec;

  @Before
  public void setUp() throws Exception {
    BlobAntiPatternObject.DEBUG_SENDJSON = true;
    HttpMethod.killswitch = false;
    exec = Executors.newSingleThreadScheduledExecutor();
    exec.submit(new Runnable() {
      public void run() {
        AsioVisitor topLevel = new ProtocolMethodDispatch();
        try {
          HttpMethod.init(new String[]{}, topLevel, 1000);

        } catch (Exception e) {
        }
      }
    });

//    EXECUTOR_SERVICE.submit(new Runnable() {
//      @Override
//      public void run() {
//        DbDelete.$().db(SOMEDB).to().fire().oneWay();
//      }
////    }).get(3, TimeUnit.SECONDS);
  }

  @After
  public void tearDown() throws Exception {

//    DbDelete.$().db(SOMEDB).to().fire().oneWay();

    try {
      HttpMethod.killswitch = true;
      HttpMethod.getSelector().close();
//      HttpMethod.broke = null;
      exec.shutdown();
      //Thread.sleep(4000);//more than 3 seconds, standard timeout
    } catch (Exception ignore) {
    }
  }
/*
  private static final String DB = "rxf_csftest";*/

  public static class CSFTest {
    private String _id, _rev;

    public String model;
    public String brand;
  }

  public interface SimpleCouchService extends CouchService<CSFTest> {
    @View(map = "function(doc){emit(doc.brand, doc); }")
    List<CSFTest> getItemsWithBrand(@Key String brand);
  }

  public interface TrivialCouchService extends CouchService<CSFTest> {

  }

  @Test
  public void testDrivers() throws IOException, TimeoutException, InterruptedException {
    try {


      {
        CouchTx tx = DbCreate.$().db(SOMEDB).to().fire().tx();
        assertNotNull(tx);
        assertTrue(tx.ok());
        assertNull(tx.getError());
      }
      {
        CouchTx tx = DocPersist.$().db(SOMEDB).validjson("{}").to().fire().tx();
        assertNotNull(tx);
        assertTrue(tx.ok());
        assertNotNull(tx.getId());
        assertNull(tx.getError());
      }
      {
        CouchTx tx = DocPersist.$().db("dne_dne").validjson("{}").to().fire().tx();
        assertNotNull(tx);
        assertFalse(tx.ok());
        assertNotNull(tx.getError());
      }
      {
        CouchTx tx = DocPersist.$().db(SOMEDB).validjson("{\"created\":true}").to().fire().tx();

        String data = DocFetch.$().db(SOMEDB).docId(tx.id()).to().fire().json();
        assertTrue(data.contains("created"));
      }
      {
        CouchTx tx = DocPersist.$().db(SOMEDB).validjson("{}").to().fire().tx();

        String data = DocFetch.$().db(SOMEDB).docId(tx.id()).to().fire().json();
        Map<String, Object> obj = GSON.<Map<String, Object>>fromJson(data, Map.class);
        obj.put("abc", "123");
        data = GSON.toJson(obj);
        CouchTx updateTx = DocPersist.$().db(SOMEDB).validjson(data).to().fire().tx();
        assertNotNull(updateTx);
      }
      {
        String doc = "{" +
            "  \"_id\" : \"_design/sample\"," +
            "  \"views\" : {" +
            "    \"foo\" : {" +
            "      \"map\" : \"function(doc){ emit(doc.name, doc); }\"" +
            "    }" +
            "  }" +
            "}";
        //TODO inconsistent with DesignDocFetch
        CouchTx tx = JsonSend.$().opaque(SOMEDB +
            "/_design/sample").validjson(doc).to().fire().tx();
        assertNotNull(tx);
        assertTrue(tx.ok());
        assertEquals(tx.id(), DESIGN_SAMPLE);
      }
      {
        // sample data
        DocPersist.$().db(SOMEDB).validjson("{\"name\":\"a\",\"brand\":\"c\"}").to().fire().tx();
        DocPersist.$().db(SOMEDB).validjson("{\"name\":\"b\",\"brand\":\"d\"}").to().fire().tx();

        //running view
        CouchResultSet<Map<String, String>> data = ViewFetch.$().db(SOMEDB).type(Map.class).view("_design/sample/_view/foo?key=\"a\"").to().fire().rows();
        assertNotNull(data);
        assertEquals(1, data.rows.size());
        assertEquals("a", data.rows.get(0).value.get("name"));
      }
      {
        //TODO no consistent way to write designdoc
        String designDoc = DesignDocFetch.$().db(SOMEDB).designDocId(DESIGN_SAMPLE).to().fire().json();
        assertNotNull(designDoc);
        Map<String, Object> obj = GSON.<Map<String, Object>>fromJson(designDoc, Map.class);

        Map<String, String> foo = (Map<String, String>) ((Map<String, Object>) obj.get("views")).get("foo");
        foo.put("map", "function(doc){ emit(doc.brand, doc); }");

        designDoc = GSON.toJson(obj);
        CouchTx tx = JsonSend.$().opaque(SOMEDB +
            "/_design/sample").validjson(designDoc).to().fire().tx();

        assertNotNull(tx);
        assertTrue(tx.ok());
        assertFalse(obj.get("_rev").equals(tx.getRev()));
        assertEquals(obj.get("_id"), tx.id());

        CouchResultSet<Map<String, String>> data = ViewFetch.$().db(SOMEDB).type(Map.class).view("_design/sample/_view/foo?key=\"d\"").to().fire().rows();
        assertNotNull(data);
        assertEquals(1, data.rows.size());
        assertEquals("b", data.rows.get(0).value.get("name"));
      }
      {
        String rev = null;
        try {
          rev = RevisionFetch.$().db(SOMEDB).docId(DESIGN_SAMPLE).to().fire().json();
          final CouchTx tx = DocDelete.$().db(SOMEDB).docId(DESIGN_SAMPLE).rev(rev).to().fire().tx();
          assert tx.ok();
        } catch (Exception e) {
          e.printStackTrace();  //todo: verify for a purpose
          fail(rev);
        }
        String designDoc = DesignDocFetch.$().db(SOMEDB).designDocId(DESIGN_SAMPLE).to().fire().json();
        assertNull(designDoc);
      }


      {
        TrivialCouchService service = CouchServiceFactory.get(TrivialCouchService.class, SOMEDB);


        final CouchTx tx = DocPersist.$().db(SOMEDB).validjson("{\"model\":\"abc\",\"brand\":\"def\"}").to().fire().tx();
        String id = tx.id();

        CSFTest obj = service.find(id);
        junit.framework.Assert.assertNotNull(obj);
        junit.framework.Assert.assertEquals("abc", obj.model);
        junit.framework.Assert.assertEquals("def", obj.brand);
      }
      {
        TrivialCouchService service = null;
        try {
          service = CouchServiceFactory.get(TrivialCouchService.class, SOMEDB);
        } catch (Throwable e) {
          e.printStackTrace();  //todo: verify for a purpose
          junit.framework.Assert.fail();
        }

        CouchTx tx = service.persist(new CSFTest());

        junit.framework.Assert.assertNotNull(tx);
        junit.framework.Assert.assertTrue(tx.ok());
        junit.framework.Assert.assertNull(tx.getError());

        CSFTest obj = service.find(tx.id());
        junit.framework.Assert.assertNull(obj.brand);
        junit.framework.Assert.assertNull(obj.model);
        obj.brand = "Best";
        obj.model = "Sample";

        CouchTx tx2 = service.persist(obj);
        junit.framework.Assert.assertEquals(tx.id(), tx2.id());
        junit.framework.Assert.assertFalse(tx.rev().equals(tx2.rev()));

        CSFTest obj2 = service.find(tx.id());
        junit.framework.Assert.assertEquals("Best", obj2.brand);
        junit.framework.Assert.assertEquals("Sample", obj2.model);
      }
      {
        SimpleCouchService service = null;
        try {
          service = CouchServiceFactory.get(SimpleCouchService.class, SOMEDB);
        } catch (Throwable e) {
          e.printStackTrace();
          fail(e.getMessage());
        }

        CSFTest a = new CSFTest();
        a.brand = "something";
        CSFTest b = new CSFTest();
        b.brand = "else";

        service.persist(a);
        service.persist(b);

        List<CSFTest> results = service.getItemsWithBrand("something");
        junit.framework.Assert.assertNotNull(results);
        junit.framework.Assert.assertEquals(1, results.size());
        junit.framework.Assert.assertEquals("something", results.get(0).brand);

        List<CSFTest> noResults = service.getItemsWithBrand("a");
        junit.framework.Assert.assertNotNull(noResults);
        junit.framework.Assert.assertEquals(0, noResults.size());
      }
      {
        //REALLY NUKE THE OLD TESTS

        try {
          String json = DocFetch.$().db("").docId("_all_dbs").to().fire().json();
          String[] strings = GSON.fromJson(json, String[].class);
          for (String s : strings) {
            if (s.startsWith(SOMEDBPREFIX)) DbDelete.$().db(s).to().fire().tx();
          }
        } catch (JsonSyntaxException e) {
          e.printStackTrace();
          fail();
        }

      }
    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }

  }

}
