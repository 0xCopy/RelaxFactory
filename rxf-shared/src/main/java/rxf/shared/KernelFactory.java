package rxf.shared;

import com.google.web.bindery.requestfactory.shared.RequestFactory;
import rxf.shared.req.Kernel;

public interface KernelFactory extends RequestFactory {
  Kernel api();
}
