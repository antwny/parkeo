package pe.parkeo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.parkeo.entity.ParkingLot;

import java.util.List;

@Repository
public interface ParkingLotRepository extends JpaRepository<ParkingLot, Long> {

    Page<ParkingLot> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT pl FROM ParkingLot pl WHERE pl.isActive = true AND " +
           "(LOWER(pl.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(pl.address) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(pl.district) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<ParkingLot> searchByNameOrAddress(@Param("query") String query, Pageable pageable);

    /**
     * Finds parking lots within a given radius (in km) using the Haversine formula.
     * Only returns active and open lots.
     */
    @Query("SELECT pl FROM ParkingLot pl WHERE pl.isActive = true AND pl.isOpen = true AND " +
           "(6371 * acos(cos(radians(:lat)) * cos(radians(pl.latitude)) * " +
           "cos(radians(pl.longitude) - radians(:lon)) + " +
           "sin(radians(:lat)) * sin(radians(pl.latitude)))) < :radiusKm " +
           "ORDER BY (6371 * acos(cos(radians(:lat)) * cos(radians(pl.latitude)) * " +
           "cos(radians(pl.longitude) - radians(:lon)) + " +
           "sin(radians(:lat)) * sin(radians(pl.latitude)))) ASC")
    List<ParkingLot> findNearby(@Param("lat") double lat, @Param("lon") double lon, @Param("radiusKm") double radiusKm);

    List<ParkingLot> findByOperatorId(Long operatorId);

    List<ParkingLot> findByOperatorIdAndIsActiveTrue(Long operatorId);
}
