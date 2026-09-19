package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.parkeo.entity.ParkingSpace;
import pe.parkeo.entity.Tariff;
import pe.parkeo.enums.TariffType;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.repository.ParkingSpaceRepository;
import pe.parkeo.repository.TariffRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final ParkingSpaceRepository parkingSpaceRepository;
    private final TariffRepository tariffRepository;
    private final pe.parkeo.repository.ReservationRepository reservationRepository;

    /**
     * Checks if a given space is available for the requested time window.
     */
    @Transactional(readOnly = true)
    public boolean isSpaceAvailable(Long spaceId, LocalDateTime startTime, LocalDateTime endTime) {
        long overlapping = reservationRepository.countOverlapping(spaceId, startTime, endTime);
        return overlapping == 0;
    }

    /**
     * Calculates the total price for a reservation based on applicable tariffs.
     * Uses HOURLY tariff by default; falls back to FIXED if no HOURLY tariff exists.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculatePrice(Long parkingLotId, Long vehicleTypeId,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        List<Tariff> tariffs = tariffRepository.findByParkingLotIdAndVehicleTypeIdAndIsActiveTrue(
                parkingLotId, vehicleTypeId);

        if (tariffs.isEmpty()) {
            // Try generic tariffs (no vehicle type restriction)
            tariffs = tariffRepository.findByParkingLotIdAndIsActiveTrue(parkingLotId).stream()
                    .filter(t -> t.getVehicleType() == null)
                    .toList();
        }

        if (tariffs.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Duration duration = Duration.between(startTime, endTime);
        long totalMinutes = duration.toMinutes();
        double hours = totalMinutes / 60.0;

        // Try HOURLY tariff first
        for (Tariff tariff : tariffs) {
            if (tariff.getTariffType() == TariffType.HOURLY) {
                return tariff.getPrice().multiply(BigDecimal.valueOf(Math.ceil(hours)));
            }
        }

        // Try DAILY
        for (Tariff tariff : tariffs) {
            if (tariff.getTariffType() == TariffType.DAILY) {
                long days = Math.max(1, duration.toDays());
                return tariff.getPrice().multiply(BigDecimal.valueOf(days));
            }
        }

        // Fallback to FIXED
        for (Tariff tariff : tariffs) {
            if (tariff.getTariffType() == TariffType.FIXED) {
                return tariff.getPrice();
            }
        }

        return BigDecimal.ZERO;
    }
}
