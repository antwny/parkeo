package pe.parkeo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import pe.parkeo.dto.request.UpdateSpaceStatusRequest;
import pe.parkeo.dto.response.ApiResponse;
import pe.parkeo.dto.response.ParkingLotResponse;
import pe.parkeo.dto.response.ParkingSpaceResponse;
import pe.parkeo.dto.response.ReservationResponse;
import pe.parkeo.entity.User;
import pe.parkeo.enums.ReservationStatus;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.exception.UnauthorizedException;
import pe.parkeo.repository.ParkingSpaceRepository;
import pe.parkeo.repository.UserRepository;
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

    @GetMapping("/parking-lots")
    @Operation(summary = "Obtener estacionamientos asignados al operador")
    public ResponseEntity<ApiResponse<List<ParkingLotResponse>>> getMyParkingLots(
            @AuthenticationPrincipal UserDetails userDetails) {
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
