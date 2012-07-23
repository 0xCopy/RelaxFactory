package ds.server;

import com.google.inject.AbstractModule;
import com.google.web.bindery.requestfactory.server.ServiceLayerDecorator;
import ds.model.*;
import rxf.server.CouchNamespace;
import rxf.server.guice.CouchModuleBuilder;
import rxf.server.guice.InjectingServiceLayerDecorator;
import rxf.server.guice.RFServiceLayerModule;

public class DealModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new CouchModuleBuilder(CouchNamespace.COUCH_DEFAULT_ORGNAME)
                .withEntity(Deal.class)
                .withEntity(Npo.class)
                .withEntity(Vendor.class)
                .withService(DealService.class)
                .withService(NpoService.class)
                .withService(VendorService.class)
                .build());
        install(new RFServiceLayerModule());
        bind(ServiceLayerDecorator.class).to(InjectingServiceLayerDecorator.class);
    }

}
