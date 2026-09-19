package pe.parkeo.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationResponse {

    private Long id;
    private String confirmationCode;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalAmount;
    private String currency;
    private Long parkingLotId;
    private String parkingLotName;
    private String parkingLotAddress;
    private Long parkingSpaceId;
    private String spaceNumber;
    private Long vehicleId;
    private String vehicleLicensePlate;
    private LocalDateTime createdAt;

    public BigDecimal getTotalPrice() {
        return totalAmount;
    }
}
