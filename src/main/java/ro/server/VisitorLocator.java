package ro.server;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.SynchronousQueue;

import com.google.web.bindery.requestfactory.shared.Locator;
import one.xio.HttpMethod;
import ro.model.Visitor;
import ro.server.rf.VisitorAsioVisitor;
import ro.server.rf.VisitorLocatorAsioVisitor;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static ro.server.KernelImpl.GSON;
import static ro.server.KernelImpl.createCouchConnection;

//import ro.server.rf.SessionFindLocatorVisitor;

/**
 * User: jim
 * Date: 4/16/12
 * Time: 1:22 PM
 */
public class VisitorLocator extends Locator<Visitor, String> {


  public static final Visitor MEMENTO = new Visitor();

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
  public Visitor create(Class<? extends Visitor> clazz) {
    Visitor ret = null;
    try {
      Callable<Visitor> callable = new Callable<Visitor>() {
        public Visitor call() throws Exception {
          VisitorLocatorAsioVisitor<? extends CouchTx, ? extends Visitor> sessionVisitor = null;
//          InetSocketAddress remote = new InetSocketAddress(LOOPBACK, 5984);
//          System.err.println("opening " + remote.toString());
//          SocketChannel channel = SocketChannel.open();
//          channel.configureBlocking(false);
//          channel.connect(remote);
          final SocketChannel channel = createCouchConnection();

          try {
            SynchronousQueue<CouchTx> retVal = new SynchronousQueue<CouchTx>();

            sessionVisitor = new VisitorAsioVisitor(channel, retVal);
            HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, sessionVisitor);
            retVal.take();
          } finally {
            KernelImpl.recycleChannel(channel);
          }


          return sessionVisitor.data;

        }
      };
      Visitor roSession = ret = KernelImpl.EXECUTOR_SERVICE.submit(callable).get();
    } catch (Throwable e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return ret;
  }

  @Override
  public Visitor find(Class<? extends Visitor> clazz, String id) {

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
  public Class<Visitor> getDomainType() {
    return Visitor.class;
  }

  @Override
  public String getId(Visitor domainObject) {
    return domainObject.getId();
  }

  @Override
  public Class<String> getIdType() {
    return String.class;
  }

  @Override
  public Object getVersion(Visitor domainObject) {
    return domainObject.getVersion();
  }

}
