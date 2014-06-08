package rxf.couch;

import org.junit.Test;

import java.nio.channels.SocketChannel;

import static rxf.core.Server.setKillswitch;
import static org.junit.Assert.assertNotNull;
import static rxf.rpc.CouchConnectionFactory.createCouchConnection;
import static rxf.rpc.CouchConnectionFactory.recycleChannel;

public class BapoTest {
  @Test
  public void testCreateCouchChannel() {
    setKillswitch(false);
    SocketChannel channel = createCouchConnection();
    assertNotNull(channel);
    recycleChannel(channel);
    setKillswitch(true);
  }

}
