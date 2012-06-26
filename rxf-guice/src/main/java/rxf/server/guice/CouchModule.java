package rxf.server.guice;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public abstract class CouchModule extends AbstractModule {
  
  protected abstract String getPrefixName();

  @Override
  protected void configure() {
    bind(String.class).annotatedWith(Names.named("namespace")).toInstance(getPrefixName());

  }

}
