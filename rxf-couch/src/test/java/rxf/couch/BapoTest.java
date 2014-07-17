package rxf.couch;

import one.xio.AsyncSingletonServer;
import org.junit.Test;

import java.nio.channels.SocketChannel;

import static org.junit.Assert.assertNotNull;
import static rxf.couch.CouchConnectionFactory.createCouchConnection;
import static rxf.couch.CouchConnectionFactory.recycleChannel;

public class BapoTest {
  @Test
  public void testCreateCouchChannel() {
    AsyncSingletonServer.killswitch.set(false);
    SocketChannel channel = createCouchConnection();
    assertNotNull(channel);
    recycleChannel(channel);
    AsyncSingletonServer.killswitch.set(true);
  }

}
