package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private final ParkingLotRepository parkingLotRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ParkingRatingRepository parkingRatingRepository;
    private final AvailabilityService availabilityService;

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final int MIN_MINUTES = 15;
    private static final int MAX_HOURS = 24;
    private static final int MAX_DAYS_AHEAD = 30;
    /** Ventana para permitir check-in antes de la hora reservada. */
    private static final int EARLY_CHECKIN_MINUTES = 30;
    /** Tiempo de gracia para cancelar (el check-in abrió la ventana). */
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Crea una reserva confirmada (no existe pago, la confirmación es inmediata).
     *  - Si viene parkingSpaceId: usa ese espacio.
     *  - Si NO viene parkingSpaceId pero sí parkingLotId: asigna un espacio libre automáticamente.
     *
     * READ_COMMITTED + bloqueo de la fila del espacio = una sola reserva por espacio/horario.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ReservationResponse createReservation(CreateReservationRequest request, Long userId) {
        LocalDateTime start = request.getStartTime();
        LocalDateTime end = request.getEndTime();

        // 1. Validar horario
        validateTimeRange(start, end);

        // 2. Vehículo del usuario y con tipo asignado
        Vehicle vehicle = vehicleRepository.findByIdAndUserId(request.getVehicleId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo", request.getVehicleId()));
        if (vehicle.getVehicleType() == null) {
            throw new ValidationException("El vehículo no tiene un tipo asignado");
        }
        Long vehicleTypeId = vehicle.getVehicleType().getId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        // 3. El mismo vehículo no puede estar en dos reservas a la vez
        if (reservationRepository.countOverlappingByVehicle(vehicle.getId(), start, end) > 0) {
            throw new ConflictException("Este vehículo ya tiene una reserva en ese horario");
        }

        // 4. Espacio: el elegido o uno asignado automáticamente (ya bloqueado y verificado)
        ParkingSpace space = (request.getParkingSpaceId() != null)
                ? lockAndCheckSpace(request.getParkingSpaceId(), vehicleTypeId, start, end)
                : assignFreeSpace(request.getParkingLotId(), vehicleTypeId, start, end);

        // 5. Estacionamiento activo y abierto
        ParkingLot lot = space.getParkingLot();
        if (!Boolean.TRUE.equals(lot.getIsActive()) || !Boolean.TRUE.equals(lot.getIsOpen())) {
            throw new ValidationException("El estacionamiento no está disponible en este momento");
        }

        // 6. Precio calculado en el servidor
        BigDecimal totalAmount = availabilityService.calculatePrice(lot.getId(), vehicleTypeId, start, end);
        if (totalAmount == null || totalAmount.signum() <= 0) {
            throw new ValidationException("No hay tarifa para este tipo de vehículo en este estacionamiento");
        }

        // 7. Guardar (auto-confirmada: el espacio queda bloqueado por horario)
        Reservation reservation = Reservation.builder()
                .user(user)
                .parkingSpace(space)
                .vehicle(vehicle)
                .parkingLot(lot)
                .startTime(start)
                .endTime(end)
                .status(ReservationStatus.CONFIRMED)
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

    /**
     * Walk-in: el operador registra un ingreso de un vehículo sin reserva previa.
     * Crea una reserva ACTIVE interna propiedad del operador para poder
     * cobrar y liberar el espacio al check-out.
     */
    @Transactional
    public ReservationDetailResponse createWalkIn(Long parkingLotId, Long vehicleTypeId,
                                                  Long requestedSpaceId, Long operatorId, String notes) {
        ParkingLot lot = parkingLotRepository.findById(parkingLotId)
                .orElseThrow(() -> new ResourceNotFoundException("Estacionamiento", parkingLotId));
        if (!Boolean.TRUE.equals(lot.getIsActive()) || !Boolean.TRUE.equals(lot.getIsOpen())) {
            throw new ValidationException("El estacionamiento no está disponible en este momento");
        }
        if (vehicleTypeId == null) {
            throw new ValidationException("El tipo de vehículo es requerido");
        }

        User operator = userRepository.findById(operatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", operatorId));

        // Vehículo marcador único global del sistema para walk-ins
        Vehicle walkInVehicle = vehicleRepository.findByLicensePlateAndIsActiveTrue("WALK-IN")
                .orElseGet(() -> vehicleRepository.save(Vehicle.builder()
                        .user(operator)
                        .licensePlate("WALK-IN")
                        .brand("Vehículo sin reserva")
                        .isActive(true)
                        .vehicleType(null)
                        .build()));

        LocalDateTime now = LocalDateTime.now(LIMA)
                .truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        LocalDateTime provisionalEnd = now.plusHours(1);

        ParkingSpace space;
        if (requestedSpaceId != null) {
            space = lockAndCheckSpace(requestedSpaceId, vehicleTypeId, now, provisionalEnd);
        } else {
            space = assignFreeSpace(parkingLotId, vehicleTypeId, now, provisionalEnd);
        }
        ParkingLot walkInLot = space.getParkingLot();
        if (!walkInLot.getId().equals(parkingLotId)) {
            throw new ValidationException("El espacio elegido no pertenece a este estacionamiento");
        }

        Reservation reservation = Reservation.builder()
                .user(operator)
                .parkingSpace(space)
                .vehicle(walkInVehicle)
                .parkingLot(walkInLot)
                .startTime(now)
                .endTime(provisionalEnd)
                .status(ReservationStatus.ACTIVE)
                .totalAmount(BigDecimal.ZERO)
                .currency("PEN")
                .confirmationCode(generateConfirmationCode())
                .notes(("WALK-IN" + (notes != null && !notes.isBlank() ? " — " + notes : "")))
                .checkedInAt(now)
                .build();

        reservation = reservationRepository.save(reservation);
        space.setStatus(SpaceStatus.OCCUPIED);
        parkingSpaceRepository.save(space);
        refreshAvailableSpaces(walkInLot);
        log.info("Walk-in created {} by operator {} in lot {}", reservation.getConfirmationCode(), operatorId, parkingLotId);

        return mapToDetailResponse(reservation);
    }

    // ─── Helpers de creación ──────────────────────────────────────────────────

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ValidationException("Debes indicar la hora de inicio y de fin");
        }
        if (!end.isAfter(start)) {
            throw new ValidationException("La hora de fin debe ser posterior a la hora de inicio");
        }
        LocalDateTime now = LocalDateTime.now(LIMA);
        if (start.isBefore(now.minusMinutes(15))) {
            throw new ValidationException("La hora de inicio ya pasó");
        }
        if (start.isAfter(now.plusDays(MAX_DAYS_AHEAD))) {
            throw new ValidationException("Solo puedes reservar hasta con " + MAX_DAYS_AHEAD + " días de anticipación");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < MIN_MINUTES) {
            throw new ValidationException("La reserva debe ser de al menos " + MIN_MINUTES + " minutos");
        }
        if (minutes > MAX_HOURS * 60L) {
            throw new ValidationException("La reserva no puede superar las " + MAX_HOURS + " horas");
        }
    }

    /** Espacio elegido por el cliente: se bloquea y se valida. */
    private ParkingSpace lockAndCheckSpace(Long spaceId, Long vehicleTypeId,
                                           LocalDateTime start, LocalDateTime end) {
        ParkingSpace space = parkingSpaceRepository.findByIdForUpdate(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio de estacionamiento", spaceId));

        if (space.getStatus() == SpaceStatus.MAINTENANCE || space.getStatus() == SpaceStatus.INACTIVE) {
            throw new ValidationException("El espacio seleccionado no está disponible: " + space.getStatus());
        }
        if (space.getVehicleType() == null || !space.getVehicleType().getId().equals(vehicleTypeId)) {
            throw new ValidationException("El espacio no es apto para el tipo de vehículo seleccionado");
        }
        // Lectura con FOR UPDATE: ve lo último confirmado por otras transacciones
        if (!reservationRepository.findOverlappingForUpdate(space.getId(), start, end).isEmpty()) {
            throw new ConflictException("El espacio ya está reservado para el período solicitado");
        }
        return space;
    }

    /** Sin espacio elegido: busca uno libre del tipo correcto en el estacionamiento. */
    private ParkingSpace assignFreeSpace(Long parkingLotId, Long vehicleTypeId,
                                         LocalDateTime start, LocalDateTime end) {
        if (parkingLotId == null) {
            throw new ValidationException("Debes indicar el estacionamiento o el espacio");
        }
        List<ParkingSpace> candidates = parkingSpaceRepository
                .findFreeSpaces(parkingLotId, vehicleTypeId, start, end, PageRequest.of(0, 5));

        for (ParkingSpace candidate : candidates) {
            // Se bloquea y se vuelve a verificar por si otro usuario lo tomó justo ahora
            ParkingSpace locked = parkingSpaceRepository.findByIdForUpdate(candidate.getId()).orElse(null);
            if (locked != null
                    && reservationRepository.findOverlappingForUpdate(locked.getId(), start, end).isEmpty()) {
                return locked;
            }
        }
        throw new ConflictException("No hay espacios disponibles para ese horario");
    }

    // ─── Consultas ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getMyReservations(Long userId, String statusParam, Pageable pageable) {
        List<ReservationStatus> statuses = parseStatuses(statusParam);
        if (statuses == null || statuses.isEmpty()) {
            if (statusParam != null && !statusParam.isBlank()) {
                throw new ValidationException("Estado inválido: " + statusParam);
            }
            return reservationRepository.findByUserId(userId, pageable).map(this::mapToResponse);
        }
        return reservationRepository.findByUserIdAndStatusIn(userId, statuses, pageable).map(this::mapToResponse);
    }

    private List<ReservationStatus> parseStatuses(String statusParam) {
        if (statusParam == null || statusParam.isBlank()) {
            return List.of();
        }
        return Arrays.stream(statusParam.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    try {
                        return ReservationStatus.valueOf(s.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new ValidationException("Estado inválido: " + s);
                    }
                })
                .collect(Collectors.toList());
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

    /** Búsqueda por código de confirmación (operador en puerta). */
    @Transactional(readOnly = true)
    public Reservation getReservationByCode(String confirmationCode) {
        String code = confirmationCode == null ? "" : confirmationCode.trim().toUpperCase();
        return reservationRepository.findByConfirmationCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", "código", confirmationCode));
    }

    /** Detalle completo de una reserva por código, para el flujo del operador. */
    @Transactional(readOnly = true)
    public ReservationDetailResponse getReservationDetailByCode(String confirmationCode) {
        return mapToDetailResponse(getReservationByCode(confirmationCode));
    }

    // ─── Cancelar ─────────────────────────────────────────────────────────────

    @Transactional
    public ReservationResponse cancelReservation(Long reservationId, Long userId, String reason) {
        Reservation reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservationId));

        // Lista blanca: solo estas se pueden cancelar
        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ValidationException(
                    "Solo se pueden cancelar reservas pendientes o confirmadas (estado actual: "
                            + reservation.getStatus() + ")");
        }

        // Política: solo hasta la hora de inicio. Después, el operador marca no-show.
        if (!LocalDateTime.now(LIMA).isBefore(reservation.getStartTime())) {
            throw new ValidationException(
                    "Ya no puedes cancelar: la hora de inicio de tu reserva ha pasado. " +
                            "Si no vas a asistir, el operador marcará la reserva como no-show.");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setCancelledAt(LocalDateTime.now(LIMA));
        reservation.setCancellationReason((reason == null || reason.isBlank()) ? "Cancelada por el cliente" : reason);

        reservation = reservationRepository.save(reservation);
        log.info("Reservation {} cancelled by user {}", reservationId, userId);
        return mapToResponse(reservation);
    }

    // ─── Check-In / Check-Out (Operador) ────────────────────────────────────

    @Transactional
    public ReservationResponse checkInReservation(Long reservationId, Long operatorId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservationId));

        ParkingLot lot = reservation.getParkingLot();
        if (lot != null && lot.getOperator() != null && operatorId != null
                && !lot.getOperator().getId().equals(operatorId)) {
            throw new UnauthorizedException("El operador no está asignado a este estacionamiento");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING
                && reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ValidationException(
                    "Solo se puede registrar ingreso en reservas pendientes o confirmadas (estado actual: "
                            + reservation.getStatus() + ")");
        }

        // Ventana de check-in: máximo 30 min antes de la hora reservada
        validateCheckInWindow(reservation);

        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setCheckedInAt(LocalDateTime.now(LIMA));

        ParkingSpace space = reservation.getParkingSpace();
        if (space != null) {
            space.setStatus(SpaceStatus.OCCUPIED);
            parkingSpaceRepository.save(space);
        }
        if (lot != null) {
            refreshAvailableSpaces(lot);
        }

        reservation = reservationRepository.save(reservation);
        log.info("Reservation {} checked-in by operator {}", reservationId, operatorId);
        return mapToResponse(reservation);
    }

    private void validateCheckInWindow(Reservation reservation) {
        LocalDateTime now = LocalDateTime.now(LIMA);
        LocalDateTime earliest = reservation.getStartTime().minusMinutes(EARLY_CHECKIN_MINUTES);
        if (now.isBefore(earliest)) {
            java.time.Duration diff = Duration.between(now, earliest);
            long mins = Math.max(1, (diff.toSeconds() + 59) / 60);
            throw new ValidationException(
                    "Aún es muy temprano para registrar el ingreso. Puedes hacer check-in desde las "
                            + earliest.toLocalTime() + " (en unos " + mins + " min).");
        }
    }

    @Transactional
    public ReservationResponse checkOutReservation(Long reservationId, Long operatorId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservationId));

        ParkingLot lot = reservation.getParkingLot();
        if (lot != null && lot.getOperator() != null && operatorId != null
                && !lot.getOperator().getId().equals(operatorId)) {
            throw new UnauthorizedException("El operador no está asignado a este estacionamiento");
        }

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new ValidationException(
                    "Solo se puede registrar salida en reservas activas (estado actual: "
                            + reservation.getStatus() + ")");
        }

        LocalDateTime now = LocalDateTime.now(LIMA);
        reservation.setStatus(ReservationStatus.COMPLETED);
        reservation.setCheckedOutAt(now);

        // Monto final: precio del tiempo real en cochera (check-in → check-out)
        try {
            Vehicle vehicle = reservation.getVehicle();
            Long vehicleTypeId = (vehicle != null && vehicle.getVehicleType() != null)
                    ? vehicle.getVehicleType().getId() : null;
            if (vehicleTypeId == null) {
                // Walk-ins: el vehículo marcador no tiene tipo; se usa el del espacio
                // asignado (siempre coincide con el tipo registrado por el operador).
                ParkingSpace spaceForPrice = reservation.getParkingSpace();
                if (spaceForPrice != null && spaceForPrice.getVehicleType() != null) {
                    vehicleTypeId = spaceForPrice.getVehicleType().getId();
                }
            }
            LocalDateTime from = reservation.getCheckedInAt() != null ? reservation.getCheckedInAt() : reservation.getStartTime();
            if (vehicleTypeId != null && from.isBefore(now)) {
                BigDecimal finalAmount = availabilityService.calculatePrice(lot.getId(), vehicleTypeId, from, now);
                if (finalAmount != null && finalAmount.signum() > 0) {
                    reservation.setTotalAmount(finalAmount);
                }
            }
        } catch (Exception ex) {
            log.warn("No se pudo recalcular el monto final de la reserva {}: {}", reservationId, ex.getMessage());
        }

        // Liberar el espacio a AVAILABLE
        ParkingSpace space = reservation.getParkingSpace();
        if (space != null) {
            space.setStatus(SpaceStatus.AVAILABLE);
            parkingSpaceRepository.save(space);
        }
        if (lot != null) {
            refreshAvailableSpaces(lot);
        }

        reservation = reservationRepository.save(reservation);
        log.info("Reservation {} checked-out by operator {} final amount {}",
                reservationId, operatorId, reservation.getTotalAmount());
        return mapToResponse(reservation);
    }

    /**
     * Fuente única de verdad para el cupo visible: cuenta los espacios
     * en estado AVAILABLE de la cochera. Elimina el drift del contador.
     */
    private void refreshAvailableSpaces(ParkingLot lot) {
        long available = parkingSpaceRepository.countAvailableByParkingLotId(lot.getId());
        lot.setAvailableSpaces((int) Math.min(available, Integer.MAX_VALUE));
    }

    // ─── Ratings ──────────────────────────────────────────────────────────────

    @Transactional
    public void rateReservation(Long reservationId, Long userId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new ValidationException("La calificación debe ser de 1 a 5");
        }
        Reservation reservation = reservationRepository.findByIdAndUserId(reservationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservationId));
        if (reservation.getStatus() != ReservationStatus.COMPLETED) {
            throw new ValidationException("Solo se pueden calificar reservas completadas");
        }
        if (parkingRatingRepository.existsByReservationId(reservationId)) {
            throw new ConflictException("Ya calificaste esta reserva");
        }
        parkingRatingRepository.save(ParkingRating.builder()
                .reservation(reservation)
                .user(reservation.getUser())
                .parkingLot(reservation.getParkingLot())
                .rating(rating)
                .comment(comment)
                .build());
        log.info("Reservation {} rated {} by user {}", reservationId, rating, userId);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getReservationsByLot(Long parkingLotId, ReservationStatus status, Pageable pageable) {
        return (status != null)
                ? reservationRepository.findByParkingLotIdAndStatus(parkingLotId, status, pageable).map(this::mapToResponse)
                : reservationRepository.findByParkingLotId(parkingLotId, pageable).map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReservationResponse> getAllReservations(ReservationStatus status, Pageable pageable) {
        return (status != null)
                ? reservationRepository.findByStatus(status, pageable).map(this::mapToResponse)
                : reservationRepository.findAll(pageable).map(this::mapToResponse);
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
                .parkingLotLatitude(r.getParkingLot().getLatitude())
                .parkingLotLongitude(r.getParkingLot().getLongitude())
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
        // 3 intentos con sufijo aleatorio para evitar colisiones del UNIQUE
        for (int attempt = 0; attempt < 3; attempt++) {
            String candidate = "PKO-" + randomCode();
            if (reservationRepository.findByConfirmationCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        return "PKO-" + UUID.randomUUID().toString().substring(0, 10).toUpperCase();
    }

    private String randomCode() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        char[] chars = new char[7];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = alphabet.charAt(RANDOM.nextInt(alphabet.length()));
        }
        return new String(chars);
    }
}
