package rxf.server;

import java.io.IOException;

import junit.framework.TestCase;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;

public class CouchDriverTest extends TestCase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
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
  
  @Override
  protected void tearDown() throws Exception {
    super.tearDown();
    HttpMethod.killswitch = false;
    Thread.sleep(4000);//more than 3 seconds, standard timeout
  }

  public void testCreateDb() throws IOException {
    //this can fail with a 415 error if the db already exists - should have some setup that deletes dbs if they exist
    CouchTx tx = CouchDriver.DbCreate.$().db("test_somedb").to().fire().tx();
    assertNotNull(tx);
    assertTrue(tx.ok());
    assertNull(tx.getError());
  }
  
  public void testCreateDoc() {
    CouchTx tx = CouchDriver.DocPersist.$().db("test_somedb").validjson("{}").to().fire().tx();
    assertNotNull(tx);
    assertTrue(tx.ok());
    assertNotNull(tx.getId());
    assertNull(tx.getError());
  }
}
