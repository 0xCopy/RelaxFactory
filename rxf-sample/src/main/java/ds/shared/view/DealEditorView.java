package ds.shared.view;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import ds.shared.rf.proxy.DealProxy;
import ds.shared.rf.proxy.NpoProxy;
import ds.shared.rf.proxy.VendorProxy;

import java.util.List;

public interface DealEditorView extends IsWidget, Editor<DealProxy> {
    void setNpoList(List<NpoProxy> npos);

    void setVendorList(List<VendorProxy> vendors);

    RequestFactoryEditorDriver<DealProxy, ? super DealEditorView> getDriver();
}
