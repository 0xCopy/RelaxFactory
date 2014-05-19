package rxf.server.daemon;

import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpMethod;
import rxf.server.Rfc822HeaderState;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Map;

import static java.nio.channels.SelectionKey.*;
import static one.xio.HttpMethod.UTF8;
import static rxf.server.Config.get;

/**
 * <ul>
 * <li> Accepts external socket connections on behalf of Couchdb or other REST server
 * <p/>
 * </ul>
 *
 * User: jnorthrup
 * Date: 10/1/13
 * Time: 7:26 PM
 */
public class ProxyDaemon extends AsioVisitor.Impl {
  /**
   * until proven otherwise, all http requests must conform to crlf line-endings, and it is the primary termination token we are seeking in bytebuffer operations.
   */
  public static final byte[] TERMINATOR = new byte[] {'\r', '\n', '\r', '\n'};
  /**
   * a shortcut to locating the Host header uses this length
   */
  public static final int HOSTPREFIXLEN = "Host: ".length();

  public static final int PROXY_PORT = Integer.parseInt(get("PROXY_PORT", "0"));
  public static final String PROXY_HOST = get("PROXY_HOST", "127.0.0.1");
  private static final boolean RPS_SHOW = "true".equals(get("RPS_SHOW", "true"));
  private static final boolean PROXY_DEBUG = "true".equals(get("PROXY_DEBUG", "false"));
  /**
   * master counter for stats on inbound requests
   */
  public static int counter = 0;
  public FileChannel hdrStream;
  /**
   * request lead-in data is placed in this buffer.
   */
  ByteBuffer cursor;

  private ProxyTask proxyTask;

  private InetSocketAddress preallocAddr;

  public ProxyDaemon(ProxyTask... proxyTask) {
    this.proxyTask = proxyTask.length > 0 ? proxyTask[0] : new ProxyTask();

    if (PROXY_PORT != 0)
      try {
        preallocAddr = new InetSocketAddress(InetAddress.getByName(PROXY_HOST), PROXY_PORT);
      } catch (UnknownHostException e) {
        e.printStackTrace();
      }
  }

  /**
   * creates a http-specific socket proxy to move bytes between innerKey and outerKey in the async framework.
   *
   * @param outerKey connection to the f5
   * @param innerKey connection to the Distributor
   * @param b        the DMA ByteBuffers where applicable
   */
  public static void pipe(SelectionKey innerKey, final SelectionKey outerKey, final ByteBuffer... b) {
    String s = "pipe-" + counter;
    final ByteBuffer ob = b.length > 1 ? b[1] : ByteBuffer.allocate(4 << 10);
    final HttpPipeVisitor ib = new HttpPipeVisitor(s + "-in", innerKey, b[0], ob);
    outerKey.interestOps(OP_READ | OP_WRITE).attach(ib);
    innerKey.interestOps(OP_WRITE);
    innerKey.attach(new HttpPipeVisitor(s + "-out", outerKey, ob, b[0]) {
      public boolean fail;

      @Override
      public void onRead(SelectionKey key) throws Exception {
        if (!ib.isLimit() || fail) {
          SocketChannel channel = (SocketChannel) key.channel();
          int read = channel.read(getInBuffer());
          switch (read) {
            case -1:
              channel.close();
            case 0:
              return;
            default:
              Rfc822HeaderState.HttpResponse httpResponse =
                  new Rfc822HeaderState().headerInterest(HttpHeaders.Content$2dLength).apply(
                      (ByteBuffer) getInBuffer().duplicate().flip()).$res();
              //              if (BlobAntiPatternObject.suffixMatchChunks(TERMINATOR, httpResponse.headerBuf()
              //                                .duplicate())) ;
              break;
          }
        }
        super.onRead(key);
      }
    });
  }

  @Override
  public void onAccept(SelectionKey key) throws Exception {
    ServerSocketChannel c = (ServerSocketChannel) key.channel();
    final SocketChannel accept = c.accept();
    accept.configureBlocking(false);
    HttpMethod.enqueue(accept, OP_READ, this);
  }

  @Override
  public void onRead(final SelectionKey outerKey) throws Exception {

    if (cursor == null)
      cursor = ByteBuffer.allocate(4 << 10);
    final SocketChannel outterChannel = (SocketChannel) outerKey.channel();
    int read = outterChannel.read(cursor);
    if (-1 != read) {
      boolean timeHeaders = RPS_SHOW && counter % 1000 == 0;
      long l = 0;

      if (timeHeaders)
        l = System.nanoTime();
      Rfc822HeaderState.HttpRequest req =
          (Rfc822HeaderState.HttpRequest) new Rfc822HeaderState().$req().headerInterest(
              HttpHeaders.Host).apply((ByteBuffer) cursor.duplicate().flip());
      ByteBuffer headersBuf = req.headerBuf();
      if (Rfc822HeaderState.suffixMatchChunks(TERMINATOR, headersBuf)) {

        int climit = cursor.position();
        if (PROXY_DEBUG) {
          String decode = UTF8.decode((ByteBuffer) headersBuf.duplicate().rewind()).toString();
          String[] split = decode.split("[\r\n]+");
          System.err.println(Arrays.deepToString(split));
        }
        req.headerString(HttpHeaders.Host, proxyTask.prefix);
        InetSocketAddress address =
            (InetSocketAddress) outterChannel.socket().getRemoteSocketAddress();

        //grab a frame of int offsets
        Map<String, int[]> headers = HttpHeaders.getHeaders((ByteBuffer) headersBuf.flip());
        int[] hosts = headers.get("Host");

        ByteBuffer slice2 =
            UTF8.encode("Host: " + proxyTask.prefix + "\r\nX-Origin-Host: " + address.toString()
                + "\r\n");

        Buffer position = cursor.limit(climit).position(headersBuf.limit());

        final ByteBuffer inwardBuffer =
            ByteBuffer.allocateDirect(8 << 10).put(
                (ByteBuffer) cursor.clear().limit(1 + hosts[0] - HOSTPREFIXLEN)).put(
                (ByteBuffer) cursor.limit(headersBuf.limit() - 2).position(hosts[1])).put(slice2)
                .put((ByteBuffer) position);
        cursor = null;

        if (PROXY_DEBUG) {
          ByteBuffer flip = (ByteBuffer) inwardBuffer.duplicate().flip();
          System.err.println(UTF8.decode(flip).toString() + "-");
          if (timeHeaders)
            System.err.println("header decode (ns):" + (System.nanoTime() - l));
        }
        counter++;

        final SocketChannel innerChannel =
            (SocketChannel) SocketChannel.open().configureBlocking(false);
        InetSocketAddress remote;
        switch (PROXY_PORT) {
          case 0:
            InetSocketAddress localSocketAddress =
                (InetSocketAddress) ((SocketChannel) outerKey.channel()).socket()
                    .getLocalSocketAddress();
            remote =
                new InetSocketAddress(InetAddress.getByName(PROXY_HOST), localSocketAddress
                    .getPort());
            break;
          default:
            remote = preallocAddr;
            break;
        }
        innerChannel.connect(remote);
        innerChannel.register(outerKey.selector().wakeup(), OP_CONNECT, new Impl() {
          @Override
          public void onConnect(SelectionKey key) throws Exception {
            if (innerChannel.finishConnect())
              pipe(key, outerKey, inwardBuffer, (ByteBuffer) ByteBuffer.allocateDirect(8 << 10)
                  .clear());
          }
        });
      }
    } else
      outerKey.cancel();
  }

  @Override
  public void onWrite(SelectionKey key) throws Exception {
    super.onWrite(key); //To change body of overridden methods use File | Settings | File Templates.
  }

}
