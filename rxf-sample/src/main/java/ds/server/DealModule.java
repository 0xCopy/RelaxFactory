package ds.server;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import com.google.web.bindery.requestfactory.server.ServiceLayerDecorator;
import ds.model.*;
import ds.server.DealSiteInit.FragMent;
import rxf.server.CouchNamespace;
import rxf.server.GwtRequestFactoryVisitor;
import rxf.server.HttpProxyImpl;
import rxf.server.guice.CouchModuleBuilder;
import rxf.server.guice.InjectingServiceLayerDecorator;
import rxf.server.guice.RFServiceLayerModule;
import rxf.server.guice.RxfModule;
import rxf.server.web.inf.ContentRootCacheImpl;
import rxf.server.web.inf.ContentRootImpl;
import rxf.server.web.inf.ContentRootNoCacheImpl;

import java.util.regex.Pattern;

import static rxf.server.web.inf.ContentRootCacheImpl.CACHE_PATTERN;
import static rxf.server.web.inf.ContentRootNoCacheImpl.NOCACHE_PATTERN;

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
        bind(ServiceLayerDecorator.class)
                .to(InjectingServiceLayerDecorator.class);


        // server setup - could/should be broken out into its own module type
        install(new RxfModule() {
            @Override
            protected void configureHttpVisitors() {

                post(Pattern.quote("/DealSite/rpc")).with(SampleRemoteServiceImpl.class);

                get(".*_escaped_fragment_=([^&]+)").with(FragMent.class);

                get("^/rxf.server.Auth/.*").with(OAuthHandler.class /*(fragment)*/);
                post("^/rxf.server.Auth/.*").with(OAuthHandler.class /*(fragment)*/);

                post("^/gwtRequest").with(GwtRequestFactoryVisitor.class);

                get("^/i(/.*)$").with(HttpProxyImpl.class);
                get(CACHE_PATTERN.pattern()).with(ContentRootCacheImpl.class);
                get(NOCACHE_PATTERN.pattern()).with(ContentRootNoCacheImpl.class);
                get(".*").with(ContentRootImpl.class);
            }
        });
        bindConstant().annotatedWith(Names.named("port")).to(8080);
        bindConstant().annotatedWith(Names.named("hostname")).to("0.0.0.0");

    }

}
