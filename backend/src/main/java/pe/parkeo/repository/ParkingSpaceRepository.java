package pe.parkeo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.parkeo.entity.ParkingSpace;
import pe.parkeo.enums.SpaceStatus;

import java.util.List;

@Repository
public interface ParkingSpaceRepository extends JpaRepository<ParkingSpace, Long> {

    List<ParkingSpace> findByParkingLotId(Long parkingLotId);

    List<ParkingSpace> findByParkingLotIdAndStatus(Long parkingLotId, SpaceStatus status);

    @Query("SELECT ps FROM ParkingSpace ps WHERE ps.parkingLot.id = :lotId " +
           "AND (:vehicleTypeId IS NULL OR ps.vehicleType.id = :vehicleTypeId) " +
           "AND ps.status = 'AVAILABLE'")
    List<ParkingSpace> findAvailable(@Param("lotId") Long lotId, @Param("vehicleTypeId") Long vehicleTypeId);

    @Query("SELECT COUNT(ps) FROM ParkingSpace ps WHERE ps.parkingLot.id = :lotId AND ps.status = 'AVAILABLE'")
    long countAvailableByParkingLotId(@Param("lotId") Long lotId);
}
