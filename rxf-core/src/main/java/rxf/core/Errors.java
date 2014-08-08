package rxf.core;

import bbcursive.Cursive;
import one.xio.AsioVisitor.Helper.F;
import one.xio.HttpHeaders;
import one.xio.HttpStatus;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

import static bbcursive.std.bb;
import static bbcursive.std.str;
import static one.xio.AsioVisitor.Helper.finishWrite;

public class Errors {

  public static void $301(SelectionKey key, String newUrl) {
    redir(key, newUrl);

  }

  public static void $303(SelectionKey key, String newUrl) {
    redir(key, newUrl);
  }

  private static void redir(SelectionKey key, final String newUrl) {
    String message = "Resource moved to <a href='" + newUrl + "'>" + newUrl + "</a>";
    ByteBuffer bb =
        bb("<html><head><title>Resource Moved</title></head><body><div>" + message
            + "</div><div><a href='/'>Back to home</a></div></body></html>");
    Rfc822HeaderState rfc822HeaderState = new Rfc822HeaderState().asResponse()//
        .status(HttpStatus.$303)//
        .headerString(HttpHeaders.Content$2dType, "text/html")//
        .headerString(HttpHeaders.Location, newUrl)//
        .headerString(HttpHeaders.Content$2dLength, str(bb.limit()))//
    ;
    finishWrite(key, new F() {
      @Override
      public void apply(SelectionKey key) throws Exception {
        key.interestOps(SelectionKey.OP_READ).attach(null);
      }
    }, bb(rfc822HeaderState), bb);
  }

  public static void $400(SelectionKey key) {
    error(key, HttpStatus.$400, "Bad Request");
  }

  public static void $401(SelectionKey key, String reason) {
    error(key, HttpStatus.$404, HttpStatus.$404.caption + ": " + reason);

  }

  public static void $404(SelectionKey key, String path) {
    error(key, HttpStatus.$404, "Not Found: " + path);
  }

  public static void $500(SelectionKey key) {
    error(key, HttpStatus.$500, "Internal Server Error");
  }

  private static void error(SelectionKey key, final HttpStatus code, String message) {
    final String html = message;
    finishWrite(key, new F() {
      @Override
      public void apply(SelectionKey key) throws Exception {
        key.interestOps(SelectionKey.OP_READ).attach(null);
      }
    }, bb((ByteBuffer) new Rfc822HeaderState().$res().status(code).headerString(
        HttpHeaders.Content$2dType, "text/html").headerString(HttpHeaders.Content$2dLength,
        String.valueOf(html.length())).asByteBuffer(), Cursive.pre.debug, Cursive.pre.rewind));

  }
}
