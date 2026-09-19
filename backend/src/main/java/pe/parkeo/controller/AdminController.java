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
    public ResponseEntity<ApiResponse<Page<ParkingLot>>> getAllParkingLots(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(parkingLotRepository.findAll(pageable)));
    }

    @PostMapping("/parking-lots")
    @Operation(summary = "Crear nuevo estacionamiento")
    public ResponseEntity<ApiResponse<ParkingLot>> createParkingLot(
            @RequestBody ParkingLot parkingLot) {
        ParkingLot saved = parkingLotRepository.save(parkingLot);
        return ResponseEntity.ok(ApiResponse.ok("Estacionamiento creado", saved));
    }

    @PutMapping("/parking-lots/{id}")
    @Operation(summary = "Actualizar estacionamiento")
    public ResponseEntity<ApiResponse<ParkingLot>> updateParkingLot(
            @PathVariable Long id,
            @RequestBody ParkingLot updates) {
        ParkingLot lot = parkingLotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estacionamiento", id));
        if (updates.getName() != null) lot.setName(updates.getName());
        if (updates.getDescription() != null) lot.setDescription(updates.getDescription());
        if (updates.getAddress() != null) lot.setAddress(updates.getAddress());
        if (updates.getDistrict() != null) lot.setDistrict(updates.getDistrict());
        if (updates.getCity() != null) lot.setCity(updates.getCity());
        if (updates.getPhone() != null) lot.setPhone(updates.getPhone());
        if (updates.getEmail() != null) lot.setEmail(updates.getEmail());
        if (updates.getIsOpen() != null) lot.setIsOpen(updates.getIsOpen());
        return ResponseEntity.ok(ApiResponse.ok("Estacionamiento actualizado", parkingLotRepository.save(lot)));
    }

    @DeleteMapping("/parking-lots/{id}")
    @Operation(summary = "Desactivar estacionamiento (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deactivateParkingLot(@PathVariable Long id) {
        ParkingLot lot = parkingLotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estacionamiento", id));
        lot.setIsActive(false);
        parkingLotRepository.save(lot);
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
