package pe.parkeo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pe.parkeo.dto.request.CreateWalkInRequest;
import pe.parkeo.dto.request.UpdateSpaceStatusRequest;
import pe.parkeo.dto.response.ApiResponse;
import pe.parkeo.dto.response.ParkingLotResponse;
import pe.parkeo.dto.response.ParkingSpaceResponse;
import pe.parkeo.dto.response.ReservationDetailResponse;
import pe.parkeo.dto.response.ReservationResponse;
import pe.parkeo.entity.User;
import pe.parkeo.enums.ReservationStatus;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.exception.UnauthorizedException;
import pe.parkeo.repository.ParkingSpaceRepository;
import pe.parkeo.repository.UserRepository;
import pe.parkeo.service.AuditService;
import pe.parkeo.service.ParkingLotService;
import pe.parkeo.service.ParkingSpaceService;
import pe.parkeo.service.ReservationService;

import java.util.List;

@RestController
@RequestMapping("/api/operator")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR')")
@RequiredArgsConstructor
@Tag(name = "Operador", description = "Endpoints para operadores de estacionamiento")
public class OperatorController {

    private final ParkingLotService parkingLotService;
    private final ParkingSpaceService parkingSpaceService;
    private final ReservationService reservationService;
    private final UserRepository userRepository;
    private final ParkingSpaceRepository parkingSpaceRepository;
    private final AuditService auditService;

    @GetMapping("/parking-lots")
    @Operation(summary = "Obtener estacionamientos asignados al operador")
    public ResponseEntity<ApiResponse<List<ParkingLotResponse>>> getMyParkingLots(
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return ResponseEntity.ok(ApiResponse.ok(
                    parkingLotService.getAllActive(PageRequest.of(0, 100)).getContent()));
        }
        Long operatorId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.getByOperator(operatorId)));
    }

    @GetMapping("/parking-lots/{id}/spaces")
    @Operation(summary = "Obtener espacios de un estacionamiento")
    public ResponseEntity<ApiResponse<List<ParkingSpaceResponse>>> getSpaces(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        validateOperatorAccessToLot(userDetails, id);
        return ResponseEntity.ok(ApiResponse.ok(parkingSpaceService.getSpacesByLot(id)));
    }

    @PatchMapping("/spaces/{spaceId}/status")
    @Operation(summary = "Actualizar estado de un espacio")
    public ResponseEntity<ApiResponse<ParkingSpaceResponse>> updateSpaceStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long spaceId,
            @Valid @RequestBody UpdateSpaceStatusRequest request) {
        // Validate operator has access to this space's parking lot
        var space = parkingSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio", spaceId));
        validateOperatorAccessToLot(userDetails, space.getParkingLot().getId());

        ParkingSpaceResponse response = parkingSpaceService.updateSpaceStatus(
                spaceId, request.getStatus(), request.getNotes());
        Long operatorId = resolveUserId(userDetails);
        User actor = userRepository.findById(operatorId).orElse(null);
        auditService.log(actor, "SPACE_STATUS_CHANGE", "parking_space", spaceId,
                request.getStatus() + (request.getNotes() != null ? " — " + request.getNotes() : ""));
        return ResponseEntity.ok(ApiResponse.ok("Estado actualizado", response));
    }

    @GetMapping("/reservations")
    @Operation(summary = "Obtener reservas de los estacionamientos del operador")
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> getReservations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long parkingLotId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        if (parkingLotId == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Se requiere 'parkingLotId'"));
        }

        validateOperatorAccessToLot(userDetails, parkingLotId);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(
                reservationService.getReservationsByLot(parkingLotId, status, pageable)));
    }

    @PatchMapping("/reservations/{id}/check-in")
    @Operation(summary = "Registrar ingreso del vehículo a la cochera")
    public ResponseEntity<ApiResponse<ReservationResponse>> checkIn(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long operatorId = resolveUserId(userDetails);
        ReservationResponse response = reservationService.checkInReservation(id, operatorId);

        User actor = userRepository.findById(operatorId).orElse(null);
        auditService.log(actor, "RESERVATION_CHECK_IN", "reservation", id, null);
        return ResponseEntity.ok(ApiResponse.ok("Ingreso registrado exitosamente", response));
    }

    @PatchMapping("/reservations/{id}/check-out")
    @Operation(summary = "Registrar salida del vehículo de la cochera")
    public ResponseEntity<ApiResponse<ReservationResponse>> checkOut(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long operatorId = resolveUserId(userDetails);
        ReservationResponse response = reservationService.checkOutReservation(id, operatorId);

        User actor = userRepository.findById(operatorId).orElse(null);
        auditService.log(actor, "RESERVATION_CHECK_OUT", "reservation", id, null);
        return ResponseEntity.ok(ApiResponse.ok("Salida registrada exitosamente", response));
    }

    // ─── Búsqueda por código (QR / boarding pass) ────────────────────────────
    @GetMapping("/reservations/by-code/{code}")
    @Operation(summary = "Buscar una reserva por código de confirmación (PKO-XXXXXXX)")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> getByCode(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String code) {
        ReservationDetailResponse detail = reservationService.getReservationDetailByCode(code);
        // El operador solo ve reservas de sus cocheras (admin ve todo)
        validateOperatorAccessToLot(userDetails, detail.getParkingLotId());
        return ResponseEntity.ok(ApiResponse.ok(detail));
    }

    // ─── Walk-in: ingreso sin reserva previa ──────────────────────────────────
    @PostMapping("/walk-ins")
    @Operation(summary = "Registrar un vehículo walk-in (sin reserva) y ocupar un espacio")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> createWalkIn(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateWalkInRequest request) {
        Long operatorId = resolveUserId(userDetails);
        validateOperatorAccessToLot(userDetails, request.getParkingLotId());

        ReservationDetailResponse response = reservationService.createWalkIn(
                request.getParkingLotId(),
                request.getVehicleTypeId(),
                request.getParkingSpaceId(),
                operatorId,
                request.getNotes());

        User actor = userRepository.findById(operatorId).orElse(null);
        auditService.log(actor, "WALK_IN_CREATED", "reservation",
                response.getId(), request.getNotes());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Ingreso walk-in registrado", response));
    }

    private Long resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", userDetails.getUsername()));
        return user.getId();
    }

    private void validateOperatorAccessToLot(UserDetails userDetails, Long lotId) {
        Long operatorId = resolveUserId(userDetails);
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            List<ParkingLotResponse> myLots = parkingLotService.getByOperator(operatorId);
            boolean hasAccess = myLots.stream().anyMatch(l -> l.getId().equals(lotId));
            if (!hasAccess) {
                throw new UnauthorizedException("No tienes acceso a este estacionamiento");
            }
        }
    }
}
