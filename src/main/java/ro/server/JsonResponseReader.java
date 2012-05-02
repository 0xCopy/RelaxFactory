package ro.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.text.MessageFormat;
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
class JsonResponseReader extends AsioVisitor.Impl {
  public long remaining;
  public long total;
  private final SynchronousQueue<String> returnTo;

  public JsonResponseReader(SynchronousQueue<String> returnTo) {
    this.returnTo = returnTo;
  }


  @Override
  public void onRead(SelectionKey key) throws IOException, InterruptedException {
    final SocketChannel channel = (SocketChannel) key.channel();
    {
      final int receiveBufferSize = KernelImpl.getReceiveBufferSize();
      ByteBuffer dst = ByteBuffer.allocate(receiveBufferSize);
      int read = channel.read(dst);
      dst.flip();

      final String rescode = (String) parseResponseCode(dst);

      KernelImpl.moveCaretToDoubleEol(dst);
      int[] bounds = HttpHeaders.getHeaders((ByteBuffer) dst.duplicate().flip()).get("Content-Length");
      if (null != bounds) {
        total = Long.parseLong(UTF8.decode((ByteBuffer) dst.duplicate().limit(bounds[1]).position(bounds[0])).toString().trim());
        remaining = total - dst.remaining();

        ByteBuffer payload;
        if (remaining <= 0) {
          payload = dst.slice();
          returnJsonStringOrErrorResponse(key, rescode, payload);
        } else {
          final LinkedList<ByteBuffer> ll = new LinkedList<ByteBuffer>();
          //
          //    synchronousQueue.clear();
          ll.add(dst.slice());
          key.interestOps(SelectionKey.OP_READ);
          key.attach(new Impl() {
            @Override
            public void onRead(SelectionKey key) throws InterruptedException, IOException {
              ByteBuffer payload = ByteBuffer.allocate(receiveBufferSize);
              int read = channel.read(payload);
              ll.add(payload);
              remaining -= read;
              if (0 == remaining) {
                payload = ByteBuffer.allocate((int) total);
                ListIterator<ByteBuffer> iter = ll.listIterator();
                while (iter.hasNext()) {
                  ByteBuffer buffer = iter.next();
                  iter.remove();
                  if (buffer.position() == total)
                    payload = (ByteBuffer) buffer.flip();
                  else
                    payload.put(buffer);     //todo: rewrite this up-kernel
                }
                returnJsonStringOrErrorResponse(key, rescode, payload);
              }
            }
          });
        }
      }
    }
  }

  private void returnJsonStringOrErrorResponse(SelectionKey key, String rescode, ByteBuffer payload) throws InterruptedException {
    key.attach(null);
    System.err.println("payload: " + UTF8.decode((ByteBuffer) payload.duplicate().rewind()));
    if (!payload.hasRemaining())
      payload.rewind();

    String trim = UTF8.decode(payload).toString().trim();
    if (rescode.startsWith("20") && rescode.length() == 3) {
      returnTo.put(trim);
    } else {
      returnTo.put(MessageFormat.format("'{'\"responseCode\":\"{0}\",\"orig\":{1}'}'", rescode, trim));
    }
  }

  private String parseResponseCode(ByteBuffer dst) {
    ByteBuffer d2 = null;
    try {
      while (!Character.isWhitespace(dst.get())) ;
      d2 = dst.duplicate();
      while (!Character.isWhitespace(dst.get())) ;
      return UTF8.decode((ByteBuffer) d2.limit(dst.position() - 1)).toString();
    } catch (Throwable e) {
//      e.printStackTrace();  //todo: verify for a purpose
    }
    return null;
  }

}
