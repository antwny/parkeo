package pe.parkeo.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.parkeo.enums.DayOfWeek;

@Entity
@Table(name = "schedules", indexes = {
    @Index(name = "idx_schedules_lot", columnList = "parking_lot_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_lot_id", nullable = false)
    private ParkingLot parkingLot;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 15)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time", nullable = false, length = 8)
    private String openTime;   // e.g. "08:00:00"

    @Column(name = "close_time", nullable = false, length = 8)
    private String closeTime;  // e.g. "22:00:00"

    @Column(name = "is_closed", nullable = false)
    @Builder.Default
    private Boolean isClosed = false;
}
