package pe.parkeo.dto.request;

import jakarta.validation.constraints.Future;
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

    @NotNull(message = "El espacio de estacionamiento es requerido")
    private Long parkingSpaceId;

    @NotNull(message = "El vehículo es requerido")
    private Long vehicleId;

    @NotNull(message = "La hora de inicio es requerida")
    @Future(message = "La hora de inicio debe ser en el futuro")
    private LocalDateTime startTime;

    @NotNull(message = "La hora de fin es requerida")
    private LocalDateTime endTime;

    @Size(max = 500)
    private String notes;
}
