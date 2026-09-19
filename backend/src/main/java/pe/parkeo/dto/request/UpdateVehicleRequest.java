package pe.parkeo.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateVehicleRequest {

    private Long vehicleTypeId;

    @Size(max = 50)
    private String brand;

    @Size(max = 50)
    private String model;

    @Size(max = 30)
    private String color;

    private Integer year;
}
