package ds.shared.activity;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import com.google.web.bindery.requestfactory.shared.Receiver;
import ds.shared.place.DealPlace;
import ds.shared.rf.DealRequestFactory;
import ds.shared.rf.proxy.DealProxy;
import ds.shared.rf.request.DealRequest;
import ds.shared.view.DealView;

public class DealActivity extends AbstractActivity {
    private final DealPlace place;

    private DealRequestFactory rf;
    private DealView view;

    public DealActivity(DealPlace place) {
        this.place = place;
    }

    //@Inject
    public void setRequestFactory(DealRequestFactory rf) {
        this.rf = rf;
    }

    //@Inject
    public void setView(DealView view) {
        this.view = view;
    }

    @Override
    public void start(final AcceptsOneWidget panel, EventBus eventBus) {
        final RequestFactoryEditorDriver<DealProxy, ? super DealView> driver = view.getDriver();
        driver.initialize(rf, view);

        final DealRequest request = rf.dealReq();
        request.find(place.getKey()).with(driver.getPaths()).fire(new Receiver<DealProxy>() {
            @Override
            public void onSuccess(DealProxy dealProxy) {
                driver.display(dealProxy);
                panel.setWidget(view);
            }
        });

    }
}
