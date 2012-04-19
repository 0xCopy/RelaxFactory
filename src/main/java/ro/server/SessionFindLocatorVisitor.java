package ro.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
import java.util.concurrent.BlockingQueue;

import one.xio.HttpMethod;
import ro.model.RoSession;

import static ro.server.CouchChangesClient.GSON;

/**
* User: jim
* Date: 4/17/12
* Time: 9:58 PM
*/
public class SessionFindLocatorVisitor extends SessionLocatorVisitor<RoSession, RoSession> {
  private final String id;

  public SessionFindLocatorVisitor(BlockingQueue<RoSession> blockingQueue, SocketChannel channel, String id) {
    super(blockingQueue, channel);
    this.id = id;
  }
/*
  @Override
  public void onRead(SelectionKey key) {
    try {
      ByteBuffer dst = ByteBuffer.allocateDirect(receiveBufferSize);
      int read = channel.read(dst);
      dst.flip();
      System.err.println(RoSessionLocator.UTF_8.decode(dst));
      dst.rewind();
      byteBufferLinkedList = null;
      if (null == headers) {
        boolean eol = false;
        while (dst.hasRemaining()) {
          byte b = dst.get();
          if ('\n' != b) {
            if ('\r' != b) {
              eol = false;
            }
          } else if (!eol) {
            eol = true;
          } else {
            headers = (ByteBuffer) dst.duplicate().flip();
            ByteBuffer cl = headers.duplicate();

            total = Integer.parseInt(HttpHeaders.getContentLength(cl).toString());
            ByteBuffer slice = dst.slice();
            remaining = total - slice.remaining();

            switch (remaining) {
              case 0:
                processBuffer(key, slice, RoSessionLocator.MEMENTO);
                break;
              default:
                byteBufferLinkedList = new LinkedList<ByteBuffer>();
                byteBufferLinkedList.add(slice);
                break;
            }
            break;
          }
        }
      } else {
        byteBufferLinkedList.add(dst);
        remaining -= read;
        if (remaining < 0) {
          throw new Error("bad code");
        } else {
          if (0 == remaining) {
            final ByteBuffer fbuf = ByteBuffer.allocateDirect(total);
            int size = byteBufferLinkedList.size();

            Collection<Callable<ByteBuffer>> r = new ArrayList<Callable<ByteBuffer>>();
            int x = 0;
            for (final ByteBuffer byteBuffer : byteBufferLinkedList) {
              int limit = byteBuffer.limit();

              final int finalX = x;
              Callable<ByteBuffer> byteBufferCallable = new Callable<ByteBuffer>() {
                @Override
                public ByteBuffer call() throws Exception {
                  ByteBuffer p = (ByteBuffer) fbuf.duplicate().position(finalX);
                  p.put((ByteBuffer) byteBuffer.position(finalX));
                  return fbuf;  //todo: verify for a purpose
                }
              };
              r.add(byteBufferCallable);
              x += byteBuffer.limit();
            }
            RoSessionLocator.EXECUTOR_SERVICE.invokeAll(r);
            processBuffer(key, fbuf, RoSessionLocator.MEMENTO);
          }
        }
      }

    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    } catch (InterruptedException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }


  }*/

  @Override
  RoSession getMemento() {
    return RoSessionLocator.MEMENTO;
  }

  @Override
  public void onConnect(SelectionKey key) {
    try {

      boolean b = channel.finishConnect();
      if (b) {
        data = RoSession.createSession();
        String cs = GSON.toJson(data);
//          System.err.println(cs);
        String format = MessageFormat.format("GET /rosession/{0} HTTP/1.1\r\n\r\n", id);
        ByteBuffer encode = HttpMethod.UTF8.encode(format);
        channel.write(encode);
        System.err.println(format);
        key.interestOps(SelectionKey.OP_READ);
      }
    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }

  @Override
  public void onWrite(SelectionKey key) {
    //todo: verify for a purpose
  }

  @Override
  public void onAccept(SelectionKey key) {
    //todo: verify for a purpose
  }

  @Override
  protected void handle(String json, RoSession couchTx) {
    data=couchTx;
  }
}
