package pe.parkeo.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleResponse {

    private Long id;
    private Long vehicleTypeId;
    private String vehicleTypeName;
    private String licensePlate;
    private String brand;
    private String model;
    private String color;
    private Integer year;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
