// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. DTO serializable; representa datos que cruzan el contrato HTTP entre la app y el backend.
package pe.parkeo.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpaceResponse {

    private Long id;
    private String spaceNumber;
    private String floor;
    private String section;
    private String status;
    private Boolean isCovered;
    private Boolean hasCharging;
    private Boolean isAccessible;
    private String vehicleTypeName;
    private Long vehicleTypeId;
}
