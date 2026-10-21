package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    public UserResponse getProfile(Long userId) {
        User user = userRepository.findByIdWithRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", userId));
        return authService.mapToUserResponse(user);
    }

    @Transactional
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
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(authService::mapToUserResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(authService::mapToUserResponse);
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String search, Pageable pageable) {
        String q = search.trim();
        return userRepository.searchAll(q, pageable).map(authService::mapToUserResponse);
    }

    /**
     * Activa/desactiva usuarios con reglas de protección:
     *  - Un admin no puede desactivarse a sí mismo.
     *  - No se puede desactivar al último administrador activo.
     *  - Al desactivar, todas sus sesiones (refresh tokens) se revocan.
     */
    @Transactional
    public UserResponse setUserStatus(Long currentAdminId, Long targetUserId, boolean isActive) {
        User target = userRepository.findByIdWithRoles(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", targetUserId));

        boolean targetIsAdmin = target.getRoles().stream()
                .anyMatch(r -> r.getName() == pe.parkeo.enums.RoleName.ROLE_ADMIN);

        if (!isActive) {
            if (currentAdminId != null && currentAdminId.equals(targetUserId)) {
                throw new ValidationException("No puedes desactivar tu propia cuenta administrador");
            }
            if (targetIsAdmin && userRepository.countActiveAdmins() <= 1) {
                throw new ValidationException(
                        "No puedes desactivar al último administrador activo del sistema");
            }
        }

        target.setIsActive(isActive);
        target = userRepository.save(target);

        if (!isActive) {
            authService.logoutAll(targetUserId);
        }
        return authService.mapToUserResponse(target);
    }
}
