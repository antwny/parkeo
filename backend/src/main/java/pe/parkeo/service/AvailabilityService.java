package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.parkeo.dto.request.CheckAvailabilityRequest;
import pe.parkeo.dto.response.AvailabilityResponse;
import pe.parkeo.dto.response.ParkingSpaceResponse;
import pe.parkeo.entity.ParkingLot;
import pe.parkeo.entity.ParkingSpace;
import pe.parkeo.entity.Schedule;
import pe.parkeo.entity.Tariff;
import pe.parkeo.enums.DayOfWeek;
import pe.parkeo.enums.ReservationStatus;
import pe.parkeo.enums.SpaceStatus;
import pe.parkeo.enums.TariffType;
import pe.parkeo.exception.ValidationException;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.repository.ParkingLotRepository;
import pe.parkeo.repository.ParkingSpaceRepository;
import pe.parkeo.repository.ReservationRepository;
import pe.parkeo.repository.TariffRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final ParkingLotRepository parkingLotRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final TariffRepository tariffRepository;
    private final ReservationRepository reservationRepository;

    /**
     * Consulta la disponibilidad general de un estacionamiento para un rango de fecha/hora
     * y tipo de vehículo, calculando el precio estimado y devolviendo los espacios libres.
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(CheckAvailabilityRequest request) {
        LocalDateTime start = request.getStartTime();
        LocalDateTime end = request.getEndTime();

        if (start == null || end == null || !end.isAfter(start)) {
            throw new ValidationException("La hora de fin debe ser posterior a la hora de inicio");
        }

        ParkingLot lot = parkingLotRepository.findById(request.getParkingLotId())
                .orElseThrow(() -> new ResourceNotFoundException("Estacionamiento", request.getParkingLotId()));

        if (!Boolean.TRUE.equals(lot.getIsActive())) {
            throw new ValidationException("El estacionamiento seleccionado no se encuentra activo");
        }

        // 1. Validar horario de atención del estacionamiento
        validateOperatingHours(lot, start, end);

        // 2. Obtener los espacios candidatos (físicamente activos y asignados al tipo de vehículo)
        List<ParkingSpace> candidateSpaces = parkingSpaceRepository
                .findByParkingLotIdAndVehicleTypeIdAndStatus(
                        lot.getId(), request.getVehicleTypeId(), SpaceStatus.AVAILABLE);

        // 3. Obtener espacios que ya tienen reservas traslapadas
        List<ReservationStatus> activeStatuses = Arrays.asList(
                ReservationStatus.PENDING,
                ReservationStatus.CONFIRMED,
                ReservationStatus.ACTIVE
        );

        List<Long> reservedSpaceIds = reservationRepository.findReservedSpaceIds(
                lot.getId(), start, end, activeStatuses);

        // 4. Filtrar solo los espacios realmente libres
        List<ParkingSpace> availableSpaces = candidateSpaces.stream()
                .filter(space -> !reservedSpaceIds.contains(space.getId()))
                .toList();

        // 5. Calcular horas y monto total estimado
        Duration duration = Duration.between(start, end);
        long totalHours = (long) Math.ceil(duration.toMinutes() / 60.0);
        BigDecimal totalAmount = calculatePrice(lot.getId(), request.getVehicleTypeId(), start, end);

        List<ParkingSpaceResponse> spaceResponses = availableSpaces.stream()
                .map(this::mapSpaceToResponse)
                .toList();

        return AvailabilityResponse.builder()
                .parkingLotId(lot.getId())
                .isAvailable(!availableSpaces.isEmpty())
                .totalHours(totalHours)
                .calculatedPrice(totalAmount)
                .currency("PEN")
                .availableSpaces(spaceResponses)
                .build();
    }

    /**
     * Revisa si un espacio específico está disponible en la ventana de tiempo elegida.
     */
    @Transactional(readOnly = true)
    public boolean isSpaceAvailable(Long spaceId, LocalDateTime startTime, LocalDateTime endTime) {
        long overlapping = reservationRepository.countOverlapping(spaceId, startTime, endTime);
        return overlapping == 0;
    }

    /**
     * Calcula el precio total de una reserva según las tarifas activas.
     * Soporta HOURLY (por hora con techo), DAILY (ceil de horas/24),
     * WEEKLY, MONTHLY y FIXED, con límites min_hours / max_hours.
     * Entre varias tarifas aplicables elige la más conveniente para el cliente.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculatePrice(Long parkingLotId, Long vehicleTypeId,
                                     LocalDateTime startTime, LocalDateTime endTime) {
        List<Tariff> tariffs = tariffRepository.findByParkingLotIdAndVehicleTypeIdAndIsActiveTrue(
                parkingLotId, vehicleTypeId);

        if (tariffs.isEmpty()) {
            // Tarifas genéricas (sin restricción de tipo de vehículo)
            tariffs = tariffRepository.findByParkingLotIdAndIsActiveTrue(parkingLotId).stream()
                    .filter(t -> t.getVehicleType() == null)
                    .toList();
        }

        if (tariffs.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long totalMinutes = Math.max(0, Duration.between(startTime, endTime).toMinutes());
        double hours = totalMinutes / 60.0;

        BigDecimal best = null;
        for (Tariff tariff : tariffs) {
            BigDecimal candidate = priceForTariff(tariff, hours);
            if (candidate != null && candidate.signum() > 0
                    && (best == null || candidate.compareTo(best) < 0)) {
                best = candidate;
            }
        }

        return best != null ? best : BigDecimal.ZERO;
    }

    private BigDecimal priceForTariff(Tariff tariff, double hours) {
        Integer minHours = tariff.getMinHours();
        Integer maxHours = tariff.getMaxHours();

        switch (tariff.getTariffType()) {
            case HOURLY: {
                long billed = (long) Math.ceil(hours);
                if (minHours != null && billed < minHours) billed = minHours;
                if (maxHours != null && billed > maxHours) billed = maxHours;
                if (billed <= 0) billed = 1;
                return tariff.getPrice().multiply(BigDecimal.valueOf(billed));
            }
            case DAILY: {
                long days = Math.max(1, (long) Math.ceil(hours / 24.0));
                return tariff.getPrice().multiply(BigDecimal.valueOf(days));
            }
            case WEEKLY: {
                long weeks = Math.max(1, (long) Math.ceil(hours / (24.0 * 7)));
                return tariff.getPrice().multiply(BigDecimal.valueOf(weeks));
            }
            case MONTHLY: {
                long months = Math.max(1, (long) Math.ceil(hours / (24.0 * 30)));
                return tariff.getPrice().multiply(BigDecimal.valueOf(months));
            }
            case FIXED: {
                // Tarifa fija aplica solo si la estadía cabe en su ventana (si define max_hours)
                if (maxHours != null && hours > maxHours) {
                    return null;
                }
                return tariff.getPrice();
            }
            default:
                return null;
        }
    }

    // ─── Helpers de apoyo ──────────────────────────────────────────────────────

    private void validateOperatingHours(ParkingLot lot, LocalDateTime start, LocalDateTime end) {
        if (lot.getSchedules() == null || lot.getSchedules().isEmpty()) {
            return;
        }

        DayOfWeek currentDay = DayOfWeek.valueOf(start.getDayOfWeek().name());

        Schedule schedule = lot.getSchedules().stream()
                .filter(s -> s.getDayOfWeek() == currentDay)
                .findFirst()
                .orElseThrow(() -> new ValidationException("El estacionamiento no opera los días " + currentDay));

        if (Boolean.TRUE.equals(schedule.getIsClosed())) {
            throw new ValidationException("El estacionamiento se encuentra cerrado los días " + currentDay);
        }

        LocalTime openTime = LocalTime.parse(schedule.getOpenTime());
        LocalTime closeTime = LocalTime.parse(schedule.getCloseTime());

        LocalTime reqStart = start.toLocalTime();
        LocalTime reqEnd = end.toLocalTime();

        if (reqStart.isBefore(openTime) || reqEnd.isAfter(closeTime)) {
            throw new ValidationException(String.format("El horario solicitado está fuera de atención (%s - %s)",
                    schedule.getOpenTime(), schedule.getCloseTime()));
        }
    }

    private ParkingSpaceResponse mapSpaceToResponse(ParkingSpace space) {
        return ParkingSpaceResponse.builder()
                .id(space.getId())
                .spaceNumber(space.getSpaceNumber())
                .floor(space.getFloor())
                .section(space.getSection())
                .isCovered(space.getIsCovered())
                .hasCharging(space.getHasCharging())
                .isAccessible(space.getIsAccessible())
                .status(space.getStatus().name())
                .build();
    }
}