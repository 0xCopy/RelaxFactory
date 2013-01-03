package rxf.server.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import one.xio.AsioVisitor;
import org.junit.*;
import rxf.server.CouchService;
import rxf.server.CouchTx;
import rxf.server.RelaxFactoryServerImpl;
import rxf.server.gen.CouchDriver;
import rxf.server.web.inf.ProtocolMethodDispatch;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static rxf.server.RelaxFactoryServerImpl.getEXECUTOR_SERVICE;

public class CouchServiceProviderTest {
	private static ScheduledExecutorService exec;
	Injector i;
	private String db;

	@BeforeClass
	public static void setUp() throws Exception {
		RelaxFactoryServerImpl.setDEBUG_SENDJSON(true);
		RelaxFactoryServerImpl.setKillswitch(false);
		exec = Executors.newScheduledThreadPool(2);
		exec.submit(new Runnable() {
			public void run() {
				AsioVisitor topLevel = new ProtocolMethodDispatch();
				try {
					RelaxFactoryServerImpl.init(topLevel/*, 1000*/);

				} catch (Exception e) {
					Assert.fail();
				}
			}
		});
		getEXECUTOR_SERVICE().schedule(new Runnable() {
			public void run() {
				Assert.fail();
			}
		}, 5, TimeUnit.SECONDS);
	}

	@AfterClass
	public static void tearDown() throws Exception {
		try {
			RelaxFactoryServerImpl.setKillswitch(true);
			RelaxFactoryServerImpl.getSelector().close();
			exec.shutdown();
		} catch (Exception ignore) {
		}
	}

	@Before
	public void before() {
		i = Guice.createInjector(new TestModule());
		db = i.getInstance(Key.get(String.class, Names
				.named(CouchModuleBuilder.NAMESPACE)))
				+ GuiceTest.class.getSimpleName().toLowerCase();
		CouchDriver.DbCreate.$().db(db).to().fire().tx();

	}

	@After
	public void after() {
		CouchDriver.DbDelete.$().db(db).to().fire().tx();
	}

	@Test
	public void testMakeSureServiceWorks() {
		SimpleCouchService service = i.getInstance(SimpleCouchService.class);
		assertNotNull(service);

		GuiceTest instance = new GuiceTest();
		instance.name = "blah";
		CouchTx tx = service.persist(instance);
		assertNotNull(tx);

		GuiceTest retrieve = service.find(tx.id());
		assertNotNull(retrieve);
		assertEquals(tx.id(), retrieve._id);
		assertEquals("blah", retrieve.name);
	}

	public interface SimpleCouchService extends CouchService<GuiceTest> {

	}

	public static class TestModule extends AbstractModule {
		@Override
		protected void configure() {
			install(new CouchModuleBuilder("test" + System.currentTimeMillis()
					+ "_").withEntity(GuiceTest.class).withService(
					SimpleCouchService.class).build());
		}
	}

	public static class GuiceTest {
		String _id;
		String name;
	}
}
