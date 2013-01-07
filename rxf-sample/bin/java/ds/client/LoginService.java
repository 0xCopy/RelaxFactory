package ds.client;

import javax.validation.ConstraintViolation;
import java.util.*;

import com.google.api.gwt.oauth2.client.Auth;
import com.google.api.gwt.oauth2.client.AuthRequest;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.GWT;
import com.google.web.bindery.event.shared.Event;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.requestfactory.shared.*;
import ds.shared.rf.DealRequestFactory;
import ds.shared.rf.proxy.LoginProxy;
import rxf.shared.KernelFactory;
import rxf.shared.prox.CouchTxProxy;

/**
 * Client only, thanks a lot, Auth...
 */

public class LoginService {
  //  private final KernelFactory rf;
  DealRequestFactory rf;
  private final Auth auth = Auth.get();
  private final EventBus eventBus;
  private AuthRequest req;

  //@Inject
  public LoginService(DealRequestFactory rf, EventBus eventBus) {
    this.rf = rf;
    this.eventBus = eventBus;

    auth.clearAllTokens();
  }

  public void logout() {
    auth.clearAllTokens();
    //tell server?
  }

  public static class LoginSuccessfulEvent extends Event<LoginSuccessfulEvent.LoginSuccessfulHandler> {
    private static final Event.Type<LoginSuccessfulHandler> TYPE = new Event.Type<LoginSuccessfulHandler>();
    //    private final AuthRequest authRequest;
    private Auth auth;
    private String token;
    public double expires;

    LoginSuccessfulEvent(Auth auth, String token) {
//      this.authRequest = authRequest;
      this();
      this.auth = auth;
      this.token = token;
    }

    public static Event.Type<LoginSuccessfulHandler> getType() {
      return TYPE;
    }

    @Override
    protected void dispatch(LoginSuccessfulHandler handler) {
      handler.onLoginSuccess(this);
    }

    @Override
    public Event.Type<LoginSuccessfulHandler> getAssociatedType() {
      return TYPE;
    }

    public Auth getAuth() {
      return auth;
    }

    public String getToken() {
      return token;
    }

    protected LoginSuccessfulEvent() {
      super();
    }

    @Override
    public Object getSource() {
      return super.getSource();
    }

    public interface LoginSuccessfulHandler {
      void onLoginSuccess(LoginSuccessfulEvent event);
    }
  }

  private void changeComplete(String token) {
    eventBus.fireEvent(new LoginSuccessfulEvent(auth, token));
    //switch to logout / clear all tokens
  }

  private void loginFailed() {
    //TODO event
  }

  public void login() {
    final String authUrl = "https://accounts.google.com/o/oauth2/auth";
    final String service = "https://www.googleapis.com/oauth2/v1/userinfo";
    final String[] scopes = {
        "https://www.googleapis.com/auth/userinfo.profile",
        "https://www.googleapis.com/auth/userinfo.email"
    };
    req = new AuthRequest(authUrl, "67460658808.apps.googleusercontent.com").withScopes(scopes);
    auth.login(req, new Callback<String, Throwable>() {
      @Override
      public void onSuccess(final String token) {
        DealRequestFactory.SendRequest send = rf.send();
        LoginProxy oath2Google = send.edit(send.create(LoginProxy.class));
        Date date = new Date();
        oath2Google.setLastModified(date);
        oath2Google.setExpires(new Date((long) (auth.expiresIn(req) + date.getTime())));
        oath2Google.setAuthUrl(authUrl);
        oath2Google.setScopes(Arrays.asList(scopes));
        oath2Google.setService(service);
        send.login(oath2Google).fire(new Receiver<CouchTxProxy>() {
          @Override
          public void onFailure(ServerFailure error) {
            super.onFailure(error);
          }

          @Override
          public void onViolation(Set<Violation> errors) {
            super.onViolation(errors);
          }

          @Override
          public void onConstraintViolation(Set<ConstraintViolation<?>> violations) {
            super.onConstraintViolation(violations);
          }

          @Override
          public void onSuccess(final CouchTxProxy couchTxProxy) {
            KernelFactory kf = GWT.create(KernelFactory.class);
            kf.initialize(rf.getEventBus());

          }
        });


      }

      @Override
      public void onFailure(Throwable reason) {
        loginFailed();
      }
    });
  }

}
