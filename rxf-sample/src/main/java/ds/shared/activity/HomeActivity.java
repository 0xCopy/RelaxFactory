package ds.shared.activity;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import ds.shared.view.HomeView;

public class HomeActivity extends AbstractActivity {
    private HomeView view;

    public void setView(HomeView view) {
        this.view = view;
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {

        //TODO factor out to view so this is jvm-able
        panel.setWidget(view);
    }

}
