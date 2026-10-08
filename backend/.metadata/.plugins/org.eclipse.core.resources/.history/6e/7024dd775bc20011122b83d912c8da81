package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pe.parkeo.dto.request.CreateReservationRequest;
import pe.parkeo.dto.response.ReservationDetailResponse;
import pe.parkeo.dto.response.ReservationResponse;
import pe.parkeo.entity.*;
import pe.parkeo.enums.ReservationStatus;
import pe.parkeo.enums.SpaceStatus;
import pe.parkeo.exception.ConflictException;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.exception.UnauthorizedException;
import pe.parkeo.exception.ValidationException;
import pe.parkeo.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final AvailabilityService availabilityService;

    /**
     * Creates a reservation with SERIALIZABLE isolation to prevent double-booking.
     * Steps:
     *  1. Validate space exists
     *  2. Validate time range (endTime > startTime, min 30 min)
     *  3. Validate vehicle belongs to user
     *  4. Check no overlapping reservations (with pessimistic lock)
     *  5. Calculate price
     *  6. Create reservation
     *  7. Return response
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public ReservationResponse createReservation(CreateReservationRequest request, Long userId) {
        // 1. Validate space
        ParkingSpace space = parkingSpaceRepository.findById(request.getParkingSpaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Espacio de estacionamiento", request.getParkingSpaceId()));

        if (space.getStatus() == SpaceStatus.MAINTENANCE || space.getStatus() == SpaceStatus.INACTIVE) {
            throw new ValidationException("El espacio seleccionado no está disponible: " + space.getStatus());
        }

        // 2. Validate time range
        if (!request.getEndTime().isAfter(request.getStartTime())) {
            throw new ValidationException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (request.getStartTime().plusMinutes(30).isAfter(request.getEndTime())) {
            throw new ValidationException("La reserva debe ser de al menos 30 minutos");
        }

        // 3. Validate vehicle ownership
        Vehicle vehicle = vehicleRepository.findByIdAndUserId(request.getVehicleId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo", request.getVehicleId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        // 4. Check for overlapping reservations (pessimistic lock applied inside)
        List<Reservation> overlapping = reservationRepository.findOverlappingForUpdate(
                space.getId(), request.getStartTime(), request.getEndTime());

        if (!overlapping.isEmpty()) {
            throw new ConflictException(
                    "El espacio ya está reservado para el período solicitado: " +
                    request.getStartTime() + " - " + request.getEndTime());
        }

        // 5. Calculate price
        ParkingLot lot = space.getParkingLot();
        Long vehicleTypeId = vehicle.getVehicleType() != null ? vehicle.getVehicleType().getId() : null;
        BigDecimal totalAmount = availabilityService.calculatePrice(
                lot.getId(), vehicleTypeId, request.getStartTime(), request.getEndTime());

        // 6. Create reservation
        Reservation reservation = Reservation.builder()
                .user(user)
                .parkingSpace(space)
                .vehicle(vehicle)
                .parkingLot(lot)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(ReservationStatus.PENDING)
                .totalAmount(totalAmount)
                .currency("PEN")
                .confirmationCode(generateConfirmationCode())
                .notes(request.getNotes())
                .build();

        reservation = reservationRepository.save(reservation);
        log.info("Reservation created: {} for user: {} space: {}",
                reservation.getConfirmationCode(), userId, space.getId());

        return mapToResponse(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getMyReservations(Long userId, String statusParam, Pageable pageable) {
        if (statusParam == null || statusParam.isBlank()) {
            return reservationRepository.findByUserId(userId, pageable).map(this::mapToResponse);
        }

        List<ReservationStatus> statuses = Arrays.stream(statusParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return ReservationStatus.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (statuses.isEmpty()) {
            return reservationRepository.findByUserId(userId, pageable).map(this::mapToResponse);
        }

        return reservationRepository.findByUserIdAndStatusIn(userId, statuses, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getMyReservations(Long userId, ReservationStatus status, Pageable pageable) {
        Page<Reservation> reservations = (status != null)
                ? reservationRepository.findByUserIdAndStatus(userId, status, pageable)
                : reservationRepository.findByUserId(userId, pageable);
        return reservations.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ReservationDetailResponse getReservationDetail(Long reservationId, Long userId, boolean isAdmin) {
        Reservation reservation;
        if (isAdmin) {
            reservation = reservationRepository.findById(reservationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservationId));
        } else {
            reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservationId));
        }
        return mapToDetailResponse(reservation);
    }

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId, Long userId, String reason) {
        Reservation reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservationId));

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new ValidationException("La reserva ya está cancelada");
        }
        if (reservation.getStatus() == ReservationStatus.COMPLETED ||
            reservation.getStatus() == ReservationStatus.ACTIVE) {
            throw new ValidationException("No se puede cancelar una reserva " + reservation.getStatus());
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now());
        reservation.setCancellationReason(reason);

        reservation = reservationRepository.save(reservation);
        log.info("Reservation {} cancelled by user {}", reservationId, userId);
        return mapToResponse(reservation);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservationsByLot(Long parkingLotId, ReservationStatus status, Pageable pageable) {
        return (status != null)
                ? reservationRepository.findByParkingLotIdAndStatus(parkingLotId, status, pageable).map(this::mapToResponse)
                : reservationRepository.findByParkingLotId(parkingLotId, pageable).map(this::mapToResponse);
    }

    // ─── Mapping ──────────────────────────────────────────────────────────────

    private ReservationResponse mapToResponse(Reservation r) {
        return ReservationResponse.builder()
                .id(r.getId())
                .confirmationCode(r.getConfirmationCode())
                .status(r.getStatus().name())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .totalAmount(r.getTotalAmount())
                .currency(r.getCurrency())
                .parkingLotId(r.getParkingLot().getId())
                .parkingLotName(r.getParkingLot().getName())
                .parkingLotAddress(r.getParkingLot().getAddress())
                .parkingSpaceId(r.getParkingSpace().getId())
                .spaceNumber(r.getParkingSpace().getSpaceNumber())
                .vehicleId(r.getVehicle().getId())
                .vehicleLicensePlate(r.getVehicle().getLicensePlate())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ReservationDetailResponse mapToDetailResponse(Reservation r) {
        ParkingLot lot = r.getParkingLot();
        ParkingSpace space = r.getParkingSpace();
        Vehicle vehicle = r.getVehicle();
        User user = r.getUser();

        return ReservationDetailResponse.builder()
                .id(r.getId())
                .confirmationCode(r.getConfirmationCode())
                .status(r.getStatus().name())
                .startTime(r.getStartTime())
                .endTime(r.getEndTime())
                .totalAmount(r.getTotalAmount())
                .currency(r.getCurrency())
                .notes(r.getNotes())
                .cancellationReason(r.getCancellationReason())
                .cancelledAt(r.getCancelledAt())
                .checkedInAt(r.getCheckedInAt())
                .checkedOutAt(r.getCheckedOutAt())
                // Lot
                .parkingLotId(lot.getId())
                .parkingLotName(lot.getName())
                .parkingLotAddress(lot.getAddress())
                .parkingLotLatitude(lot.getLatitude())
                .parkingLotLongitude(lot.getLongitude())
                // Space
                .parkingSpaceId(space.getId())
                .spaceNumber(space.getSpaceNumber())
                .floor(space.getFloor())
                .section(space.getSection())
                // Vehicle
                .vehicleId(vehicle.getId())
                .vehicleLicensePlate(vehicle.getLicensePlate())
                .vehicleBrand(vehicle.getBrand())
                .vehicleModel(vehicle.getModel())
                .vehicleColor(vehicle.getColor())
                // User
                .userId(user.getId())
                .userName(user.getFirstName() + " " + user.getLastName())
                .userEmail(user.getEmail())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private String generateConfirmationCode() {
        return "PKO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
