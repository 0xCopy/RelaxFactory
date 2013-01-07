package ds.shared.activity;

import java.util.List;

import ds.shared.place.DealPlace;
import ds.shared.place.DealSearchPlace;
import ds.shared.rf.DealRequestFactory;
import ds.shared.rf.proxy.DealProxy;
import ds.shared.view.DealSearchView;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.requestfactory.shared.Receiver;

public class DealSearchActivity extends AbstractActivity {
  private final DealSearchPlace place;

  private DealRequestFactory rf;
  private PlaceController placeController;

  private DealSearchView view;
  public DealSearchActivity(DealSearchPlace place) {
    this.place = place;
  }

  public void setRequestFactory(DealRequestFactory rf) {
    this.rf = rf;
  }
  public void setPlaceController(PlaceController placeController) {
    this.placeController = placeController;
  }
  public void setView(DealSearchView view) {
    this.view = view;
  }

  @Override
  public void start(final AcceptsOneWidget panel, EventBus eventBus) {
    view.setPresenter(this);

    // deliberate (for now) null check - if we're starting the search activity with no query, leave it blank
    // or use the last one used (view is probably a singleton)
    if (place.getCriteria() != null) {
      view.setSearchQuery(place.getCriteria());
    }

    rf.dealReq().findByProduct(place.getCriteria()).fire(new Receiver<List<DealProxy>>() {
      @Override
      public void onSuccess(List<DealProxy> dealProxies) {
        panel.setWidget(view);
        view.setSearchResults(dealProxies);

      }
    });
  }

  public void focusOnDeal(DealProxy deal) {
    placeController.goTo(new DealPlace(deal.getId()));
  }
  public void searchFor(String query) {
    placeController.goTo(new DealSearchPlace(query));
  }

}
