package ds.shared.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class DealSearchPlace extends Place {
  private String criteria;
  public DealSearchPlace(String criteria) {
    this.criteria = criteria;
  }
  
  public String getCriteria() {
    return criteria;
  }

  @Prefix("!deal-search")
  public static class Tokenizer implements PlaceTokenizer<DealSearchPlace> {
    public DealSearchPlace getPlace(String token) {
      return new DealSearchPlace(token);
    }
    @Override
    public String getToken(DealSearchPlace place) {
      return place.criteria;
    }
  }
}
