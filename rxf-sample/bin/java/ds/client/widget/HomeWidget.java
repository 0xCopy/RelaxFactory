package ds.client.widget;

import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;

import ds.shared.view.HomeView;

public class HomeWidget implements HomeView {

  @Override
  public Widget asWidget() {
    return new Hyperlink("Search test", "!deal-search:test");//should probably be a field instead
  }

}
