package pe.parkeo.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {

    private Long parkingLotId;
    private Boolean isAvailable;
    private Long totalHours;
    private BigDecimal calculatedPrice;
    private String currency;
    private List<ParkingSpaceResponse> availableSpaces;
}