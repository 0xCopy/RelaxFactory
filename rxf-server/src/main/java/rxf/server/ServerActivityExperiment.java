package rxf.server;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.user.client.ui.*;
import com.google.web.bindery.requestfactory.shared.*;
import com.google.web.bindery.requestfactory.vm.RequestFactorySource;

/**
 * Experiment in trying to let the server render content based on places. This is initially for cheap/easy
 * SEO, but could potentially be used also for priming the client page with content before js loads.
 * <p/>
 * This is in the rxf.server package as it should probably belong in RelaxFactory itself, but in this
 * project for east testing, changing without going through the PR process.
 *
 * @author colin
 */
public abstract class ServerActivityExperiment {

  private SimpleEventBus simpleEventBus = new SimpleEventBus();

  @SuppressWarnings({"GwtCssResourceErrors"})
  public String generateContentForToken(String token) {
    // 0. prefix token with expected !
    token = "!" + token;

    // 1. turn token into place
    Place place = getHistoryMapper().getPlace(token);

    // 2. turn place into activity
    Activity activity = getActivityMapper().getActivity(place);

    // 3. use activity to render content via rf, etc
    FakeIsWidgetContainer panel = new FakeIsWidgetContainer();
    try {
      activity.start(panel, getEventBus());
    } catch (Throwable e) {
      e.printStackTrace();
    } finally {
    }
    //TODO we are assuming that this is synchronous, which isn't safe, instead
    //     this whole method should be asynchronous. This probably means that
    //     the FakeIsWidgetContainer should own the next step

    // 4. put this in bigger context (html template)

    return "<html><body>" + panel.toString() + "</body></html>";
  }

  protected EventBus getEventBus() {
    return simpleEventBus;
  }

  /**
   * Creates a PlaceHistoryMapper. Would be nice if this could be generated like client code can
   * do from tokenizers and {@literal @}Prefix annotations.
   */
  protected abstract PlaceHistoryMapper getHistoryMapper();

  /**
   * Creates an ActivityMapper - same type as the client uses should be acceptable.
   */
  protected abstract ActivityMapper getActivityMapper();

  protected <R extends RequestFactory> R createRequestFactory(Class<R> rfClass) {
    R rf = RequestFactorySource.create(rfClass);
    rf.initialize(getEventBus(), new RequestTransport() {
      @Override
      public void send(String payload, TransportReceiver receiver) {
        try {
          receiver.onTransportSuccess(GwtRequestFactoryVisitor.SIMPLE_REQUEST_PROCESSOR.process(payload));
        } catch (Exception ex) {
          receiver.onTransportFailure(new ServerFailure(ex.getMessage()));
        }
      }
    });

    return rf;
  }

  private class FakeIsWidgetContainer implements AcceptsOneWidget {
    private IsWidget notWidget = new IsWidget() {
      @Override
      public Widget asWidget() {
        // TODO Auto-generated method stub
        return null;
      }

      public String toString() {
        return "";
      }
    };

    @Override
    public void setWidget(IsWidget w) {
      this.notWidget = w;
    }

    @Override
    public String toString() {
      return notWidget.toString();
    }
  }
}
