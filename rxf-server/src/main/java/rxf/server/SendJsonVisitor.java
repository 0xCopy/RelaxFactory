package rxf.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
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
  private final String[] pathIdVer;
  private final SynchronousQueue<String> returnTo;

  public SendJsonVisitor(String json, SynchronousQueue<String> returnTo, String... pathIdVer) {
    this.json = json;
    this.returnTo = returnTo;
    this.pathIdVer = pathIdVer;
//    assert this.pathIdVer.length > 1;
    /*  if (SendJsonVisitor.DEBUG_SENDJSON) {
      System.err.println("sendJson audit: " + BlobAntiPatternObject.arrToString(pathIdVer) + wheresWaldo());
      if (pathIdVer.length < 1) throw new Error("new path code required here");
    }*/
  }

  @Override
  public void onWrite(final SelectionKey key) throws UnsupportedEncodingException {
    String method;
    String call;
    final boolean fresh = pathIdVer.length < 3;

    method = fresh ? "POST" : "PUT";


    String path = "";
    for (int i = 0; i < pathIdVer.length; i++) {
      String s = pathIdVer[i];
      switch (i) {
        case 2:
          path += "?rev=" + s;
          break;
        default:
          path += '/' + s;
      }
    }
//    call = MessageFormat.format("{0} /{1} HTTP/1.1\r\nContent-Type: application/json\r\nContent-Length: {2}\r\n\r\n{3}", method, path, json.length(), json);
    call = (new StringBuilder().append(method).append(" ").append(path.replace("//", "/")).append(" HTTP/1.1\r\nContent-Type: application/json\r\nContent-Length: ").append(json.getBytes(BlobAntiPatternObject.UTF8CHARSET).length).append("\r\n\r\n").append(json).toString()).replace("//", "/");
    if (DEBUG_SENDJSON) {
      System.err.println("dsj: attempting call to " + call + " " + wheresWaldo());
    }
    ByteBuffer encode = UTF8.encode(call);
    SocketChannel channel = (SocketChannel) key.channel();
    try {
      channel.write(encode);
      key.selector().wakeup();
      key.interestOps(SelectionKey.OP_READ).attach(BlobAntiPatternObject.createJsonResponseReader(returnTo));
    } catch (IOException e) {
      e.printStackTrace();  //
    }
  }
}

