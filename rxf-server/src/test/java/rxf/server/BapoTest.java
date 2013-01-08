package rxf.server;

import org.junit.Test;

import java.nio.channels.SocketChannel;

import static org.junit.Assert.assertNotNull;

public class BapoTest{
  @Test
  public void testCreateCouchChannel(){
    RelaxFactoryServer.App.get().setKillswitch(false);
    SocketChannel channel=BlobAntiPatternRelic.createCouchConnection();
    assertNotNull(channel);
    BlobAntiPatternRelic.recycleChannel(channel);
    RelaxFactoryServer.App.get().setKillswitch(true);
  }

}
