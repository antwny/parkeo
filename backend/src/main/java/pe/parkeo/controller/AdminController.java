package pe.parkeo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pe.parkeo.dto.response.ApiResponse;
import pe.parkeo.dto.response.ParkingLotResponse;
import pe.parkeo.dto.response.ReservationDetailResponse;
import pe.parkeo.dto.response.ReservationResponse;
import pe.parkeo.dto.response.UserResponse;
import pe.parkeo.entity.ParkingLot;
import pe.parkeo.entity.User;
import pe.parkeo.enums.ReservationStatus;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.repository.ParkingLotRepository;
import pe.parkeo.repository.ReservationRepository;
import pe.parkeo.repository.UserRepository;
import pe.parkeo.service.AuditService;
import pe.parkeo.service.ParkingLotService;
import pe.parkeo.service.ReservationService;
import pe.parkeo.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Administración", description = "Endpoints de administración del sistema")
public class AdminController {

    private final UserService userService;
    private final ParkingLotService parkingLotService;
    private final ReservationService reservationService;
    private final ParkingLotRepository parkingLotRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    // ─── Users ────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "Listar todos los usuarios (paginado)")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(ApiResponse.ok(userService.searchUsers(search, pageable)));
        }
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers(pageable)));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Activar o desactivar usuario")
    public ResponseEntity<ApiResponse<UserResponse>> setUserStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        Boolean isActive = body.get("isActive");
        if (isActive == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Se requiere el campo 'isActive'"));
        }
        UserResponse response = userService.setUserStatus(resolveAdminId(userDetails), id, isActive);

        auditService.log(resolveAdminId(userDetails), isActive ? "USER_ACTIVATED" : "USER_DEACTIVATED",
                "user", id, null);
        return ResponseEntity.ok(ApiResponse.ok(
                isActive ? "Usuario activado" : "Usuario desactivado y sus sesiones revocadas",
                response));
    }

    // ─── Parking Lots ─────────────────────────────────────────────────────────

    @GetMapping("/parking-lots")
    @Operation(summary = "Listar todos los estacionamientos (incluye inactivos)")
    public ResponseEntity<ApiResponse<Page<ParkingLotResponse>>> getAllParkingLots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.getAllForAdmin(pageable)));
    }

    @PutMapping("/parking-lots/{id}/status")
    @Operation(summary = "Cambiar estado de estacionamiento (abierto/activo)")
    public ResponseEntity<ApiResponse<ParkingLotResponse>> setParkingLotStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        Boolean isOpen = body.get("isOpen");
        Boolean isActive = body.get("isActive");
        ParkingLotResponse response = parkingLotService.updateLotStatus(id, isOpen, isActive);
        auditService.log(resolveAdminId(userDetails), "PARKING_LOT_STATUS_CHANGE",
                "parking_lot", id, (isOpen != null ? "isOpen=" + isOpen + " " : "")
                + (isActive != null ? "isActive=" + isActive : ""));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/parking-lots/{id}/operator")
    @Operation(summary = "Asignar operador a un estacionamiento")
    public ResponseEntity<ApiResponse<ParkingLotResponse>> assignOperator(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long operatorId = body.get("operatorId");
        ParkingLotResponse response = parkingLotService.assignOperator(id, operatorId);
        auditService.log(resolveAdminId(userDetails), "OPERATOR_ASSIGNED",
                "parking_lot", id, operatorId != null ? "operatorId=" + operatorId : "operator unassigned");
        return ResponseEntity.ok(ApiResponse.ok("Operador asignado exitosamente", response));
    }

    @DeleteMapping("/parking-lots/{id}")
    @Operation(summary = "Desactivar estacionamiento (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deactivateParkingLot(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        parkingLotService.updateLotStatus(id, null, false);
        auditService.log(resolveAdminId(userDetails), "PARKING_LOT_DEACTIVATED",
                "parking_lot", id, null);
        return ResponseEntity.ok(ApiResponse.ok("Estacionamiento desactivado", null));
    }

    // ─── Reservations ─────────────────────────────────────────────────────────

    @GetMapping("/reservations")
    @Operation(summary = "Listar todas las reservas del sistema (paginado)")
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> getAllReservations(
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(reservationService.getAllReservations(status, pageable)));
    }

    @GetMapping("/reservations/{id}")
    @Operation(summary = "Detalle completo de una reserva (vista admin)")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> getReservationDetail(
            @PathVariable Long id) {
        // isAdmin = true: el admin puede ver cualquier reserva
        return ResponseEntity.ok(ApiResponse.ok(reservationService.getReservationDetail(id, null, true)));
    }

    // ─── Statistics ───────────────────────────────────────────────────────────

    @GetMapping("/statistics")
    @Operation(summary = "Estadísticas generales del sistema")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalParkingLots", parkingLotRepository.count());
        stats.put("totalReservations", reservationRepository.count());
        stats.put("pendingReservations", reservationRepository.countByStatus(ReservationStatus.PENDING));
        stats.put("confirmedReservations", reservationRepository.countByStatus(ReservationStatus.CONFIRMED));
        stats.put("activeReservations", reservationRepository.countByStatus(ReservationStatus.ACTIVE));
        stats.put("completedReservations", reservationRepository.countByStatus(ReservationStatus.COMPLETED));
        stats.put("cancelledReservations", reservationRepository.countByStatus(ReservationStatus.CANCELLED));
        stats.put("noShowReservations", reservationRepository.countByStatus(ReservationStatus.NO_SHOW));
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Long resolveAdminId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .map(User::getId)
                .orElse(null);
    }
}
