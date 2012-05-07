package ro.server.rf;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.concurrent.BlockingQueue;

import one.xio.HttpMethod;
import ro.model.Visitor;
import ro.server.CouchTx;

import static ro.server.KernelImpl.GSON;

/**
 * User: jim
 * Date: 4/17/12
 * Time: 7:20 PM
 */
public class VisitorVisitor extends VisitorLocatorVisitor<CouchTx, Visitor> {

  public static final CouchTx MEMENTO = new CouchTx();

  public VisitorVisitor(SocketChannel channel, BlockingQueue<CouchTx> blockingQueue) {
    super(blockingQueue, channel);
  }


  @Override
  public void onWrite(SelectionKey key) throws Exception {
    data = Visitor.createSession();
    String cs = GSON.toJson(data);
    String format = MessageFormat.format("POST /rosession HTTP/1.1\r\nContent-Type: application/json\r\nContent-Length: {0,number,#}\r\n\r\n{1}", cs.length(), cs);
    ByteBuffer encode = HttpMethod.UTF8.encode(format);
    channel.write(encode);
    System.err.println(format);
    key.interestOps(SelectionKey.OP_READ);
  }


  @Override
  protected void handle(String json, CouchTx couchTx) {
    System.err.println("creation: " + json);
    data.setId(couchTx.id);
    data.setVersion(couchTx.rev);
  }

  @Override
  CouchTx getMemento() {
    return MEMENTO;  //todo: verify for a purpose
  }
}
