package pe.parkeo.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReservationRequest {

    /** Estacionamiento. Obligatorio si no se envía parkingSpaceId (asignación automática). */
    private Long parkingLotId;

    /** Espacio específico. Opcional: si no viene, el servidor asigna uno libre del estacionamiento. */
    private Long parkingSpaceId;

    @NotNull(message = "El vehículo es requerido")
    private Long vehicleId;

    // La validación de "no en el pasado" la hace ReservationService con la hora de Lima.
    @NotNull(message = "La hora de inicio es requerida")
    private LocalDateTime startTime;

    @NotNull(message = "La hora de fin es requerida")
    private LocalDateTime endTime;

    @Size(max = 500)
    private String notes;
}