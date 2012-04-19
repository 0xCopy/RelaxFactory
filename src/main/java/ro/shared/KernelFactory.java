package ro.shared;

import com.google.web.bindery.requestfactory.shared.RequestFactory;
import ro.shared.req.Kernel;

public interface KernelFactory extends RequestFactory {
  Kernel api();

}
