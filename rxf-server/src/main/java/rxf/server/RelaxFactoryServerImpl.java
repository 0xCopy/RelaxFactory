package rxf.server;

import one.xio.AsioVisitor;
import one.xio.HttpStatus;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.StrictMath.min;
import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static one.xio.AsioVisitor.Impl;

/**
 * Created with IntelliJ IDEA.
 * User: jim
 * Date: 1/2/13
 * Time: 8:12 PM
 * To change this template use File | Settings | File Templates.
 */
public class RelaxFactoryServerImpl implements RelaxFactoryServer {
	private boolean DEBUG_SENDJSON = System.getenv().containsKey(
			"DEBUG_SENDJSON");
	private InetAddress LOOPBACK;
	private int receiveBufferSize;
	private int sendBufferSize;
	private InetSocketAddress COUCHADDR;
	private ScheduledExecutorService EXECUTOR_SERVICE = Executors
			.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 3);
	private Random RANDOM = new Random();
	public static Charset UTF8 = Charset.forName("UTF8");
	//    private static int DEFAULT_EXP = 0;
	private Thread selectorThread;
	private boolean killswitch = false;
	private int port = 8080;
	private Selector selector;
	private ConcurrentLinkedQueue<Object[]> q = new ConcurrentLinkedQueue<Object[]>();

	private AsioVisitor topLevel;
	private InetAddress hostname;

	private ServerSocketChannel serverSocketChannel;

	public static Selector getSelector() {
		return ((RelaxFactoryServerImpl) App.get()).selector;
	}

	static public void setSelector(Selector selector) {
		((RelaxFactoryServerImpl) App.get()).selector = selector;
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
		boolean add = ((RelaxFactoryServerImpl) App.get()).q.add(toArray(
				channel, op, s));
		Selector selector1 = getSelector();
		if (null != selector1)
			selector1.wakeup();
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

		setSelector(Selector.open());
		((RelaxFactoryServerImpl) App.get()).selectorThread = Thread
				.currentThread();

		synchronized (a) {
			int kick = 0;
			int maxKick = 10;
			long timeoutMax = 1024, timeout = 1;

			while (!((RelaxFactoryServerImpl) App.get()).killswitch) {
				while (!((RelaxFactoryServerImpl) App.get()).q.isEmpty()) {
					Object[] s = ((RelaxFactoryServerImpl) App.get()).q
							.remove();
					SelectableChannel x = (SelectableChannel) s[0];
					Selector sel = getSelector();
					Integer op = (Integer) s[1];
					Object att = s[2];
					try {
						x.register(sel, op, att);
					} catch (Throwable e) {

					}
				}
				int select = ((RelaxFactoryServerImpl) App.get()).selector
						.select(timeout);

				timeout = 0 == select ? min(timeout << 1, timeoutMax) : 1;
				if (0 == select)
					continue;
				Set<SelectionKey> keys = ((RelaxFactoryServerImpl) App.get()).selector
						.selectedKeys();

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
		}
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
		((RelaxFactoryServerImpl) App.get()).port = port;
	}

	public static Charset getUTF8() {
		return UTF8;
	}

	public static void setUTF8(Charset UTF8) {
		((RelaxFactoryServerImpl) App.get()).UTF8 = UTF8;
	}

	public static Thread getSelectorThread() {
		return ((RelaxFactoryServerImpl) App.get()).selectorThread;
	}

	public static void setSelectorThread(Thread selectorThread) {
		((RelaxFactoryServerImpl) App.get()).selectorThread = selectorThread;
	}

	public static boolean isKillswitch() {
		return ((RelaxFactoryServerImpl) App.get()).killswitch;
	}

	public static void setKillswitch(boolean killswitch) {
		((RelaxFactoryServerImpl) App.get()).killswitch = killswitch;
	}

	public static ConcurrentLinkedQueue<Object[]> getQ() {
		return ((RelaxFactoryServerImpl) App.get()).q;
	}

	public static void setQ(ConcurrentLinkedQueue<Object[]> q) {
		((RelaxFactoryServerImpl) App.get()).q = q;
	}

	public static Random getRANDOM() {
		return ((RelaxFactoryServerImpl) App.get()).RANDOM;
	}

	public static void setRANDOM(Random RANDOM) {
		((RelaxFactoryServerImpl) App.get()).RANDOM = RANDOM;
	}

	public static boolean isDEBUG_SENDJSON() {
		return ((RelaxFactoryServerImpl) App.get()).DEBUG_SENDJSON;
	}

	public static void setDEBUG_SENDJSON(boolean DEBUG_SENDJSON) {
		((RelaxFactoryServerImpl) App.get()).DEBUG_SENDJSON = DEBUG_SENDJSON;
	}

	public static InetAddress getLOOPBACK() {
		return ((RelaxFactoryServerImpl) App.get()).LOOPBACK;
	}

	public static void setLOOPBACK(InetAddress LOOPBACK) {
		((RelaxFactoryServerImpl) App.get()).LOOPBACK = LOOPBACK;
	}

	public static InetSocketAddress getCOUCHADDR() {
		return ((RelaxFactoryServerImpl) App.get()).COUCHADDR;
	}

	public static void setCOUCHADDR(InetSocketAddress COUCHADDR) {
		((RelaxFactoryServerImpl) App.get()).COUCHADDR = COUCHADDR;
	}

	public static ScheduledExecutorService getEXECUTOR_SERVICE() {
		return ((RelaxFactoryServerImpl) App.get()).EXECUTOR_SERVICE;
	}

	public static void setEXECUTOR_SERVICE(
			ScheduledExecutorService EXECUTOR_SERVICE) {
		((RelaxFactoryServerImpl) App.get()).EXECUTOR_SERVICE = EXECUTOR_SERVICE;
	}

	public static int getReceiveBufferSize() {
		return ((RelaxFactoryServerImpl) App.get()).receiveBufferSize;
	}

	public static void setReceiveBufferSize(int receiveBufferSize) {
		((RelaxFactoryServerImpl) App.get()).receiveBufferSize = receiveBufferSize;
	}

	public static int getSendBufferSize() {
		return ((RelaxFactoryServerImpl) App.get()).sendBufferSize;
	}

	public static void setSendBufferSize(int sendBufferSize) {
		((RelaxFactoryServerImpl) App.get()).sendBufferSize = sendBufferSize;
	}

	@Override
	public void init(String hostname, int port, AsioVisitor topLevel)
			throws UnknownHostException {
		assert topLevel == null && serverSocketChannel == null : "Can't call init twice";
		this.topLevel = topLevel;
		this.setPort(port);
		this.hostname = InetAddress.getByName(hostname);
	}
	static {
		try {
			try {
				setLOOPBACK((InetAddress) InetAddress.class.getMethod(
						"getLoopBackAddress").invoke(null));
			} catch (NoSuchMethodException e) {
				setLOOPBACK(InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
				System.err.println("java 6 LOOPBACK detected");
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		setCOUCHADDR(new InetSocketAddress(getLOOPBACK(), 5984));
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

	public AsioVisitor getTopLevel() {
		return topLevel;
	}

	public void setTopLevel(AsioVisitor topLevel) {
		this.topLevel = topLevel;
	}

	public InetAddress getHostname() {
		return hostname;
	}

	public void setHostname(InetAddress hostname) {
		this.hostname = hostname;
	}

	public ServerSocketChannel getServerSocketChannel() {
		return serverSocketChannel;
	}

	public void setServerSocketChannel(ServerSocketChannel serverSocketChannel) {
		this.serverSocketChannel = serverSocketChannel;
	}
}
