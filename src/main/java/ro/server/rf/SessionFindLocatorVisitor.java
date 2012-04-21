//package ro.server.rf;
//
//import java.io.IOException;
//import java.nio.ByteBuffer;
//import java.nio.channels.SelectionKey;
//import java.nio.channels.SocketChannel;
//import java.text.MessageFormat;
//import java.util.concurrent.BlockingQueue;
//
//import one.xio.HttpMethod;
//import ro.model.RoSession;
//import ro.server.RoSessionLocator;
//
//import static ro.server.CouchChangesClient.GSON;
//
///**
//* User: jim
//* Date: 4/17/12
//* Time: 9:58 PM
//*/
//public class SessionFindLocatorVisitor extends SessionLocatorVisitor<RoSession, RoSession> {
//  private final String id;
//
//  public SessionFindLocatorVisitor(BlockingQueue<RoSession> blockingQueue, SocketChannel channel, String id) {
//    super(blockingQueue, channel);
//    this.id = id;
//  }
//
//
//
//  @Override
//  RoSession getMemento() {
//    return RoSessionLocator.MEMENTO;
//  }
//
//  @Override
//  public void onConnect(SelectionKey key) {
//    try {
//
//      boolean b = channel.finishConnect();
//      if (b) {
//        data = RoSession.createSession();
//        String cs = GSON.toJson(data);
////          System.err.println(cs);
//        String format = MessageFormat.format("GET /rosession/{0} HTTP/1.1\r\n\r\n", id);
//        System.err.println("attempting to connect to "+format);
//        ByteBuffer encode = HttpMethod.UTF8.encode(format);
//        channel.write(encode);
//        System.err.println(format);
//        key.interestOps(SelectionKey.OP_READ);
//      }
//    } catch (IOException e) {
//      e.printStackTrace();  //todo: verify for a purpose
//    }
//  }
//
//  @Override
//  public void onWrite(SelectionKey key) {
//    //todo: verify for a purpose
//  }
//
//  @Override
//  public void onAccept(SelectionKey key) {
//    //todo: verify for a purpose
//  }
//
//  @Override
//  protected void handle(String json, RoSession couchTx) {
//    data=couchTx;
//  }
//}
