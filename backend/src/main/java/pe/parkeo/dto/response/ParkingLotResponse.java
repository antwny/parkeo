package pe.parkeo.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLotResponse {

    private Long id;
    private String name;
    private String address;
    private String district;
    private String city;
    private Double latitude;
    private Double longitude;
    private Integer totalSpaces;
    private Integer availableSpaces;
    private String imageUrl;
    private Boolean isOpen;
    private Double distanceKm;       // populated when doing nearby search
    private Double averageRating;
    private LocalDateTime createdAt;

    public Integer getTotalCapacity() {
        return totalSpaces;
    }

    public Double getRating() {
        return averageRating;
    }

    public Double getDistance() {
        return distanceKm;
    }
}
