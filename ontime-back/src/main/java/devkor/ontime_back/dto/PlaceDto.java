package devkor.ontime_back.dto;

import devkor.ontime_back.entity.Place;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class PlaceDto {
    private UUID placeId;
    private String placeName;
    public static PlaceDto fromEntity(Place place) {
        return new PlaceDto(place.getPlaceId(), place.getPlaceName());
    }
}