package rxf.server;

import one.xio.AsioVisitor;
import rxf.server.gen.CouchDriver;

import java.io.IOException;
import java.io.Serializable;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static rxf.server.BlobAntiPatternObject.LOOPBACK;
import static rxf.server.BlobAntiPatternObject.getReceiveBufferSize;
import static one.xio.HttpMethod.UTF8;

/**
 * revisit this with new API's, it's not expected to be current but non-trivial to get it right.
 * <p/>
 * User: jim
 * Date: 2/12/12
 * Time: 10:24 PM
 */
public class CouchChangesClient extends AsioVisitor.Impl {

  public String feedname = "example";
  public Serializable port = 5984;

  boolean active = false;
  public final int POLL_HEARTBEAT_MS = 45000;
  public final byte[] ENDL = new byte[] {/*'\n',*/'\r', '\n'};
  public boolean scriptExit2 = false;
  public String hostname = LOOPBACK.getHostAddress();

  public CouchChangesClient(String feedname) {
    this.feedname = feedname;
  }

  public CouchChangesClient() {
  }

  public String getFeedString() {
    return "/" + feedname + "/_changes?include_docs=true&feed=continuous&heartbeat="
        + POLL_HEARTBEAT_MS;
  }

  public void onWrite(SelectionKey key) throws IOException {
    Object[] attachment = (Object[]) key.attachment();
    SocketChannel channel = (SocketChannel) key.channel();
    String str = "HEAD " + getFeedString() + " HTTP/1.1\r\n\r\n";
    System.err.println("attempting " + str);
    attachment[1] = UTF8.encode(str);
    channel.write(ByteBuffer.wrap(str.getBytes()));
    key.selector().wakeup();
    key.interestOps(OP_READ);
  }

  /**
   * handles a control socket read
   *
   * @param key{$CLCONTROL,feedstr,pending}
   *
   */

  public void onRead(SelectionKey key) {
    SocketChannel channel = (SocketChannel) key.channel();

    try {
      final ByteBuffer b = ByteBuffer.allocateDirect(getReceiveBufferSize());
      //            ByteBuffer b = ByteBuffer.allocateDirect(333);
      int sofar = channel.read(b);
      final String s = UTF8.decode((ByteBuffer) b.rewind()).toString();
      if (s.startsWith("HTTP/1.1 20")) {
        active = true;
        key.selector().wakeup();
        key.interestOps(OP_WRITE).attach(new Object[] {new Impl() {
          @Override
          public void onWrite(SelectionKey key) throws Exception {
            Object[] attachment = (Object[]) key.attachment();
            SocketChannel channel = (SocketChannel) key.channel();
            String str = "GET " + getFeedString() + " HTTP/1.1\r\n\r\n";
            System.err.println("attempting " + str);
            attachment[1] = UTF8.encode(str);
            channel.write(ByteBuffer.wrap(str.getBytes()));
            key.selector().wakeup();
            key.interestOps(OP_READ);
          }

          @Override
          public void onRead(SelectionKey key) throws Exception {
            final SelectableChannel channel1 = key.channel();
            final SocketChannel channel = (SocketChannel) channel1;
            final ByteBuffer b = ByteBuffer.allocateDirect(getReceiveBufferSize());
            ((SocketChannel) channel).read(b);
            b.flip();
            final Object[] attachment = (Object[]) key.attachment(); //todo: nuke the attachment arrays
            Object prev = attachment.length > 2 ? attachment[2] : null;
            boolean stuff = false;
            ByteBuffer wrap = ByteBuffer.wrap(ENDL);
            b.mark();
            b.position(b.limit() - ENDL.length);

            if (0 != wrap.compareTo(b)) {
              stuff = true;
            }
            b.reset();

            Object[] objects = {b, prev};

            if (stuff) {
              Object[] ob = {this, attachment[1], objects};
              key.attach(ob);
            } else {
              key.attach(new Object[] {this, attachment[1]});
              //offload the heavy stuff to some other core if possible
              EXECUTOR_SERVICE.submit(new UpdateStreamRecvTask(objects));
            }
          }
        }, getFeedString()});
      } else {
        final CouchChangesClient prev = this;
        key.selector().wakeup();
        key.interestOps(OP_WRITE).attach(new Impl() {

          @Override
          public void onWrite(SelectionKey key) throws Exception {

            String str = "PUT /" + feedname + "/ HTTP/1.1\r\n\r\n";
            ByteBuffer encode = UTF8.encode(str);
            System.err.println("attempting db creation  " + str);
            ((SocketChannel) key.channel()).write(ByteBuffer.wrap(str.getBytes()));
            key.selector().wakeup();
            key.interestOps(OP_READ);
            key.attach(prev);
            scriptExit2 = true;
          }
        });
      }
    } catch (SocketException e) {
      e.printStackTrace(); //todo: verify for a purpose
    } catch (IOException e) {
      e.printStackTrace(); //todo: verify for a purpose
    }
  }

  public final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

  public class UpdateStreamRecvTask implements Runnable {
    Object[] slist;
    ByteBuffer buffer;
    Deque<ByteBuffer> linkedList; //doesn't get used if only for a single read buffer
    int bufsize;

    public UpdateStreamRecvTask(Object[] blist) {
      slist = blist;
      bufsize = 0;
    }

    public void run() {

      //grab the total size of the buffers and reorder them into a forward list.

      do {

        ByteBuffer byteBuffer = (ByteBuffer) slist[0];
        slist = (Object[]) slist[1];
        if (0 == bufsize) {
          if (null == slist) {
            buffer = byteBuffer;
            break;//optimization
          }
          linkedList = new LinkedList<ByteBuffer>();
        }
        bufsize += byteBuffer.limit();
        linkedList.addFirst(byteBuffer);

      } while (null != slist);

      if (null == buffer) {
        buffer = ByteBuffer.allocateDirect(bufsize);

        for (ByteBuffer netBuffer : linkedList) {
          buffer.put(netBuffer);
        }
      }

      buffer.rewind();
      System.err.println("MsgSize: " + buffer.limit());
      int i = ENDL.length - 1;
      byte b1 = ENDL[i];
      do {
        ByteBuffer b = buffer.slice();
        while (b.hasRemaining() && b.get() != b1);
        b.flip();
        Integer integer = Integer.valueOf(UTF8.decode(b).toString().trim(), 0x10);
        System.err.println("RecordSize: " + integer);
        buffer = ((ByteBuffer) buffer.position(b.limit())).slice();
        ByteBuffer handoff = (ByteBuffer) buffer.slice().limit(integer);
        final String trim = UTF8.decode(handoff).toString().trim();
        //        System.err.println("RecordId: " + trim);
        final LinkedHashMap couchChange = CouchDriver.GSON.fromJson(trim, LinkedHashMap.class);

        EXECUTOR_SERVICE.submit(getDocUpdateHandler(couchChange));
        buffer.position(handoff.limit() + ENDL.length);
        buffer = buffer.slice();
      } while (buffer.hasRemaining());
    }

  }

  public Runnable getDocUpdateHandler(final LinkedHashMap couchChange) {

    return new Runnable() {

      public void run() {
        System.err.println("+++ " + couchChange.get("id"));
      }
    };
  }
}
