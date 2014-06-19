package rxf.core;

import one.xio.AsioVisitor;
import one.xio.HttpHeaders;
import one.xio.HttpStatus;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Errors {

  public static void $301(SelectionKey key, String newUrl) {
    redir(key, newUrl);
  }

  public static void $303(SelectionKey key, String newUrl) {
    redir(key, newUrl);
  }

  private static void redir(SelectionKey key, final String newUrl) {
    String message = "Resource moved to <a href='" + newUrl + "'>" + newUrl + "</a>";
    final String html =
        "<html><head><title>Resource Moved</title></head><body><div>" + message
            + "</div><div><a href='/'>Back to home</a></div></body></html>";
    key.attach(new AsioVisitor.Impl() {
      @Override
      public void onWrite(SelectionKey key) throws Exception {
        ByteBuffer headers =
            new Rfc822HeaderState().$res().status(HttpStatus.$303).headerString(
                HttpHeaders.Content$2dType, "text/html").headerString(HttpHeaders.Location, newUrl)
                .headerString(HttpHeaders.Content$2dLength, String.valueOf(html.length())).as(
                    ByteBuffer.class);

        ((SocketChannel) key.channel()).write(headers);
        ((SocketChannel) key.channel()).write(UTF_8.encode(html));
        key.selector().wakeup();
        key.interestOps(SelectionKey.OP_READ).attach(null);
      }
    });
    key.interestOps(SelectionKey.OP_WRITE);
  }

  public static void $400(SelectionKey key) {
    error(key, HttpStatus.$400, "Bad Request");
  }

  public static void $404(SelectionKey key, String path) {
    error(key, HttpStatus.$404, "Not Found: " + path);
  }

  public static void $500(SelectionKey key) {
    error(key, HttpStatus.$500, "Internal Server Error");
  }

  private static void error(SelectionKey key, final HttpStatus code, String message) {
    final String html = message;
    key.attach(new AsioVisitor.Impl() {
      @Override
      public void onWrite(SelectionKey key) throws Exception {
        ByteBuffer headers =
            new Rfc822HeaderState().$res().status(code).headerString(HttpHeaders.Content$2dType,
                "text/html").headerString(HttpHeaders.Content$2dLength,
                String.valueOf(html.length())).as(ByteBuffer.class);

        ((SocketChannel) key.channel()).write(headers);
        ((SocketChannel) key.channel()).write(UTF_8.encode(html));
        key.selector().wakeup();
        key.interestOps(SelectionKey.OP_READ).attach(null);
      }
    });
    key.interestOps(SelectionKey.OP_WRITE);
  }
}
