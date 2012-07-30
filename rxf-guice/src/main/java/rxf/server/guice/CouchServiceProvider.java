package rxf.server.guice;

import java.util.concurrent.ExecutionException;

import rxf.server.CouchService;
import rxf.server.CouchServiceFactory;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

public class CouchServiceProvider<T extends CouchService<?>>
		implements
			Provider<T> {
	@Inject
	@Named(CouchModuleBuilder.NAMESPACE)
	private String namespace;
	private final Class<T> type;
	public CouchServiceProvider(Class<T> serviceType) {
		this.type = serviceType;
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
