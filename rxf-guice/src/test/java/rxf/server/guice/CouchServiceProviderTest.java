package rxf.server.guice;

import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import rxf.server.BlobAntiPatternObject;
import rxf.server.CouchService;
import rxf.server.CouchTx;
import rxf.server.gen.CouchDriver;
import rxf.server.web.inf.ProtocolMethodDispatch;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

public class CouchServiceProviderTest {
  private static ScheduledExecutorService exec;
  @BeforeClass
  public static void setUp() throws Exception {
    BlobAntiPatternObject.DEBUG_SENDJSON = true;
    HttpMethod.killswitch = false;
    exec = Executors.newScheduledThreadPool(2);
    exec.submit(new Runnable() {
      public void run() {
        AsioVisitor topLevel = new ProtocolMethodDispatch();
        try {
          HttpMethod.init(new String[]{}, topLevel, 1000);

        } catch (Exception e) {
          Assert.fail();
        }
      }
    });
    EXECUTOR_SERVICE.schedule(new Runnable() {
      public void run() {
        Assert.fail();
      }
    }, 5, TimeUnit.SECONDS);
  }

  @AfterClass
  public static void tearDown() throws Exception {
    try {
      HttpMethod.killswitch = true;
      HttpMethod.getSelector().close();
      exec.shutdown();
    } catch (Exception ignore) {
    }
  }
  
  Injector i;
  private String db;
  @Before
  public void before() {
    i = Guice.createInjector(new TestModule());
    db = i.getInstance(Key.get(String.class, Names.named(CouchModuleBuilder.NAMESPACE))) + GuiceTest.class.getSimpleName().toLowerCase();
    CouchDriver.DbCreate.$().db(db).to().fire().tx();
    
  }
  @After
  public void after() {
    CouchDriver.DbDelete.$().db(db).to().fire().tx();
  }
  public static class TestModule extends AbstractModule {
    @Override
    protected void configure() {
      install(new CouchModuleBuilder("test" + System.currentTimeMillis() + "_")
      .withEntity(GuiceTest.class)
      .withService(SimpleCouchService.class)
      .build());
    }
  }

  public interface SimpleCouchService extends CouchService<GuiceTest> {

  }
  public static class GuiceTest {
    String _rev, _id;
    String name;
  }

  @Test
  public void testMakeSureServiceWorks() {
    SimpleCouchService service = i.getInstance(SimpleCouchService.class);
    Assert.assertNotNull(service);

    GuiceTest instance = new GuiceTest();
    instance.name = "blah";
    CouchTx tx = service.persist(instance);
    Assert.assertNotNull(tx);
    
    GuiceTest retrieve = service.find(tx.id());
    Assert.assertNotNull(retrieve);
    Assert.assertEquals(tx.id(), retrieve._id);
    Assert.assertEquals("blah", retrieve.name);
  }
}
