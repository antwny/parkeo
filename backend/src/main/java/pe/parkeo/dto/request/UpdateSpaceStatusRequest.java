package pe.parkeo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import pe.parkeo.enums.SpaceStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateSpaceStatusRequest {

    @NotBlank(message = "El estado es requerido")
    private String status;

    private String notes;
}
