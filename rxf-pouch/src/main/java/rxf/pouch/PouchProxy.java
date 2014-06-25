package rxf.pouch;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanCodex;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.Splittable;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * usage:
 * 
 * <pre>
 * String dbSpec = "http://" + Window.Location.getHost() + "/api/campaigns";
 * final PouchProxy campaigns = PouchProxy._create("campaigns");
 * campaigns.replicateFrom(dbSpec, new AsyncCallback<PouchProxy>() {
 *
 *   public void onFailure(Throwable caught) {
 * }
 *   public void onSuccess(PouchProxy result) {
 * campaigns.allDocs(new EnumMap<PouchProxy.AllDocOptions, Object>(PouchProxy.AllDocOptions.class) {{
 * put(PouchProxy.AllDocOptions.include_docs, true);
 * }}, new AsyncCallback<ViewResults.Results>() {
 *   public void onFailure(Throwable caught) {
 * caught.fillInStackTrace();
 * Window.alert(caught.getLocalizedMessage());
 * }
 *   public void onSuccess(final ViewResults.Results result) {
 *
 * GWT.runAsync(new RunAsyncCallback() {
 *   public void onFailure(Throwable reason) {
 *
 * }
 *   public void onSuccess() {
 * List<ViewResults.Results.Record> rows = result.getRows();
 * int size = rows.size();
 * ArrayList<Campaign> arr = new ArrayList<Campaign>();
 * for (ViewResults.Results.Record r : rows) {
 * Campaign doc = AutoBeanCodex.decode((ResFactory) GWT.create(ResFactory.class), Campaign.class, r.getDoc()).as();
 * arr.add(doc);
 * }
 * CampaignView campaignView = new CampaignView();
 * RootPanel.get().add(campaignView);
 * campaignView.init(arr);
 *
 * }
 * });
 *
 * }
 * }
 * );
 * }
 * });
 * </pre>
 */
@SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
public class PouchProxy extends JavaScriptObject {
  static PouchAutobeanFactory generic = GWT.create(PouchAutobeanFactory.class);

  protected PouchProxy() {
  }

  public static native <P> JavaScriptObject wrapSuccesFailCallback(AsyncCallback<P> cb)/*-{
                                                                                       return  $entry(function (e, r) {
                                                                                       var err = e, response = r;
                                                                                       if (err)
                                                                                       ( cb.@com.google.gwt.user.client.rpc.AsyncCallback::onFailure(Ljava/lang/Throwable;)(@java.lang.Exception::new(Ljava/lang/String;)(JSON.stringify(err))));
                                                                                       else (cb.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;)(response))
                                                                                       })
                                                                                       }-*/;

  public static final native JavaScriptObject parse(String json)
  /*-{
      return JSON.parse(json)

  }-*/;

  public static <T> void replicate(String from, String to, ReplicateOptions.ReplicateCall<T> options) {
    Object encode = AutoBeanCodex.encode(generic.create(ReplicateOptions.class, options));
    if (!GWT.isScript()) {
      String payload = ((Splittable) encode).getPayload();
      System.err.println("rep options: " + payload);
      encode = parse(payload);
    }
    JSONObject jsonObject = new JSONObject((JavaScriptObject) encode);
    if (null != options.getComplete()) {
      jsonObject.put("complete", new JSONObject(wrapSuccesFailCallback(options.getComplete())));
    }
    if (null != options.getContinuous()) {
      jsonObject.put("continuous", new JSONObject(wrapSuccesFailCallback(options.getContinuous())));
    }
    if (null != options.getOnChange()) {
      jsonObject.put("onChange", new JSONObject(wrapSuccesFailCallback(options.getOnChange())));
    }
    _replicate(from, to, jsonObject.getJavaScriptObject());
  }

  public static native void _replicate(String from, String to, JavaScriptObject options)
  /*-{
      var replicate = $wnd.PouchDB.replicate(from, to, options);
  }-*/;

  public static native PouchProxy create(String dbSpec)
  /*-{
      return new $wnd.PouchDB(dbSpec)
  }-*/;

  public static void delete(String name) {

    _delete(name);
  }

  public static native void _delete(String name)/*-{
                                                $wnd.PouchDB.destroy(name);
                                                }-*/;

  public static native String b64decode(String a) /*-{
                                                  return window.atob(a);
                                                  }-*/;

  /**
   * <ul>
   * Fetch multiple documents, deleted document are only included if options.keys is specified.
   * <p/>
   * <li>options.include_docs: Include the document in each row in the doc field
   * <li>options.conflicts: Include conflicts in the _conflicts field of a doc
   * <li>options.startkey & options.endkey: Get documents with keys in a certain range
   * <li>options.descending: Reverse the order of the output table
   * <li>options.keys: array of keys you want to get
   * <ul>
   * <li>neither startkey nor endkey can be specified with this option
   * <li>the rows are returned in the same order as the supplied "keys" array
   * <li>the row for a deleted document will have the revision ID of the deletion, and an extra key "deleted":true in
   * the "value" property
   * <li>the row for a nonexistent document will just contain an "error" property with the value "not_found"
   * </ul>
   * <li>options.attachments: Include attachment data
   * 
   * @param docId
   * @param callback
   * @param options
   * @return
   */
  public final void fetchDoc(String docId, AsyncCallback<Splittable> callback, FetchOptions options) {
    // new JSONObject()

    _fetchDoc(docId, callback, (null == options) ? JavaScriptObject.createObject()
        : parse(AutoBeanCodex.encode(generic.fetchoptions(options)).getPayload()));
  }

  private native PouchProxy _fetchDoc(String docId, AsyncCallback<Splittable> callback,
      JavaScriptObject options) /*-{
                                this.get(docId, options,
                                $entry(function (err, doc) {
                                var e = err;
                                var d = doc;
                                if (err) (callback.@com.google.gwt.user.client.rpc.AsyncCallback::onFailure(Ljava/lang/Throwable;))(e); else if (@com.google.gwt.core.client.GWT::isScript()()) (callback.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;))(d);
                                else  (callback.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;))(@com.google.web.bindery.autobean.shared.impl.StringQuoter::split(Ljava/lang/String;)(JSON.stringify(d)));
                                }));
                                return this;
                                }-*/;

  /**
   * Create a new document or update an existing document. If the document already exists you must specify its revision
   * _rev, otherwise a conflict will occur.
   * <p/>
   * There are some restrictions on valid property names of the documents, these are explained here.
   */
  public final <T> PouchProxy put(AutoBean<T> t, AsyncCallback<String> cb) {
    JavaScriptObject o;
    if (!GWT.isScript()) {
      // System.out.println(AutoBeanCodex.encode(t));
      // System.out.println(AutoBeanCodex.encode(t).getPayload());
      o = parse(AutoBeanCodex.encode(t).getPayload());
    } else {
      o = (JavaScriptObject) t;
    }
    return _put(o, cb);
  }

  /**
   * Create a new document or update an existing document. If the document already exists you must specify its revision
   * _rev, otherwise a conflict will occur.
   * <p/>
   * There are some restrictions on valid property names of the documents, these are explained here.
   */
  public final <T> PouchProxy post(AutoBean<T> t, AsyncCallback<String> cb) {
    JavaScriptObject o;
    if (!GWT.isScript()) {
      o = parse(AutoBeanCodex.encode(t).getPayload());
    } else {
      o = (JavaScriptObject) t;
    }
    return _post(o, cb);
  }

  protected final native PouchProxy _put(JavaScriptObject autobean, AsyncCallback<String> cb)
  /*-{
      this.put(autobean, $entry(function (err, response) {
          var e = err, r = response;
          if (err) (cb.@com.google.gwt.user.client.rpc.AsyncCallback::onFailure(Ljava/lang/Throwable;))(@java.io.IOException::new(Ljava/lang/String;))(JSON.stringify(e));
          else
              (cb.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;))(JSON.stringify(r));
      }));
      return this;
  }-*/;

  protected final native PouchProxy _post(JavaScriptObject autobean, AsyncCallback<String> cb)
  /*-{
      this.post(autobean,
          $entry(
              function (err, response) {
                  var e = err;
                  var r = response;
                  if (err)
                      (cb.@com.google.gwt.user.client.rpc.AsyncCallback::onFailure(Ljava/lang/Throwable;)(@java.io.IOException::new(Ljava/lang/String;))(JSON.stringify(e)));
                  else
                      (cb.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;))(JSON.stringify(r));
              }));
      return this;
  }-*/;

  public final PouchProxy allDocs(AllDocOptions options, final AsyncCallback<ViewResults.Results> cb) {

    String alldocOptions;
    alldocOptions =
        null == options ? "{}" : AutoBeanCodex.encode(generic.allDocs(options)).getPayload();
    _allDocs(alldocOptions, new AsyncCallback<JavaScriptObject>() {

      public void onFailure(Throwable caught) {
        Window.alert(caught.getMessage());
      }

      public void onSuccess(JavaScriptObject result) {

        PouchAutobeanFactory resFactory = GWT.create(PouchAutobeanFactory.class);
        Class<ViewResults.Results> resultsClass = ViewResults.Results.class;
        AutoBean<ViewResults.Results> decode;

        if (!GWT.isScript()) {

          String x = new JSONObject(result).toString();
          // System.err.println(x);
          decode = AutoBeanCodex.decode(resFactory, resultsClass, x);
        } else
          decode = AutoBeanCodex.decode(resFactory, resultsClass, (Splittable) result.cast());
        // System.err.println("decoded successfully");
        cb.onSuccess(decode.as());
      }
    });
    return this;
  }

  /**
   * Fetch multiple documents, deleted document are only included if options.keys is specified.
   */
  public final native void _allDocs(String alldocOptions, AsyncCallback<JavaScriptObject> cb)
  /*-{
      var parse = JSON.parse(alldocOptions);
      this.allDocs(parse,
          $entry(function (e1, r1) {
              var e = e1, r = r1;
              if (e) {
                  (cb.@com.google.gwt.user.client.rpc.AsyncCallback::onFailure(Ljava/lang/Throwable;)(@java.io.IOException::new(Ljava/lang/String;))(e.toSource()));
              } else {
                  (cb.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;))(r);
              }
          }))
  }-*/;

  public interface PouchAutobeanFactory extends AutoBeanFactory {
    AutoBean<ViewResults.Results> pouchResults();

    AutoBean<ViewResults.Results> pouchResults(ViewResults.Results x);

    AutoBean<AllDocOptions> allDocs();

    AutoBean<AllDocOptions> allDocs(AllDocOptions a);

    AutoBean<ReplicateOptions> replicateOptions();

    AutoBean<FetchOptions> fetchoptions();

    AutoBean<FetchOptions> fetchoptions(FetchOptions options);
  }

  public interface FetchOptions {
    /**
     * Fetch specific revision of a document. Defaults to winning revision (see couchdb guide.
     */
    String getRev();

    /**
     * Include revision history of the document
     */
    Boolean getRevs();

    /**
     * Include a list of revisions of the document, and their availability.
     */
    List<String> getRevs_info();

    /**
     * Fetch all leaf revisions if openrevs="all" or fetch all leaf revisions specified in openrevs array. Leaves will
     * be returned in the same order as specified in input array
     */
    Splittable getOpen_revs();

    /**
     * If specified conflicting leaf revisions will be attached in _conflicts array
     */
    Boolean getConflicts();

    /**
     * Include attachment data
     */
    Boolean getAttachments();

    /**
     * Include sequence number of the revision in the database
     */
    Boolean getLocal_seq();

    /**
     * An object of options to be sent to the ajax requester. In Node they are sent ver batim to request with the
     * exception of: options.ajax.cache: Appends a random string to the end of all HTTP GET requests to avoid them being
     * cached on IE. Set this to true to prevent this happening.
     */
    Splittable getAjax();
  }

  interface PutOptions {

  }

  public interface ReplicateOptions {
    /**
     * undocumented
     */
    Integer getBatch_size();

    /**
     * Reference a filter function from a design document to selectively get updates.
     */
    String getFilter();

    /**
     * Query params send to the filter function.
     */
    Set<String> getQuery_params();

    /**
     * Only replicate docs with these ids.
     */
    Set<String> getDoc_ids();

    /**
     * Initialize the replication on the couch. The response is the CouchDB POST _replicate response and is different
     * from the PouchDB replication response. Also, Splittable get_onChange is not supported on couch replications.
     */
    Boolean getServer();

    /**
     * Create target database if it does not exist. Only for couch replications.
     */
    Boolean getCreateTarget();

    interface ReplicateCall<T> extends ReplicateOptions {
      /**
       * Function called when all changes have been processed.
       */
      AsyncCallback getComplete();

      /**
       * Function called on each change processed..
       */
      AsyncCallback<T> getOnChange();

      /**
       * If true starts subscribing to future changes in the source database and continue replicating them.
       */
      AsyncCallback<T> getContinuous();

    }

  }

  public interface AllDocOptions {
    /**
     * Include the document in each row in the doc field
     */

    boolean isInclude_docs();

    /**
     * Include conflicts in the _conflicts field of a doc
     */

    boolean isConflicts();

    /**
     * Include attachment data
     */

    boolean isAttachments();

    /**
     * Get documents with keys in a certain range descending: Reverse the order of the output table
     */

    String getStartkey();

    /**
     * Get documents with keys in a certain range descending: Reverse the order of the output table
     */

    String getEndkey();

    /**
     * array of keys you want to get neither startkey nor endkey can be specified with this option
     * <p/>
     * the rows are returned in the same order as the supplied "keys" array the row for a deleted document will have the
     * revision ID of the deletion, and an extra id "deleted":true in the "value" property the row for a nonexistent
     * document will just contain an "error" property with the value "not_found"
     */

    Set<String> getKeys();

  }

  public interface ViewResults {

    Map<String, String> getErr();

    Results getResults();

    interface Response {
      boolean getOk();

      String getId();

      String getRev();

      String getError();
    }

    /**
     * { "total_rows": 1, "rows": [ { "doc": { "_id": "0B3358C1-BA4B-4186-8795-9024203EB7DD", "_rev":
     * "1-5782E71F1E4BF698FA3793D9D5A96393", "blog_post": "my blog post" }, "id":
     * "0B3358C1-BA4B-4186-8795-9024203EB7DD", "id": "0B3358C1-BA4B-4186-8795-9024203EB7DD", "value": { "rev":
     * "1-5782E71F1E4BF698FA3793D9D5A96393" } } ] }
     */

    interface Results {
      @AutoBean.PropertyName("total_rows")
      double getTotalRows();

      List<Record> getRows();

      interface Record {
        String getId();

        Long getSeq();

        String getKey();

        List<Splittable> getChanges();

        Splittable getValue();

        Splittable getDoc();
      }
    }
  }

  public abstract static class AllAllDocOptions implements AllDocOptions {

    public boolean isInclude_docs() {
      return true;
    }
  }
}
