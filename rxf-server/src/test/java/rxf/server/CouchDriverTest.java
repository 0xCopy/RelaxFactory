package rxf.server;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static rxf.server.BlobAntiPatternObject.GSON;

import java.io.IOException;
import java.util.Map;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests out the db, cleaning up after itself. These must be run in order to work correctly and clean up.
 *
 */
public class CouchDriverTest {

  @Before
  public void setUp() throws Exception {
    BlobAntiPatternObject.DEBUG_SENDJSON = true;
    HttpMethod.killswitch = false;
    new Thread() {
      public void run() {
        AsioVisitor topLevel = new ProtocolMethodDispatch();
        try {
          HttpMethod.init(new String[]{}, topLevel, 1000);
        } catch (IOException e) {
        }
      }
    }.start();
  }

  @After
  public void tearDown() throws Exception {
    HttpMethod.killswitch = false;
    HttpMethod.getSelector().close();
    //Thread.sleep(4000);//more than 3 seconds, standard timeout
  }

  @Test
  public void testCreateDb() throws IOException {
    //this can fail with a 415 error if the db already exists - should have some setup that deletes dbs if they exist
    CouchTx tx = CouchDriver.DbCreate.$().db("test_somedb").to().fire().tx();
    assertNotNull(tx);
    assertTrue(tx.ok());
    assertNull(tx.getError());
  }

  @Test
  public void testCreateDoc() {
    CouchTx tx = CouchDriver.DocPersist.$().db("test_somedb").validjson("{}").to().fire().tx();
    assertNotNull(tx);
    assertTrue(tx.ok());
    assertNotNull(tx.getId());
    assertNull(tx.getError());
  }

  @Test
  public void testFetchDoc() {
    CouchTx tx = CouchDriver.DocPersist.$().db("test_somedb").validjson("{\"created\":true}").to().fire().tx();

    String data = CouchDriver.DocFetch.$().db("test_somedb").docId(tx.id()).to().fire().pojo();
    assertTrue(data.contains("created"));
  }

  @Test
  public void testUpdateDoc() {
    CouchTx tx = CouchDriver.DocPersist.$().db("test_somedb").validjson("{}").to().fire().tx();

    String data = CouchDriver.DocFetch.$().db("test_somedb").docId(tx.id()).to().fire().pojo();
    Map<String, Object> obj = GSON.<Map<String, Object>>fromJson(data, Map.class);
    obj.put("abc", "123");
    data = GSON.toJson(obj);
    CouchTx updateTx = CouchDriver.DocPersist.$().db("test_somedb").validjson(data).to().fire().tx();
    assertNotNull(updateTx);
  }

  @Test
  public void testCreateDesignDoc() {
    String doc = "{" +
        "  \"_id\" : \"_design/sample\"," +
        "  \"views\" : {" +
        "    \"foo\" : {" +
        "      \"map\" : \"function(doc){ emit(doc.name, doc); }\"" +
        "    }" +
        "  }" +
        "}";
    //TODO inconsistent with DesignDocFetch
    CouchTx tx = CouchDriver.JsonSend.$().opaque("test_somedb/_design/sample").validjson(doc).to().fire().tx();
    assertNotNull(tx);
    assertTrue(tx.ok());
    assertEquals(tx.id(), "_design/sample");
  }

  @Test
  public void testRunDesignDocView() {
    // sample data
    CouchDriver.DocPersist.$().db("test_somedb").validjson("{\"name\":\"a\",\"brand\":\"c\"}").to().fire().tx();
    CouchDriver.DocPersist.$().db("test_somedb").validjson("{\"name\":\"b\",\"brand\":\"d\"}").to().fire().tx();

    //running view
    CouchResultSet<Map<String, String>> data = CouchDriver.ViewFetch.<Map<String, String>>$().db("test_somedb").type(Map.class).view("_design/sample/_view/foo?key=\"a\"").to().fire().rows();
    assertNotNull(data);
    assertEquals(1, data.rows.size());
    assertEquals("a", data.rows.get(0).value.get("name"));
  }

  @Test
  public void testUpdateDesignDocView() {
    //TODO no consistent way to write designdoc
    String designDoc = CouchDriver.DesignDocFetch.$().db("test_somedb").designDocId("_design/sample").to().fire().pojo();
    assertNotNull(designDoc);
    Map<String, Object> obj = GSON.<Map<String, Object>>fromJson(designDoc, Map.class);

    Map<String, String> foo = (Map<String, String>) ((Map<String, Object>) obj.get("views")).get("foo");
    foo.put("map", "function(doc){ emit(doc.brand, doc); }");

    designDoc = GSON.toJson(obj);
    CouchTx tx = CouchDriver.JsonSend.$().opaque("test_somedb/_design/sample").validjson(designDoc).to().fire().tx();
    
    assertNotNull(tx);
    assertTrue(tx.ok());
    assertFalse(obj.get("_rev").equals(tx.getRev()));
    assertEquals(obj.get("_id"), tx.id());
    
    CouchResultSet<Map<String, String>> data = CouchDriver.ViewFetch.<Map<String, String>>$().db("test_somedb").type(Map.class).view("_design/sample/_view/foo?key=\"d\"").to().fire().rows();
    assertNotNull(data);
    assertEquals(1, data.rows.size());
    assertEquals("b", data.rows.get(0).value.get("name"));
  }

  @Test
  public void testDeleteDb() {
    CouchTx tx = CouchDriver.DbDelete.$().db("test_somedb").to().fire().tx();
    assertNotNull(tx);
    assertTrue(tx.ok());
    assertNull(tx.getError());
  }
}
