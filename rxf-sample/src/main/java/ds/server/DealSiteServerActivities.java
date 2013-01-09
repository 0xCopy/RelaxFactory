package ds.server;

import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.impl.AbstractPlaceHistoryMapper;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.requestfactory.gwt.client.RequestFactoryEditorDriver;
import com.google.web.bindery.requestfactory.gwt.client.testing.MockRequestFactoryEditorDriver;
import ds.shared.activity.DealActivityMapper;
import ds.shared.activity.DealSearchActivity;
import ds.shared.place.DealPlace;
import ds.shared.place.DealSearchPlace;
import ds.shared.place.HomePlace;
import ds.shared.rf.DealRequestFactory;
import ds.shared.rf.proxy.DealProxy;
import ds.shared.view.DealSearchView;
import ds.shared.view.DealView;
import ds.shared.view.HomeView;

import java.util.List;

public class DealSiteServerActivities extends ServerActivityExperiment {

    protected PlaceHistoryMapper getHistoryMapper() {
        //TODO should be generated instead via reflection, why doesn't GWT already have this?
        return new AbstractPlaceHistoryMapper<Void>() {
            protected AbstractPlaceHistoryMapper.PrefixAndToken getPrefixAndToken(Place place) {
                final String prefix, token;
                if (place instanceof HomePlace) {
                    token = new HomePlace.Tokenizer().getToken((HomePlace) place);
                    prefix = "!home";
                } else if (place instanceof DealPlace) {
                    token = new DealPlace.Tokenizer().getToken((DealPlace) place);
                    prefix = "!deal";
                } else if (place instanceof DealSearchPlace) {
                    token = new DealSearchPlace.Tokenizer().getToken((DealSearchPlace) place);
                    prefix = "!deal-search";
                } else {
                    return null;
                }

                return new AbstractPlaceHistoryMapper.PrefixAndToken(prefix, token);
            }

            protected PlaceTokenizer<?> getTokenizer(String prefix) {
                if ("!home".equals(prefix)) {
                    return new HomePlace.Tokenizer();
                } else if ("!deal".equals(prefix)) {
                    return new DealPlace.Tokenizer();
                } else if ("!deal-search".equals(prefix)) {
                    return new DealSearchPlace.Tokenizer();
                }
                return null;
            }
        };
    }


    protected ActivityMapper getActivityMapper() {
        HomeView homeView = new HomeView() {
            public Widget asWidget() {
                return null;
            }

            public String toString() {
                return "<a href=\"#!deal-search:test\">Search test</a>";
            }
        };
        DealView dealView = new DealView() {
            public Widget asWidget() {
                return null;
            }

            public RequestFactoryEditorDriver<DealProxy, ? super DealView> getDriver() {
                // binding stuff that is expected to be invoked before toString. More thought required here, this is gross
                return new MockRequestFactoryEditorDriver<DealProxy, DealView>();
            }

            public String toString() {
                return "This is a deal";
            }
        };
        DealSearchView dealSearchView = new DealSearchView() {
            private List<DealProxy> results;
            private String query;

            public Widget asWidget() {
                return null;
            }

            public void setSearchResults(List<DealProxy> deals) {
                this.results = deals;
            }

            public void setSearchQuery(String query) {
                this.query = query;
            }

            public void setPresenter(DealSearchActivity presenter) {
                //no-op
            }

            public String toString() {
                SafeHtmlBuilder sb = new SafeHtmlBuilder();
                sb.appendHtmlConstant("<div>Search Terms").appendEscaped(query).appendHtmlConstant("</div>");
                sb.appendHtmlConstant("<ul>");
                for (DealProxy deal : results) {
                    sb.appendHtmlConstant("<li>");
                    sb.append(SafeHtmlUtils.fromTrustedString("<a href='#!deal:" + deal.getId() + "'>"));
                    sb.appendEscaped(deal.getProduct());
                    sb.appendHtmlConstant("</a><br />");
                    sb.appendEscaped(deal.getProductDescription());
                    sb.appendHtmlConstant("</li>");
                }
                sb.appendHtmlConstant("</ul>");
                return sb.toSafeHtml().asString();
            }
        };

        DealActivityMapper mapper = new DealActivityMapper();
        mapper.setHomeView(homeView);
        mapper.setDealView(dealView);
        mapper.setDealSearchView(dealSearchView);
        mapper.setRequestFactory(createRequestFactory(DealRequestFactory.class));

        return mapper;
    }

}
