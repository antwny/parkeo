// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Controlador REST; recibe solicitudes HTTP, delega al servicio y define la respuesta del endpoint.
package pe.parkeo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.parkeo.dto.response.ApiResponse;
import pe.parkeo.dto.response.ParkingLotDetailResponse;
import pe.parkeo.dto.response.ParkingLotResponse;
import pe.parkeo.dto.response.ParkingSpaceResponse;
import pe.parkeo.entity.Schedule;
import pe.parkeo.entity.Tariff;
import pe.parkeo.repository.ScheduleRepository;
import pe.parkeo.repository.TariffRepository;
import pe.parkeo.service.AvailabilityService;
import pe.parkeo.service.ParkingLotService;
import pe.parkeo.service.ParkingSpaceService;

import java.util.List;

@RestController
@RequestMapping("/api/parking")
@RequiredArgsConstructor
@Tag(name = "Estacionamientos", description = "Consulta de estacionamientos y disponibilidad")
public class ParkingLotController {

    private final ParkingLotService parkingLotService;
    private final ParkingSpaceService parkingSpaceService;
    private final ScheduleRepository scheduleRepository;
    private final TariffRepository tariffRepository;
    private final AvailabilityService availabilityService;

    @GetMapping
    @Operation(summary = "Listar todos los estacionamientos activos (con paginación)")
    /** Consulta y devuelve: get all. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<Page<ParkingLotResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.getAllActive(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle completo de un estacionamiento")
    /** Consulta y devuelve: get by id. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<ParkingLotDetailResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.getById(id)));
    }

    @GetMapping("/nearby")
    @Operation(summary = "Buscar estacionamientos por proximidad geográfica")
    /** Consulta y devuelve: get nearby. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<List<ParkingLotResponse>>> getNearby(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam(defaultValue = "5.0") double radius) {
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.findNearby(lat, lon, radius)));
    }

    @GetMapping("/search")
    @Operation(summary = "Buscar estacionamientos por nombre o dirección")
    /** Ejecuta la operación indicada por el nombre: search. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<Page<ParkingLotResponse>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(parkingLotService.search(q, pageable)));
    }

    @GetMapping("/{id}/schedules")
    @Operation(summary = "Obtener horarios del estacionamiento")
    /** Consulta y devuelve: get schedules. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<List<Schedule>>> getSchedules(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(scheduleRepository.findByParkingLotId(id)));
    }

    @GetMapping("/{id}/tariffs")
    @Operation(summary = "Obtener tarifas del estacionamiento")
    /** Consulta y devuelve: get tariffs. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<List<Tariff>>> getTariffs(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(tariffRepository.findByParkingLotIdAndIsActiveTrue(id)));
    }

    @GetMapping(value = {"/{id}/availability", "/{id}/spaces/available"})
    @Operation(summary = "Obtener espacios disponibles del estacionamiento")
    /** Consulta y devuelve: get availability. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<List<ParkingSpaceResponse>>> getAvailability(
            @PathVariable Long id,
            @RequestParam(required = false) Long vehicleTypeId) {
        List<ParkingSpaceResponse> available = parkingSpaceService.getAvailableSpaces(id, vehicleTypeId);
        return ResponseEntity.ok(ApiResponse.ok(available));
    }
}
