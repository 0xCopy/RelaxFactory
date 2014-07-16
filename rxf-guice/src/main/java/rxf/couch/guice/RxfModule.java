package rxf.couch.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.internal.UniqueAnnotations;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import rxf.rpc.RelaxFactoryServer;
import rxf.rpc.RelaxFactoryServerImpl;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Guice bindings to configure RelaxFactory with injection for each visitor impl. Binds a singleton instance of
 * {@link rxf.rpc.RelaxFactoryServer} which can then be manipulated elsewhere.
 */
public class RxfModule extends AbstractModule {
  private Integer port;
  private String hostname;

  private List<VisitorDef> defs = new ArrayList<VisitorDef>();

  // ugly practice, consider a private module instead
  private AsioVisitor topLevel;

  protected final void configure() {
    if (port != null) {
      bindConstant().annotatedWith(Names.named("port")).to(port);
    } else {
      requireBinding(Key.get(Integer.class, Names.named("port")));
    }
    if (hostname != null) {
      bindConstant().annotatedWith(Names.named("hostname")).to(hostname);
    } else {
      requireBinding(Key.get(String.class, Names.named("hostname")));
    }

    configureHttpVisitors();

    for (VisitorDef def : defs) {
      bind(VisitorDef.class).annotatedWith(UniqueAnnotations.create()).toInstance(def);
    }

    topLevel = new InjectedTopLevelVisitor();
    requestInjection(topLevel);
  }

  @Provides
  @Singleton
  protected RelaxFactoryServer provideServer(@Named("port") Integer port,
      @Named("hostname") String hostname) throws UnknownHostException {
    RelaxFactoryServer server = new RelaxFactoryServerImpl();
    server.init(hostname, port, topLevel);
    return server;
  }

  protected void configureHttpVisitors() {

  }

  protected VisitorKeyBindingBuilder handle(final HttpMethod verb, final String regex) {
    return new VisitorKeyBindingBuilder() {

      public void with(Key<? extends AsioVisitor> implKey) {
        defs.add(new VisitorDef(verb, regex, implKey));
      }

      public void with(Class<? extends AsioVisitor> implKey) {
        defs.add(new VisitorDef(verb, regex, Key.get(implKey)));
      }

      public void with(AsioVisitor impl) {
        Key<AsioVisitor> key = Key.get(AsioVisitor.class, UniqueAnnotations.create());
        bind(key).toInstance(impl);
        defs.add(new VisitorDef(verb, regex, key));
      }
    };
  }

  protected VisitorKeyBindingBuilder post(String regex) {
    return handle(HttpMethod.POST, regex);
  }

  protected VisitorKeyBindingBuilder get(String regex) {
    return handle(HttpMethod.GET, regex);
  }

  protected VisitorKeyBindingBuilder put(String regex) {
    return handle(HttpMethod.PUT, regex);
  }

  protected VisitorKeyBindingBuilder delete(String regex) {
    return handle(HttpMethod.DELETE, regex);
  }

  public interface VisitorKeyBindingBuilder {
    void with(Class<? extends AsioVisitor> implKey);

    void with(Key<? extends AsioVisitor> implKey);

    void with(AsioVisitor impl);
  }

}
