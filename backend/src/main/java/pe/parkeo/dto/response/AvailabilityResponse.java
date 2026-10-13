// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. DTO serializable; representa datos que cruzan el contrato HTTP entre la app y el backend.
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