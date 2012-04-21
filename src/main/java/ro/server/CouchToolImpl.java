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

  public static void setProperty(String id, String key, String value) {

  }

  public static LinkedHashMap fetchMapById(final String key) throws IOException, InterruptedException {
    String take = fetchJsonById(key);
    return CouchChangesClient.GSON.fromJson(take, LinkedHashMap.class);
  }

  public static String fetchJsonById(final String key) throws IOException, InterruptedException {
    final SocketChannel channel = CouchChangesClient.createConnection();
    final SynchronousQueue[] sq = {new SynchronousQueue()};
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
      public void onWrite(SelectionKey selectionKey) {
        try {
          String format = (MessageFormat.format("GET /{0}/{1} HTTP/1.1\r\n\r\n", INSTANCE, key));
          channel.write(UTF8.encode(format));
        } catch (IOException e) {
          e.printStackTrace();  //todo: verify for a purpose
        }
//          System.err.println(format);
        selectionKey.attach(new AsioVisitor() {
          public long remaining;
          public long total;

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
                sq[0].put(UTF8.decode(payload).toString().trim());
              } else {
                final LinkedList<ByteBuffer> ll = new LinkedList<ByteBuffer>();
                sq[0] = new SynchronousQueue<String>();
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
                        sq[0].put(UTF8.decode(payload).toString().trim());
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
        });
        selectionKey.interestOps(SelectionKey.OP_READ);
      }

      @Override
      public void onAccept(SelectionKey key) {
      }
    });

    return (String) sq[0].take();
  }
}