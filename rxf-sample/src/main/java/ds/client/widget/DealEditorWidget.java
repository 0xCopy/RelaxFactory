package ds.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.text.shared.AbstractRenderer;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.datepicker.client.DateBox;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import ds.shared.rf.proxy.DealProxy;
import ds.shared.rf.proxy.NpoProxy;
import ds.shared.rf.proxy.VendorProxy;
import ds.shared.view.DealEditorView;

import java.util.List;

public class DealEditorWidget implements DealEditorView {
    interface Binder extends UiBinder<Widget, DealEditorWidget> {
    }

    private static final Binder binder = GWT.create(Binder.class);

    interface Driver extends RequestFactoryEditorDriver<DealProxy, DealEditorWidget> {
    }

    private Widget root;

    @UiField
    TextBox product;

    @UiField
    TextArea productDescription;

    @UiField
    IntegerBox limit;

    @UiField
    DateBox expire;

    //DoubleBox amount; //commented out becasue there is no float box, and the field is float

    //these shouldn't be valueListBoxes, but we can't really do suggestboxes yet
    @UiField(provided = true)
    ValueListBox<VendorProxy> vendor;
    @UiField(provided = true)
    ValueListBox<NpoProxy> npo;


    public DealEditorWidget() {
        vendor = new ValueListBox<VendorProxy>(new AbstractRenderer<VendorProxy>() {
            @Override
            public String render(VendorProxy object) {
                return object.getName();
            }
        });
        npo = new ValueListBox<NpoProxy>(new AbstractRenderer<NpoProxy>() {
            @Override
            public String render(NpoProxy object) {
                return object.getName();
            }
        });
        root = binder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return root;
    }

    @Override
    public RequestFactoryEditorDriver<DealProxy, ? super DealEditorView> getDriver() {
        return GWT.create(Driver.class);
    }

    @Override
    public void setNpoList(List<NpoProxy> npos) {
        npo.setAcceptableValues(npos);
    }

    @Override
    public void setVendorList(List<VendorProxy> vendors) {
        vendor.setAcceptableValues(vendors);
    }
}
