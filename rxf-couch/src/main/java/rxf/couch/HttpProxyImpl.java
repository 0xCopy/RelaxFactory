package rxf.couch;

import one.xio.AsioVisitor.Impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.channels.SelectionKey.*;
import static one.xio.HttpHeaders.Content$2dLength;
import static rxf.couch.Server.UTF8;
import static rxf.couch.Rfc822HeaderState.staticHeaderStrings;

/**
 * User: jim
 * Date: 6/4/12
 * Time: 1:40 AM
 */
public class HttpProxyImpl extends Impl {
  public static final String[] HEADER_INTEREST = staticHeaderStrings(Content$2dLength);
  private final Pattern passthroughExpr;

  public HttpProxyImpl(Pattern passthroughExpr) {
    this.passthroughExpr = passthroughExpr;
  }

  @Override
  public void onWrite(final SelectionKey browserKey) throws Exception {

    browserKey.selector().wakeup();
    browserKey.interestOps(OP_READ);
    String path;
    Rfc822HeaderState state = null;
    for (Object o : Arrays.asList(browserKey.attachment())) {
      if (o instanceof Rfc822HeaderState) {
        ActionBuilder.get().state(state = (Rfc822HeaderState) o);
        break;
      }
    }
    if (null == state) {
      throw new Error("this GET proxy requires " + Rfc822HeaderState.class.getCanonicalName()
          + " in " + SelectionKey.class.getCanonicalName() + ".attachments :(");
    }

    path = state.pathResCode();
    Matcher matcher = passthroughExpr.matcher(path);
    if (matcher.matches()) {
      String link = matcher.group(1);

      final String req =
          "GET " + link + " HTTP/1.1\r\n" + "Accept: image/*, text/*\r\n" + "Connection: close\r\n"
              + "\r\n";

      final SocketChannel couchConnection = BlobAntiPatternObject.createCouchConnection();
      RelaxFactoryServerImpl.enqueue(couchConnection, OP_CONNECT | OP_WRITE, new Impl() {
        @Override
        public void onRead(final SelectionKey couchKey) throws Exception {
          SocketChannel channel = (SocketChannel) couchKey.channel();
          final ByteBuffer dst = ByteBuffer.allocateDirect(4 << 10);
          int read = channel.read(dst);
          Rfc822HeaderState proxyState = new Rfc822HeaderState(HEADER_INTEREST);
          final int total = Integer.parseInt(proxyState.headerString(Content$2dLength));
          final SocketChannel browserChannel = (SocketChannel) browserKey.channel();
          try {

            int write = browserChannel.write((ByteBuffer) dst.rewind());
          } catch (IOException e) {
            couchConnection.close();
            return;
          }

          couchKey.selector().wakeup();
          couchKey.interestOps(OP_READ).attach(new Impl() {
            final ByteBuffer sharedBuf = ByteBuffer.allocateDirect(Math.min(total, 4 << 10));
            private Impl browserSlave = new Impl() {
              @Override
              public void onWrite(SelectionKey key) throws Exception {
                try {
                  int write = browserChannel.write(dst);
                  if (!dst.hasRemaining() && remaining == 0)
                    browserChannel.close();
                  browserKey.selector().wakeup();
                  browserKey.interestOps(0);
                  couchKey.selector().wakeup();
                  couchKey.interestOps(OP_READ).selector().wakeup();
                } catch (Exception e) {
                  browserChannel.close();
                } finally {
                }
              }
            };
            public int remaining = total;

            {
              browserKey.attach(browserSlave);
            }

            @Override
            public void onRead(final SelectionKey couchKey) throws Exception {

              if (browserKey.isValid() && remaining != 0) {
                dst.compact();//threadsafety guarantee by monothreaded selector

                remaining -= couchConnection.read(dst);
                dst.flip();
                couchKey.selector().wakeup();
                couchKey.interestOps(0);
                browserKey.selector().wakeup();
                browserKey.interestOps(OP_WRITE).selector().wakeup();

              } else {
                BlobAntiPatternObject.recycleChannel(couchConnection);
              }
            }
          });
        }

        @Override
        public void onWrite(SelectionKey couchKey) throws Exception {
          couchConnection.write(UTF8.encode(req));
          couchKey.selector().wakeup();
          couchKey.interestOps(OP_READ);
        }
      });
    }
  }

}
