package ds.admin.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import com.google.web.bindery.requestfactory.shared.Receiver;
import ds.client.DealEntryPoint;
import ds.client.LoginService;
import ds.client.widget.DealEditorWidget;
import ds.shared.rf.DealRequestFactory;
import ds.shared.rf.DealRequestFactory.SendRequest;
import ds.shared.rf.proxy.DealProxy;
import ds.shared.rf.proxy.NpoProxy;
import ds.shared.rf.proxy.VendorProxy;
import ds.shared.rf.request.NpoRequest;
import ds.shared.rf.request.VendorRequest;
import ds.shared.view.DealEditorView;
import rxf.shared.prox.CouchTxProxy;

import java.util.List;

/*
 * Notice the superclass ugly hack going on here (and in the module) to keep admin stuff out of the main
 * Deal module. Dirty hack, should be dealt with some other way (probably through new presenters, not a
 * new module).
 *
 */
public class DealAdminEntryPoint extends DealEntryPoint {

  private final class DealEditorPopup extends EditorPopup<DealProxy, RequestFactoryEditorDriver<DealProxy, ? super DealEditorView>, DealEditorView> {
    private SendRequest req;

    private DealEditorPopup(DealEditorView ed) {
      RequestFactoryEditorDriver<DealProxy, ? super DealEditorView> driver = ed.getDriver();
      driver.initialize(rf, ed);
      init(driver, ed);
    }

    @Override
    protected void startEdit(DealProxy obj, RequestFactoryEditorDriver<DealProxy, ? super DealEditorView> driver) {
      req = rf.send();
      if (obj == null) {
        obj = req.create(DealProxy.class);
      }
      req.deal(obj).to(new Receiver<CouchTxProxy>() {
        @Override
        public void onSuccess(CouchTxProxy arg0) {
          hide();
        }
      });
      driver.edit(obj, req);
    }

    @Override
    protected boolean save(RequestFactoryEditorDriver<DealProxy, ? super DealEditorView> driver) {
      driver.flush().fire();
      return false;
    }
  }

  private DealRequestFactory rf;
  private DealEditorView dealEditorView = new DealEditorWidget();

  @Override
  public void onModuleLoad() {
    EventBus eventBus = new SimpleEventBus();
    rf = GWT.create(DealRequestFactory.class);
    rf.initialize(eventBus);
    LoginService loginService = new LoginService(rf, eventBus);

   //Verify that we are logged in, and are admin. all other loading waits on this, ha just kidding
    loginService.login();
    //TODO when we have real login and some way of checking that we are admin, continue on.
    afterLogin();

  }

  private void afterLogin() {
    Button createNpo = new Button("Create NPO");

    Button createVendor = new Button("Create Vendor");

    Button createDeal = new Button("Create Deal", new ClickHandler() {

      public void onClick(ClickEvent event) {
        // load up possible NPO and vendor entries
        VendorRequest req = rf.vendorReq();
        req.findAll().to(new Receiver<List<VendorProxy>>() {
          public void onSuccess(List<VendorProxy> results) {
            dealEditorView.setVendorList(results);
          }
        });
        NpoRequest npoReq = req.append(rf.npoReq());
        npoReq.findAll().to(new Receiver<List<NpoProxy>>() {
          @Override
          public void onSuccess(List<NpoProxy> results) {
            dealEditorView.setNpoList(results);
          }
        });

        req.fire(new Receiver<Void>() {
          @Override
          public void onSuccess(Void arg0) {
            //TODO stop pretending colin's fingers are a di tool
            new DealEditorPopup(dealEditorView).show(null);
          }
        });
      }
    });

    RootPanel.get().add(createDeal);

  }

}
