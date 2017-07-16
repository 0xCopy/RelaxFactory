package rxf.couch.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import rxf.couch.service.CouchService;
import rxf.couch.service.CouchServiceFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Module generateDriver to facilitate declaring {@link CouchService} types and the entities they will manage. When
 * created, will wire up the {@link CouchServiceFactory} to create requested service instances.
 */
public class CouchModuleBuilder {
  public static final String NAMESPACE = "couch_namespace";
  private final String ns;
  private final List<Class<?>> entities = new ArrayList<Class<?>>();
  private final List<Class<? extends CouchService<?>>> services =
      new ArrayList<Class<? extends CouchService<?>>>();

  /**
   * Creates a new generateDriver. No namespace has been declared, a String constant must be bound to {@literal @Named(}
   * {@value #NAMESPACE}) to allow this to correctly create service instances.
   */
  public CouchModuleBuilder() {
    this(null);
  }

  /**
   * Creates a new generateDriver, with the given namespace for all databases.
   * 
   * @param namespace
   */
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

      protected void configure() {
        if (ns != null) {
          bindConstant().annotatedWith(Names.named(NAMESPACE)).to(ns);
        } else {
          requireBinding(Key.get(String.class, Names.named(NAMESPACE)));
        }

        // TODO setup some init for the entities so their DBs are known to exist before needed

        for (Class<? extends CouchService<?>> service : services) {
          bindService(service);
        }
      }

      private <S extends CouchService<?>> void bindService(Class<S> serviceType) {
        assert hasAnEntity(serviceType);
        CouchServiceProvider<S> provider = new CouchServiceProvider<S>(serviceType);
        requestInjection(provider);
        bind(serviceType).toProvider(provider).asEagerSingleton();
      }

      /**
       * Helps validate that the current setup makes sense
       * 
       * @param serviceType
       * @return
       */
      private boolean hasAnEntity(Class<? extends CouchService<?>> serviceType) {
        return true;
      }
    };
  }
}
