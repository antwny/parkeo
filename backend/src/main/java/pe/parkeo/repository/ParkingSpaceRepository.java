package pe.parkeo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.parkeo.entity.ParkingSpace;
import pe.parkeo.enums.SpaceStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

    List<ParkingSpace> findByParkingLotId(Long parkingLotId);

    List<ParkingSpace> findByParkingLotIdAndStatus(Long parkingLotId, SpaceStatus status);

    // Método derivado necesario para AvailabilityService
    List<ParkingSpace> findByParkingLotIdAndVehicleTypeIdAndStatus(
            Long parkingLotId, Long vehicleTypeId, SpaceStatus status);

    @Query("SELECT ps FROM ParkingSpace ps WHERE ps.parkingLot.id = :lotId " +
           "AND (:vehicleTypeId IS NULL OR ps.vehicleType.id = :vehicleTypeId) " +
           "AND ps.status = 'AVAILABLE'")
    List<ParkingSpace> findAvailable(@Param("lotId") Long lotId, @Param("vehicleTypeId") Long vehicleTypeId);

    @Query("SELECT COUNT(ps) FROM ParkingSpace ps WHERE ps.parkingLot.id = :lotId AND ps.status = 'AVAILABLE'")
    long countAvailableByParkingLotId(@Param("lotId") Long lotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ps FROM ParkingSpace ps WHERE ps.id = :id")
    Optional<ParkingSpace> findByIdForUpdate(@Param("id") Long id);

    /**
     * Espacios del tipo correcto que NO tienen reservas solapadas en ese horario.
     */
    @Query("SELECT ps FROM ParkingSpace ps WHERE ps.parkingLot.id = :lotId " +
           "AND ps.vehicleType.id = :vehicleTypeId " +
           "AND ps.status NOT IN ('MAINTENANCE', 'INACTIVE') " +
           "AND NOT EXISTS (SELECT 1 FROM Reservation r WHERE r.parkingSpace = ps " +
           "    AND r.status IN ('PENDING', 'CONFIRMED', 'IN_USE') " +
           "    AND :startTime < r.endTime AND :endTime > r.startTime) " +
           "ORDER BY ps.id")
    List<ParkingSpace> findFreeSpaces(
            @Param("lotId") Long lotId,
            @Param("vehicleTypeId") Long vehicleTypeId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );
}