package rxf.server.guice;

import java.util.concurrent.ExecutionException;

import rxf.server.CouchService;
import rxf.server.CouchServiceFactory;

import com.google.inject.Inject;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderWithExtensionVisitor;

public class CouchServiceProvider<T extends CouchService<?>> implements ProviderWithExtensionVisitor<T> {
  private String namespace;
  private Class<T> type;
  @Inject
  public CouchServiceProvider(@Named("namespace") String namespace) {
    this.namespace = namespace;
  }
  
  public <B, V> V acceptExtensionVisitor(BindingTargetVisitor<B, V> visitor, ProviderInstanceBinding<? extends B> binding) {
    // "In practice, the 'B' type will always be a supertype of 'T'."
    TypeLiteral<?> k = binding.getKey().getTypeLiteral();
    this.type = (Class<T>) k.getRawType().asSubclass(CouchService.class);
    
    return visitor.visit(binding);
  }
  
  public T get() {
    try {
      return CouchServiceFactory.get(type, namespace);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (ExecutionException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

}
