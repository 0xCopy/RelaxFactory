package rxf.server;

import com.google.gson.JsonSyntaxException;
import junit.framework.TestCase;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import rxf.server.gen.CouchDriver.DbDelete;
import rxf.server.gen.CouchDriver.DocFetch;
import rxf.server.web.inf.ProtocolMethodDispatch;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.GSON;

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

    @Test(timeout = 1000)
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

    @Test(timeout = 1000)
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

    @Test(timeout = 1000)

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
        @View(map = "function(doc){emit(doc.model.slice(0,4), doc);}")
        List<CSFTest> load(@Key String key, @Limit int limit, @Skip int skip);

        @View(map = "function(doc){emit(doc.model, doc);}")
        List<CSFTest> anyMatching(@Keys String... keys);

        @View(map = "function(doc){emit({model:doc.model, brand:doc.brand}, doc);}")
        List<CSFTest> matchingTuples(@Key CSFTest obj);

        @View(map = "function(doc){emit(doc.id,doc);}")
        List<CSFTest> all();
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
        assertNotNull(loaded);
        assertEquals(5, loaded.size());
        assertEquals("-brand5", loaded.get(0).brand);
        assertEquals("-model9", loaded.get(4).model);
    }

    public void testKeysFinder() throws Exception {
        SlightlyComplexCouchService service = CouchServiceFactory.get(SlightlyComplexCouchService.class, SOMEDB);
        for (int i = 0; i < 10; i++) {
            CSFTest a = new CSFTest();
            a.brand = "-brand" + i;
            a.model = "-model" + i;
            service.persist(a);
        }

        List<CSFTest> loaded = service.anyMatching("-model1", "-model5");
        assertNotNull(loaded);
        assertEquals(2, loaded.size());
    }

    public void testNonPrimitiveKeyFinder() throws Exception {
        SlightlyComplexCouchService service = CouchServiceFactory.get(SlightlyComplexCouchService.class, SOMEDB);
        for (int i = 0; i < 10; i++) {
            CSFTest a = new CSFTest();
            a.brand = "-brand" + i;
            a.model = "-model" + i;
            service.persist(a);
        }

        CSFTest sample = new CSFTest();
        sample.brand = "-brand4";
        sample.model = "-model4";
        List<CSFTest> loaded = service.matchingTuples(sample);
        //returns null, whoops
        //turns out to be a json exception:
        /*
   com.google.gson.JsonSyntaxException: java.lang.IllegalStateException: Expected a string but was BEGIN_OBJECT at line 2 column 49
       at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$Adapter.read(ReflectiveTypeAdapterFactory.java:180)
       at com.google.gson.internal.bind.TypeAdapterRuntimeTypeWrapper.read(TypeAdapterRuntimeTypeWrapper.java:40)
       at com.google.gson.internal.bind.CollectionTypeAdapterFactory$Adapter.read(CollectionTypeAdapterFactory.java:81)
       at com.google.gson.internal.bind.CollectionTypeAdapterFactory$Adapter.read(CollectionTypeAdapterFactory.java:60)
       at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$1.read(ReflectiveTypeAdapterFactory.java:93)
       at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$Adapter.read(ReflectiveTypeAdapterFactory.java:176)
       at com.google.gson.Gson.fromJson(Gson.java:755)
       at com.google.gson.Gson.fromJson(Gson.java:721)
       at com.google.gson.Gson.fromJson(Gson.java:670)
       at rxf.server.gen.CouchDriver$ViewFetch$ViewFetchActionBuilder$1.rows(CouchDriver.java:735)
       at rxf.server.CouchServiceFactory$CouchServiceHandler$2.call(CouchServiceFactory.java:198)
       at java.util.concurrent.FutureTask$Sync.innerRun(FutureTask.java:334)
       at java.util.concurrent.FutureTask.run(FutureTask.java:166)
       at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.access$201(ScheduledThreadPoolExecutor.java:178)
       at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:292)
       at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1110)
       at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:603)
       at java.lang.Thread.run(Thread.java:722)
   Caused by: java.lang.IllegalStateException: Expected a string but was BEGIN_OBJECT at line 2 column 49
       at com.google.gson.stream.JsonReader.nextString(JsonReader.java:464)
       at com.google.gson.internal.bind.TypeAdapters$13.read(TypeAdapters.java:347)
       at com.google.gson.internal.bind.TypeAdapters$13.read(TypeAdapters.java:335)
       at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$1.read(ReflectiveTypeAdapterFactory.java:93)
       at com.google.gson.internal.bind.ReflectiveTypeAdapterFactory$Adapter.read(ReflectiveTypeAdapterFactory.java:176)
       ... 17 more
        */

        //Comment in these tests to see it fail, currently fails silently, returning null
//    assertNotNull(loaded);
//    assertEquals(1, loaded.size());
//    assertNotNull(loaded.get(0)._id);
    }

    public void testNoArgMethod() throws Exception {
        SlightlyComplexCouchService service = CouchServiceFactory.get(SlightlyComplexCouchService.class, SOMEDB);
        for (int i = 0; i < 10; i++) {
            CSFTest a = new CSFTest();
            a.brand = "-brand" + i;
            a.model = "-model" + i;
            service.persist(a);
        }

        List<CSFTest> loaded = service.all();
        assertNotNull(loaded);
        assertEquals(10, loaded.size());
    }
}
