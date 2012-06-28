package rxf.server;

import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.GSON;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import rxf.server.gen.CouchDriver.DbDelete;
import rxf.server.gen.CouchDriver.DocFetch;
import rxf.server.web.inf.ProtocolMethodDispatch;

import com.google.gson.JsonSyntaxException;

/**
 * Tests out the db, cleaning up after itself. These must be run in order to work correctly and clean up.
 */
public class CouchServiceTest extends TestCase {

  public static final String SOMEDBPREFIX = "test_somedb_";
  public static final String SOMEDB = SOMEDBPREFIX + System.currentTimeMillis();   //ordered names of testdbs for failure postmortem....
  public ScheduledExecutorService exec;

  @BeforeClass
  public void setUp() throws Exception {
    BlobAntiPatternObject.DEBUG_SENDJSON = true;
    HttpMethod.killswitch = false;
    exec = Executors.newScheduledThreadPool(2);
    exec.submit(new Runnable() {
      public void run() {
        AsioVisitor topLevel = new ProtocolMethodDispatch();
        try {
          HttpMethod.init(new String[]{}, topLevel, 1000);

        } catch (Exception e) {
          fail();
        }
      }
    });
    nukeTestDbs();
    EXECUTOR_SERVICE.schedule(new Runnable() {
      public void run() {
        fail();
      }
    }, 5, TimeUnit.SECONDS);
  }

  static void nukeTestDbs() {
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

  @AfterClass
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

  public static class CSFTest {
    public String _id, _rev;

    public String model;
    public String brand;
  }

  public interface SimpleCouchService extends CouchService<CSFTest> {
    @View(map = "function(doc){emit(doc.brand, doc); }")
    List<CSFTest> getItemsWithBrand(@Key String brand);
  }

  public interface TrivialCouchService extends CouchService<CSFTest> {

  }


  public void testTrivialFetchDoc() throws InterruptedException, ExecutionException {
    TrivialCouchService service = CouchServiceFactory.get(TrivialCouchService.class, SOMEDB);


    CSFTest entity = new CSFTest();


    entity.model = "abc";
    entity.brand = "def";


    CouchTx tx = service.persist(entity);
    String id = tx.id();

    CSFTest obj = service.find(id);
    junit.framework.Assert.assertNotNull(obj);
    junit.framework.Assert.assertEquals("abc", obj.model);
    junit.framework.Assert.assertEquals("def", obj.brand);


  }

//  public void testRevisionAndDelete() {
//    String rev = null;
//    try {
//      rev = RevisionFetch.$().db(SOMEDB).docId(DESIGN_SAMPLE).to().fire().json();
//      CouchTx tx = DocDelete.$().db(SOMEDB).docId(DESIGN_SAMPLE).rev(rev).to().fire().tx();
//      assert tx.ok();
//    } catch (Exception e) {
//      e.printStackTrace();
//      fail(rev);
//    }
//    String designDoc = DesignDocFetch.$().db(SOMEDB).designDocId(DESIGN_SAMPLE).to().fire().json();
//    assertNull(designDoc);
//  }

  @Test
  public void testTrivialFinders() {
    try {
      TrivialCouchService service = null;
      service = CouchServiceFactory.get(TrivialCouchService.class, SOMEDB);


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
    } catch (Exception e) {
      fail();
    }
  }

  public void testSimpleFinder() {
    try {
      SimpleCouchService service = null;

      service = CouchServiceFactory.get(SimpleCouchService.class, SOMEDB);


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

    } catch (Exception e) {
      fail();
    }
  }

  public void testSimpleFinderEmptyRowset() {
    try {
      SimpleCouchService service = null;

      service = CouchServiceFactory.get(SimpleCouchService.class, SOMEDB);


      CSFTest a = new CSFTest();
      a.brand = "something";
      CSFTest b = new CSFTest();
      b.brand = "else";

      service.persist(a);
      service.persist(b);

      List<CSFTest> noResults = service.getItemsWithBrand("a");
      assert (null == noResults || noResults.isEmpty());
      junit.framework.Assert.assertEquals(0, noResults.size());
    } catch (Exception e) {
      fail();
    }
  }
  
  public interface SlightlyComplexCouchService extends CouchService<CSFTest> {
    @View(map="function(doc){emit(doc.model.slice(0,4), doc);}")
    List<CSFTest> load(@Key String key, @Limit int limit, @Skip int skip);
  }
  public void testMultiParamFinder() throws Exception {
    SlightlyComplexCouchService service = CouchServiceFactory.get(SlightlyComplexCouchService.class, SOMEDB);
    for (int i = 0; i < 10; i++) {
      CSFTest a = new CSFTest();
      a.brand = "-brand" + i;
      a.model = "-model" + i;
      service.persist(a);
    }
    
    List<CSFTest> loaded = service.load("-mod", 5, 5);
    assertEquals(5, loaded.size());
    assertEquals("-brand5", loaded.get(0).brand);
    assertEquals("-model9", loaded.get(4).model);
  }

}
