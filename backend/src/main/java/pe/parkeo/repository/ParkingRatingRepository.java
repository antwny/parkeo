package pe.parkeo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.parkeo.entity.ParkingRating;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParkingRatingRepository extends JpaRepository<ParkingRating, Long> {

    boolean existsByReservationId(Long reservationId);

    Optional<ParkingRating> findByReservationId(Long reservationId);

    @Query("SELECT AVG(r.rating) FROM ParkingRating r WHERE r.parkingLot.id = :parkingLotId")
    Double findAverageRatingByParkingLotId(@Param("parkingLotId") Long parkingLotId);

    List<ParkingRating> findByParkingLotIdOrderByCreatedAtDesc(Long parkingLotId);
}
