package ro.server.rf;

import java.io.IOException;
import java.net.SocketException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;

import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import ro.server.KernelImpl;

import static ro.server.CouchChangesClient.GSON;

/**
 * User: jim
 * Date: 4/17/12
 * Time: 8:18 PM
 */
public abstract class SessionLocatorVisitor<TxPojo, DataPojo> implements AsioVisitor {
  public DataPojo data;
  protected BlockingQueue<TxPojo> blockingQueue;
  protected int receiveBufferSize;
  public ByteBuffer headers;
  protected final SocketChannel channel;
  protected int total;
  protected int remaining;
  private boolean active;
  protected LinkedList<ByteBuffer> byteBufferLinkedList;

  public SessionLocatorVisitor(final BlockingQueue<TxPojo> blockingQueue, SocketChannel channel) {
    this.blockingQueue = blockingQueue;
    try {
      receiveBufferSize = channel.socket().getReceiveBufferSize();
    } catch (SocketException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
    this.channel = channel;
  }

  public void processBuffer(SelectionKey key, ByteBuffer slice, TxPojo memento) throws InterruptedException {
    String json = HttpMethod.UTF8.decode(slice).toString();
    System.err.println("<<<" + json);
    TxPojo tx = (TxPojo) GSON.fromJson(json, memento.getClass());
    handle(json, tx);
    blockingQueue.put(tx);
    key.cancel();
  }

  protected abstract void handle(String json, TxPojo couchTx);

  @Override
  final
  public void onRead(SelectionKey key) {
    try {
      ByteBuffer dst = ByteBuffer.allocateDirect(receiveBufferSize);
      int read = channel.read(dst);
      dst.flip();
      System.err.println(HttpMethod.UTF8.decode(dst));
      dst.rewind();
      byteBufferLinkedList = null;
      if (null == headers) {
        bisectFirstPacketIntoHeaders(key, dst);
      } else {
        pileOnBufferSegment(key, dst, read);
      }

    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    } catch (InterruptedException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }

  private void pileOnBufferSegment(SelectionKey key, ByteBuffer dst, int read) throws InterruptedException {
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
        KernelImpl.EXECUTOR_SERVICE.invokeAll(r);
        processBuffer(key, fbuf, getMemento());
      }
    }
  }

  abstract TxPojo getMemento();

  public void bisectFirstPacketIntoHeaders(SelectionKey key, ByteBuffer dst) throws InterruptedException {
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

        Map<String, int[]> headers1 = HttpHeaders.getHeaders((ByteBuffer) cl.rewind());
        System.err.println((headers1.keySet().toString()));

        int[] ints = headers1.get("Content-Length");
        Buffer position = cl.clear().limit(ints[1]).position(ints[0]);
        String decode = HttpMethod.UTF8.decode((ByteBuffer) position).toString().trim();
//        headers1.keySet()
        total = Integer.parseInt(decode);


        ByteBuffer slice = dst.slice();
        remaining = total - slice.remaining();

        switch (remaining) {
          case 0:
            processBuffer(key, slice, getMemento());
            break;
          default:
            byteBufferLinkedList = new LinkedList<ByteBuffer>();
            byteBufferLinkedList.add(slice);
            break;
        }
        break;
      }
    }
  }
}
