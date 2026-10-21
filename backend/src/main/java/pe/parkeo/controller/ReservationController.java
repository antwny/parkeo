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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pe.parkeo.dto.request.CreateRatingRequest;
import pe.parkeo.dto.request.CreateReservationRequest;
import pe.parkeo.dto.response.ApiResponse;
import pe.parkeo.dto.response.ReservationDetailResponse;
import pe.parkeo.dto.response.ReservationResponse;
import pe.parkeo.entity.User;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.repository.UserRepository;
import pe.parkeo.service.AuditService;
import pe.parkeo.service.ReservationService;

import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Gestión de reservas de estacionamiento")
public class ReservationController {

    private final ReservationService reservationService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @GetMapping
    @Operation(summary = "Obtener mis reservas")
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> getMyReservations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Long userId = resolveUserId(userDetails);
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.ok(reservationService.getMyReservations(userId, status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle de una reserva")
    public ResponseEntity<ApiResponse<ReservationDetailResponse>> getReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        
        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.ok(reservationService.getReservationDetail(id, userId, false)));
    }

    @PostMapping
    @Operation(summary = "Crear una nueva reserva")
    public ResponseEntity<ApiResponse<ReservationResponse>> createReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateReservationRequest request) {
        
        Long userId = resolveUserId(userDetails);
        ReservationResponse response = reservationService.createReservation(request, userId);

        User user = userRepository.findById(userId).orElse(null);
        auditService.log(user, "RESERVATION_CREATED", "reservation", response.getId(), null);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created("Reserva creada exitosamente", response));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancelar una reserva")
    public ResponseEntity<ApiResponse<ReservationResponse>> cancelReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        
        Long userId = resolveUserId(userDetails);
        String reason = (body != null) ? body.get("reason") : null;
        ReservationResponse response = reservationService.cancelReservation(id, userId, reason);

        User user = userRepository.findById(userId).orElse(null);
        auditService.log(user, "RESERVATION_CANCELLED", "reservation", id, reason);
        
        return ResponseEntity.ok(ApiResponse.ok("Reserva cancelada", response));
    }

    @PostMapping("/{id}/rating")
    @Operation(summary = "Calificar una reserva completada (1-5 estrellas)")
    public ResponseEntity<ApiResponse<Void>> rateReservation(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody CreateRatingRequest request) {

        Long userId = resolveUserId(userDetails);
        reservationService.rateReservation(id, userId, request.getRating(), request.getComment());
        return ResponseEntity.ok(ApiResponse.ok("¡Gracias por tu calificación!", null));
    }

    private Long resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", userDetails.getUsername()));
        return user.getId();
    }
}