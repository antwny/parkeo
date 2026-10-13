// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Servicio de negocio Spring; coordina validaciones, persistencia y reglas del área indicada.
package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.parkeo.dto.response.UserResponse;
import pe.parkeo.entity.User;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.exception.UnauthorizedException;
import pe.parkeo.exception.ValidationException;
import pe.parkeo.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    @Transactional(readOnly = true)
    /** Consulta y devuelve: get profile. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public UserResponse getProfile(Long userId) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
        return authService.mapToUserResponse(user);
    }

    @Transactional
    /** Actualiza el recurso o estado indicado: update profile. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public UserResponse updateProfile(Long userId, String firstName, String lastName, String phone, String documentNumber) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        if (firstName != null && !firstName.isBlank()) user.setFirstName(firstName);
        if (lastName != null && !lastName.isBlank()) user.setLastName(lastName);
        if (phone != null) user.setPhone(phone);
        if (documentNumber != null) user.setDocumentNumber(documentNumber);

        user = userRepository.save(user);
        return authService.mapToUserResponse(user);
    }

    @Transactional
    /** Ejecuta la operación indicada por el nombre: change password. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new UnauthorizedException("La contraseña actual es incorrecta");
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new ValidationException("La nueva contraseña no puede ser igual a la contraseña actual");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    // ─── Admin methods ─────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    /** Consulta y devuelve: get all users. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(authService::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    /** Actualiza el valor indicado: set user status. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public UserResponse setUserStatus(Long userId, boolean isActive) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
        user.setIsActive(isActive);
        user = userRepository.save(user);
        return authService.mapToUserResponse(user);
    }
}
