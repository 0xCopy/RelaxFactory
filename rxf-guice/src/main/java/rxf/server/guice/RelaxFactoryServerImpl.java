package rxf.server.guice;

import static java.nio.channels.SelectionKey.OP_ACCEPT;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ServerSocketChannel;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;

/**
 * @todo move to -server
 *
 */
public class RelaxFactoryServerImpl implements RelaxFactoryServer {

	private AsioVisitor topLevel;
	private int port;
	private InetAddress hostname;

	private ServerSocketChannel serverSocketChannel;
	@Override
	public void init(String hostname, int port, AsioVisitor topLevel)
			throws UnknownHostException {
		assert topLevel == null && serverSocketChannel == null : "Can't call init twice";
		this.topLevel = topLevel;
		this.port = port;
		this.hostname = InetAddress.getByName(hostname);
	}

	@Override
	public void start() throws IOException {
		assert serverSocketChannel == null : "Can't start already started server";
		serverSocketChannel = ServerSocketChannel.open();
		InetSocketAddress addr = new InetSocketAddress(hostname, port);
		serverSocketChannel.socket().bind(addr);
		port = serverSocketChannel.socket().getLocalPort();
		System.out.println(hostname.getHostAddress() + ":" + port);
		serverSocketChannel.configureBlocking(false);

		HttpMethod.enqueue(serverSocketChannel, OP_ACCEPT, topLevel);
		HttpMethod.init(topLevel);
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public void stop() throws IOException {
		HttpMethod.killswitch = true;
		HttpMethod.getSelector().close();
		serverSocketChannel.close();
	}

}
