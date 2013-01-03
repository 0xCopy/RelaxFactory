package rxf.server;

import one.xio.AsioVisitor;
import one.xio.AsioVisitor.Impl;
import one.xio.HttpStatus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.lang.Math.min;

public class RelaxFactoryServerImpl implements RelaxFactoryServer {

	private static InetAddress LOOPBACK;
	private boolean DEBUG_SENDJSON = System.getenv().containsKey(
			"DEBUG_SENDJSON");
	private int receiveBufferSize;
	private int sendBufferSize;
	private InetSocketAddress COUCHADDR;
	private ScheduledExecutorService EXECUTOR_SERVICE = Executors
			.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() + 3);
	//    private  int DEFAULT_EXP = 0;
	private Thread selectorThread;
	private boolean killswitch = false;
	private int port = 8080;
	private Selector selector;
	private ConcurrentLinkedQueue<Object[]> q = new ConcurrentLinkedQueue<Object[]>();
	private AsioVisitor topLevel;
	private InetAddress hostname;
	private ServerSocketChannel serverSocketChannel;

	public RelaxFactoryServerImpl() {

		setCOUCHADDR(new InetSocketAddress(getLOOPBACK(), 5984));

	}

	static public String wheresWaldo(int... depth) {
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

	public static RelaxFactoryServer createRelaxFactoryServerImpl() {
		return new RelaxFactoryServerImpl();
	}

	@Override
	public Selector getSelector() {
		return selector;
	}

	@Override
	public RelaxFactoryServer setSelector(Selector selector) {
		this.selector = selector;
		return this;
	}

	@Override
	public Object[] toArray(Object... t) {
		return t;
	}

	/**
	 * handles the threadlocal ugliness if any to registering user threads into the selector/reactor pattern
	 *
	 *
	 * @param channel the socketchanel
	 * @param op      int ChannelSelector.operator
	 * @param s       the payload: grammar {enum,data1,data..n}
	 * @throws java.nio.channels.ClosedChannelException
	 *
	 */
	@Override
	public RelaxFactoryServer enqueue(SelectableChannel channel, int op,
			Object... s) throws ClosedChannelException {
		boolean add = q.add(toArray(channel, op, s));
		Selector selector1 = getSelector();
		if (null != selector1)
			selector1.wakeup();
		return this;
	}

	@Override
	public RelaxFactoryServer response(SelectionKey key, HttpStatus httpStatus)
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

		return this;
	}

	@Override
	public RelaxFactoryServer init(AsioVisitor protocoldecoder, String... a)
			throws IOException {

		setSelector(Selector.open());
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
		}
		return this;
	}

	@Override
	public AsioVisitor inferAsioVisitor(AsioVisitor default$, SelectionKey key) {
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

	@Override
	public Charset getUTF8() {
		return UTF8;
	}

	@Override
	public Thread getSelectorThread() {
		return selectorThread;
	}

	@Override
	public RelaxFactoryServer setSelectorThread(Thread selectorThread) {
		this.selectorThread = selectorThread;
		return this;
	}

	@Override
	public boolean isKillswitch() {
		return killswitch;
	}

	@Override
	public RelaxFactoryServer setKillswitch(boolean killswitch) {
		this.killswitch = killswitch;
		return this;
	}

	@Override
	public ConcurrentLinkedQueue<Object[]> getQ() {
		return q;
	}

	@Override
	public RelaxFactoryServer setQ(ConcurrentLinkedQueue<Object[]> q) {
		this.q = q;
		return this;
	}

	@Override
	public boolean isDEBUG_SENDJSON() {
		return DEBUG_SENDJSON;
	}

	@Override
	public RelaxFactoryServer setDEBUG_SENDJSON(boolean DEBUG_SENDJSON) {
		this.DEBUG_SENDJSON = DEBUG_SENDJSON;
		return this;
	}

	@Override
	public InetAddress getLOOPBACK() {
		return LOOPBACK;
	}

	@Override
	public InetSocketAddress getCOUCHADDR() {
		return COUCHADDR;
	}

	@Override
	public RelaxFactoryServer setCOUCHADDR(InetSocketAddress COUCHADDR) {
		this.COUCHADDR = COUCHADDR;
		return this;
	}

	@Override
	public ScheduledExecutorService getEXECUTOR_SERVICE() {
		return EXECUTOR_SERVICE;
	}

	@Override
	public RelaxFactoryServer setEXECUTOR_SERVICE(
			ScheduledExecutorService EXECUTOR_SERVICE) {
		this.EXECUTOR_SERVICE = EXECUTOR_SERVICE;
		return this;
	}

	@Override
	public int getReceiveBufferSize() {
		return receiveBufferSize;
	}

	@Override
	public RelaxFactoryServer setReceiveBufferSize(int receiveBufferSize) {
		this.receiveBufferSize = receiveBufferSize;
		return this;
	}

	@Override
	public int getSendBufferSize() {
		return sendBufferSize;
	}

	@Override
	public RelaxFactoryServer setSendBufferSize(int sendBufferSize) {
		this.sendBufferSize = sendBufferSize;
		return this;
	}

	@Override
	public RelaxFactoryServer init(String hostname, int port,
			AsioVisitor topLevel) throws UnknownHostException {
		assert topLevel == null && serverSocketChannel == null : "Can't call init twice";
		this.topLevel = topLevel;
		this.setPort(port);
		this.hostname = InetAddress.getByName(hostname);
		return this;
	}

	static {
		try {

			LOOPBACK = (InetAddress.getByAddress(new byte[]{127, 0, 0, 1}));
			System.err.println("java 6 LOOPBACK detected");
		} catch (UnknownHostException e) {
			e.printStackTrace(); //To change body of catch statement use File | Settings | File Templates.
		}
	}

	@Override
	public RelaxFactoryServer start() throws IOException {
		assert serverSocketChannel == null : "Can't start already started server";
		serverSocketChannel = ServerSocketChannel.open();
		InetSocketAddress addr = new InetSocketAddress(hostname, getPort());
		serverSocketChannel.socket().bind(addr);
		setPort(serverSocketChannel.socket().getLocalPort());
		System.out.println(hostname.getHostAddress() + ":" + getPort());
		serverSocketChannel.configureBlocking(false);

		enqueue(serverSocketChannel, SelectionKey.OP_ACCEPT, topLevel);
		init(topLevel);
		return this;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public RelaxFactoryServer setPort(int port) {
		this.port = port;
		return this;
	}

	@Override
	public RelaxFactoryServer stop() throws IOException {
		this.killswitch = true;
		getSelector().close();
		serverSocketChannel.close();
		return this;
	}

	@Override
	public AsioVisitor getTopLevel() {
		return topLevel;
	}

	@Override
	public RelaxFactoryServer setTopLevel(AsioVisitor topLevel) {
		this.topLevel = topLevel;
		return this;
	}

	@Override
	public InetAddress getHostname() {
		return hostname;
	}

	@Override
	public RelaxFactoryServer setHostname(InetAddress hostname) {
		this.hostname = hostname;
		return this;
	}

	@Override
	public ServerSocketChannel getServerSocketChannel() {
		return serverSocketChannel;
	}

	@Override
	public RelaxFactoryServer setServerSocketChannel(
			ServerSocketChannel serverSocketChannel) {
		this.serverSocketChannel = serverSocketChannel;
		return this;
	}
}
