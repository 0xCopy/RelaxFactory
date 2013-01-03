package rxf.server;

import org.junit.Test;

import java.nio.channels.SocketChannel;

import static org.junit.Assert.assertNotNull;

public class BapoTest {
	@Test
	public void testCreateCouchChannel() {
		RelaxFactoryServerImpl.killswitch = false;
		SocketChannel channel = BlobAntiPatternObject.createCouchConnection();
		assertNotNull(channel);
		BlobAntiPatternObject.recycleChannel(channel);
		RelaxFactoryServerImpl.killswitch = true;
	}

}
