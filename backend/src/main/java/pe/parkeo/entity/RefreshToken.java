// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Entidad JPA; mapea el concepto del dominio a persistencia relacional y sus relaciones.
package pe.parkeo.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "refresh_tokens", indexes = {
    @Index(name = "idx_refresh_tokens_token", columnList = "token"),
    @Index(name = "idx_refresh_tokens_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private Boolean isRevoked = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Indica si se cumple la condición indicada: is expired. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /** Indica si se cumple la condición indicada: is valid. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public boolean isValid() {
        return !isRevoked && !isExpired();
    }
}
