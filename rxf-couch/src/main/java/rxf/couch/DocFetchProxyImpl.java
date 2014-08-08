package rxf.couch;

// import com.colinalworth.gwt.viola.web.server.mvp.ViolaServerApp;

import com.google.gwt.safehtml.shared.UriUtils;
import one.xio.HttpStatus;
import rxf.core.Errors;
import rxf.core.Rfc822HeaderState;
import rxf.core.Tx;
import rxf.couch.driver.CouchMetaDriver;
import rxf.rpc.RpcHelper;
import rxf.shared.KeepMatcher;
import rxf.web.inf.ContentRootImpl;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.regex.MatchResult;

import static java.nio.channels.SelectionKey.OP_READ;
import static one.xio.AsioVisitor.Helper.finishWrite;
import static one.xio.AsioVisitor.Helper.park;
import static one.xio.HttpHeaders.Content$2dLength;
import static one.xio.HttpHeaders.Content$2dType;
import static rxf.core.Rfc822HeaderState.HttpRequest;
import static rxf.core.Rfc822HeaderState.HttpResponse;
import static rxf.couch.gen.CouchDriver.DocFetch;

/**
 * adapted from viola HttpProxyImpl.
 * <p/>
 * shows how to secure a proxy regex using $1 in regex passed in.
 */
@KeepMatcher
public class DocFetchProxyImpl extends ContentRootImpl {

  public void onRead(final SelectionKey outerKey) throws Exception {

    if (outerKey.attachment() instanceof Object[]) {
      Object[] ar = (Object[]) outerKey.attachment();
      for (Object o : ar) {
        if (o instanceof Rfc822HeaderState) {
          setReq((HttpRequest) o);
          break;
        }
        if (o instanceof MatchResult) {
          setMatchResults((MatchResult) o);
          break;
        }
      }
    }
    if (null != getReq() && null != getMatchResults(outerKey))
      park(outerKey, new Helper.F() {
        @Override
        public void apply(SelectionKey key) throws Exception {

          final HttpRequest outerRequest = getReq();
          final String path = outerRequest.path();

          String link = transformLink(outerKey);
          if (link.endsWith("/")) {
            link += "index.html";
          }
          // sets up the threadlocals for CouchMetaDriver calls
          new DocFetch().db("").docId(link).to().state().$res().addHeaderInterest(Content$2dType);

          RpcHelper.EXECUTOR_SERVICE.submit(new Runnable() {
            // static calls happen in outer thread.
            DbKeysBuilder dbKeysBuilder = DbKeysBuilder.get();
            Tx tx = Tx.current();

            public void run() {
              DbKeysBuilder.currentKeys.set(dbKeysBuilder);
              Tx.current(tx);
              try {
                HttpResponse innerResponse = tx.state().$res();
                // getReq() is never sent in any part to couchdb here.
                CouchMetaDriver.DocFetch.visit(dbKeysBuilder, tx);

                HttpStatus innerStatus = innerResponse.statusEnum();
                if (HttpStatus.$404 == innerStatus) {
                  Errors.$404(outerKey, path);
                  return;
                }
                HttpResponse outerResponse = outerRequest.$res();
                String ctype = innerResponse.headerString(Content$2dType);
                String clen = innerResponse.headerString(Content$2dLength);
                ByteBuffer responseHeaders = outerResponse.status(innerStatus)//
                    .headerString(Content$2dType, ctype)//
                    .headerString(Content$2dLength, clen)//
                    .asByteBuffer();
                finishWrite(new Runnable() {

                  public void run() {
                    outerKey.interestOps(OP_READ).attach(null);
                  }
                }, responseHeaders, tx.payload());
              } catch (Exception e) {
                Errors.$500(outerKey);
                e.printStackTrace();
              }
            }
          });
        }
      });
    else {
      Errors.$500(outerKey);
    }
  }

  /**
   * very default implementation
   * 
   * @return a transformed string
   * @param key
   */
  public String transformLink(SelectionKey key) {
    MatchResult matcher = getMatchResults(key);
    return UriUtils.sanitizeUri(getPrefix() + matcher.group(1) + getSuffix());
  }

  /**
   * prepended to inner request
   * 
   * @return string
   */
  public String getPrefix() {
    return "";
  }

  /**
   * appended to inner request
   * 
   * @return string
   */
  public String getSuffix() {
    return "";
  }

}
