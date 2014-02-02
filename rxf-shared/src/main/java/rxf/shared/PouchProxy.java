package rxf.shared;

import com.google.common.base.Joiner;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.autobean.shared.AutoBean;
import com.google.web.bindery.autobean.shared.AutoBeanFactory;
import com.google.web.bindery.autobean.shared.Splittable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 usage:
 <code>                    String dbSpec = "http://" + Window.Location.getHost() + "/api/campaigns";
                    final PouchProxy campaigns = PouchProxy._create("campaigns");
                    campaigns.replicateFrom(dbSpec, new AsyncCallback<PouchProxy>() {
                        @Override
                        public void onFailure(Throwable caught) {
                        }

                        @Override
                        public void onSuccess(PouchProxy result) {
                            campaigns.allDocs(new EnumMap<PouchProxy.AllDocOptions, Object>(PouchProxy.AllDocOptions.class) {{
                                                  put(PouchProxy.AllDocOptions.include_docs, true);
                                              }}, new AsyncCallback<ViewResults.Results>() {
                                                  @Override
                                                  public void onFailure(Throwable caught) {
                                                      caught.fillInStackTrace();
                                                      Window.alert(caught.getLocalizedMessage());
                                                  }

                                                  @Override
                                                  public void onSuccess(final ViewResults.Results result) {

                                                      GWT.runAsync(new RunAsyncCallback() {
                                                          @Override
                                                          public void onFailure(Throwable reason) {
                                                              //To change body of implemented methods use File | Settings | File Templates.
                                                          }

                                                          @Override
                                                          public void onSuccess() {
                                                              List<ViewResults.Results.Record> rows = result.getRows();
                                                              int size = rows.size();
                                                              ArrayList<Campaign> arr = new ArrayList<Campaign>();
                                                              for (ViewResults.Results.Record r : rows) {
                                                                  Campaign doc = AutoBeanCodex.decode((ResFactory) GWT.create(ResFactory.class), Campaign.class, r.getDoc()).as();
                                                                  arr.add(doc);
                                                              }
                                                              CampaignView campaignView = new CampaignView();
                                                              RootPanel.get().add(campaignView);
                                                              campaignView.init(arr);

                                                          }
                                                      });

                                                  }
                                              }
                            );
                        }
                    });
</code>



 */
@SuppressWarnings("ToArrayCallWithZeroLengthArrayArgument")
public class PouchProxy extends JavaScriptObject {

  protected PouchProxy() {
  }

  native public static PouchProxy _create(String dbSpec) /*-{
                                                         return new $wnd.PouchDB(dbSpec)
                                                         }-*/;

  /*continuous:true,*/
  public final native void _replicateFrom(String dbSpec, AsyncCallback<PouchProxy> cb)/*-{
                                                                                      var ret = this;
                                                                                      this.replicate.from(dbSpec, null == cb ? {} : { complete: function () {
                                                                                      cb.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;)(ret);
                                                                                      } });
                                                                                      }-*/;

  final <ResFactory extends AutoBeanFactory> void allDocs(EnumMap<AllDocOptions, ?> options,
      final AsyncCallback<ViewResults.Results> cb, final Class<ResFactory> factoryClass) {

    List<String> list = new ArrayList<String>();
    for (Map.Entry<AllDocOptions, ?> allDocOptionsEntry : options.entrySet()) {
      list.add(allDocOptionsEntry.getKey().express(allDocOptionsEntry.getValue()));
    }
    String s =
        new StringBuilder().append("{").append(Joiner.on(',').join(list.toArray(new String[0])))
            + "}";
    _allDocs(s, new AsyncCallback<JavaScriptObject>() {

      @Override
      public void onFailure(Throwable caught) {
        Window.alert(caught.getMessage());
      }

      @Override
      public void onSuccess(JavaScriptObject result) {
        //TODO: pure autobeans codex work here.
        // cb.onSuccess(new JsoReader<ViewResults.Results, ViewResults.Results>((ResFactory) GWT.create(factoryClass), ViewResults.Results.class).read(null, result));
      }
    }

    );
  }

  /**
   * Fetch multiple documents, deleted document are only included if options.keys is specified.
   */
  final native public void _allDocs(String jsonOptions, AsyncCallback<JavaScriptObject> cb)/*-{
                                                                                           this.allDocs(eval('(' + jsonOptions + ')'), function (e, r) {

                                                                                           if (!!e) {
                                                                                           cb.@com.google.gwt.user.client.rpc.AsyncCallback::onFailure(Ljava/lang/Throwable;)(@java.io.IOException::new(Ljava/lang/String;)(e.toSource()));
                                                                                           } else {
                                                                                           cb.@com.google.gwt.user.client.rpc.AsyncCallback::onSuccess(Ljava/lang/Object;)(r);
                                                                                           }
                                                                                           })
                                                                                           }-*/;

  public final void replicateFrom(String dbSpec, AsyncCallback<PouchProxy> pouchProxyAsyncCallback) {
    _replicateFrom(dbSpec, pouchProxyAsyncCallback);
  }

  public static enum AllDocOptions {

    /**
     * Include the document in each row in the doc field
     */
    include_docs {
      @Override
      String expressValue(Object val) {
        return String.valueOf(val);
      }
    },
    /**
     * Include conflicts in the _conflicts field of a doc
     */
    conflicts {
      @Override
      String expressValue(Object val) {
        return String.valueOf(val);
      }
    },
    /**
     * Get documents with keys in a certain range descending: Reverse the order of the output table
     */
    startkey, /*: Get documents with keys in a certain range descending: Reverse the order of the output table */
    /**
     * Get documents with keys in a certain range descending: Reverse the order of the output table
     */
    endkey,
    /**
     * array of keys you want to get
     * neither startkey nor endkey can be specified with this option
     * <p/>
     * the rows are returned in the same order as the supplied "keys" array
     * the row for a deleted document will have the revision ID of the deletion, and an extra id "deleted":true in the "value" property
     * the row for a nonexistent document will just contain an "error" property with the value "not_found"
     */
    keys {
      @Override
      String expressValue(Object val) {

        String[] val1 = (String[]) val;
        for (int i = 0; i < val1.length; i++) {
          val1[i] = '"' + val1[i] + '"';
        }
        return '[' + Joiner.on(',').join(val1) + ']';
      }
    },
    /**
     * Include attachmain driver ment data
     */
    attachments {
      @Override
      String expressValue(Object val) {
        return String.valueOf(val);
      }
    };

    String express(Object val) {
      return "" + name() + ":" + expressValue(val);
    }

    String expressValue(Object val) {
      return "\"+val+\"";
    }
  }

}
