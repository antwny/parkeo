// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. DTO serializable; representa datos que cruzan el contrato HTTP entre la app y el backend.
package pe.parkeo.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLotResponse {

    private Long id;
    private String name;
    private String address;
    private String district;
    private String city;
    private Double latitude;
    private Double longitude;
    private Integer totalSpaces;
    private Integer availableSpaces;
    private String imageUrl;
    private Boolean isOpen;
    private Boolean isActive;
    private Double distanceKm;       // populated when doing nearby search
    private Double averageRating;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime createdAt;

    /** Consulta y devuelve: get total capacity. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public Integer getTotalCapacity() {
        return totalSpaces;
    }

    /** Consulta y devuelve: get rating. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public Double getRating() {
        return averageRating;
    }

    /** Consulta y devuelve: get distance. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public Double getDistance() {
        return distanceKm;
    }
}
