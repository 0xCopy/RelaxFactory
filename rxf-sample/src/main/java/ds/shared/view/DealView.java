package ds.shared.view;

import ds.shared.rf.proxy.DealProxy;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;

public interface DealView extends IsWidget, Editor<DealProxy> {

  RequestFactoryEditorDriver<DealProxy, ? super DealView> getDriver();

}