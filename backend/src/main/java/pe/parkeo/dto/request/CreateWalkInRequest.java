package pe.parkeo.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateWalkInRequest {

    @NotNull(message = "El estacionamiento es requerido")
    private Long parkingLotId;

    @NotNull(message = "El tipo de vehículo es requerido")
    private Long vehicleTypeId;

    /** Opcional: el operador puede elegir el espacio manualmente. */
    private Long parkingSpaceId;

    private String notes;
}
