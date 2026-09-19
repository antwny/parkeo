package pe.parkeo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.parkeo.entity.Schedule;
import pe.parkeo.enums.DayOfWeek;

import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByParkingLotId(Long parkingLotId);

    Optional<Schedule> findByParkingLotIdAndDayOfWeek(Long parkingLotId, DayOfWeek dayOfWeek);
}
