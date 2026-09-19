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

    public Integer getTotalCapacity() {
        return totalSpaces;
    }

    public Double getRating() {
        return averageRating;
    }

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
