package ro.client;

import java.util.EnumMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.Request;
import ro.shared.KernelFactory;
import ro.shared.prox.RoSessionProxy;
import ro.shared.req.Kernel;


/**
 * User: jim
 * Date: 4/12/12
 * Time: 7:56 PM
 */
public class Welcome implements EntryPoint {
  static EnumMap<UserDetails, String> gather = new EnumMap<UserDetails, String>(UserDetails.class);
  public static KernelFactory requestFactory;
  public static RoSessionProxy session;


  public void onModuleLoad() {
    final EventBus eventBus = new SimpleEventBus();
    requestFactory = GWT.create(KernelFactory.class);
    requestFactory.initialize(eventBus);
    Kernel api = requestFactory.api();
    Request<RoSessionProxy> currentSession = api.getCurrentSession();

    currentSession.fire(new Receiver<RoSessionProxy>() {
      private String id;

      @Override
      public void onSuccess(RoSessionProxy response) {
        session = response;
        String id = response.getId();
        doController();

      }
    });
  }

  void doController() {
    checkCriterion();
  }

  void checkCriterion() {
    for (final UserDetails userDetails : UserDetails.values()) {
      if (!gather.containsKey(userDetails)) {
        GWT.runAsync(new RunAsyncCallback() {
          @Override
          public void onFailure(Throwable reason) {
          }

          @Override
          public void onSuccess() {
            requestFactory.couch().getSessionProperty(session.getId(), userDetails.getKey()).fire(
                new Receiver<String>() {
                  @Override
                  public void onSuccess(String response) {

                    if (null == response) {
                      DialogBox dialog = userDetails.createDialog();
                      userDetails.decorateDialog(dialog, new MyRunAsyncCallback());
                      dialog.center();
                      dialog.show();
                    }
                  }
                });
          }
        });

        break;
      }
    }
  }


  class MyRunAsyncCallback implements RunAsyncCallback {
    @Override
    public void onFailure(Throwable reason) {

    }

    @Override
    public void onSuccess() {
      doController();
    }
  }
}