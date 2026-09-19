package pe.parkeo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import pe.parkeo.enums.SpaceStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "parking_spaces", indexes = {
    @Index(name = "idx_spaces_lot", columnList = "parking_lot_id"),
    @Index(name = "idx_spaces_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkingSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parking_lot_id", nullable = false)
    private ParkingLot parkingLot;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_type_id")
    private VehicleType vehicleType;

    @Column(name = "space_number", nullable = false, length = 20)
    private String spaceNumber;

    @Column(name = "floor", length = 10)
    private String floor;

    @Column(name = "section", length = 20)
    private String section;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SpaceStatus status = SpaceStatus.AVAILABLE;

    @Column(name = "is_covered", nullable = false)
    @Builder.Default
    private Boolean isCovered = false;

    @Column(name = "has_charging", nullable = false)
    @Builder.Default
    private Boolean hasCharging = false;

    @Column(name = "is_accessible", nullable = false)
    @Builder.Default
    private Boolean isAccessible = false;

    @Column(name = "notes", length = 255)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
