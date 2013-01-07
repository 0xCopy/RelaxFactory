package ds.shared.place;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;

@WithTokenizers({DealPlace.Tokenizer.class, HomePlace.Tokenizer.class, DealSearchPlace.Tokenizer.class})
public interface DealPlaceHistoryMapper extends PlaceHistoryMapper {
}
