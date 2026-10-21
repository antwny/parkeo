package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.parkeo.dto.response.ParkingSpaceResponse;
import pe.parkeo.entity.ParkingLot;
import pe.parkeo.entity.ParkingSpace;
import pe.parkeo.enums.ReservationStatus;
import pe.parkeo.enums.SpaceStatus;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.exception.ValidationException;
import pe.parkeo.repository.ParkingLotRepository;
import pe.parkeo.repository.ParkingSpaceRepository;
import pe.parkeo.repository.ReservationRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingSpaceService {

    private final ParkingSpaceRepository parkingSpaceRepository;
    private final ParkingLotRepository parkingLotRepository;
    private final ReservationRepository reservationRepository;

    @Transactional(readOnly = true)
    public List<ParkingSpaceResponse> getSpacesByLot(Long parkingLotId) {
        return parkingSpaceRepository.findByParkingLotId(parkingLotId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ParkingSpaceResponse> getAvailableSpaces(Long parkingLotId, Long vehicleTypeId) {
        return parkingSpaceRepository.findAvailable(parkingLotId, vehicleTypeId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Cambia el estado de un espacio con reglas de coherencia:
     *  - RESERVED solo se asigna automáticamente al crear la asignación del sistema, no manualmente.
     *  - No se puede liberar (AVAILABLE) un espacio con un vehículo aún estacionado (reserva ACTIVE).
     *  - El contador available_spaces de la cochera siempre se recalcula (fuente única de verdad).
     */
    @Transactional
    public ParkingSpaceResponse updateSpaceStatus(Long spaceId, String statusStr, String notes) {
        ParkingSpace space = parkingSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio de estacionamiento", spaceId));

        SpaceStatus status;
        try {
            status = SpaceStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Estado inválido: " + statusStr);
        }

        if (status == SpaceStatus.RESERVED) {
            throw new ValidationException(
                    "El estado RESERVED se asigna automáticamente por el sistema al confirmar una reserva");
        }

        if (status == SpaceStatus.AVAILABLE
                && reservationRepository.existsByParkingSpaceIdAndStatus(spaceId, ReservationStatus.ACTIVE)) {
            throw new ValidationException(
                    "No puedes liberar este espacio: hay un vehículo estacionado (reserva activa). "
                            + "Registra la salida del vehículo primero.");
        }

        if (status == SpaceStatus.OCCUPIED
                && reservationRepository.existsByParkingSpaceIdAndStatus(spaceId, ReservationStatus.ACTIVE)
                && space.getStatus() != SpaceStatus.OCCUPIED) {
            throw new ValidationException(
                    "Este espacio ya tiene un vehículo con check-in. Usa la reserva activa para gestionarlo.");
        }

        space.setStatus(status);
        if (notes != null) space.setNotes(notes);

        space = parkingSpaceRepository.save(space);
        refreshLotCounter(space.getParkingLot());
        return mapToResponse(space);
    }

    /**
     * Fuente única de verdad: available_spaces = COUNT(spaces AVAILABLE).
     */
    private void refreshLotCounter(ParkingLot lot) {
        long available = parkingSpaceRepository.countAvailableByParkingLotId(lot.getId());
        lot.setAvailableSpaces((int) Math.min(available, Integer.MAX_VALUE));
        parkingLotRepository.save(lot);
    }

    public ParkingSpaceResponse mapToResponse(ParkingSpace space) {
        return ParkingSpaceResponse.builder()
                .id(space.getId())
                .spaceNumber(space.getSpaceNumber())
                .floor(space.getFloor())
                .section(space.getSection())
                .status(space.getStatus().name())
                .isCovered(space.getIsCovered())
                .hasCharging(space.getHasCharging())
                .isAccessible(space.getIsAccessible())
                .vehicleTypeName(space.getVehicleType() != null ? space.getVehicleType().getName() : null)
                .vehicleTypeId(space.getVehicleType() != null ? space.getVehicleType().getId() : null)
                .build();
    }
}
