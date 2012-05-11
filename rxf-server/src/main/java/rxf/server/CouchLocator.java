package rxf.server;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.SynchronousQueue;

import com.google.web.bindery.requestfactory.shared.Locator;
import one.xio.HttpMethod;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static rxf.server.BlobAntiPatternObject.GSON;
import static rxf.server.BlobAntiPatternObject.createCouchConnection;
import static rxf.server.BlobAntiPatternObject.executeCouchRequest;
import static rxf.server.BlobAntiPatternObject.recycleChannel;

/**
 * User: jim
 * Date: 5/10/12
 * Time: 7:37 AM
 */
public abstract class CouchLocator<T> extends Locator<T, String> {

  private String orgname = "rxf_";//default

  public String getPathPrefix(   ) {
    return getOrgname()+ getDomainType().getSimpleName().toLowerCase();
  }

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
  public T create(Class<? extends T> clazz) {
    String format = "POST " +
        /**
         * mandatory
         */
        '/'
        + getPathPrefix()
        +
        /**
         * mandatory
         */'/'
        + " HTTP/1.1\r\nContent-Type: application/json\r\nContent-Length: 2" +
//        "\r\n" + "Debug: " + wheresWaldo().trim() +
        "\r\n\r\n{}";
    SocketChannel couchConnection = null;
    T ret = null;
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
      recycleChannel(couchConnection);
    }
    return ret;
  }

  @Override
  public T find(Class<? extends T> clazz, String id) {

    String s = null;
    try {
      SocketChannel channel = createCouchConnection();
      String take;
      try {
        SynchronousQueue<String> retVal = new SynchronousQueue<String>();
        HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, BlobAntiPatternObject.fetchJsonByPath(getPathPrefix() + '/' + id, channel, retVal));
        take = retVal.take();
      } finally {
        recycleChannel(channel);
      }
      s = take;
    } catch ( Exception ignored) {

    }

    return GSON.fromJson(s, getDomainType());
  }

  /**
   * used by CouchAgent to create event channels on entities by sending it a locator
   *
   * @return
   */
  @Override
  abstract public Class<T> getDomainType();

  @Override
  abstract public String getId(T domainObject);

  @Override
  public Class<String> getIdType() {
    return String.class;
  }

  @Override
  abstract public Object getVersion(T domainObject);

  public String getOrgname() {
    return orgname;
  }
}