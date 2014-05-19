package rxf.rpc;

import one.xio.AsioVisitor;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 1/2/13
 * Time: 8:12 PM
 * To change this template use File | Settings | File Templates.
 */
public interface RelaxFactoryServer {

  void init(String hostname, int port, AsioVisitor topLevel) throws UnknownHostException;

  void start() throws IOException;

  void stop() throws IOException;

  boolean isRunning();

  /**
   * Returns the port the couch has started on. Useful in the case where
   * {@link #init(String, int, one.xio.AsioVisitor)} was invoked with 0, {@link #start()} called,
   * and the couch selected its own port.
   *
   * @return
   */
  int getPort();

  InheritableThreadLocal<RelaxFactoryServer> rxfTl = new InheritableThreadLocal();

  class App {
    public static RelaxFactoryServer get() {
      RelaxFactoryServer relaxFactoryServer = rxfTl.get();
      if (null == relaxFactoryServer) {
        relaxFactoryServer = new RelaxFactoryServerImpl();
        rxfTl.set(relaxFactoryServer);
      }
      return relaxFactoryServer;
    }
  }
}
