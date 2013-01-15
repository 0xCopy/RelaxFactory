package one.xio;

import java.io.IOError;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import static java.lang.Character.isWhitespace;
import static java.lang.StrictMath.min;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpStatus.*;

/**
 * See  http://www.w3.org/Protocols/rfc2616/rfc2616-sec9.html
 * User: jim
 * Date: May 6, 2009
 * Time: 10:12:22 PM
 */
public enum HttpMethod implements AsioVisitor {
	GET {
		public void onWrite(SelectionKey key) {
			Object[] a = (Object[]) key.attachment();
			Xfer xfer = (Xfer) a[1];
			xfer.sendChunk(key);
		}

		public void onConnect(SelectionKey key) {
			onAccept(key);
		}

		/**
		 * enrolls a new SelectionKey to the methods
		 *
		 * @param key
		 * @throws IOException
		 */
		@Override
		public void onRead(SelectionKey key) {
			try {
				assert key.attachment() instanceof ByteBuffer;
				ByteBuffer buffer = (ByteBuffer) key.attachment();
				CharSequence parameters = methodParameters(buffer);

				String strings[] = parameters.toString().split("[ #?]"), fname = strings[0];

				RandomAccessFile fnode = new RandomAccessFile("./"
						+ fname.replace("../", "./"), "r");

				if (fnode.getFD().valid()) {
					FileChannel fc = fnode.getChannel();
					SocketChannel channel = (SocketChannel) key.channel();
					Xfer xfer = new Xfer(fc, fname);
					response(key, $200);

					try {
						ByteBuffer buffer1 = ByteBuffer.allocateDirect(channel
								.socket().getSendBufferSize());
						//
						MimeType mimeType = null;
						try {
							String substring = fname.substring(fname
									.lastIndexOf('.') + 1);
							mimeType = MimeType.valueOf(substring);
						} catch (Exception ignored) {
							throw new IOError(ignored);
						}
						String x = (mimeType == null
								? "\r\n"
								: ("Content-Type: " + mimeType.contentType + "\r\n"));
						CharBuffer c = (CharBuffer) buffer1.asCharBuffer()
								.append(
										"Connection: close\r\n" + x
												+ "Content-Length: "
												+ fc.size()).append("\r\n\r\n")
								.flip();
						channel.write(UTF8.encode(c));
						key.interestOps(OP_WRITE);
						key.attach(toArray(this, xfer));
					} catch (Exception ignored) {
						ignored.printStackTrace();
					}
					return;
				}
			} catch (Exception ignored) {
				ignored.printStackTrace();
			}
			try {
				response(key, $404);
				key.cancel();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		class Xfer {
			long progress;
			FileChannel fc;
			//            long creation = System.currentTimeMillis();
			//            long completion = -1L;
			public CharSequence name;
			//            public long chunk;
			//            private boolean pipeline = false;

			private void sendChunk(SelectionKey key) {
				SocketChannel channel = null;
				if (fc.isOpen() && key.isValid() && key.channel().isOpen()) {
					try {
						SocketChannel channel1 = (SocketChannel) key.channel();
						channel = channel1;
						int sendBufferSize = channel1.socket()
								.getSendBufferSize();

						progress += this.fc.transferTo(progress, Math.min(
								getRemaining(), Math.min(getRemaining(),
										sendBufferSize)/*(++chunk) << 8*/),
								channel);
						if (getRemaining() < 1) {
							try {
								fc.close();
							} catch (IOException ignored) {
							}
							key.attach(null);
							key.interestOps(OP_READ);//pipeline requests
						}

					} catch (Exception e) {
						key.cancel();
						try {
							fc.close();
						} catch (IOException ignored) {
						}
						fc = null;
						try {
							if (channel != null) {
								channel.close();
							}
						} catch (IOException ignored) {
						}
					}
				}
			}

			public Xfer(FileChannel fc, CharSequence name) {
				this.fc = fc;
				this.name = name;
			}

			long getRemaining() {
				try {
					return fc.size() - progress;
				} catch (Exception e) {
					return 0;
				}
			}

			public CharSequence logEntry() throws IOException {
				return new StringBuilder().append(getClass().getName()).append(
						':').append(name).append(' ').append(progress).append(
						'/').append(getRemaining());
			}

		}
	},

	POST, PUT, HEAD, DELETE, TRACE, CONNECT, OPTIONS, HELP, VERSION,
	/**
	 * http method cracker for new connections and pipeline resumes.
	 */
	$ {
		public void onAccept(SelectionKey selectionKey) {
			try {
				// note this is a required serversocketchannel not to be confused with the rest of the boilerplate
				ServerSocketChannel serverSocketChannel1 = (ServerSocketChannel) selectionKey
						.channel();
				SocketChannel socketChannel = serverSocketChannel1.accept();
				socketChannel.configureBlocking(false);
				enqueue(socketChannel, SelectionKey.OP_READ, $);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * this is where we take the input channel bytes, and write them to an output channel
		 *
		 * @param key
		 */
		@Override
		public void onWrite(SelectionKey key) {

			Object[] att = (Object[]) key.attachment();

			if (att != null) {
				HttpMethod method = (HttpMethod) att[0];
				method.onWrite(key);
				return;
			}
			key.cancel();
		}

		/**
		 * this is where we implement http 1.1. request handling
		 * <p/>
		 * Lifecycle of the attachemnts is
		 * <ol>
		 * <li> null means new socket
		 * <li>we attach(buffer) during the onConnect
		 * <li> we <i>expect</i> Object[HttpMethod,*,...] to be present for ongoing connections to delegate
		 * </ol>
		 *
		 * @param key
		 * @throws IOException
		 */
		@Override
		public void onRead(SelectionKey key) {
			try {
				SocketChannel channel = (SocketChannel) key.channel();
				int receiveBufferSize = channel.socket().getReceiveBufferSize();
				ByteBuffer dst = ByteBuffer.allocateDirect(receiveBufferSize);
				int read = channel.read(dst);
				if (-1 == read) {
					key.cancel();
					return;
				}
				dst.flip();
				try {
					ByteBuffer duplicate = dst.duplicate();
					while (duplicate.hasRemaining()
							&& !Character.isWhitespace(duplicate.get()));
					HttpMethod method = HttpMethod.valueOf(UTF8.decode(
							(ByteBuffer) duplicate.flip()).toString().trim());
					dst.limit(read).position(0);
					key.attach(dst);
					method.onRead(key);

				} catch (Exception e) {
					CharBuffer methodName = UTF8.decode((ByteBuffer) dst
							.clear().limit(read));
					System.err.println("BOOM " + methodName);
					e.printStackTrace();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onConnect(SelectionKey key) {
		}
	},
	;
	public static Charset UTF8 = Charset.forName("UTF8");
	//    private static int DEFAULT_EXP = 0;
	public static Thread selectorThread;
	public static boolean killswitch = false;
	public static int port = 8080;
	static Charset charset = UTF8;
	static CharsetDecoder decoder = charset.newDecoder();
	private static CompletionException COMPLETION_EXCEPTION = new CompletionException();
	private static Selector selector;
	private final static ConcurrentLinkedQueue<Object[]> q = new ConcurrentLinkedQueue<>();
    ByteBuffer token = (ByteBuffer) ByteBuffer.wrap(name().getBytes()).rewind()
			.mark();
	int margin = name().length() + 1;

	public static Selector getSelector() {
		return selector;
	}

	static public void setSelector(Selector selector) {
		HttpMethod.selector = selector;
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
	 * @throws ClosedChannelException
	 */
	public static void enqueue(SelectableChannel channel, int op, Object... s)
			throws ClosedChannelException {
		boolean add = q.add(toArray(channel, op, s));
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

	private static void response(SelectionKey key, HttpStatus httpStatus)
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

	public static void enqueue(AsioVisitor asioVisitor) {
		//To change body of created methods use File | Settings | File Templates.
	}

	public static void main(final String... a) throws IOException {

		try {

			ServerSocketChannel serverSocketChannel;
			serverSocketChannel = ServerSocketChannel.open();
			serverSocketChannel.socket().bind(
					new java.net.InetSocketAddress(port));
			serverSocketChannel.configureBlocking(false);

			enqueue(serverSocketChannel, SelectionKey.OP_ACCEPT, $);

		} catch (Throwable e11) {
			e11.printStackTrace();
		}

		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		init($, a);
	}

	public static void init(AsioVisitor protocoldecoder, String... a)
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
					//          System.err.println("" + op + "/" + String.valueOf(att));
					try {
                        SelectionKey register = x.register(sel, op, att);
                        assert null!=register;
                    } catch (Throwable e) {
                        e.printStackTrace();
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
										System.err.println("origin" + s);
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

    public static void setKillswitch(boolean killswitch) {
        HttpMethod.killswitch = killswitch;
    }

    /**
	 * deduce a few parse optimizations
	 *
	 * @param request
	 * @return
	 */

	boolean recognize(ByteBuffer request) {

		if (isWhitespace(request.get(margin)))
			for (int i = 0; i < margin - 1; i++)
				if (request.get(i) != token.get(i))
					return false;
		return true;
	}

	/**
	 * tokenizes method parameters
	 *
	 * @param before
	 * @return byte-array buffer of method parameters
	 */
	public ByteBuffer tokenize(ByteBuffer before) {

		ByteBuffer after = (ByteBuffer) before.duplicate().position(0);

		boolean isBlank = true, wasBlank;
		int prevIdx = 0;
		before.position(margin);
		char b = 0;
		while (b != '\n' && after.position() < margin) {
			wasBlank = isBlank;
			b = (char) (before.get() & 0xff);
			isBlank = isWhitespace(b & 0xff);

			if ((!isBlank) && wasBlank) {
				after.put((byte) ((byte) (before.position() & 0xff) - 1));

				//        System.out.println("token found: " + before.duplicate().position(prevIdx));
			}
		}
		//    simple way to write at least one but more on occasion
		while (after.put((byte) 0).position() < margin);

		return (ByteBuffer) after.position(0);
	}

	public CharSequence methodParameters(ByteBuffer indexEntries)
			throws IOException {

		indexEntries.position(0);
		int last = 0;
		int b;

		// start from 0 and traverese to null terminator inserted during the tokenization...
		while ((b = indexEntries.get()) != 0
				&& (indexEntries.position() <= margin))
			last = b & 0xff;

		int len = indexEntries.position();

		//this should be between 40 and 300 something....
		indexEntries.position(last);

		while (!Character.isISOControl(b = indexEntries.get() & 0xff)
				&& !Character.isWhitespace(b) && ('\n' != b) && ('\r' != b)
				&& ('\t' != b));

		return decoder
				.decode((ByteBuffer) indexEntries.flip().position(margin));

	}

	@Override
	public void onRead(SelectionKey key) {
		Object o = key.attachment();
		if (o instanceof ByteBuffer) {
			this.tokenize((ByteBuffer) o);
		}
	}

	/**
	 * enrolls a new SelectionKey to the methods
	 *
	 * @param key
	 * @throws IOException
	 */
	@Override
	public void onConnect(SelectionKey key) {

		try {
			response(key, $501);
			ByteBuffer b = (ByteBuffer) key.attachment();

			SelectableChannel channel = key.channel();
			SocketChannel c = (SocketChannel) channel;

			c.write((ByteBuffer) b.rewind());

		} catch (IOException ignored) {
		} finally {
			try {
				key.channel().close();
			} catch (IOException ignored) {
			}
			key.cancel();

		}
	}

	@Override
	public void onWrite(SelectionKey key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void onAccept(SelectionKey key) {
		throw new UnsupportedOperationException();
	}

	static class CompletionException extends Throwable {
	}
}
