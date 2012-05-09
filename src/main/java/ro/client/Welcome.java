package ro.client;

import java.util.EnumMap;
import java.util.Map;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;
import com.google.web.bindery.requestfactory.shared.Receiver;
import com.google.web.bindery.requestfactory.shared.ServerFailure;
import ro.shared.KernelFactory;
import ro.shared.req.SessionTool;


/**
 * User: jim
 * Date: 4/12/12
 * Time: 7:56 PM
 */
public class Welcome implements EntryPoint {
  static EnumMap<UserDetails, String> gather = new EnumMap<UserDetails, String>(UserDetails.class);
  public static KernelFactory requestFactory;


  public void onModuleLoad() {

    final EventBus eventBus = new SimpleEventBus();
    requestFactory = GWT.create(KernelFactory.class);
    requestFactory.initialize(eventBus);
    doController();

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
            requestFactory.couch().getSessionProperty(userDetails.getKey()).fire(
                new Receiver<String>() {
                  @Override
                  public void onFailure(ServerFailure error) {
                    super.onFailure(error);
                  }

                  @Override
                  public void onSuccess(String response) {

                    if (null == response) {
                      DialogBox dialog = userDetails.createDialog();
                      userDetails.decorateDialog(dialog, new MyRunAsyncCallback());
                      dialog.center();
                      dialog.show();
                    } else {
                      gather.put(userDetails, response);
                      doController();
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
      GWT.log("failure");
      RuntimeException runtimeException = new RuntimeException();
      throw runtimeException;
    }

    @Override
    public void onSuccess() {
      doController();
      SessionTool couch = requestFactory.couch();
      for (Map.Entry<UserDetails, String> e : gather.entrySet()) {
        Receiver<Void> receiver = new Receiver<Void>() {
          @Override
          public void onSuccess(Void response) {
            GWT.log("session version" + response);
          }
        };
        couch.setSessionProperty(e.getKey().getKey(), e.getValue()).to(receiver);

      }
      if (couch.isChanged()) couch.fire();
    }

    private class StringReceiver extends Receiver<String> {
      @Override
      public void onSuccess(String response) {
        //todo: verify for a purpose
      }
    }
  }
}