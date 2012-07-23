package rxf.server.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.web.bindery.requestfactory.server.ServiceLayer;
import com.google.web.bindery.requestfactory.server.ServiceLayerDecorator;
import com.google.web.bindery.requestfactory.server.SimpleRequestProcessor;
import rxf.server.GwtRequestFactoryVisitor;

public class RFServiceLayerModule extends AbstractModule {
	@Override
	protected void configure() {
		requireBinding(ServiceLayerDecorator.class);

		requestStaticInjection(this.getClass());
	}

	@Provides
	SimpleRequestProcessor provideSimpleRequestProcessor(
			ServiceLayerDecorator sld) {
		return new SimpleRequestProcessor(ServiceLayer.create(sld));
	}

	@Inject
	static void injectGwtRequestFactoryVisitor(SimpleRequestProcessor srp) {
		GwtRequestFactoryVisitor.SIMPLE_REQUEST_PROCESSOR = srp;
	}
}
