package ro.server;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;

import com.google.web.bindery.requestfactory.server.ServiceLayer;
import com.google.web.bindery.requestfactory.server.SimpleRequestProcessor;
import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;

import static one.xio.HttpMethod.UTF8;

/**
 * a POST interception wrapper for http protocol cracking
 * User: jim
 * Date: 4/18/12
 * Time: 12:37 PM
 */
class RfPostWrapper extends AsioVisitor.Impl {


  public static final SimpleRequestProcessor SIMPLE_REQUEST_PROCESSOR = new SimpleRequestProcessor(ServiceLayer.create());

  @Override
  public void onRead(final SelectionKey key) throws IOException {
    SocketChannel channel = (SocketChannel) key.channel();
    int receiveBufferSize = channel.socket().getReceiveBufferSize();
    ByteBuffer dst = ByteBuffer.allocateDirect(receiveBufferSize);
    final int read = channel.read(dst);
    if (-1 == read) {
      key.cancel();
    } else {
      dst.flip();
      ByteBuffer duplicate = dst.duplicate();
      while (duplicate.hasRemaining() && !Character.isWhitespace(duplicate.get())) ;
      String trim = UTF8.decode((ByteBuffer) duplicate.flip()).toString().trim();
      HttpMethod method = HttpMethod.valueOf(trim);
      dst.limit(read).position(0);
      key.attach(dst);
      switch (method) {
        case POST:
          KernelImpl.moveCaretToDoubleEol(dst);
          ByteBuffer headers = (ByteBuffer) dst.duplicate().flip();
          System.err.println("+++ headers: " + UTF8.decode((ByteBuffer) headers.duplicate().rewind()).toString());
          Map<String, int[]> headers1 = HttpHeaders.getHeaders(headers);
          int[] ints = headers1.get("Content-Length");


          ByteBuffer duplicate1 = (ByteBuffer) headers.duplicate().rewind();
          String trim1 = UTF8.decode((ByteBuffer) duplicate1.limit(ints[1]).position(ints[0])).toString().trim();
          long total = Long.parseLong(trim1);

          final long[] remaining = {total - dst.remaining()};
          if (remaining[0] == 0) {
            KernelImpl.EXECUTOR_SERVICE.submit(new RfProcessTask(headers, dst, key));

          } else {
            if (dst.capacity() - dst.position() >= total) {
              headers = ByteBuffer.allocateDirect(dst.position()).put(headers);
              //alert: buhbye headers
              dst.compact().limit((int) total);

            } else {
              dst = ByteBuffer.allocateDirect((int) total).put(dst);
            }
            final ByteBuffer finalDst = dst;
            final ByteBuffer finalHeaders = headers;
            key.attach(new AsioVisitor.Impl() {
              @Override
              public void onRead(SelectionKey selectionKey) throws IOException {
                ((SocketChannel) selectionKey.channel()).read(finalDst);
                if (!finalDst.hasRemaining()) {
                  KernelImpl.EXECUTOR_SERVICE.submit(new RfProcessTask(finalHeaders, finalDst, key));
                }
              }
            });
          }
          break;
        default:
          method.onRead(key);
          break;
      }
    }
  }


  public void onConnect(SelectionKey key) {
    HttpMethod.$.onConnect(key);
  }


  @Override
  public void onAccept(SelectionKey key) throws IOException {
    ServerSocketChannel channel = (ServerSocketChannel) key.channel();
    SocketChannel accept = channel.accept();
    accept.configureBlocking(false);
    HttpMethod.enqueue(accept, SelectionKey.OP_READ, this);

  }

  private static class RfProcessTask implements Runnable {
    private final ByteBuffer headers;
    private final ByteBuffer data;
    SelectionKey key;

    public RfProcessTask(ByteBuffer headers, ByteBuffer data, SelectionKey key) {
      this.headers = headers;
      this.data = data;
      this.key = key;

    }

    @Override
    public void run() {
      KernelImpl.ThreadLocalHeaders.set(headers);
      final SocketChannel socketChannel = (SocketChannel) key.channel();
      final InetAddress remoteSocketAddress = socketChannel.socket().getInetAddress();
      KernelImpl.ThreadLocalInetAddress.set(remoteSocketAddress);
      String trim = UTF8.decode(data).toString().trim();
      final String process = SIMPLE_REQUEST_PROCESSOR.process(trim);
      String sc = setOutboundCookies();
      int length = process.length();
      final String s1 = "HTTP/1.1 200 OK\r\n" +
          sc +
          "Content-Type: application/json\r\n" +
          "Content-Length: " + length + "\r\n\r\n";
      key.attach(new AsioVisitor.Impl() {
        @Override
        public void onWrite(SelectionKey selectionKey) throws IOException {
          ((SocketChannel) key.channel()).write(UTF8.encode(s1 + process));
          System.err.println("debug: " + s1 + process);
          key.attach(null);
          key.interestOps(SelectionKey.OP_READ);
        }
      });
      key.interestOps(SelectionKey.OP_WRITE);

    }

    private String setOutboundCookies() {
      System.err.println("+++ headers " + UTF8.decode((ByteBuffer) headers.rewind()).toString());
      Map setCookiesMap = KernelImpl.ThreadLocalSetCookies.get();
      String sc = "";
      if (null != setCookiesMap && !setCookiesMap.isEmpty()) {
        sc = "";

        Iterator<Map.Entry<String, String>> iterator = setCookiesMap.entrySet().iterator();
        if (iterator.hasNext()) {
          do {
            Map.Entry<String, String> stringStringEntry = iterator.next();
            sc += "Set-Cookie: " + stringStringEntry.getKey() + "=" + stringStringEntry.getValue().trim();
            if (iterator.hasNext()) sc += "; ";
            sc += "\r\n";
          } while (iterator.hasNext());
        }

      }
      return sc;
    }
  }
}
