package ro.server;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import one.xio.AsioVisitor;
import one.xio.HttpMethod;

import static java.nio.channels.SelectionKey.OP_READ;
import static java.nio.channels.SelectionKey.OP_WRITE;
import static one.xio.HttpMethod.UTF8;
import static one.xio.HttpMethod.killswitch;
import static one.xio.HttpMethod.toArray;

/**
 * Created by IntelliJ IDEA.
 * User: jim
 * Date: 2/12/12
 * Time: 10:24 PM
 */
public class CouchChangesClient implements AsioVisitor {

  public String feedname = "example";
  public Serializable port = 5984;
  public static InetAddress LOOPBACK = null;

  static {
    try {
      try {
        LOOPBACK = (InetAddress) InetAddress.class.getMethod("getLoopBackAddress").invoke(null);


      } catch (NoSuchMethodException e) {
        LOOPBACK = (InetAddress) InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        System.err.println("java 6 LOOPBACK detected");
      } catch (InvocationTargetException e) {
        e.printStackTrace();  //todo: verify for a purpose
      } catch (IllegalAccessException e) {
        e.printStackTrace();  //todo: verify for a purpose
      }
    } catch (UnknownHostException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }

  boolean active = false;
  public final int POLL_HEARTBEAT_MS = 45000;
  public final byte[] ENDL = new byte[]{/*'\n',*/ '\r', '\n'};
  public boolean scriptExit2 = false;
  public static final Gson GSON = new GsonBuilder()
//     .registerTypeAdapter(Id.class, new IdTypeAdapter())
      .enableComplexMapKeySerialization()
//     .serializeNulls()
      .setDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz")
      .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
      .setPrettyPrinting()
//     .setVersion(1.0)
      .create();
  public String hostname = LOOPBACK.getHostAddress();

  public CouchChangesClient(String feedname) {
    this.feedname = feedname;
  }

  public CouchChangesClient() {
  }

  static SocketChannel createConnection() throws IOException {
    System.err.println("opening " + new InetSocketAddress(LOOPBACK, 5984).toString());
    final SocketChannel channel = SocketChannel.open();
    channel.configureBlocking(false);
    channel.connect(new InetSocketAddress(LOOPBACK, 5984));
    return channel;
  }

  public String getFeedString() {
    return "/" + feedname + "/_changes?include_docs=true&feed=continuous&heartbeat=" +
        POLL_HEARTBEAT_MS;
  }

  public void onWrite(SelectionKey key) {
    Object[] attachment = (Object[]) key.attachment();
    SocketChannel channel = (SocketChannel) key.channel();

    try {
      Object pongContents = attachment[1];
      channel.write((ByteBuffer) pongContents);
      key.interestOps(OP_READ);
    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }

  @Override
  public void onAccept(SelectionKey key) {
    throw new UnsupportedOperationException("OnAccept unused");
  }

  public void onConnect(SelectionKey key) {
    Object[] attachment = (Object[]) key.attachment();
    SocketChannel channel = (SocketChannel) key.channel();
    try {
      if (channel.finishConnect()) {
        String str = "GET " + getFeedString() + " HTTP/1.1\r\n\r\n";
        System.err.println("attempting "+str);
        attachment[1] = UTF8.encode(str);
        key.interestOps(OP_WRITE);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * handles a control socket read
   *
   * @param key{$CLCONTROL,feedstr,pending}
   *
   */

  public void onRead(SelectionKey key) {
    SocketChannel channel = (SocketChannel) key.channel();
    Object[] attachment = (Object[]) key.attachment();

    try {
      ByteBuffer b = ByteBuffer.allocateDirect(channel.socket().getReceiveBufferSize());
//            ByteBuffer b = ByteBuffer.allocateDirect(333);
      int sofar = channel.read(b);
      if (active) {
        b.flip();

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
          Object[] ob = {attachment[0], attachment[1], objects};
          key.attach(ob);
        } else {
          key.attach(new Object[]{attachment[0], attachment[1]});


          //offload the heavy stuff to some other core if possible
          EXECUTOR_SERVICE.submit(
              new UpdateStreamRecvTask(objects));
        }
      } else {
        String s = UTF8.decode((ByteBuffer) b.rewind()).toString();
        if (s.startsWith("HTTP/1.1 200")) {
          active = true;
        } else if (s.startsWith("HTTP/1.1 201")) {
          killswitch = true;
          System.err.println("bailing out on 201!");
          if (scriptExit2) System.exit(2);
        } else {
          String str = "PUT /" + feedname + "/ HTTP/1.1\r\n\r\n";
          ByteBuffer encode = UTF8.encode(str);
          attachment[1] = encode;
          key.attach(toArray(this, encode));
          key.interestOps(OP_WRITE);
          System.err.println("attempting db creation (ignore 201 and restart)" + str);
          scriptExit2 = true;
        }
      }


    } catch (SocketException e) {
      e.printStackTrace();  //todo: verify for a purpose
    } catch (IOException e) {
      e.printStackTrace();  //todo: verify for a purpose
    }
  }

  public final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();


  public class UpdateStreamRecvTask implements Runnable {
    Object[] slist;
    ByteBuffer buffer;
    Deque<ByteBuffer> linkedList;       //doesn't get used if only for a single read buffer
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
        while (b.hasRemaining() && b.get() != b1) ;
        b.flip();
        Integer integer = Integer.valueOf(UTF8.decode(b).toString().trim(), 0x10);
        System.err.println("RecordSize: " + integer);
        buffer = ((ByteBuffer) buffer.position(b.limit())).slice();
        ByteBuffer handoff = (ByteBuffer) buffer.slice().limit(integer);
        final String trim = UTF8.decode(handoff).toString().trim();
        //        System.err.println("RecordId: " + trim);
        final LinkedHashMap couchChange = GSON.fromJson(trim, LinkedHashMap.class);

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


  static public void main(String... args) throws IOException {
    CouchChangesClient couchChangesClient = new CouchChangesClient();

    int i = 0;
    if (i < args.length)
      couchChangesClient.feedname = args[i++];
    if (i < args.length)
      couchChangesClient.hostname = args[i++];
    if (i < args.length)
      couchChangesClient.port = args[i++];


    InetSocketAddress remote = new InetSocketAddress(couchChangesClient.hostname, (Integer) couchChangesClient.port);
    SocketChannel channel = SocketChannel.open();
    channel.configureBlocking(false);
    channel.connect(remote);

    String feedString = couchChangesClient.getFeedString();
    System.err.println("feedstring: " + feedString);
    HttpMethod.enqueue(channel, SelectionKey.OP_CONNECT, couchChangesClient, feedString);
    HttpMethod.init(args, HttpMethod.$); // no http
  }
}
