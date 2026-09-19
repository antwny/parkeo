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
import pe.parkeo.dto.request.CreateReservationRequest;
import pe.parkeo.dto.response.ApiResponse;
import pe.parkeo.dto.response.ReservationDetailResponse;
import pe.parkeo.dto.response.ReservationResponse;
import pe.parkeo.entity.User;
import pe.parkeo.enums.ReservationStatus;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.repository.UserRepository;
import pe.parkeo.service.ReservationService;

import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Gestión de reservas de estacionamiento")
public class ReservationController {

    private final ReservationService reservationService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Obtener mis reservas")
    public ResponseEntity<ApiResponse<Page<ReservationResponse>>> getMyReservations(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) ReservationStatus status,
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

        if (request.getEndTime() != null && request.getStartTime() != null &&
                !request.getEndTime().isAfter(request.getStartTime())) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("La hora de fin debe ser posterior a la hora de inicio"));
        }

        ReservationResponse response = reservationService.createReservation(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Reserva creada exitosamente", response));
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
        return ResponseEntity.ok(ApiResponse.ok("Reserva cancelada", response));
    }

    private Long resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", userDetails.getUsername()));
        return user.getId();
    }
}
