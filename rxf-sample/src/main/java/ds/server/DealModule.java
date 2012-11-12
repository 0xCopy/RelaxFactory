package ds.server;

import java.util.regex.Pattern;

import rxf.server.CouchNamespace;
import rxf.server.GwtRequestFactoryVisitor;
import rxf.server.HttpProxyImpl;
import rxf.server.guice.CouchModuleBuilder;
import rxf.server.guice.InjectingServiceLayerDecorator;
import rxf.server.guice.RFServiceLayerModule;
import rxf.server.guice.RxfModule;
import rxf.server.web.inf.ContentRootImpl;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.google.web.bindery.requestfactory.server.ServiceLayerDecorator;

import ds.model.Deal;
import ds.model.DealService;
import ds.model.Npo;
import ds.model.NpoService;
import ds.model.Vendor;
import ds.model.VendorService;
import ds.server.DealSiteInit.FragMent;

public class DealModule extends AbstractModule {

	@Override
	protected void configure() {


		//Data wiring
		install(new CouchModuleBuilder(CouchNamespace.COUCH_DEFAULT_ORGNAME)
			.withEntity(Deal.class)
			.withEntity(Npo.class)
			.withEntity(Vendor.class)
			.withService(DealService.class)
			.withService(NpoService.class)
			.withService(VendorService.class)
			.build());

		// basic RF wiring
		install(new RFServiceLayerModule());
		bind(ServiceLayerDecorator.class).to(InjectingServiceLayerDecorator.class);


		// server setup - could/should be broken out into its own module type
		install(new RxfModule(){
			@Override
			protected void configureHttpVisitors() {

				post(Pattern.quote("/DealSite/rpc")).with(SampleRemoteServiceImpl.class);

				get(".*_escaped_fragment_=([^&]+)").with(FragMent.class);

				get("^/rxf.server.Auth/.*").with(OAuthHandler.class /*(fragment)*/);
				post("^/rxf.server.Auth/.*").with(OAuthHandler.class /*(fragment)*/);

				post("^/gwtRequest").with(GwtRequestFactoryVisitor.class);
				
				get("^/i(/.*)$").with(HttpProxyImpl.class);

				get(".*").with(ContentRootImpl.class);
			}
		});
		bindConstant().annotatedWith(Names.named("port")).to(8080);
		bindConstant().annotatedWith(Names.named("hostname")).to("0.0.0.0");
		
	}

}
