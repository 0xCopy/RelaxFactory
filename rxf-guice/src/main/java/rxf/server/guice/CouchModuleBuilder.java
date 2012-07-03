package rxf.server.guice;

import java.util.ArrayList;
import java.util.List;

import rxf.server.CouchService;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;

public class CouchModuleBuilder {
  public static final String NAMESPACE = "couch_namespace";
  private final String ns;
  private final List<Class<?>> entities = new ArrayList<Class<?>>();
  private final List<Class<? extends CouchService<?>>> services = new ArrayList<Class<? extends CouchService<?>>>();
  
  public CouchModuleBuilder(String namespace) {
    this.ns = namespace;
  }
  public CouchModuleBuilder withEntity(Class<?> entityType) {
    entities.add(entityType);
    return this;
  }
  public CouchModuleBuilder withService(Class<? extends CouchService<?>> serviceType) {
    services.add(serviceType);
    return this;
  }
  
  public Module build() {
    return new AbstractModule() {
      @Override
      protected void configure() {
        if (ns != null) {
          bindConstant().annotatedWith(Names.named(NAMESPACE)).to(ns);
        } else {
          requireBinding(Key.get(String.class, Names.named(NAMESPACE)));
        }
        
        //TODO setup some init for the entities so their DBs are known to exist before needed
        
        for (Class<? extends CouchService<?>> service : services) {
          bindService(service);
        }
      }

      private <S extends CouchService<?>> void bindService(Class<S> serviceType) {
        assert hasAnEntity(serviceType);
        CouchServiceProvider<S> provider = new CouchServiceProvider<S>(serviceType);
        requestInjection(provider);
        bind(serviceType).toProvider(provider);
      }
      /**
       * Helps validate that the current setup makes sense
       * @param serviceType
       * @return
       */
      private boolean hasAnEntity(Class<? extends CouchService<?>> serviceType) {
        return true;
      }
    };
  }
}
