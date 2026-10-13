// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Controlador REST; recibe solicitudes HTTP, delega al servicio y define la respuesta del endpoint.
package pe.parkeo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.parkeo.dto.request.CheckAvailabilityRequest;
import pe.parkeo.dto.response.ApiResponse;
import pe.parkeo.dto.response.AvailabilityResponse;
import pe.parkeo.service.AvailabilityService;

@RestController
@RequestMapping("/api/v1/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @PostMapping("/check")
    /** Comprueba la condición indicada: check availability. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkAvailability(
            @Valid @RequestBody CheckAvailabilityRequest request) {

        AvailabilityResponse response = availabilityService.checkAvailability(request);
        return ResponseEntity.ok(
                ApiResponse.ok("Consulta de disponibilidad realizada con éxito", response));
    }
}