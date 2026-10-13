// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. DTO serializable; representa datos que cruzan el contrato HTTP entre la app y el backend.
package pe.parkeo.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckAvailabilityRequest {

    @NotNull(message = "El id de la cochera es requerido")
    private Long parkingLotId;

    @NotNull(message = "El tipo de vehículo es requerido")
    private Long vehicleTypeId;

    @NotNull(message = "La hora de inicio es requerida")
    @Future(message = "La hora de inicio debe ser en el futuro")
    private LocalDateTime startTime;

    @NotNull(message = "La hora de fin es requerida")
    private LocalDateTime endTime;
}