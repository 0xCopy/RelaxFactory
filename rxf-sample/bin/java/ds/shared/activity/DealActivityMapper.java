package ds.shared.activity;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import ds.shared.place.DealPlace;
import ds.shared.place.DealSearchPlace;
import ds.shared.place.HomePlace;
import ds.shared.rf.DealRequestFactory;
import ds.shared.view.DealSearchView;
import ds.shared.view.DealView;
import ds.shared.view.HomeView;

public class DealActivityMapper implements ActivityMapper {
    private DealRequestFactory rf;
    private PlaceController placeController;
    private DealView dealView;
    private HomeView homeView;
    private DealSearchView dealSearchView;

    @Override
    public Activity getActivity(Place place) {
        if (place instanceof DealPlace) {
            DealActivity activity = new DealActivity((DealPlace) place);
            activity.setRequestFactory(rf);//manual DI, replace with Gin
            activity.setView(dealView);//manual DI
            return activity;
        } else if (place instanceof HomePlace) {
            HomeActivity activity = new HomeActivity();
            activity.setView(homeView);//manual DI
            return activity;
        } else if (place instanceof DealSearchPlace) {
            DealSearchActivity activity = new DealSearchActivity((DealSearchPlace) place);
            activity.setRequestFactory(rf);//manual DI
            activity.setPlaceController(placeController);
            activity.setView(dealSearchView);
            return activity;
        }
        assert false : "No activity for " + place.getClass();
        return null;
    }

    public void setRequestFactory(DealRequestFactory rf) {
        this.rf = rf;
    }

    public void setPlaceController(PlaceController placeController) {
        this.placeController = placeController;
    }

    public void setDealView(DealView dealView) {
        this.dealView = dealView;
    }

    public void setHomeView(HomeView homeView) {
        this.homeView = homeView;
    }

    public void setDealSearchView(DealSearchView dealSearchView) {
        this.dealSearchView = dealSearchView;
    }
}
