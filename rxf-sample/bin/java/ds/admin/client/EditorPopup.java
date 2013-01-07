package ds.admin.client;

import com.google.gwt.editor.client.Editor;
import com.google.gwt.editor.client.EditorDriver;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.PopupPanel;

public abstract class EditorPopup<T, D extends EditorDriver<?>, E extends Editor<T> & IsWidget> {
  private final PopupPanel popup = new PopupPanel(true, true);
  private final FlowPanel container = new FlowPanel();
  
  private D driver;
  private E editor;
  
  private boolean active = false;
  
  protected EditorPopup() {
    
  }
  
  public final void show(T obj) {
    assert false == active : "Already shown, be sure to call hide() on an instance before reusing";
    active = true;
    assert null != driver && null != editor : "init must be called before edit, do it in the ctor";
    if (null == popup.getWidget()) {
      container.add(editor);
      container.add(new Button("cancel", new ClickHandler() {
         public void onClick(ClickEvent event) {
          hide();
        }
      }));
      container.add(new Button("save", new ClickHandler() {
         public void onClick(ClickEvent event) {
          if (save(driver)) {
            hide();
          }
        }
      }));
      
      popup.setWidget(container);
    }
    startEdit(obj, driver);
    popup.show();
  }
  
  
  protected final void init(D driver, E editor) {
    assert null == this.driver && null == this.editor : "Can't call init twice";
    assert null != driver && null != editor : "driver and editor must be non-null";
    
    this.driver = driver;
    this.editor = editor;
    
    
  }
  protected void hide() {
    active = false;
    popup.hide();
  }
  
  protected abstract void startEdit(T obj, D driver);
  
  protected abstract boolean save(D driver);
}
