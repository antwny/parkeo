package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.parkeo.dto.request.CreateVehicleRequest;
import pe.parkeo.dto.request.UpdateVehicleRequest;
import pe.parkeo.dto.response.VehicleResponse;
import pe.parkeo.entity.User;
import pe.parkeo.entity.Vehicle;
import pe.parkeo.entity.VehicleType;
import pe.parkeo.exception.ConflictException;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.repository.UserRepository;
import pe.parkeo.repository.VehicleRepository;
import pe.parkeo.repository.VehicleTypeRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(Long userId) {
        return vehicleRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public VehicleResponse createVehicle(Long userId, CreateVehicleRequest request) {
        // Normalizar formato de placa peruana (ej: ABC-123)
        String plate = normalizeLicensePlate(request.getLicensePlate());

        if (vehicleRepository.existsByLicensePlateAndIsActiveTrue(plate)) {
            throw new ConflictException("Ya existe un vehículo registrado con la placa: " + plate);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de vehículo", request.getVehicleTypeId()));

        Vehicle vehicle = Vehicle.builder()
                .user(user)
                .vehicleType(vehicleType)
                .licensePlate(plate)
                .brand(sanitizeText(request.getBrand()))
                .model(sanitizeText(request.getModel()))
                .color(sanitizeText(request.getColor()))
                .year(request.getYear())
                .isActive(true)
                .build();

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public VehicleResponse updateVehicle(Long userId, Long vehicleId, UpdateVehicleRequest request) {
        Vehicle vehicle = vehicleRepository.findByIdAndUserId(vehicleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo", vehicleId));

        if (request.getVehicleTypeId() != null) {
            VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tipo de vehículo", request.getVehicleTypeId()));
            vehicle.setVehicleType(vehicleType);
        }
        if (request.getBrand() != null) vehicle.setBrand(sanitizeText(request.getBrand()));
        if (request.getModel() != null) vehicle.setModel(sanitizeText(request.getModel()));
        if (request.getColor() != null) vehicle.setColor(sanitizeText(request.getColor()));
        if (request.getYear() != null) vehicle.setYear(request.getYear());

        return mapToResponse(vehicleRepository.save(vehicle));
    }

    @Transactional
    public void deleteVehicle(Long userId, Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findByIdAndUserId(vehicleId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehículo", vehicleId));
        vehicle.setIsActive(false);
        vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public List<VehicleType> getVehicleTypes() {
        return vehicleTypeRepository.findByIsActiveTrue();
    }

    // --- MÉTODOS DE NORMALIZACIÓN Y SANITIZACIÓN ---

    private String normalizeLicensePlate(String rawPlate) {
        if (rawPlate == null) return null;
        // Remueve todo lo que no sea letra o número y convierte a mayúsculas
        String clean = rawPlate.toUpperCase().replaceAll("[^A-Z0-9]", "");
        
        // Si ingresaron 6 caracteres (ej. ABC123), inserta el guion
        if (clean.length() == 6) {
            return clean.substring(0, 3) + "-" + clean.substring(3);
        }
        return clean;
    }

    private String sanitizeText(String input) {
        if (input == null) return null;
        // Elimina espacios múltiples y limpia extremos
        return input.trim().replaceAll("\\s+", " ");
    }

    private VehicleResponse mapToResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .vehicleTypeId(vehicle.getVehicleType().getId())
                .vehicleTypeName(vehicle.getVehicleType().getName())
                .licensePlate(vehicle.getLicensePlate())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .color(vehicle.getColor())
                .year(vehicle.getYear())
                .isActive(vehicle.getIsActive())
                .createdAt(vehicle.getCreatedAt())
                .build();
    }
}