package rxf.server.guice;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.web.bindery.requestfactory.server.ServiceLayerDecorator;
import com.google.web.bindery.requestfactory.shared.Locator;
import com.google.web.bindery.requestfactory.shared.ServiceLocator;

/**
 * Simple SLD subclass to delegate locator/servicelocator creation to guice
 *
 * @author colin
 */
public class InjectingServiceLayerDecorator extends ServiceLayerDecorator {
	@Inject
	Injector injector;

	@Override
	public <T extends Locator<?, ?>> T createLocator(Class<T> clazz) {
		return injector.getInstance(clazz);
	}

	@Override
	public <T extends ServiceLocator> T createServiceLocator(Class<T> clazz) {
		return injector.getInstance(clazz);
	}
}
