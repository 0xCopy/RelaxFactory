package ds.shared.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.place.shared.Prefix;

public class DealPlace extends Place {
    private final String key;

    public DealPlace(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    @Prefix("!deal")
    public static class Tokenizer implements PlaceTokenizer<DealPlace> {
        @Override
        public DealPlace getPlace(String token) {
            return new DealPlace(token);
        }

        @Override
        public String getToken(DealPlace place) {
            return place.getKey();
        }
    }
}
