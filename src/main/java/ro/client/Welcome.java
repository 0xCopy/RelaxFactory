package ro.client;

import java.util.EnumMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.shared.Receiver;
import ro.shared.KernelFactory;
import ro.shared.prox.RoSessionProxy;
import ro.shared.req.Kernel;


/**
 * User: jim
 * Date: 4/12/12
 * Time: 7:56 PM
 */
public class Welcome implements EntryPoint {
  static EnumMap<UserDetails, String> e = new EnumMap<UserDetails, String>(UserDetails.class);
  private KernelFactory requestFactory;


  public void onModuleLoad() {
    final EventBus eventBus = new SimpleEventBus();
    requestFactory = GWT.create(KernelFactory.class);
    requestFactory.initialize(eventBus);
    Kernel api = requestFactory.api();
    api.getCurrentSession().fire(new Receiver<RoSessionProxy>() {
      @Override
      public void onSuccess(RoSessionProxy response) {
        String id = response.getId();
        Window.alert("id: " + id);
      }
    });

    doController();
  }

  private void doController() {
    checkCriterion();

  }

  private void checkCriterion() {
    for (UserDetails userDetails : UserDetails.values()) {
      if (!e.containsKey(userDetails)) {
        DialogBox dialog = userDetails.createDialog();
        userDetails.decorateDialog(dialog, new MyRunAsyncCallback());
        dialog.center();
        dialog.show();
        break;
      }
    }


  }


  private class MyRunAsyncCallback implements RunAsyncCallback {
    @Override
    public void onFailure(Throwable reason) {

    }

    @Override
    public void onSuccess() {
      doController();
    }
  }
}