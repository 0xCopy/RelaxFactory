package rxf.server;

import one.xio.AsioVisitor;
import rxf.server.gen.CouchDriver;
import rxf.server.web.inf.ProtocolMethodDispatch;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.nio.channels.SelectionKey.OP_ACCEPT;
import static rxf.server.CouchNamespace.COUCH_DEFAULT_ORGNAME;
import static rxf.server.RelaxFactoryServerImpl.wheresWaldo;

/**
 * <a href='http://www.antipatterns.com/briefing/sld024.htm'> Blob Anti Pattern </a>
 * used here as a pattern to centralize the antipatterns
 * User: jim
 * Date: 4/17/12
 * Time: 11:55 PM
 */
public class BlobAntiPatternRelic {

	public static SocketChannel createCouchConnection() {
		while (!RelaxFactoryServerImpl.isKillswitch()) {
			try {
				SocketChannel channel = SocketChannel
						.open(RelaxFactoryServerImpl.getCOUCHADDR());
				channel.configureBlocking(false);
				return channel;
			} catch (IOException e) {
				System.err.println("----- very bad connection failure in "
						+ wheresWaldo(4));
				e.printStackTrace();
			} finally {
			}
		}
		return null;
	}

	public static <T> String deepToString(T... d) {
		return Arrays.deepToString(d) + wheresWaldo();
	}

	public static <T> String arrToString(T... d) {
		return Arrays.deepToString(d);
	}

	public static void recycleChannel(SocketChannel channel) {
		try {
			channel.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static int getReceiveBufferSize() {
		if (0 == RelaxFactoryServerImpl.getReceiveBufferSize())
			try {
				SocketChannel couchConnection = createCouchConnection();
				RelaxFactoryServerImpl.setReceiveBufferSize(couchConnection
						.socket().getReceiveBufferSize());
				recycleChannel(couchConnection);
			} catch (IOException ignored) {
			}

		return RelaxFactoryServerImpl.getReceiveBufferSize();
	}

	public static void setReceiveBufferSize(int receiveBufferSize) {
		receiveBufferSize = receiveBufferSize;
	}

	public static int getSendBufferSize() {
		if (0 == RelaxFactoryServerImpl.getSendBufferSize())
			try {
				SocketChannel couchConnection = createCouchConnection();
				RelaxFactoryServerImpl.setSendBufferSize(couchConnection
						.socket().getReceiveBufferSize());
				recycleChannel(couchConnection);
			} catch (IOException ignored) {
			}
		return RelaxFactoryServerImpl.getSendBufferSize();
	}

	public static void setSendBufferSize(int sendBufferSize) {
		sendBufferSize = sendBufferSize;
	}

	public static String inferRevision(Map map) {
		String rev = (String) map.get("_rev");
		if (null == rev)
			rev = (String) map.get("rev");
		if (null == rev) {
			rev = (String) map.get("version");
		}
		if (null == rev)
			rev = (String) map.get("ver");
		return rev;
	}

	public static String dequote(String s) {
		String ret = s;
		if (null != s && ret.startsWith("\"") && ret.endsWith("\"")) {
			ret = ret.substring(1, ret.lastIndexOf('"'));
		}

		return ret;
	}

	//test
	public static void main(String... args) throws Exception {
		//		GeoIpService.startGeoIpService();
		startServer(args);
	}

	public static void startServer(String... args) throws IOException {
		AsioVisitor topLevel;
		ServerSocketChannel serverSocketChannel;
		final String port;
		InetAddress hostname;
		{
			String json = "{}";
			for (String arg : args) {
				json += arg;
			}
			final Properties properties = CouchDriver.GSON.fromJson(json,
					Properties.class);
			topLevel = new ProtocolMethodDispatch();
			serverSocketChannel = ServerSocketChannel.open();
			port = properties.getProperty("port", "8080");
			hostname = InetAddress.getByName(properties.getProperty("hostname",
					"0.0.0.0"));
		}
		serverSocketChannel.socket().bind(
				new InetSocketAddress(hostname, Integer.parseInt(port)));
		serverSocketChannel.configureBlocking(false);
		RelaxFactoryServerImpl
				.enqueue(serverSocketChannel, OP_ACCEPT, topLevel);
		RelaxFactoryServerImpl.init(topLevel, args);
	}

	public static TimeUnit getDefaultCollectorTimeUnit() {
		return RelaxFactoryServerImpl.isDEBUG_SENDJSON()
				? TimeUnit.HOURS
				: CouchDriver.defaultCollectorTimeUnit;
	}

	public static ByteBuffer avoidStarvation(ByteBuffer buf) {
		if (0 == buf.remaining())
			buf.rewind();
		return buf;
	}

	public static String getDefaultOrgName() {
		return COUCH_DEFAULT_ORGNAME;
	}

	/**
	 * byte-compare of suffixes
	 *
	 * @param terminator  the token used to terminate presumably unbounded growth of a list of buffers
	 * @param currentBuff current ByteBuffer which does not necessarily require a list to perform suffix checks.
	 * @param prev        a linked list which holds previous chunks
	 * @return whether the suffix composes the tail bytes of current and prev buffers.
	 */
	static public boolean suffixMatchChunks(byte[] terminator,
			ByteBuffer currentBuff, ByteBuffer... prev) {
		ByteBuffer tb = currentBuff;
		int prevMark = prev.length;
		int backtrack = 0;
		boolean mismatch = false;
		int bl = terminator.length;
		int rskip = 0;
		for (int i = bl - 1; i >= 0 && !mismatch; i--) {
			rskip++;
			int comparisonOffset = tb.position() - rskip;
			if (comparisonOffset < 0) {
				prevMark--;
				if (prevMark < 0) {
					mismatch = true;
					break;
				} else {
					tb = prev[prevMark];
					rskip = 0;
					i++;
				}
			} else {
				byte aByte = terminator[i];
				byte b = tb.get(comparisonOffset);
				if (aByte != b) {
					mismatch = true;
				}
			}
		}
		return !mismatch;
	}

}
