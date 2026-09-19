package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.parkeo.dto.response.ParkingSpaceResponse;
import pe.parkeo.entity.ParkingSpace;
import pe.parkeo.enums.SpaceStatus;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.repository.ParkingSpaceRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ParkingSpaceService {

    private final ParkingSpaceRepository parkingSpaceRepository;

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

    @Transactional
    public ParkingSpaceResponse updateSpaceStatus(Long spaceId, String statusStr, String notes) {
        ParkingSpace space = parkingSpaceRepository.findById(spaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Espacio de estacionamiento", spaceId));

        SpaceStatus status;
        try {
            status = SpaceStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new pe.parkeo.exception.ValidationException("Estado inválido: " + statusStr);
        }

        space.setStatus(status);
        if (notes != null) space.setNotes(notes);

        return mapToResponse(parkingSpaceRepository.save(space));
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
