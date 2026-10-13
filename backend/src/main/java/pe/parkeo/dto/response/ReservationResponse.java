// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. DTO serializable; representa datos que cruzan el contrato HTTP entre la app y el backend.
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
    // Si en ReservationDetailResponse estos campos son BigDecimal, usa BigDecimal aquí también
    private Double parkingLotLatitude;
    private Double parkingLotLongitude;
    private Long parkingSpaceId;
    private String spaceNumber;
    private Long vehicleId;
    private String vehicleLicensePlate;
    private LocalDateTime createdAt;

    /** Consulta y devuelve: get total price. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public BigDecimal getTotalPrice() {
        return totalAmount;
    }
}