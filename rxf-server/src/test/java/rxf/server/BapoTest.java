package rxf.server;

import java.nio.channels.SocketChannel;

import one.xio.HttpMethod;

import junit.framework.TestCase;

public class BapoTest extends TestCase {
	public void testCreateCouchChannel() {
		HttpMethod.killswitch = false;
		SocketChannel channel = BlobAntiPatternObject.createCouchConnection();
		assertNotNull(channel);
		BlobAntiPatternObject.recycleChannel(channel);
		HttpMethod.killswitch = true;
	}

}
