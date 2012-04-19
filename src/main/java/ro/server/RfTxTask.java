package ro.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.Callable;

import com.google.web.bindery.requestfactory.server.ServiceLayer;
import com.google.web.bindery.requestfactory.server.SimpleRequestProcessor;
import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;

import static one.xio.HttpMethod.UTF8;

/**
 * User: jim
 * Date: 4/18/12
 * Time: 1:21 PM
 */
class RfTxTask implements Callable<Object>, AsioVisitor {
  public static final SimpleRequestProcessor SIMPLE_REQUEST_PROCESSOR = new SimpleRequestProcessor(ServiceLayer.create());
  public int total;
  public LinkedList<ByteBuffer> byteBufferLinkedList;
  public int remaining;
  public ByteBuffer headers;
  private final SelectionKey key;

  public RfTxTask(SelectionKey key) {
    this.key = key;
  }

  public void pileOnBufferSegment(ByteBuffer dst, int read) throws InterruptedException {
    byteBufferLinkedList.add(dst);
    remaining -= read;
    if (0 > remaining) {
      throw new Error("bad code");
    } else {
      if (0 == remaining) {
        ByteBuffer byteBuffer = gatherBuffers();
        processBuffer(byteBuffer);
      }
    }
  }

  ByteBuffer gatherBuffers() throws InterruptedException {
    final ByteBuffer fbuf = ByteBuffer.allocateDirect(total);
    int size = byteBufferLinkedList.size();

    Collection<Callable<ByteBuffer>> r = new ArrayList<Callable<ByteBuffer>>();
    int x = 0;
    ListIterator<ByteBuffer> iterator1 = byteBufferLinkedList.listIterator();
    while (iterator1.hasNext()) {
      final ByteBuffer byteBuffer = iterator1.next();
      iterator1.remove();
      int limit = byteBuffer.limit();

      final int finalX = x;
      Callable<ByteBuffer> byteBufferCallable = new Callable<ByteBuffer>() {
        @Override
        public ByteBuffer call() throws Exception {
          ByteBuffer p = (ByteBuffer) fbuf.duplicate().position(finalX);
          p.put(byteBuffer);

          return p;  //todo: verify for a purpose
        }
      };
      r.add(byteBufferCallable);
      x += byteBuffer.limit();
    }
    KernelImpl.EXECUTOR_SERVICE.invokeAll(r);
    return fbuf;
  }

  public void bisectFirstPacketIntoHeaders(ByteBuffer dst) throws InterruptedException {
    boolean eol = false;
    while (dst.hasRemaining()) {
      byte b = dst.get();
      if ('\n' == b) {
        if (eol) {
          headers = (ByteBuffer) dst.duplicate().flip();
          System.err.println("h: " + UTF8.decode(headers).toString());
          ByteBuffer cl = headers.duplicate();

          CharBuffer contentLength = null;
          try {
            Map<String, int[]> hm = HttpHeaders.getHeaders(headers);
            System.err.println(Arrays.toString(hm.keySet().toArray()));
            int[] ints = hm.get("Content-Length");

            total = Integer.parseInt(HttpMethod.UTF8.decode((ByteBuffer) headers.limit(ints[1]).position(ints[0])).toString().trim());
            ByteBuffer slice = dst.slice();
            remaining = total - slice.remaining();
            switch (remaining) {
              case 0:
                processBuffer(slice);
                break;
              default:
                byteBufferLinkedList = new LinkedList<ByteBuffer>();
                byteBufferLinkedList.add(slice);
                break;
            }
            break;
          } catch (Exception e) {
            e.printStackTrace();  //todo: verify for a purpose
          } finally {
          }

        } else {
          eol = true;
        }
      } else {
        if ('\r' != b) {
          eol = false;
        }
      }
    }
  }


  private void processBuffer(ByteBuffer byteBuffer) {
    String s = UTF8.decode(byteBuffer).toString();
    System.err.println("+++ headers " + UTF8.decode(headers).toString());
    System.err.println("+++ process " + s);


    final String process = SIMPLE_REQUEST_PROCESSOR.process(s);


    int length = process.length();
    String s1 = "HTTP/1.1 200 OK\r\n" +
        "Content-Type: application/json\r\n" +
        "Content-Length: " + length + "\r\n\r\n";

    try {
      String debug = s1 + process;
      ((SocketChannel) key.channel()).write(UTF8.encode(debug));
      System.err.println("debug: " + debug);
    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }

  public Object call() throws Exception {

    try {
      Object attachment = key.attachment();
      ByteBuffer dst1 = null;
      if (attachment instanceof ByteBuffer) {
        resolve((ByteBuffer) attachment);
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {

      key.attach(this);
    }
    return null;
  }

  void resolve(ByteBuffer attachment) throws InterruptedException {
    ByteBuffer dst1;
    dst1 = (ByteBuffer) attachment;
    dst1.rewind();
    byteBufferLinkedList = null;
    if (null == headers) {
      bisectFirstPacketIntoHeaders(dst1);
    } else {
      pileOnBufferSegment(dst1, dst1.limit());
    }
  }

  @Override
  public void onRead(SelectionKey selectionKey) {
    try {
      SocketChannel channel = (SocketChannel) key.channel();
      ByteBuffer dst = ByteBuffer.allocateDirect(channel.socket().getReceiveBufferSize());
      int read = channel.read(dst);
      dst.flip();
      pileOnBufferSegment(dst, read);
    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    } catch (InterruptedException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }

  @Override
  public void onConnect(SelectionKey selectionKey) {
    //todo: verify for a purpose
  }

  @Override
  public void onWrite(SelectionKey selectionKey) {
    //todo: verify for a purpose
  }

  @Override
  public void onAccept(SelectionKey selectionKey) {
    //todo: verify for a purpose
  }
}
