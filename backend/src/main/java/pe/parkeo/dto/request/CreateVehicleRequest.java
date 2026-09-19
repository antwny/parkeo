package pe.parkeo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVehicleRequest {

    @NotNull(message = "El tipo de vehículo es requerido")
    private Long vehicleTypeId;

    @NotBlank(message = "La placa es requerida")
    @Size(min = 6, max = 20, message = "La placa debe tener entre 6 y 20 caracteres")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "La placa solo puede contener letras mayúsculas, números y guiones")
    private String licensePlate;

    @Size(max = 50)
    private String brand;

    @Size(max = 50)
    private String model;

    @Size(max = 30)
    private String color;

    private Integer year;
}
