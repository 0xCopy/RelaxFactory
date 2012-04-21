package ro.server;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.SynchronousQueue;

import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import ro.model.RoSession;

import static one.xio.HttpMethod.UTF8;

/**
 * User: jim
 * Date: 4/20/12
 * Time: 5:04 PM
 */
public class CouchToolImpl {

  public static final RoSessionLocator RO_SESSION_LOCATOR = new RoSessionLocator();
  public static final String INSTANCE = "rosession";

  /**
   * @param id
   * @param key
   * @return
   * @throws InterruptedException
   */
  static public String getProperty(String id, final String key) throws InterruptedException {
    RoSession ret;
    LinkedHashMap linkedHashMap = null;
    try {
      linkedHashMap = fetchMapById(key);
    } catch (ClosedChannelException e) {
    } catch (IOException e) {
    }
    assert linkedHashMap != null;
    return (String) linkedHashMap.get(key);
  }

  /**
   * @param json
   * @return new _rev
   */
  public static CouchTx sendJson(final String json, final String... idver) throws IOException {
    final SocketChannel channel = CouchChangesClient.createConnection();
    final SynchronousQueue synchronousQueue = new SynchronousQueue();
    HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, new AsioVisitor() {

      @Override
      public void onWrite(final SelectionKey selectionKey) {
        String method;
        String call;
        method= idver.length == 0 ? "POST /" : "PUT /";

        String identifier = "";
        for (int i = 0; i < idver.length; i++) {
          String s = idver[i];
          switch (i) {
            case 0:
              identifier += s;
              break;
            case 1:
              identifier+= "?rev=" + s;
              break;
          }
        }

        call= MessageFormat.format("{0}{1} HTTP/1.1\r\nContent-Type: application/json\r\nContent-Length: {2}\r\n\r\n{3}", method, identifier, json.length(), json);
        ByteBuffer encode = UTF8.encode(call);
        SocketChannel channel = (SocketChannel) selectionKey.channel();
        try {
          channel.write(encode);
          selectionKey.attach(new JsonResponseReader(synchronousQueue, selectionKey));
        selectionKey.interestOps(SelectionKey.OP_READ);

        } catch (IOException e) {
          e.printStackTrace();  //todo: verify for a purpose
        }

      }

      @Override
      public void onAccept(SelectionKey key) {

      }

      @Override
      public void onRead(SelectionKey key) {

      }

      @Override
      public void onConnect(SelectionKey key) {
        try {
          if (((SocketChannel) key.channel()).finishConnect()) {
            key.interestOps(SelectionKey.OP_WRITE);
          }
        } catch (IOException e) {
          e.printStackTrace();  //todo: verify for a purpose
        }
      }
    });


    try {
      return CouchChangesClient.GSON.fromJson(String.valueOf(synchronousQueue.take()), CouchTx.class);
    } catch (InterruptedException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    return null;
  }

  public static LinkedHashMap fetchMapById(final String key) throws IOException, InterruptedException {
    String take = fetchJsonById(key);
    return CouchChangesClient.GSON.fromJson(take, LinkedHashMap.class);
  }

  public static String fetchJsonById(final String key) throws IOException, InterruptedException {
    final SocketChannel channel = CouchChangesClient.createConnection();
    final SynchronousQueue synchronousQueue = new SynchronousQueue();
    HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, new AsioVisitor() {
      @Override
      public void onRead(SelectionKey key) {
      }

      @Override
      public void onConnect(SelectionKey key) {
        try {
          if (((SocketChannel) key.channel()).finishConnect()) {
            key.interestOps(SelectionKey.OP_WRITE);
          }
        } catch (IOException e) {
          e.printStackTrace();  //todo: verify for a purpose
        }
      }

      @Override
      public void onWrite(final SelectionKey selectionKey) {
        try {
          String format = (MessageFormat.format("GET /{0}/{1} HTTP/1.1\r\n\r\n", INSTANCE, key));
          channel.write(UTF8.encode(format));
        } catch (IOException e) {
          e.printStackTrace();  //todo: verify for a purpose
        }
//          System.err.println(format);
        selectionKey.attach(new JsonResponseReader(synchronousQueue, selectionKey));
        selectionKey.interestOps(SelectionKey.OP_READ);
      }

      @Override
      public void onAccept(SelectionKey key) {
      }
    });

    return (String) synchronousQueue.take();
  }

  private static class JsonResponseReader implements AsioVisitor {
    public long remaining;
    public long total;
    private final SynchronousQueue synchronousQueue;
    private final SelectionKey selectionKey;

    public JsonResponseReader(SynchronousQueue synchronousQueue, SelectionKey selectionKey) {
      this.synchronousQueue = synchronousQueue;
      this.selectionKey = selectionKey;
    }

    @Override
    public void onRead(SelectionKey key) {
      final SocketChannel channel = (SocketChannel) key.channel();
      try {
        final int receiveBufferSize = channel.socket().getReceiveBufferSize();
        ByteBuffer dst = ByteBuffer.allocateDirect(receiveBufferSize);
        int read = channel.read(dst);
        dst.flip();
        while (dst.hasRemaining()) {
          byte b = dst.get();
          boolean eol = false;
          if ('\n' != b) {
            if ('\r' != b) {
              eol = false;
            }
          } else if (!eol) {
            eol = true;
          } else {
            break;

          }

        }
        ByteBuffer headers = (ByteBuffer) dst.duplicate().flip();//doesn't handle gigantic headers beyond packet boundaries
        int[] ints = HttpHeaders.getHeaders(headers).get("Content-Length");
        total = Long.parseLong(UTF8.decode((ByteBuffer) headers.duplicate().limit(ints[1]).position(ints[0])).toString().trim());
        remaining = total - read;

        ByteBuffer payload;
        if (remaining <= 0) {
          payload = dst.slice();
          synchronousQueue.put(UTF8.decode(payload).toString().trim());
        } else {
          final LinkedList<ByteBuffer> ll = new LinkedList<ByteBuffer>();
//          synchronousQueue.clear();
          ll.add(dst.slice());
          key.attach(new AsioVisitor() {
            @Override
            public void onRead(SelectionKey key) {
              try {
                ByteBuffer payload = ByteBuffer.allocateDirect(receiveBufferSize);
                int read = channel.read(payload);
                ll.add(payload);
                remaining -= read;
                if (0 == remaining) {
                  payload = ByteBuffer.allocateDirect((int) total);
                  ListIterator<ByteBuffer> iter = ll.listIterator();
                  while (iter.hasNext()) {
                    ByteBuffer buffer = iter.next();
                    iter.remove();
                    payload.put((ByteBuffer) buffer.rewind());
                  }
                  selectionKey.attach(null);
                  selectionKey.interestOps(0);
                  synchronousQueue.put(UTF8.decode(payload).toString().trim());
                }
              } catch (IOException e) {
                e.printStackTrace();  //todo: verify for a purpose
              } catch (InterruptedException e) {
                e.printStackTrace();  //todo: verify for a purpose
              }
            }

            @Override
            public void onConnect(SelectionKey key) {
            }

            @Override
            public void onWrite(SelectionKey key) {
            }

            @Override
            public void onAccept(SelectionKey key) {
            }
          }

          );
          key.interestOps(SelectionKey.OP_READ);
        }

      } catch (SocketException e) {
        e.printStackTrace();  //todo: verify for a purpose
      } catch (IOException e) {
        e.printStackTrace();  //todo: verify for a purpose
      } catch (InterruptedException e) {
        e.printStackTrace();  //todo: verify for a purpose
      }

    }


    @Override
    public void onConnect(SelectionKey key) {
    }

    @Override
    public void onWrite(SelectionKey key) {
    }

    @Override
    public void onAccept(SelectionKey key) {
    }
  }
}