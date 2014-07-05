package rxf.couch;

// import com.colinalworth.gwt.viola.web.server.mvp.ViolaServerApp;

import com.google.gwt.safehtml.shared.UriUtils;
import one.xio.HttpStatus;
import rxf.core.Errors;
import rxf.core.Rfc822HeaderState;
import rxf.couch.driver.CouchMetaDriver;
import rxf.rpc.RpcHelper;
import rxf.shared.KeepMatcher;
import rxf.shared.PreRead;
import rxf.web.inf.ContentRootImpl;
import rxf.web.inf.FinishWrite;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.regex.MatchResult;

import static java.nio.channels.SelectionKey.OP_READ;
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
@PreRead
@KeepMatcher
public class DocFetchProxyImpl extends ContentRootImpl {
  @Override
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
    if (null != getReq() && null != getMatchResults()) {
      outerKey.interestOps(0);
      final HttpRequest outerRequest = getReq();
      final String path = outerRequest.path();

      String link = transformLink();
      if (link.endsWith("/")) {
        link += "index.html";
      }
      // sets up the threadlocals for CouchMetaDriver calls
      new DocFetch().db("").docId(link).to().state().$res().addHeaderInterest(Content$2dType);

      RpcHelper.EXECUTOR_SERVICE.submit(new Runnable() {
        // static calls happen in outer thread.
        DbKeysBuilder dbKeysBuilder = DbKeysBuilder.get();
        ActionBuilder actionBuilder = ActionBuilder.get();

        public void run() {
          DbKeysBuilder.currentKeys.set(dbKeysBuilder);
          ActionBuilder.currentAction.set(actionBuilder);
          try {
            HttpResponse innerResponse = actionBuilder.state().$res();
            // getReq() is never sent in any part to couchdb here.
            ByteBuffer outerPayload = CouchMetaDriver.DocFetch.visit(dbKeysBuilder, actionBuilder);
            HttpStatus innerStatus = innerResponse.statusEnum();
            if (HttpStatus.$404 == innerStatus) {
              Errors.$404(outerKey, path);
              return;
            }
            HttpResponse outerResponse = outerRequest.$res();
            ByteBuffer byteBuffer =
                outerResponse.status(innerStatus).headerString(Content$2dType,
                    innerResponse.headerString(Content$2dType)).headerString(Content$2dLength,
                    innerResponse.headerString(Content$2dLength)).asByteBuffer();
            Runnable task = new Runnable() {
              @Override
              public void run() {
                outerKey.interestOps(OP_READ).attach(null);

              }
            };
            FinishWrite.finishWrite(outerKey, task, byteBuffer, outerPayload);
          } catch (Exception e) {
            Errors.$500(outerKey);
            e.printStackTrace();
          }
        }
      });
    } else {
      Errors.$500(outerKey);
    }
  }

  /**
   * very default implementation
   * 
   * @return a transformed string
   */
  public String transformLink() {
    MatchResult matcher = getMatchResults();
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
