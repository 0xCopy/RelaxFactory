package rxf.server.guice;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.web.bindery.requestfactory.shared.ServiceLocator;

/**
 * Simple ServiceLocator subtype to request a service instance from Guice. Referenced like this:
 * <pre>
{@literal @}Service(value = MyService.class, locator = InjectingServiceLocator.class)
public interface MyRequest extends RequestContext {
  //...
}</pre>
 * where {@code MyService} is a class with non-static methods matching the calls in {@code MyRequest}.
 * 
 *
 */
public class InjectingServiceLocator implements ServiceLocator {
  @Inject Injector injector;

  public Object getInstance(Class<?> clazz) {
    return injector.getInstance(clazz);
  }

}
