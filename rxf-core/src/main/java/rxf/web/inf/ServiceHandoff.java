package rxf.web.inf;

import rxf.core.Rfc822HeaderState;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public interface ServiceHandoff {
  ByteBuffer getCursor();

  void setCursor(ByteBuffer cursor);

  SocketChannel getChannel();

  void setChannel(SocketChannel channel);

  Rfc822HeaderState.HttpRequest getReq();

  void setReq(Rfc822HeaderState.HttpRequest req);
}
