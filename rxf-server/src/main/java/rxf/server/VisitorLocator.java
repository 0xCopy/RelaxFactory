package rxf.server;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.SynchronousQueue;

import com.google.web.bindery.requestfactory.shared.Locator;
import one.xio.HttpMethod;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static rxf.server.KernelImpl.GSON;
import static rxf.server.KernelImpl.createCouchConnection;
import static rxf.server.KernelImpl.executeCouchRequest;
import static rxf.server.KernelImpl.recycleChannel;

//import rxf.server.rf.SessionFindLocatorVisitor;

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
    String format = "POST " +
        /**
         * mandatory
         */
        '/'
        + clazz.getSimpleName().toLowerCase()
        +
        /**
         * mandatory
         */'/'
        + " HTTP/1.1\r\nContent-Type: application/json;charset=utf-8\r\nContent-Length: 2" +
//        "\r\n" + "Debug: " + wheresWaldo().trim() +
        "\r\n\r\n{}";
    SocketChannel couchConnection = null;
    Visitor ret = null;
    try {

      final SynchronousQueue<String> takeFrom = new SynchronousQueue<String>();
      couchConnection = createCouchConnection();
      executeCouchRequest(couchConnection, takeFrom, format);
      String take = takeFrom.take();
      CouchTx couchTx = GSON.fromJson(take, CouchTx.class);
      ret = find(clazz, couchTx.id);
    } catch (ClosedChannelException e) {
      e.printStackTrace();  //todo: verify for a purpose
    } catch (InterruptedException e) {
      e.printStackTrace();  //todo: verify for a purpose
    } finally {
      try {
        recycleChannel(couchConnection);
      } catch (IOException ignored) {
      }
    }
    return ret;
  }

  @Override
  public Visitor find(Class<? extends Visitor> clazz, String id) {

    String s = null;
    try {
      SocketChannel channel = createCouchConnection();
      String take;
      try {
        SynchronousQueue<String> retVal = new SynchronousQueue<String>();
        HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, KernelImpl.fetchJsonByIdVisitor(clazz.getSimpleName().toLowerCase() + '/' + id, channel, retVal));
        take = retVal.take();
      } finally {
        recycleChannel(channel);
      }
      s = take;
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
