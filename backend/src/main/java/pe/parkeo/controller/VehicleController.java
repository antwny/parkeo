// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Controlador REST; recibe solicitudes HTTP, delega al servicio y define la respuesta del endpoint.
package pe.parkeo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import pe.parkeo.dto.request.CreateVehicleRequest;
import pe.parkeo.dto.request.UpdateVehicleRequest;
import pe.parkeo.dto.response.ApiResponse;
import pe.parkeo.dto.response.VehicleResponse;
import pe.parkeo.entity.User;
import pe.parkeo.entity.VehicleType;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.repository.UserRepository;
import pe.parkeo.service.VehicleService;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehículos", description = "Gestión de vehículos del usuario")
public class VehicleController {

    private final VehicleService vehicleService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Obtener mis vehículos")
    /** Consulta y devuelve: get my vehicles. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getMyVehicles(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = resolveUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.getMyVehicles(userId)));
    }

    @PostMapping
    @Operation(summary = "Registrar un nuevo vehículo")
    /** Crea y devuelve: create vehicle. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateVehicleRequest request) {
        Long userId = resolveUserId(userDetails);
        VehicleResponse response = vehicleService.createVehicle(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Vehículo registrado", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar vehículo")
    /** Actualiza el recurso o estado indicado: update vehicle. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateVehicleRequest request) {
        Long userId = resolveUserId(userDetails);
        VehicleResponse response = vehicleService.updateVehicle(userId, id, request);
        return ResponseEntity.ok(ApiResponse.ok("Vehículo actualizado", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar vehículo")
    /** Elimina o desactiva el recurso indicado: delete vehicle. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<Void>> deleteVehicle(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = resolveUserId(userDetails);
        vehicleService.deleteVehicle(userId, id);
        return ResponseEntity.ok(ApiResponse.ok("Vehículo eliminado", null));
    }

    @GetMapping("/types")
    @Operation(summary = "Obtener tipos de vehículo disponibles")
    /** Consulta y devuelve: get vehicle types. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<List<VehicleType>>> getVehicleTypes() {
        return ResponseEntity.ok(ApiResponse.ok(vehicleService.getVehicleTypes()));
    }

    /** Resuelve el valor o dependencia solicitada: resolve user id. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    private Long resolveUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", userDetails.getUsername()));
        return user.getId();
    }
}
