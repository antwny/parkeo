package pe.parkeo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.parkeo.entity.User;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    @Query("SELECT u FROM User u WHERE LOWER(CONCAT(u.firstName, ' ', u.lastName, ' ', u.email)) " +
            "LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<User> searchAll(@Param("q") String q, Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE u.isActive = true AND r.name = :role")
    long countActiveAdminsByRole(@Param("role") pe.parkeo.enums.RoleName role);

    default long countActiveAdmins() {
        return countActiveAdminsByRole(pe.parkeo.enums.RoleName.ROLE_ADMIN);
    }
}
