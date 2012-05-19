package rxf.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.SynchronousQueue;

import com.google.gson.JsonSyntaxException;
import com.google.web.bindery.requestfactory.shared.Locator;
import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;

import static java.lang.Math.max;
import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.getSelector;
import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.GSON;
import static rxf.server.BlobAntiPatternObject.arrToString;
import static rxf.server.BlobAntiPatternObject.createCouchConnection;
import static rxf.server.BlobAntiPatternObject.getPathIdVer;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;
import static rxf.server.BlobAntiPatternObject.getSendBufferSize;
import static rxf.server.BlobAntiPatternObject.moveCaretToDoubleEol;
import static rxf.server.BlobAntiPatternObject.recycleChannel;

/**
 * User: jim
 * Date: 5/10/12
 * Time: 7:37 AM
 */
public abstract class CouchLocator<T> extends Locator<T, String> {

  private String orgname = "rxf_";//default


  public String getPathPrefix() {
    return getOrgname() + getDomainType().getSimpleName().toLowerCase();
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
    try {
      return clazz.newInstance();
    } catch (InstantiationException e) {
      e.printStackTrace();  //todo: verify for a purpose
    } catch (IllegalAccessException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    throw new UnsupportedOperationException("no default ctor " + HttpMethod.wheresWaldo(3));
  }

  @Override
  public T find(Class<? extends T> clazz, String id) {

    String s = null;
    try {
      SocketChannel channel = createCouchConnection();
      String take;
      try {
        SynchronousQueue<String> retVal = new SynchronousQueue<String>();
        HttpMethod.enqueue(channel, OP_CONNECT | OP_WRITE, BlobAntiPatternObject.fetchJsonByPath(channel, retVal));
        take = retVal.take();
      } finally {
        recycleChannel(channel);
      }
      s = take;
    } catch (Exception ignored) {

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

  public CouchTx persist(final T domainObject) throws Exception {
    HttpMethod method = HttpMethod.POST;
    String[] pathIdPrefix = {getPathPrefix()};
    String deliver, payload = deliver = GSON.toJson(domainObject);
    final ByteBuffer encode1 = UTF8.encode(deliver);


    Map cheat = null;
    cheat = GSON.fromJson(payload, Map.class);
    boolean hasRev = cheat.containsKey("_rev");
    boolean hasId = cheat.containsKey("_id");
    Object id = cheat.get("_id");
    if (hasId)
      if (!hasRev) {
        pathIdPrefix = new String[]{getPathPrefix(), (String) id};
        method = HttpMethod.HEAD;
      } else {
        Object rev = cheat.get("_rev");
        pathIdPrefix = new String[]{getPathPrefix(), (String) id, (String) rev,};
        method = HttpMethod.PUT;
      }

    final SynchronousQueue<ByteBuffer> sq = new SynchronousQueue<ByteBuffer>();

    switch (method) {
      case HEAD: {
        String m = new StringBuilder().append(method.name()).append(" ").append(getPathIdVer(pathIdPrefix)).append(" HTTP/1.1\r\n\r\n").toString();
        final String finalM1 = m;
        HttpMethod.enqueue(createCouchConnection(), OP_WRITE, new AsioVisitor.Impl() {


          @Override
          public void onRead(SelectionKey key) throws Exception {
//                        fetch from  ETag: "3-9a5fe45b4e065e3604f1f746816c1926"
            ByteBuffer headerBuf = ByteBuffer.allocateDirect(getReceiveBufferSize());
            final SocketChannel channel = (SocketChannel) key.channel();
            int read = channel.read(headerBuf);

            headerBuf.flip();
            while (headerBuf.hasRemaining() && '\n' == (headerBuf.get())) ;//pv

            int mark = headerBuf.position();
            ByteBuffer methodBuf = (ByteBuffer) headerBuf.duplicate().flip().position(mark);
            String[] resCode = UTF8.decode(methodBuf).toString().trim().split(" ");
            if ((resCode[1]).startsWith("20")) {
              int[] headers = HttpHeaders.getHeaders((ByteBuffer) headerBuf.clear()).get("ETag");

              sq.put((ByteBuffer) headerBuf.position(headers[0]).limit(headers[1]));
            }
          }

          @Override
          public void onWrite(SelectionKey key) throws Exception {
            final SocketChannel channel = (SocketChannel) key.channel();
            channel.write(UTF8.encode(finalM1));
            key.interestOps(OP_READ);
          }
        });

        ByteBuffer take = sq.take();
        String newVer = UTF8.decode(take).toString();
        System.err.println("HEAD appends " + arrToString(pathIdPrefix) + " with " + newVer);
        pathIdPrefix = new String[]{getPathPrefix(), (String) id, newVer};
      }
      case PUT:
      case POST: {
        try {
          StringBuilder m = new StringBuilder().append(method.name()).append(" ").append(getPathIdVer(pathIdPrefix)).append(" HTTP/1.1\r\n").append("Content-Length: ").append(encode1.limit()).append("\r\nAccept: */*\r\nContent-Type: application/json\r\n\r\n");
          final ByteBuffer encode = UTF8.encode(m.toString());


          final ByteBuffer buf = (ByteBuffer) ByteBuffer.allocateDirect(max(getSendBufferSize(), encode.limit() + encode1.limit())).put(encode).put(encode1).flip();

          final ByteBuffer take = EXECUTOR_SERVICE.submit(new Callable<ByteBuffer>() {
            public ByteBuffer call() {
              try {
                final Selector selector = getSelector();
                createCouchConnection().register(selector, OP_CONNECT | OP_WRITE).attach(new AsioVisitor.Impl() {
                  @Override
                  public void onWrite(SelectionKey key) throws IOException {
                    final SocketChannel channel = (SocketChannel) key.channel();
                    try {
                      channel.write(buf);
                    } catch (IOException e) {
                      channel.socket().close();
                    }
                    if (!buf.hasRemaining()) {
                      buf.clear();
                      key.selector().wakeup();
                      key.interestOps(OP_READ).attach(new Impl() {
                        @Override
                        public void onRead(SelectionKey key) throws Exception {

                          try {
                            final SocketChannel channel = (SocketChannel) key.channel();
                            channel.read(buf);
                            int position = moveCaretToDoubleEol((ByteBuffer) buf.flip()).position();
                            if (buf.hasRemaining()) {
                              ByteBuffer headers = ((ByteBuffer) buf.duplicate().flip()).slice();
                              Map<String, int[]> map = HttpHeaders.getHeaders(headers);
                              if (map.containsKey("Content-Length")) {
                                int[] bounds = map.get("Content-Length");
                                String trim = UTF8.decode((ByteBuffer) headers.clear().position(bounds[0]).limit(bounds[1])).toString().trim();
                                long l = Long.parseLong(trim);
                                final ByteBuffer cursor = ByteBuffer.allocateDirect((int) l).put(buf);
                                if (!cursor.hasRemaining()) {
                                  sq.put((ByteBuffer) cursor.flip());
                                  return;
                                }
                                Impl prev = this;
                                key.attach(new Impl() {
                                  @Override
                                  public void onRead(SelectionKey key) throws Exception {
                                    try {
                                      final SocketChannel channel = (SocketChannel) key.channel();
                                      channel.read(cursor);
                                      if (!cursor.hasRemaining()) {
                                        sq.put((ByteBuffer) cursor.flip());
                                      }
                                    } catch (Throwable e) {
                                      e.printStackTrace();  //todo: verify for a purpose
                                    }
                                  }
                                });
                              }
                            } else {
                              buf.clear().position(position);
                            }//still reading headers.
                          } catch (RuntimeException e) {
                            e.printStackTrace();  //todo: verify for a purpose
                          }

                        }
                      });
                    }
                  }
                });
              } catch (RuntimeException e) {
                e.printStackTrace();  //todo: verify for a purpose
              } catch (Exception e) {
                e.printStackTrace();  //todo: verify for a purpose
              }

              try {
                return sq.take();
              } catch (RuntimeException e) {
                e.printStackTrace();  //todo: verify for a purpose
              } catch (InterruptedException e) {
                e.printStackTrace();  //todo: verify for a purpose
              }
              return null;
            }
          }).get();
          return GSON.fromJson(UTF8.decode(take).toString(), CouchTx.class);
        } catch (InterruptedException e) {
          e.printStackTrace();  //todo: verify for a purpose
        } catch (ExecutionException e) {
          e.printStackTrace();  //todo: verify for a purpose
        } catch (JsonSyntaxException e) {
          e.printStackTrace();  //todo: verify for a purpose
        }
      }
      default:
        return null;
    }
  }

  List<T> findAll() {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }

  List<T> search(String queryParm) {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }

  /**
   * tbd -- longpolling feed rf token
   *
   * @param queryParm
   * @return
   */
  String searchAsync(String queryParm) {
    return null;  //To change body of created methods use File | Settings | File Templates.
  }
}