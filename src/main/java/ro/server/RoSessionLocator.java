package ro.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;

import com.google.web.bindery.requestfactory.shared.Locator;
import one.xio.HttpMethod;
import ro.model.RoSession;
import ro.server.rf.SessionCreateVisitor;
import ro.server.rf.SessionLocatorVisitor;

import static ro.server.KernelImpl.GSON;
import static ro.server.KernelImpl.LOOPBACK;

//import ro.server.rf.SessionFindLocatorVisitor;

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

          SynchronousQueue<CouchTx> retVal = new SynchronousQueue<CouchTx>();

          sessionVisitor = new SessionCreateVisitor(channel, retVal);
          HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, sessionVisitor);
          retVal.take();
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

    String s = null;
    try {
      s = SessionToolImpl.fetchSessionJsonById(id);
    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    } catch (InterruptedException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }

    return GSON.fromJson(s, getDomainType());

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
