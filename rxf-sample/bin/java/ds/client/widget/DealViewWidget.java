package ds.client.widget;

import ds.shared.rf.proxy.DealProxy;
import ds.shared.view.DealView;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DateLabel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.NumberLabel;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;

public class DealViewWidget implements DealView {
  interface Binder extends UiBinder<Widget, DealViewWidget> {}
  private static final Binder binder = GWT.create(Binder.class);
  
  interface Driver extends RequestFactoryEditorDriver<DealProxy, DealViewWidget> {}
  
  public interface Style extends CssResource {
    String top();
    String npodetail();
    String vendordetail();
    String prod();
    String desc();
    String clear();
  }

  private Widget root;

  @UiField Style style;

  @UiField Label product;
  @UiField Label productDescription;
  @UiField NumberLabel<Integer> limit;

  @UiField DateLabel expire;
  @UiField NumberLabel<Float> amount;

  @Path("vendor.name")
  @UiField Label vendorHighlights;
  @Path("npo.name")
  @UiField Label npoHighlights;


  @Path("npo.name")
  @UiField Label npoName;
  @Path("npo.description")
  @UiField Label npoDetails;
  @Path("npo.contactInfo")
  @UiField ContactViewWidget npoContactInfo;

  @Path("vendor.name")
  @UiField Label vendorName;
  @Path("vendor.contactInfo")
  @UiField ContactViewWidget vendorContactInfo;
  @Path("vendor.description")
  @UiField Label vendorDetails;


  public DealViewWidget() {
    root = binder.createAndBindUi(this);
  }

  @Override
  public Widget asWidget() {
    return root;
  }

  @Override
  public RequestFactoryEditorDriver<DealProxy, ? super DealView> getDriver() {
    return GWT.create(Driver.class);
  }
  
  
  //TODO these next two should actually be anchors instead of looking like them
  @UiHandler("vendorName")
  void showOtherDealsFromVendor(ClickEvent click) {
    //todo
  }
  @UiHandler("npoName")
  void showOtherDealsFromNpo(ClickEvent click) {
    //todo
  }

}
