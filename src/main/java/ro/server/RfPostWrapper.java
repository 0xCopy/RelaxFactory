package ro.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
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
import static ro.server.KernelImpl.EXECUTOR_SERVICE;
import static ro.server.KernelImpl.ThreadLocalHeaders;
import static ro.server.KernelImpl.ThreadLocalSetCookies;

/**
 * a POST interception wrapper for http protocol cracking
 * User: jim
 * Date: 4/18/12
 * Time: 12:37 PM
 */
class RfPostWrapper implements AsioVisitor {


  public static final SimpleRequestProcessor SIMPLE_REQUEST_PROCESSOR = new SimpleRequestProcessor(ServiceLayer.create());

  @Override
  public void onRead(final SelectionKey key) {
    ThreadLocal<SelectionKey> key1 = new ThreadLocal<SelectionKey>();
    key1.set(key);
    try {
      SocketChannel channel = (SocketChannel) key.channel();
      int receiveBufferSize = channel.socket().getReceiveBufferSize();
      ByteBuffer dst = ByteBuffer.allocateDirect(receiveBufferSize);
      int read = channel.read(dst);
      if (-1 == read) {
        key.cancel();
      } else {
        dst.flip();
        try {
          ByteBuffer duplicate = dst.duplicate();
          while (duplicate.hasRemaining() && !Character.isWhitespace(duplicate.get())) ;
          String trim = UTF8.decode((ByteBuffer) duplicate.flip()).toString().trim();


          HttpMethod method = HttpMethod.valueOf(trim);
          dst.limit(read).position(0);
          key.attach(dst);
          switch (method) {
            case POST:

              EXECUTOR_SERVICE.submit(new RfCallable(key));
              break;
            default:
              method.onRead(key);
              break;
          }

        } catch (Exception e) {
          CharBuffer methodName = UTF8.decode((ByteBuffer) dst.clear().limit(read));
          System.err.println("BOOM " + methodName);
          e.printStackTrace();  //todo: verify for a purpose
        }
      }


    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }


  public void onConnect(SelectionKey key) {
    HttpMethod.$.onConnect(key);
  }

  @Override
  public void onWrite(SelectionKey key) {
  }

  @Override
  public void onAccept(SelectionKey key) {
    try {
      ServerSocketChannel channel = (ServerSocketChannel) key.channel();
      SocketChannel accept = channel.accept();
      accept.configureBlocking(false);
      HttpMethod.enqueue(accept, SelectionKey.OP_READ, this);
    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }

  }

  private static class RfCallable implements Callable<Object>, AsioVisitor {
    //  public static final SimpleRequestProcessor SIMPLE_REQUEST_PROCESSOR = new SimpleRequestProcessor(ServiceLayer.create());
    public int total;
    public LinkedList<ByteBuffer> byteBufferLinkedList;
    public int remaining;
    private final SelectionKey key;
    private Map<String, String> setCookiesMap;
    private ByteBuffer headers;

    public RfCallable(SelectionKey key) {
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
      EXECUTOR_SERVICE.invokeAll(r);
      return fbuf;
    }

    public void bisectFirstPacketIntoHeaders(ByteBuffer dst) throws InterruptedException {
      boolean eol = false;
      while (dst.hasRemaining()) {
        byte b = dst.get();
        if ('\n' == b) {
          if (eol) {
            ThreadLocalHeaders.set(headers = (ByteBuffer) dst.duplicate().flip());
            System.err.println("h: " + UTF8.decode(headers).toString());
            ByteBuffer cl = headers.duplicate();

            CharBuffer contentLength = null;
            try {
              Map<String, int[]> hm = HttpHeaders.getHeaders(headers);
              System.err.println(Arrays.toString(hm.keySet().toArray()));
              int[] ints = hm.get("Content-Length");

              total = Integer.parseInt(HttpMethod.UTF8.decode((ByteBuffer) headers.duplicate().limit(ints[1]).position(ints[0])).toString().trim());
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

    void processBuffer(ByteBuffer payload) {
      System.err.println("+++ headers " + UTF8.decode((ByteBuffer) headers.rewind()).toString());
      String s = UTF8.decode(payload).toString();
      System.err.println("+++ process " + s);


//      ThreadLocalKey.set(key);
      final String process = SIMPLE_REQUEST_PROCESSOR.process(s);
      setCookiesMap = ThreadLocalSetCookies.get();
      String sc = "";
      if (null != setCookiesMap && !setCookiesMap.isEmpty()) {
        sc = "Set-Cookie: ";

        Iterator<Map.Entry<String, String>> iterator = setCookiesMap.entrySet().iterator();
        if (iterator.hasNext()) {
          do {
            Map.Entry<String, String> stringStringEntry = iterator.next();
            sc += stringStringEntry.getKey() + "=" + stringStringEntry.getValue().trim();
            if (iterator.hasNext()) sc += "; ";
          } while (iterator.hasNext());
        }
        sc += "\r\n";
      }
      int length = process.length();
      String s1 = "HTTP/1.1 200 OK\r\n" +
          sc +
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
      ThreadLocalHeaders.set(headers);
      try {
        Object attachment = key.attachment();
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
      attachment.rewind();
      byteBufferLinkedList = null;
      if (null == headers) {
        bisectFirstPacketIntoHeaders(attachment);
      } else {
        pileOnBufferSegment(attachment, attachment.limit());
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
}
