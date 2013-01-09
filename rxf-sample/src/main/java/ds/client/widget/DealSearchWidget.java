package ds.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.client.SafeHtmlTemplates;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.*;
import ds.shared.activity.DealSearchActivity;
import ds.shared.rf.proxy.DealProxy;
import ds.shared.view.DealSearchView;

import java.util.List;

public class DealSearchWidget implements DealSearchView {
    interface Binder extends UiBinder<Widget, DealSearchWidget> {
    }

    private static Binder uiBinder = GWT.create(Binder.class);

    private final Widget root;

    @UiField
    TextBox searchTerms;
    @UiField
    FlowPanel searchResults;
    @UiField
    Button searchBtn;

    private DealSearchActivity presenter;

    public DealSearchWidget() {
        root = uiBinder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return root;
    }

    @Override
    public void setPresenter(DealSearchActivity presenter) {
        this.presenter = presenter;
    }

    interface DealTemplate extends SafeHtmlTemplates {
        @Template("<div style='border:1px solid gray;border-bottom-width:0'><div style='font-size:large'>{0}</div>{1}</div>")
        SafeHtml render(String title, String desc);
    }

    @Override
    public void setSearchQuery(String query) {
        searchTerms.setValue(query);
    }

    @Override
    public void setSearchResults(List<DealProxy> deals) {
        searchResults.clear();
        DealTemplate template = GWT.create(DealTemplate.class);
        for (final DealProxy deal : deals) {
            HTML widget = new HTML();
            widget.setHTML(template.render(deal.getProduct(), deal.getProductDescription()));
            widget.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    presenter.focusOnDeal(deal);
                }
            });
            searchResults.add(widget);
        }
    }

    @UiHandler("searchBtn")
    void clickSearch(ClickEvent event) {
        presenter.searchFor(searchTerms.getValue());
    }

}
