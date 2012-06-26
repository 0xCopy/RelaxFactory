package rxf.server.guice;

import junit.framework.Assert;

import org.junit.Test;

import rxf.server.CouchService;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

public class CouchServiceProviderTest {
  public static class TestModule extends CouchModule {
    protected String getPrefixName() {
      return "test" + System.currentTimeMillis();
    }
    @Override
    protected void configure() {
      super.configure();
      bind(TrivialCouchService.class).toProvider(new TypeLiteral<CouchServiceProvider<TrivialCouchService>>(){});
    }
  }
  
  public interface TrivialCouchService extends CouchService<Object> {
    
  }
  @Test
  public void testCreateTrivialService() {
    Injector i = Guice.createInjector(new TestModule());
    i.getBinding(TrivialCouchService.class);
    TrivialCouchService service = i.getInstance(TrivialCouchService.class);
    Assert.assertNotNull(service);
  }
}
