package ro.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.web.bindery.requestfactory.shared.Receiver;

import static ro.client.Welcome.gather;
import static ro.client.Welcome.requestFactory;

/**
 * User: jim
 * Date: 4/13/12
 * Time: 10:16 AM
 */
enum UserDetails {
  ConfirmCity("Welcome,  what city are you in?", "ShowLocalPage") {
    {
      key = "city";
    }

    @Override
    public void decorateDialog(final DialogBox dialog, final RunAsyncCallback callback) {

      dialog.setText(this.caption);
      FlowPanel flowPanel = new FlowPanel();
      dialog.setWidget(flowPanel);

      final ListBox list = new ListBox();
      list.addItem("SF Bay");
      flowPanel.add(list);

      addCloseButton(dialog, callback, flowPanel, new HasText() {
        @Override
        public String getText() {

          return list.getItemText(list.getSelectedIndex());

        }

        @Override
        public void setText(String text) {
        }
      });
    }
  },
  EnterEmailAddress("What is your Email Address?") {{
    key = "email";
  }},
  ZipGender("A few final details...") {{
    key = "zipGender";
  }

    @Override
    public void decorateDialog(DialogBox dialog, RunAsyncCallback callback) {

      dialog.setText(this.caption);
      FlowPanel flowPanel = new FlowPanel();
      dialog.setWidget(flowPanel);
      flowPanel.add(new Label("Zip Code"));
      final TextBox w = new TextBox() {{
        setText("94444");
      }};
      flowPanel.add(w);
      flowPanel.add(new Label("Gender"));
      final ListBox w1 = new ListBox() {{
        addItem("select");
        addItem("Male");
        addItem("Female");
      }};
      flowPanel.add(w1);

      addCloseButton(dialog, callback, flowPanel, new HasText() {
        @Override
        public String getText() {
          return w.getText() + ":" + w1.getItemText(w1.getSelectedIndex());
        }

        @Override
        public void setText(String text) {
        }
      });
    }
  };
  String caption;
  String pageChange;
  String key;

  UserDetails(String... details) {
    this.caption = details[0];
    if (details.length > 1) this.pageChange = details[1];
  }

  void addCloseButton(final DialogBox dialog, final RunAsyncCallback callback, FlowPanel flowPanel, final HasText w) {
    flowPanel.add(new Button("OK") {{
      addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          final String text = w.getText();
          requestFactory.couch().setSessionProperty(key, text).fire(new Receiver<String>() {

            @Override
            public void onSuccess(String response) {
//              session.setVersion(response);
              gather.put(UserDetails.this, text);
              dialog.hide();
              GWT.runAsync(callback);
            }
          });
        }
      });
    }});
  }

  public DialogBox createDialog() {
    DialogBox dialogBox = new DialogBox();
    dialogBox.setModal(true);
    dialogBox.setAnimationEnabled(true);
    return dialogBox;
  }

  public void decorateDialog(final DialogBox dialog, final RunAsyncCallback callback) {

    dialog.setText(this.caption);
    FlowPanel flowPanel = new FlowPanel();
    dialog.setWidget(flowPanel);
    final TextBox w = new TextBox();
    flowPanel.add(w);
    addCloseButton(dialog, callback, flowPanel, w);
  }

  public String getKey() {
    return key;
  }
}
