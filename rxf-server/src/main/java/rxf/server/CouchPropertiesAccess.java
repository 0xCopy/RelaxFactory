package rxf.server;

import java.lang.reflect.InvocationTargetException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Exchanger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.gson.reflect.TypeToken;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;

import static java.nio.channels.SelectionKey.OP_CONNECT;
import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static rxf.server.BlobAntiPatternObject.EXECUTOR_SERVICE;
import static rxf.server.BlobAntiPatternObject.GSON;
import static rxf.server.BlobAntiPatternObject.UTF8CHARSET;
import static rxf.server.BlobAntiPatternObject.createCouchConnection;
import static rxf.server.BlobAntiPatternObject.getPathIdVer;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;
import static rxf.server.BlobAntiPatternObject.getSendBufferSize;
import static rxf.server.BlobAntiPatternObject.inferRevision;

/**
 * User: jim
 * Date: 5/10/12
 * Time: 7:59 AM
 */
public abstract class CouchPropertiesAccess<T> {
  private CouchLocator<T> memento;

  private String callMethod(T domainObject, String method) {
    Object id = null;
    try {
      id = ((Class<T>) new TypeToken<T>() {
      }.getType()).getMethod(method).invoke(domainObject);
    } catch (IllegalAccessException e) {
      e.printStackTrace();  //
    } catch (InvocationTargetException e) {
      e.printStackTrace();  //
    } catch (NoSuchMethodException e) {
      e.printStackTrace();  //
    }
    return (String) id;
  }

  public CouchPropertiesAccess() {
  }

  public String getSessionProperty(String eid, String key) {
    try {
      String path = getLocator().getPathPrefix() + '/' + eid;
      return BlobAntiPatternObject.getGenericDocumentProperty(path, key);
    } catch (Exception e) {
      e.printStackTrace();  //
    }
    return null;
  }

  /**
   * @param key
   * @param value
   * @return new version string
   * @throws java.nio.channels.ClosedChannelException
   *
   * @throws InterruptedException
   */
  public String setSessionProperty(String key, final String value) throws ClosedChannelException, InterruptedException, TimeoutException {
    final Exchanger<String> outer = new Exchanger<String>();

    HttpMethod.enqueue(createCouchConnection(), OP_WRITE | OP_CONNECT, new AsioVisitor.Impl() {

      @Override
      public void onWrite(SelectionKey key) throws Exception {
        final String pathPrefix = getLocator().getPathPrefix();
        final String path = pathPrefix;
        final String id = BlobAntiPatternObject.getSessionCookieId();
        String ver = null;
        BlobAntiPatternObject.getPathIdVer(path, id);
        final String hdr = "GET " + path + " HTTP/1.1\r\nAccept: */*\r\n\r\n";
        final SelectableChannel channel = key.channel();
        ((SocketChannel) channel).write(UTF8CHARSET.encode(hdr));
        key.interestOps(OP_READ).selector().wakeup();
        key.attach(new Impl() {
          @Override
          public void onRead(final SelectionKey key) throws Exception {
            final ByteBuffer cursor = ByteBuffer.allocateDirect(getReceiveBufferSize());
            final int read = ((SocketChannel) channel).read(cursor);
            if (-1 == read) {

              outer.exchange(null, 3, TimeUnit.SECONDS);
              return;
            }
            cursor.flip();
            final Rfc822HeaderState rfc822HeaderPrefix = new Rfc822HeaderState(RfPostWrapper.CONTENT_LENGTH);
            final String rescode = rfc822HeaderPrefix.apply(cursor).cookies(BlobAntiPatternObject.MYGEOIPSTRING).getPathRescode();
            if (!rescode.equals("200")) {
              outer.exchange(null, 3, TimeUnit.SECONDS);
              return;
            }
            Callable<Void> callable = new Callable<Void>() {
              public Exchanger<ByteBuffer> inner = new Exchanger<ByteBuffer>();

              public Void call() throws Exception {
                EXECUTOR_SERVICE.submit(new Runnable() {
                  public void run() {
                    final int remaining = Integer.parseInt((String) rfc822HeaderPrefix.getHeaderStrings().get(RfPostWrapper.CONTENT_LENGTH));
                    final ByteBuffer payload;
//                  final Exchanger<ByteBuffer> inner;
                    if (remaining == cursor.remaining()) {
                      try {
                        inner.exchange(cursor.slice());
                      } catch (InterruptedException e) {
                        e.printStackTrace();  //todo: verify for a purpose
                      }
                    } else {
                      payload = ByteBuffer.allocateDirect(remaining).put(cursor);

                      key.attach(new Impl() {
                        @Override
                        public void onRead(SelectionKey key) throws Exception {
                          ((SocketChannel) channel).read(payload);

                          if (!payload.hasRemaining()) {
                            inner.exchange((ByteBuffer) payload.flip());
                          } //  V
                        }   //  V
                      });   //  V
                    }       //  V
                  }         //  V
                });         //  V
                final ByteBuffer exchange = inner.exchange(null, 3, TimeUnit.SECONDS);
                final String json = UTF8.decode(exchange).toString();


                final Map data = BlobAntiPatternObject.GSON.fromJson(UTF8.decode(exchange).toString().trim(), Map.class);
                final String ver = inferRevision(data);
                final String pathIdVer = getPathIdVer(path, id, ver);
                data.put(key, value);
                final String outbound = GSON.toJson(data);
                final ByteBuffer endBuffer = UTF8.encode(outbound);

                String hdr = "PUT " + pathIdVer + " HTTP/1.1\r\nContent-Length: " + endBuffer.limit() +
                    "\r\nAccept: */*\r\n\r\n" + json;
                final ByteBuffer encode = UTF8.encode(hdr);//short


                HttpMethod.enqueue(createCouchConnection(), OP_WRITE, new Impl() {
                  @Override
                  public void onWrite(final SelectionKey key) throws Exception {
                    final SocketChannel channel = (SocketChannel) key.channel();
                    channel.write(encode);
                    if (!encode.hasRemaining()) {
                      final Buffer clear = encode.clear();
                      key.interestOps(OP_READ).attach(new Impl() {
                        @Override
                        public void onRead(SelectionKey key) throws Exception {
                          final ByteBuffer dst = ByteBuffer.allocateDirect(getSendBufferSize());
                          channel.read(dst);

                          final Rfc822HeaderState ETag = new Rfc822HeaderState("ETag").apply((ByteBuffer) dst.flip());
                          if (rfc822HeaderPrefix.getPathRescode().startsWith("20")) {
                            outer.exchange(rfc822HeaderPrefix.getCookieStrings().get("ETag"));
                          } else {
                            outer.exchange("error: " + UTF8.decode((ByteBuffer) dst.rewind()).toString().trim());
                          }
                        }
                      });

                    }
                  }
                });

                return null;
              }
            };
            EXECUTOR_SERVICE.submit(callable);

          }
        });
      }
    });
    outer.exchange(null, 3, TimeUnit.SECONDS);
    return key;
  }

  abstract
  public CouchLocator<T> getLocator();


}
