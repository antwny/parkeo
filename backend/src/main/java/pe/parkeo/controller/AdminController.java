package pe.parkeo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.parkeo.dto.response.ApiResponse;
import pe.parkeo.dto.response.ParkingLotResponse;
import pe.parkeo.dto.response.ReservationDetailResponse;
import pe.parkeo.dto.response.ReservationResponse;
import pe.parkeo.dto.response.UserResponse;
import pe.parkeo.entity.ParkingLot;
import pe.parkeo.enums.ReservationStatus;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.repository.ParkingLotRepository;
import pe.parkeo.repository.ReservationRepository;
import pe.parkeo.service.ParkingLotService;
import pe.parkeo.service.ReservationService;
import pe.parkeo.service.UserService;

import java.util.HashMap;
import java.util.List;
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

    // ─── Users ────────────────────────────────────────────────────────────────

    @GetMapping("/users")
    @Operation(summary = "Listar todos los usuarios")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getAllUsers()));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Activar o desactivar usuario")
    public ResponseEntity<ApiResponse<UserResponse>> setUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        Boolean isActive = body.get("isActive");
        if (isActive == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Se requiere el campo 'isActive'"));
        }
        return ResponseEntity.ok(ApiResponse.ok(userService.setUserStatus(id, isActive)));
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
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        Boolean isOpen = body.get("isOpen");
        Boolean isActive = body.get("isActive");
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.updateLotStatus(id, isOpen, isActive)));
    }

    @PutMapping("/parking-lots/{id}/operator")
    @Operation(summary = "Asignar operador a un estacionamiento")
    public ResponseEntity<ApiResponse<ParkingLotResponse>> assignOperator(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long operatorId = body.get("operatorId");
        return ResponseEntity.ok(ApiResponse.ok("Operador asignado exitosamente",
                parkingLotService.assignOperator(id, operatorId)));
    }

    @DeleteMapping("/parking-lots/{id}")
    @Operation(summary = "Desactivar estacionamiento (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deactivateParkingLot(@PathVariable Long id) {
        parkingLotService.updateLotStatus(id, null, false);
        return ResponseEntity.ok(ApiResponse.ok("Estacionamiento desactivado", null));
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
        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
