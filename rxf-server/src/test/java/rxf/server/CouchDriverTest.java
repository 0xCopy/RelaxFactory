package rxf.server;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.gson.JsonSyntaxException;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import org.junit.*;
import rxf.server.gen.CouchDriver;
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

  @Test
  public void testDrivers() {
    try {

      {  //REALLY NUKE THE OLD TESTS

        String json = DocFetch.$().db("").docId("_all_dbs").to().fire().json();
        String[] strings = GSON.fromJson(json, String[].class);
        for (String s : strings) {
          if (s.startsWith(SOMEDBPREFIX)) CouchDriver.DbDelete.$().db(s).to().fire().tx();
        }

      }
      {//this can fail with a 415 error if the db already exists - should have some setup that deletes dbs if they exist
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
        assertEquals(tx.id(), "_design/sample");
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
        String designDoc = DesignDocFetch.$().db(SOMEDB).designDocId("_design/sample").to().fire().json();
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
        Rfc822HeaderState state = new Rfc822HeaderState("ETag");
        String designDoc = DesignDocFetch.$().db(SOMEDB).designDocId("_design/sample").to().state(state).fire().json();
        String rev = state.headerString("ETag");
        assertNotNull(rev);
        rev = rev.substring(1, rev.length() - 1);
        CouchTx tx = DocDelete.$().db(SOMEDB).docId("_design/sample").rev(rev).to().fire().tx();
        assertNotNull(tx);
        assertTrue(tx.ok());
        assertNull(tx.error());

        designDoc = DesignDocFetch.$().db(SOMEDB).designDocId("_design/sample").to().fire().json();
        assertNull(designDoc);
      }
      {
        CouchTx tx = ensureGone();
        assertNotNull(tx);
        assertTrue(tx.ok());
        assertNull(tx.getError());
      }

    } catch (JsonSyntaxException e) {
      e.printStackTrace();
      fail();
    }

  }

  private CouchTx ensureGone() {
    return DbDelete.$().db(SOMEDB).to().fire().tx();
  }
}
