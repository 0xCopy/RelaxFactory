package ro.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;

import com.google.web.bindery.requestfactory.shared.Locator;
import one.xio.HttpMethod;
import ro.model.RoSession;

import static ro.server.CouchChangesClient.LOOPBACK;

/**
 * User: jim
 * Date: 4/16/12
 * Time: 1:22 PM
 */
public class RoSessionLocator extends Locator<RoSession, String> {


  public static final RoSession MEMENTO = new RoSession();

  /**
   * <pre>
   * POST /rosession HTTP/1.1
   * Content-Type: application/json
   * Content-Length: 133
   *
   * [data not shown]
   * HTTP/1.1 201 Created
   *
   * [data not shown]
   * </pre>
   *
   * @param clazz
   * @return
   */
  @Override
  public RoSession create(Class<? extends RoSession> clazz) {
    RoSession ret = null;
    try {
      Callable<RoSession> callable = new Callable<RoSession>() {
        public RoSession call() throws Exception {
          SessionLocatorVisitor<? extends CouchTx, ? extends RoSession> sessionVisitor = null;
          InetSocketAddress remote = new InetSocketAddress(LOOPBACK, 5984);
          System.err.println("opening " + remote.toString());
          SocketChannel channel = SocketChannel.open();
          channel.configureBlocking(false);
          channel.connect(remote);

          SynchronousQueue<CouchTx> blockingQueue = new SynchronousQueue<CouchTx>();

          sessionVisitor = new SessionCreateVisitor(channel, blockingQueue);
          HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, sessionVisitor);
          CouchTx take = blockingQueue.take();
          //      System.err.println("data received asyncronously!");
          return sessionVisitor.data;

        }
      };
      RoSession roSession = ret = KernelImpl.EXECUTOR_SERVICE.submit(callable).get();
    } catch (Throwable e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return ret;
  }

  @Override
  public RoSession find(Class<? extends RoSession> clazz, String id) {
    BlockingQueue<RoSession> blockingQueue = new SynchronousQueue<RoSession>();
    SessionLocatorVisitor<RoSession, RoSession> sessionVisitor = null;
    try {
      System.err.println("opening " + new InetSocketAddress(LOOPBACK, 5984).toString());
      SocketChannel channel = SocketChannel.open();
      blockingQueue = new SynchronousQueue<RoSession>();
      channel.configureBlocking(false);
      channel.connect(new InetSocketAddress(LOOPBACK, 5984));
      sessionVisitor = new SessionFindLocatorVisitor(blockingQueue, channel, id);
      HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, sessionVisitor);


    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }

    try {
      RoSession take = blockingQueue.take();
//      System.err.println("data received asyncronously!");
      return take;
    } catch (InterruptedException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return sessionVisitor.data;
  }

  @Override
  public Class<RoSession> getDomainType() {
    return RoSession.class;
  }

  @Override
  public String getId(RoSession domainObject) {
    return domainObject.getId();
  }

  @Override
  public Class<String> getIdType() {
    return String.class;
  }

  @Override
  public Object getVersion(RoSession domainObject) {
    return domainObject.getVersion();
  }

}
