package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pe.parkeo.entity.Reservation;
import pe.parkeo.enums.ReservationStatus;
import pe.parkeo.repository.ReservationRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Ciclo de vida de reservas:
 *  - Marca NO_SHOW las reservas confirmadas cuyo horario de inicio ya pasó
 *    y que nunca registraron check-in (con una ventana de gracia).
 * De este modo un espacio nunca queda bloqueado por una reserva abandonada.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReservationExpiryService {

    private final ReservationRepository reservationRepository;

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    /** Minutos de gracia tras la hora de inicio antes de marcar no-show. */
    private static final int NO_SHOW_GRACE_MINUTES = 15;

    @Scheduled(fixedDelayString = "60000", initialDelay = 15000)
    @Transactional
    public void expireStaleReservations() {
        LocalDateTime now = LocalDateTime.now(LIMA);
        LocalDateTime threshold = now.minusMinutes(NO_SHOW_GRACE_MINUTES);
        List<ReservationStatus> statuses = List.of(ReservationStatus.PENDING, ReservationStatus.CONFIRMED);

        List<Reservation> stale = reservationRepository
                .findByStatusInAndStartTimeBefore(statuses, threshold);

        if (stale.isEmpty()) {
            return;
        }

        for (Reservation reservation : stale) {
            reservation.setStatus(ReservationStatus.NO_SHOW);
        }
        reservationRepository.saveAll(stale);
        log.info("Reservas vencidas marcadas como NO_SHOW: {}", stale.size());
    }
}
