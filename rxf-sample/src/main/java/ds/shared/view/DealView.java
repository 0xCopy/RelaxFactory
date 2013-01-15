package ds.shared.view;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import ds.shared.rf.proxy.DealProxy;

public interface DealView extends IsWidget, Editor<DealProxy> {

    RequestFactoryEditorDriver<DealProxy, ? super DealView> getDriver();

}