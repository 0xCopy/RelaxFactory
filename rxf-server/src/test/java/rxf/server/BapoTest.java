package rxf.server;

import junit.framework.TestCase;
import one.xio.HttpMethod;

import java.nio.channels.SocketChannel;

public class BapoTest extends TestCase {
    public void testCreateCouchChannel() {
        HttpMethod.killswitch = false;
        SocketChannel channel = BlobAntiPatternObject.createCouchConnection();
        assertNotNull(channel);
        BlobAntiPatternObject.recycleChannel(channel);
        HttpMethod.killswitch = true;
    }

}
