package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.parkeo.dto.response.ParkingLotDetailResponse;
import pe.parkeo.dto.response.ParkingLotResponse;
import pe.parkeo.entity.ParkingLot;
import pe.parkeo.entity.Schedule;
import pe.parkeo.entity.Tariff;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.repository.ParkingLotRepository;
import pe.parkeo.repository.ScheduleRepository;
import pe.parkeo.repository.TariffRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;
    private final ScheduleRepository scheduleRepository;
    private final TariffRepository tariffRepository;

    @Transactional(readOnly = true)
    public Page<ParkingLotResponse> getAllActive(Pageable pageable) {
        return parkingLotRepository.findByIsActiveTrue(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ParkingLotDetailResponse getById(Long id) {
        ParkingLot lot = parkingLotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estacionamiento", id));
        return mapToDetailResponse(lot);
    }

    @Transactional(readOnly = true)
    public List<ParkingLotResponse> findNearby(double lat, double lon, double radiusKm) {
        return parkingLotRepository.findNearby(lat, lon, radiusKm).stream()
                .map(lot -> {
                    ParkingLotResponse response = mapToResponse(lot);
                    // Calculate distance for display
                    double dist = haversineDistance(lat, lon, lot.getLatitude(), lot.getLongitude());
                    response.setDistanceKm(Math.round(dist * 100.0) / 100.0);
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ParkingLotResponse> search(String query, Pageable pageable) {
        return parkingLotRepository.searchByNameOrAddress(query, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ParkingLotResponse> getByOperator(Long operatorId) {
        return parkingLotRepository.findByOperatorIdAndIsActiveTrue(operatorId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private ParkingLotResponse mapToResponse(ParkingLot lot) {
        return ParkingLotResponse.builder()
                .id(lot.getId())
                .name(lot.getName())
                .address(lot.getAddress())
                .district(lot.getDistrict())
                .city(lot.getCity())
                .latitude(lot.getLatitude())
                .longitude(lot.getLongitude())
                .totalSpaces(lot.getTotalSpaces())
                .availableSpaces(lot.getAvailableSpaces())
                .imageUrl(lot.getImageUrl())
                .isOpen(lot.getIsOpen())
                .createdAt(lot.getCreatedAt())
                .build();
    }

    private ParkingLotDetailResponse mapToDetailResponse(ParkingLot lot) {
        List<Schedule> schedules = scheduleRepository.findByParkingLotId(lot.getId());
        List<Tariff> tariffs = tariffRepository.findByParkingLotIdAndIsActiveTrue(lot.getId());

        List<ParkingLotDetailResponse.ScheduleInfo> scheduleInfos = schedules.stream()
                .map(s -> ParkingLotDetailResponse.ScheduleInfo.builder()
                        .dayOfWeek(s.getDayOfWeek().name())
                        .openTime(s.getOpenTime())
                        .closeTime(s.getCloseTime())
                        .isClosed(s.getIsClosed())
                        .build())
                .collect(Collectors.toList());

        List<ParkingLotDetailResponse.TariffInfo> tariffInfos = tariffs.stream()
                .map(t -> ParkingLotDetailResponse.TariffInfo.builder()
                        .id(t.getId())
                        .tariffType(t.getTariffType().name())
                        .price(t.getPrice())
                        .vehicleTypeName(t.getVehicleType() != null ? t.getVehicleType().getName() : "Todos")
                        .minHours(t.getMinHours())
                        .maxHours(t.getMaxHours())
                        .build())
                .collect(Collectors.toList());

        List<ParkingLotDetailResponse.ServiceInfo> services = lot.getServices().stream()
                .map(s -> ParkingLotDetailResponse.ServiceInfo.builder()
                        .id(s.getId())
                        .name(s.getName())
                        .icon(s.getIcon())
                        .build())
                .collect(Collectors.toList());

        return ParkingLotDetailResponse.builder()
                .id(lot.getId())
                .name(lot.getName())
                .description(lot.getDescription())
                .address(lot.getAddress())
                .district(lot.getDistrict())
                .city(lot.getCity())
                .latitude(lot.getLatitude())
                .longitude(lot.getLongitude())
                .phone(lot.getPhone())
                .email(lot.getEmail())
                .totalSpaces(lot.getTotalSpaces())
                .availableSpaces(lot.getAvailableSpaces())
                .imageUrl(lot.getImageUrl())
                .isOpen(lot.getIsOpen())
                .schedules(scheduleInfos)
                .tariffs(tariffInfos)
                .services(services)
                .createdAt(lot.getCreatedAt())
                .build();
    }

    /**
     * Haversine distance formula in kilometers.
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
