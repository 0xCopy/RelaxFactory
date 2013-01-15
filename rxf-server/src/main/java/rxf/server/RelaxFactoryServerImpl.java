package rxf.server;

import one.xio.AsioVisitor;
import one.xio.HttpMethod;
import one.xio.HttpStatus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.StrictMath.min;
import static java.nio.channels.SelectionKey.OP_ACCEPT;

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 1/2/13
 * Time: 8:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class RelaxFactoryServerImpl implements RelaxFactoryServer {

	public static Charset UTF8 = Charset.forName("UTF8");
	//    private static int DEFAULT_EXP = 0;
	public static Thread selectorThread;
	public static boolean killswitch = false;
	public static int port = 8080;
	static Charset charset = UTF8;
	public static CharsetDecoder decoder = charset.newDecoder();
	private static Selector selector;
	private static ConcurrentLinkedQueue<Object[]> q = new ConcurrentLinkedQueue<Object[]>();
	private static Random RANDOM = new Random();
	private AsioVisitor topLevel;
	private InetAddress hostname;

	private ServerSocketChannel serverSocketChannel;

	public static Selector getSelector() {
		return selector;
	}

	static public void setSelector(Selector selector) {
		selector = selector;
	}

	public static Object[] toArray(Object... t) {
		return t;
	}

	/**
	 * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
	 *
	 * @param channel the socketchanel
	 * @param op      int ChannelSelector.operator
	 * @param s       the payload: grammar {enum,data1,data..n}
	 * @throws java.nio.channels.ClosedChannelException
	 */
	public static void enqueue(SelectableChannel channel, int op, Object... s)
			throws ClosedChannelException {
		//		boolean add = q.add(toArray(channel, op, s));
		//		Selector selector1 = getSelector();
		//		if (null != selector1)
		//			selector1.wakeup();
		HttpMethod.enqueue(channel, op, s);
	}

	public static String wheresWaldo(int... depth) {
		int d = depth.length > 0 ? depth[0] : 2;
		Throwable throwable = new Throwable();
		Throwable throwable1 = throwable.fillInStackTrace();
		StackTraceElement[] stackTrace = throwable1.getStackTrace();
		String ret = "";
		for (int i = 2, end = min(stackTrace.length - 1, d); i <= end; i++) {
			StackTraceElement stackTraceElement = stackTrace[i];
			ret += "\tat " + stackTraceElement.getClassName() + "."
					+ stackTraceElement.getMethodName() + "("
					+ stackTraceElement.getFileName() + ":"
					+ stackTraceElement.getLineNumber() + ")\n";

		}
		return ret;
	}

	public static void response(SelectionKey key, HttpStatus httpStatus)
			throws IOException {
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			ByteBuffer buffer = ByteBuffer.allocateDirect(channel.socket()
					.getSendBufferSize());
			CharBuffer charBuffer = (CharBuffer) buffer.asCharBuffer().append(
					"HTTP/1.1 ").append(httpStatus.name().substring(1)).append(
					' ').append(httpStatus.caption).append("\r\n").flip();
			ByteBuffer out = UTF8.encode(charBuffer);
			((SocketChannel) key.channel()).write(out);
		} catch (Exception ignored) {
		}

	}

	public static void init(AsioVisitor protocoldecoder, String... a)
			throws IOException {

		/*	setSelector(Selector.open());
			selectorThread = Thread.currentThread();

			synchronized (a) {
				int kick = 0;
				int maxKick = 10;
				long timeoutMax = 1024, timeout = 1;

				while (!killswitch) {
					while (!q.isEmpty()) {
						Object[] s = q.remove();
						SelectableChannel x = (SelectableChannel) s[0];
						Selector sel = getSelector();
						Integer op = (Integer) s[1];
						Object att = s[2];
						try {
							x.register(sel, op, att);
						} catch (Throwable e) {

						}
					}
					int select = selector.select(timeout);

					timeout = 0 == select ? min(timeout << 1, timeoutMax) : 1;
					if (0 == select)
						continue;
					Set<SelectionKey> keys = selector.selectedKeys();

					for (Iterator<SelectionKey> i = keys.iterator(); i.hasNext();) {
						SelectionKey key = i.next();
						i.remove();

						if (key.isValid()) {
							SelectableChannel channel = key.channel();
							try {
								AsioVisitor m = inferAsioVisitor(protocoldecoder,
										key);

								if (key.isValid() && key.isWritable()) {
									if (!((SocketChannel) channel).socket()
											.isOutputShutdown()) {
										m.onWrite(key);
									} else {
										key.cancel();
									}
								}
								if (key.isValid() && key.isReadable()) {
									if (!((SocketChannel) channel).socket()
											.isInputShutdown()) {
										m.onRead(key);
									} else {
										key.cancel();
									}
								}
								if (key.isValid() && key.isAcceptable()) {
									m.onAccept(key);
								}
								if (key.isValid() && key.isConnectable()) {
									m.onConnect(key);
								}
							} catch (Throwable e) {
								Object attachment = key.attachment();
								if (attachment instanceof Object[]) {
									Object[] objects = (Object[]) attachment;
									System.err.println("BadHandler: "
											+ java.util.Arrays
													.deepToString(objects));

								} else
									System.err.println("BadHandler: "
											+ String.valueOf(attachment));

								if (AsioVisitor.$DBG) {
									AsioVisitor asioVisitor = inferAsioVisitor(
											protocoldecoder, key);
									if (asioVisitor instanceof Impl) {
										Impl visitor = (Impl) asioVisitor;
										if (AsioVisitor.$origins
												.containsKey(visitor)) {
											String s = AsioVisitor.$origins
													.get(visitor);
											System.err.println("origin " + s);
										}
									}
								}
								e.printStackTrace();
								key.attach(null);
								channel.close();
							}
						}
					}
				}
			}*/
		HttpMethod.init(protocoldecoder, a);
	}

	static AsioVisitor inferAsioVisitor(AsioVisitor default$, SelectionKey key) {
		Object attachment = key.attachment();
		AsioVisitor m;
		if (null == attachment)
			m = default$;
		if (attachment instanceof Object[]) {
			for (Object o : ((Object[]) attachment)) {
				attachment = o;
				break;
			}
		}
		if (attachment instanceof Iterable) {
			Iterable iterable = (Iterable) attachment;
			for (Object o : iterable) {
				attachment = o;
				break;
			}
		}
		if (attachment instanceof AsioVisitor) {
			m = (AsioVisitor) attachment;

		} else {

			m = default$;
		}
		return m;
	}

	public static void setPort(int port) {
		RelaxFactoryServerImpl.port = port;
	}

	@Override
	public void init(String hostname, int port, AsioVisitor topLevel)
			throws UnknownHostException {
		assert topLevel == null && serverSocketChannel == null : "Can't call init twice";
		this.topLevel = topLevel;
		this.setPort(port);
		this.hostname = InetAddress.getByName(hostname);
	}

	@Override
	public void start() throws IOException {
		assert serverSocketChannel == null : "Can't start already started server";
		serverSocketChannel = ServerSocketChannel.open();
		InetSocketAddress addr = new InetSocketAddress(hostname, getPort());
		serverSocketChannel.socket().bind(addr);
		setPort(serverSocketChannel.socket().getLocalPort());
		System.out.println(hostname.getHostAddress() + ":" + getPort());
		serverSocketChannel.configureBlocking(false);

		enqueue(serverSocketChannel, OP_ACCEPT, topLevel);
		init(topLevel);
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public void stop() throws IOException {
		killswitch = true;
		getSelector().close();
		serverSocketChannel.close();
	}

}
