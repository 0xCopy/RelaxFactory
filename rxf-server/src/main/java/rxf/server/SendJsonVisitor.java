package rxf.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.concurrent.SynchronousQueue;

import one.xio.AsioVisitor;

import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.wheresWaldo;

/**
 * User: jim
 * Date: 4/23/12
 * Time: 10:20 PM
 */
class SendJsonVisitor extends AsioVisitor.Impl {
  public static final boolean DEBUG_SENDJSON = System.getenv().containsKey("DEBUG_SENDJSON");
  private final String json;
  private final String[] idver;
  private final SynchronousQueue returnTo;

  public SendJsonVisitor(String json, SynchronousQueue returnTo, String... idver) {
    this.json = json;
    this.returnTo = returnTo;
    this.idver = idver;
  }

  @Override
  public void onWrite(final SelectionKey key) {
    String method;
    String call;
    method = idver.length < 1 ? "POST" : "PUT";

    String path = "";
    for (int i = 0; i < idver.length; i++) {
      String s = idver[i];
      switch (i) {
        case 0:
          path += s;
          break;
        case 1:
          path += "?rev=" + s;
          break;
      }
    }
//    call = MessageFormat.format("{0} /{1} HTTP/1.1\r\nContent-Type: application/json\r\nContent-Length: {2}\r\n\r\n{3}", method, path, json.length(), json);
    call = MessageFormat.format("{0} /{1} HTTP/1.1\r\nContent-Type: application/json\r\nContent-Length: " + json.length() + "\r\n\r\n{2}", method, path, json).replace("//", "/");
    if (DEBUG_SENDJSON) {
      System.err.println("dsj: attempting call to " + call + " " + wheresWaldo());
    }
    ByteBuffer encode = UTF8.encode(call);
    SocketChannel channel = (SocketChannel) key.channel();
    try {
      channel.write(encode);
      key.attach(BlobAntiPatternObject.createJsonResponseReader(returnTo));
      key.selector().wakeup();
      key.interestOps(SelectionKey.OP_READ);
    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }
}

