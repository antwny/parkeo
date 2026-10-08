package pe.parkeo.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.parkeo.entity.Reservation;
import pe.parkeo.enums.ReservationStatus;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Page<Reservation> findByUserId(Long userId, Pageable pageable);

    Page<Reservation> findByUserIdAndStatus(Long userId, ReservationStatus status, Pageable pageable);

    Page<Reservation> findByUserIdAndStatusIn(Long userId, Collection<ReservationStatus> statuses, Pageable pageable);

    Optional<Reservation> findByIdAndUserId(Long id, Long userId);

    Optional<Reservation> findByConfirmationCode(String confirmationCode);

    Page<Reservation> findByParkingLotId(Long parkingLotId, Pageable pageable);

    Page<Reservation> findByParkingLotIdAndStatus(Long parkingLotId, ReservationStatus status, Pageable pageable);

    /**
     * Devuelve los IDs de los espacios ocupados o reservados en una cochera para un rango de tiempo.
     * Usado por AvailabilityService.
     */
    @Query("SELECT DISTINCT r.parkingSpace.id FROM Reservation r " +
           "WHERE r.parkingLot.id = :parkingLotId " +
           "AND r.status IN :statuses " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    List<Long> findReservedSpaceIds(
            @Param("parkingLotId") Long parkingLotId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("statuses") Collection<ReservationStatus> statuses
    );

    /**
     * Overlap check para un espacio específico.
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.parkingSpace.id = :spaceId " +
           "AND r.status IN ('PENDING', 'CONFIRMED', 'IN_USE') " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    long countOverlapping(
            @Param("spaceId") Long spaceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Excluye una reserva específica (para actualizaciones de rango de tiempo).
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.parkingSpace.id = :spaceId " +
           "AND r.id <> :excludeId " +
           "AND r.status IN ('PENDING', 'CONFIRMED', 'IN_USE') " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    long countOverlappingExcluding(
            @Param("spaceId") Long spaceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId
    );

    /**
     * Bloqueo pesimista para evitar reservas concurrentes en la misma plaza.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reservation r WHERE r.parkingSpace.id = :spaceId " +
           "AND r.status IN ('PENDING', 'CONFIRMED', 'IN_USE') " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    List<Reservation> findOverlappingForUpdate(
            @Param("spaceId") Long spaceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Comprueba si el vehículo ya tiene una reserva activa/pendiente en ese mismo horario.
     */
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.vehicle.id = :vehicleId " +
           "AND r.status IN ('PENDING', 'CONFIRMED', 'IN_USE') " +
           "AND r.startTime < :endTime AND r.endTime > :startTime")
    long countOverlappingByVehicle(
            @Param("vehicleId") Long vehicleId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    // Métodos de estadísticas y conteo
    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.status = :status")
    long countByStatus(@Param("status") ReservationStatus status);

    @Query("SELECT COUNT(r) FROM Reservation r WHERE r.parkingLot.operator.id = :operatorId")
    long countByOperatorId(@Param("operatorId") Long operatorId);
}