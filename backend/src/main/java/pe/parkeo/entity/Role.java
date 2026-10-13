// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Entidad JPA; mapea el concepto del dominio a persistencia relacional y sus relaciones.
package pe.parkeo.entity;

import jakarta.persistence.*;
import lombok.*;
import pe.parkeo.enums.RoleName;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "name", length = 20, nullable = false, unique = true)
    private RoleName name;
}
