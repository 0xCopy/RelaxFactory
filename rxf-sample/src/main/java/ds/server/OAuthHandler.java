package ds.server;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import one.xio.AsioVisitor.Impl;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import rxf.server.*;
import rxf.server.Rfc822HeaderState.HttpRequest;

import static rxf.server.BlobAntiPatternObject.HEADER_TERMINATOR;
import static rxf.server.BlobAntiPatternObject.deepToString;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;

/**
 * User: jim
 * Date: 7/16/12
 * Time: 6:11 PM
 */
public class OAuthHandler extends Impl implements PreRead {
  HttpRequest req;
   ByteBuffer cursor = null;
   private SocketChannel channel;
   String payload;


   @Override
   public void onRead(SelectionKey key) throws Exception {
     channel = (SocketChannel) key.channel();
     if (cursor == null) {
       if (key.attachment() instanceof Object[]) {
         Object[] ar = (Object[]) key.attachment();
         for (Object o : ar) {
           if (o instanceof ByteBuffer) {
             cursor = (ByteBuffer) o;
             continue;
           }
           if (o instanceof Rfc822HeaderState) {
             req = ((Rfc822HeaderState) o).$req();
           }
         }
       }
       key.attach(this);
     }
     cursor = null == cursor ? ByteBuffer.allocateDirect(getReceiveBufferSize()) : cursor.hasRemaining() ? cursor : ByteBuffer.allocateDirect(cursor.capacity() << 1).put((ByteBuffer) cursor.rewind());
     int read = channel.read(cursor);
     if (read == -1)
       key.cancel();
     Buffer flip = cursor.duplicate().flip();
     req = (HttpRequest) ActionBuilder.get().state().$req().apply((ByteBuffer) flip);
     if (!BlobAntiPatternObject.suffixMatchChunks(HEADER_TERMINATOR, req.headerBuf())) {
       return;
     }
     cursor = cursor.slice();
     int remaining = Integer.parseInt(req.headerString(HttpHeaders.Content$2dLength));
     final Impl prev = this;
     if (cursor.remaining() != remaining) key.attach(new Impl() {
       @Override
       public void onRead(SelectionKey key) throws Exception {
         int read1 = channel.read(cursor);
         if (read1 == -1)
           key.cancel();
        else if (!cursor.hasRemaining()) {

           key.interestOps(SelectionKey.OP_WRITE).attach(prev);
         }
       }

     });
     key.interestOps(SelectionKey.OP_WRITE);
   }

  @Override
  public String toString() {
    return "OAuthHandler{" +
        "req=" + req +
        ", cursor=" + cursor +
        ", channel=" + channel +
        ", payload='" + payload + '\'' +
        '}';
  }

  @Override
  public void onWrite(SelectionKey key) throws Exception {
    System.err.println(deepToString("???", "payload:",HttpMethod.UTF8.decode((ByteBuffer) cursor.rewind())));
    key.cancel();
  }
}
