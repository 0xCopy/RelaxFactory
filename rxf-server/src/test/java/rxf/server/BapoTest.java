package rxf.server;

import org.junit.Test;

import java.nio.channels.SocketChannel;

import static org.junit.Assert.assertNotNull;

public class BapoTest {
	@Test
	public void testCreateCouchChannel() {
		RelaxFactoryServerImpl.setKillswitch(false);
		SocketChannel channel = BlobAntiPatternRelic.createCouchConnection();
		assertNotNull(channel);
		BlobAntiPatternRelic.recycleChannel(channel);
		RelaxFactoryServerImpl.setKillswitch(true);
	}

}
