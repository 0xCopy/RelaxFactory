package rxf.server;

import one.xio.HttpMethod;
import org.junit.Test;

import java.nio.channels.SocketChannel;

import static org.junit.Assert.assertNotNull;

public class BapoTest {
	@Test
	public void testCreateCouchChannel() {
		HttpMethod.killswitch = false;
		SocketChannel channel = BlobAntiPatternObject.createCouchConnection();
		assertNotNull(channel);
		BlobAntiPatternObject.recycleChannel(channel);
		HttpMethod.killswitch = true;
	}

}
