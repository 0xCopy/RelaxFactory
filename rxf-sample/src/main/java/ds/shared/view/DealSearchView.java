package ds.shared.view;

import com.google.gwt.user.client.ui.IsWidget;
import ds.shared.activity.DealSearchActivity;
import ds.shared.rf.proxy.DealProxy;

import java.util.List;

public interface DealSearchView extends IsWidget {
    void setSearchResults(List<DealProxy> deals);

    void setPresenter(DealSearchActivity presenter);

    void setSearchQuery(String query);
}
