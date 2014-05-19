package rxf.server;

import org.junit.Test;

import java.nio.channels.SocketChannel;

import static rxf.server.Server.setKillswitch;
import static org.junit.Assert.assertNotNull;
import static rxf.server.BlobAntiPatternObject.createCouchConnection;
import static rxf.server.BlobAntiPatternObject.recycleChannel;

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
