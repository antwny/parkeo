// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Acceso/abstracción de datos para el área indicada; conecta operaciones de negocio con sus fuentes.
package pe.parkeo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.parkeo.entity.Role;
import pe.parkeo.enums.RoleName;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}
