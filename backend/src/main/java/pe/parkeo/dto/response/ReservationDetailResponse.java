package pe.parkeo.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationDetailResponse {

    private Long id;
    private String confirmationCode;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal totalAmount;
    private String currency;
    private String notes;
    private String cancellationReason;
    private LocalDateTime cancelledAt;
    private LocalDateTime checkedInAt;
    private LocalDateTime checkedOutAt;

    // Parking lot info
    private Long parkingLotId;
    private String parkingLotName;
    private String parkingLotAddress;
    private Double parkingLotLatitude;
    private Double parkingLotLongitude;

    // Space info
    private Long parkingSpaceId;
    private String spaceNumber;
    private String floor;
    private String section;

    // Vehicle info
    private Long vehicleId;
    private String vehicleLicensePlate;
    private String vehicleBrand;
    private String vehicleModel;
    private String vehicleColor;

    // User info
    private Long userId;
    private String userName;
    private String userEmail;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
