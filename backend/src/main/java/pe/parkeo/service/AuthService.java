package pe.parkeo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.parkeo.dto.request.LoginRequest;
import pe.parkeo.dto.request.RefreshTokenRequest;
import pe.parkeo.dto.request.RegisterRequest;
import pe.parkeo.dto.response.AuthResponse;
import pe.parkeo.dto.response.UserResponse;
import pe.parkeo.entity.RefreshToken;
import pe.parkeo.entity.Role;
import pe.parkeo.entity.User;
import pe.parkeo.enums.RoleName;
import pe.parkeo.exception.ConflictException;
import pe.parkeo.exception.ResourceNotFoundException;
import pe.parkeo.exception.UnauthorizedException;
import pe.parkeo.repository.RefreshTokenRepository;
import pe.parkeo.repository.RoleRepository;
import pe.parkeo.repository.UserRepository;
import pe.parkeo.security.JwtTokenProvider;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final pe.parkeo.config.JwtConfig jwtConfig;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Ya existe un usuario registrado con el email: " + request.getEmail());
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", RoleName.ROLE_USER));

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .documentNumber(request.getDocumentNumber())
                .isActive(true)
                .emailVerified(false)
                .build();
        user.getRoles().add(userRole);

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", request.getEmail()));

        // Update last login timestamp
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("User logged in: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Refresh token no encontrado o inválido"));

        if (!refreshToken.isValid()) {
            throw new UnauthorizedException("El refresh token ha expirado o ha sido revocado");
        }

        // Revoke old refresh token (rotation)
        refreshToken.setIsRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        log.info("Token refreshed for user: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenRepository.findByToken(refreshTokenValue).ifPresent(token -> {
            token.setIsRevoked(true);
            refreshTokenRepository.save(token);
            log.info("User logged out, refresh token revoked");
        });
    }

    @Transactional
    public void logoutAll(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
        log.info("All sessions revoked for userId: {}", userId);
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────────

    private AuthResponse generateAuthResponse(User user) {
        String primaryRole = user.getRoles().stream()
                .map(r -> r.getName().name())
                .findFirst()
                .orElse(RoleName.ROLE_USER.name());

        // Prefer admin role if present
        for (Role role : user.getRoles()) {
            if (role.getName() == RoleName.ROLE_ADMIN) {
                primaryRole = RoleName.ROLE_ADMIN.name();
                break;
            }
            if (role.getName() == RoleName.ROLE_OPERATOR) {
                primaryRole = RoleName.ROLE_OPERATOR.name();
            }
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), primaryRole);
        String rawRefreshToken = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(rawRefreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtConfig.getRefreshExpiration() / 1000))
                .isRevoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        UserResponse userResponse = mapToUserResponse(user);
        return AuthResponse.of(accessToken, rawRefreshToken, jwtConfig.getExpiration() / 1000, userResponse);
    }

    public UserResponse mapToUserResponse(User user) {
        List<String> roles = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .documentNumber(user.getDocumentNumber())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .roles(roles)
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    /**
     * Cleanup expired and revoked tokens daily at 2 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredAndRevoked(LocalDateTime.now());
        log.info("Expired/revoked refresh tokens cleaned up");
    }
}
