// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. DTO serializable; representa datos que cruzan el contrato HTTP entre la app y el backend.
package pe.parkeo.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingLotDetailResponse {

    private Long id;
    private String name;
    private String description;
    private String address;
    private String district;
    private String city;
    private Double latitude;
    private Double longitude;
    private String phone;
    private String email;
    private Integer totalSpaces;
    private Integer availableSpaces;
    private String imageUrl;
    private Boolean isOpen;
    private Double averageRating;
    private Long totalRatings;
    private List<ScheduleInfo> schedules;
    private List<TariffInfo> tariffs;
    private List<ServiceInfo> services;
    private LocalDateTime createdAt;

    /** Consulta y devuelve: get total capacity. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public Integer getTotalCapacity() {
        return totalSpaces;
    }

    /** Consulta y devuelve: get rating. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public Double getRating() {
        return averageRating;
    }

    /** Consulta y devuelve: get rating count. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public Long getRatingCount() {
        return totalRatings != null ? totalRatings : 0L;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ScheduleInfo {
        private String dayOfWeek;
        private String openTime;
        private String closeTime;
        private Boolean isClosed;

        /** Consulta y devuelve: get is open. Consulte el catálogo de métodos y conceptos en la guía maestra. */
        public Boolean getIsOpen() {
            return isClosed != null ? !isClosed : true;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TariffInfo {
        private Long id;
        private String tariffType;
        private BigDecimal price;
        private String vehicleTypeName;
        private Integer minHours;
        private Integer maxHours;

        /** Consulta y devuelve: get vehicle type. Consulte el catálogo de métodos y conceptos en la guía maestra. */
        public String getVehicleType() {
            return vehicleTypeName;
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ServiceInfo {
        private Long id;
        private String name;
        private String icon;
    }
}
