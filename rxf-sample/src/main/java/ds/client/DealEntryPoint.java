package ds.client;

import com.google.gwt.activity.shared.ActivityManager;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.place.shared.PlaceHistoryHandler;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.SimpleLayoutPanel;
import com.google.web.bindery.event.shared.EventBus;
import com.google.web.bindery.event.shared.SimpleEventBus;

import ds.client.LoginService.LoginSuccessfulEvent;
import ds.client.LoginService.LoginSuccessfulEvent.LoginSuccessfulHandler;
import ds.client.widget.DealSearchWidget;
import ds.client.widget.DealViewWidget;
import ds.client.widget.HomeWidget;
import ds.shared.activity.DealActivityMapper;
import ds.shared.place.DealPlaceHistoryMapper;
import ds.shared.place.HomePlace;
import ds.shared.rf.DealRequestFactory;
import rxf.shared.KernelFactory;

public class DealEntryPoint implements EntryPoint {


  @Override
  public void onModuleLoad() {
    //no gin magic for now, just wiring it up by hand
    EventBus eventBus = new SimpleEventBus();
    DealRequestFactory rf = GWT.create(DealRequestFactory.class);
    rf.initialize(eventBus);
    KernelFactory kernelFactory = GWT.<KernelFactory>create(KernelFactory.class);
    kernelFactory.initialize(eventBus);
    final LoginService loginService = new LoginService(rf, eventBus);

    //default outer app (desktop, ios)
    DockLayoutPanel outer = new DockLayoutPanel(Unit.PX);
    SimpleLayoutPanel display = new SimpleLayoutPanel();
    
    //TODO factor this out to a view
    HTMLPanel north = new HTMLPanel(SafeHtmlUtils.fromSafeConstant("<div>Deal Viewer <div id='top-right' style='float:right'></div></div>"));
    final Anchor login = new Anchor("Login");
    login.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        //TODO bad, i18n wrecks this
        if (login.getText().equals("Login")) {
          loginService.login();
        } else {
          loginService.logout();
        }
      }
    });
    eventBus.addHandler(LoginSuccessfulEvent.getType(), new LoginSuccessfulHandler() {
      @Override
      public void onLoginSuccess(LoginSuccessfulEvent event) {
        login.setText("Log out");
      }
    });
    
    north.add(login, "top-right");
    outer.addNorth(north, 25);
    outer.add(display);
    RootLayoutPanel.get().add(outer);


    //place wiring
    PlaceHistoryMapper mapper = GWT.create(DealPlaceHistoryMapper.class);
    PlaceHistoryHandler handler = new PlaceHistoryHandler(mapper);
    PlaceController placeController = new PlaceController(eventBus);
    handler.register(placeController, eventBus, new HomePlace());

    //activity wiring
    DealActivityMapper activityMapper = new DealActivityMapper();
    //manual DI
    activityMapper.setRequestFactory(rf);
    activityMapper.setDealView(new DealViewWidget());
    activityMapper.setHomeView(new HomeWidget());
    activityMapper.setDealSearchView(new DealSearchWidget());
    activityMapper.setPlaceController(placeController);
    
    ActivityManager manager = new ActivityManager(activityMapper, eventBus);
    manager.setDisplay(display);

    // start up
    handler.handleCurrentHistory();
  }
}