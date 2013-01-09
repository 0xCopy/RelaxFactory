package ds.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.editor.client.LeafValueEditor;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import ds.shared.rf.proxy.ContactProxy;

public class ContactViewWidget implements IsWidget, LeafValueEditor<ContactProxy> {
    interface Tpl extends SafeHtmlTemplates {
        @Template("{0}<br />{1}<br />{2}<br/>{3}, {4} {5} {6}")
        SafeHtml address(String addr1, String addr2, String addr3, String city, String state, String zip, String country);

        @Template("<a href='mailto:{0}'>{0}</a>")
        SafeHtml email(String email);

        @Template("{0}")
        SafeHtml phone(String phone);
    }

    //TODO get distracted and build a SafeHtmlTemplates source for vm
    private final Tpl tpl = GWT.create(Tpl.class);
    private final HTML root = new HTML();

    private ContactProxy value;


    public Widget asWidget() {
        return root;
    }

    public ContactProxy getValue() {
        return value;
    }

    public void setValue(ContactProxy value) {
        this.value = value;
        update();
    }

    private void update() {
        SafeHtmlBuilder sb = new SafeHtmlBuilder();
        if (value != null) {
            String addr1 = orBlank(value.getAddr1());
            String addr2 = orBlank(value.getAddr2());
            String addr3 = orBlank(value.getAddr3());
            String city = orBlank(value.getCity());
            String state = orBlank(value.getState());
            String zip = orBlank(value.getZip());
            String country = orBlank(value.getCountry());

            sb.append(tpl.address(addr1, addr2, addr3, city, state, zip, country));
            if (value.getEmail() != null) {
                sb.appendHtmlConstant("<br />").append(tpl.email(value.getEmail()));
            }
            if (value.getPhone() != null) {
                sb.appendHtmlConstant("<br />").append(tpl.phone(value.getPhone()));
            }
        }
        root.setHTML(sb.toSafeHtml());
    }

    private String orBlank(String str) {
        return str == null ? "" : str;
    }

}
