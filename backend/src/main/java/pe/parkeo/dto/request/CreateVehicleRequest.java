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
    @Size(min = 6, max = 10, message = "La placa debe tener un formato válido (ej. ABC-123 o P12-345)")
    @Pattern(regexp = "^[A-Z0-9]{3}-?[A-Z0-9]{3}$", message = "La placa debe tener un formato válido de Perú (ej. ABC-123)")
    private String licensePlate;

    @Size(max = 50, message = "La marca no puede exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ ]*$", message = "La marca solo puede contener letras, números y espacios")
    private String brand;

    @Size(max = 50, message = "El modelo no puede exceder 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ ]*$", message = "El modelo solo puede contener letras, números y espacios")
    private String model;

    @Size(max = 30, message = "El color no puede exceder 30 caracteres")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]*$", message = "El color solo puede contener letras y espacios")
    private String color;

    private Integer year;
}