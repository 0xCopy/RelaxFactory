package ds.shared.view;

import java.util.List;

import ds.shared.activity.DealSearchActivity;
import ds.shared.rf.proxy.DealProxy;

import com.google.gwt.user.client.ui.IsWidget;

public interface DealSearchView extends IsWidget {
  void setSearchResults(List<DealProxy> deals);
  void setPresenter(DealSearchActivity presenter);
  void setSearchQuery(String query);
}
