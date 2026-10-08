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
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkAvailability(
            @Valid @RequestBody CheckAvailabilityRequest request) {

        AvailabilityResponse response = availabilityService.checkAvailability(request);
        return ResponseEntity.ok(
                ApiResponse.ok("Consulta de disponibilidad realizada con éxito", response));
    }
}