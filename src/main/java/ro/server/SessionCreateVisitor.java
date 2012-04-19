package ro.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.concurrent.BlockingQueue;

import one.xio.HttpMethod;
import ro.model.RoSession;

import static ro.server.CouchChangesClient.GSON;

/**
 * User: jim
 * Date: 4/17/12
 * Time: 7:20 PM
 */
public class SessionCreateVisitor extends SessionLocatorVisitor<CouchTx, RoSession> {

  public static final CouchTx MEMENTO = new CouchTx();

  public SessionCreateVisitor(SocketChannel channel,   BlockingQueue<CouchTx> blockingQueue) {
    super(blockingQueue, channel);
  }


  /**
   * <pre>POST /rosession HTTP/1.1
   * User-Agent: curl/7.21.6 (x86_64-pc-linux-gnu) libcurl/7.21.6 OpenSSL/1.0.0e zlib/1.2.3.4 libidn/1.22 librtmp/2.3
   * Host: localhost:5984
   * Accept: * / *
   * Content-Type: application/json
   * Content-Length: 133
   * </pre>
   *
   * @param key
   */
  @Override
  public void onConnect(SelectionKey key) {

    try {

      boolean b = channel.finishConnect();
      if (b) {
        data = RoSession.createSession();
        String cs = GSON.toJson(data);
        String format = MessageFormat.format("POST /rosession HTTP/1.1\r\nContent-Type: application/json\r\nContent-Length: {0}\r\n\r\n{1}", cs.length(), cs);
        ByteBuffer encode = HttpMethod.UTF8.encode(format);
        channel.write(encode);
        System.err.println(format);
        key.interestOps(SelectionKey.OP_READ);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Override
  public void onWrite(SelectionKey key) {
    System.err.println("hi");
  }

  @Override
  public void onAccept(SelectionKey key) {
    System.err.println("hi");
  }

  @Override
  protected void handle(String json, CouchTx couchTx) {
    System.err.println("creation: "+json);
    data.setId(couchTx.id);data.setVersion(couchTx.rev);
  }

  @Override
  CouchTx getMemento() {
    return MEMENTO;  //todo: verify for a purpose
  }
}
