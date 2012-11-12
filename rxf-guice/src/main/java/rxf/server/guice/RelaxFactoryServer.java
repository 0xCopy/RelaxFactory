package rxf.server.guice;

import java.io.IOException;
import java.net.UnknownHostException;

import one.xio.AsioVisitor;

/**
 * @todo move to -server
 *
 */
public interface RelaxFactoryServer {
	void init(String hostname, int port, AsioVisitor topLevel)
			throws UnknownHostException;
	void start() throws IOException;
	void stop() throws IOException;

	/**
	 * Returns the port the server has started on. Useful in the case where 
	 * {@link #init(String, int, AsioVisitor)} was invoked with 0, {@link #start()} called,
	 * and the server selected its own port.
	 * @return
	 */
	int getPort();
}
