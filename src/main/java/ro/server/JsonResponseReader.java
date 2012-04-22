package ro.server;

import java.io.IOException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.concurrent.SynchronousQueue;

import one.xio.AsioVisitor;
import one.xio.HttpHeaders;

import static one.xio.HttpMethod.UTF8;

/**
 * User: jim
 * Date: 4/21/12
 * Time: 12:10 PM
 */
class JsonResponseReader implements AsioVisitor {
  public long remaining;
  public long total;
  private final SynchronousQueue synchronousQueue;

  public JsonResponseReader(SynchronousQueue<String> synchronousQueue) {
    this.synchronousQueue = synchronousQueue;
  }


  @Override
  public void onRead(SelectionKey key) {
    final SocketChannel channel = (SocketChannel) key.channel();
    try {
      final int receiveBufferSize = channel.socket().getReceiveBufferSize();
      ByteBuffer dst = ByteBuffer.allocateDirect(receiveBufferSize);
      int read = channel.read(dst);
      dst.flip();
      byte b = 0;
      boolean eol = false;

      moveCaretToDoubleEol(dst);
      int[] bounds = HttpHeaders.getHeaders((ByteBuffer) dst.duplicate().flip()).get("Content-Length");
      if (null == bounds)
        return;
      total = Long.parseLong(UTF8.decode((ByteBuffer) dst.duplicate().limit(bounds[1]).position(bounds[0])).toString().trim());
      remaining = total - dst.remaining();

      ByteBuffer payload;
      if (remaining <= 0) {
        payload = dst.slice();
        key.attach(null);
        synchronousQueue.put(UTF8.decode(payload).toString().trim());

      } else {
        final LinkedList<ByteBuffer> ll = new LinkedList<ByteBuffer>();
//
//    synchronousQueue.clear();
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
                key.attach(null);

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

  public static void moveCaretToDoubleEol(ByteBuffer dst) {
    byte b;
    boolean eol = false;
    while (dst.hasRemaining() && (b = dst.get()) != -1) {
      if (b != '\n') {
        if (b != '\r') {
          eol = false;
        }
      } else {
        if (!eol) {
          eol = true;
        } else {
          break;
        }
      }
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
