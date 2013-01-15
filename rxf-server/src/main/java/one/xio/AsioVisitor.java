package one.xio;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.WeakHashMap;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;

/**
 * User: jim
 * Date: 4/15/12
 * Time: 11:50 PM
 */
public interface AsioVisitor {
	final boolean $DBG = null != System.getenv("DEBUG_VISITOR_ORIGINS");
	WeakHashMap<Impl, String> $origins = $DBG
			? new WeakHashMap<Impl, String>()
			: null;

	void onRead(SelectionKey key) throws Exception;

	void onConnect(SelectionKey key) throws Exception;

	void onWrite(SelectionKey key) throws Exception;

	void onAccept(SelectionKey key) throws Exception;

	class Impl implements AsioVisitor {
		{
			if ($DBG)
				$origins.put(this, HttpMethod.wheresWaldo(4));
		}

		public Impl preRead(Object... env) {
			return this;
		}

		public Impl preWrite(Object... env) {
			return this;
		}

		@Override
		public void onRead(SelectionKey key) throws Exception {
			System.err.println("fail: " + key.toString());
			final SocketChannel channel = (SocketChannel) key.channel();
			final int receiveBufferSize = channel.socket()
					.getReceiveBufferSize();
			final String trim = HttpMethod.UTF8.decode(
					ByteBuffer.allocateDirect(receiveBufferSize)).toString()
					.trim();

			throw new UnsupportedOperationException("found " + trim + " in "
					+ getClass().getCanonicalName());
		}

		/**
		 * this doesn't change very often for outbound web connections
		 *
		 * @param key
		 * @throws Exception
		 */
		@Override
		public void onConnect(SelectionKey key) throws Exception {
			if (((SocketChannel) key.channel()).finishConnect())
				key.interestOps(OP_WRITE);
		}

		@Override
		public void onWrite(SelectionKey key) throws Exception {
			final SocketChannel channel = (SocketChannel) key.channel();
			System.err.println("buffer underrun?: "
					+ channel.socket().getRemoteSocketAddress());
			throw new UnsupportedOperationException("found in "
					+ getClass().getCanonicalName());
		}

		@Override
		public void onAccept(SelectionKey key) throws Exception {

			ServerSocketChannel c = (ServerSocketChannel) key.channel();
			final SocketChannel accept = c.accept();
			HttpMethod.enqueue(accept, OP_READ | OP_WRITE, key.attachment());

		}
	}
}
