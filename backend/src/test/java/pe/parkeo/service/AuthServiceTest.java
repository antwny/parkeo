package pe.parkeo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.parkeo.config.JwtConfig;
import pe.parkeo.dto.request.LoginRequest;
import pe.parkeo.dto.request.RegisterRequest;
import pe.parkeo.dto.response.AuthResponse;
import pe.parkeo.entity.Role;
import pe.parkeo.entity.User;
import pe.parkeo.enums.RoleName;
import pe.parkeo.exception.ConflictException;
import pe.parkeo.repository.RefreshTokenRepository;
import pe.parkeo.repository.RoleRepository;
import pe.parkeo.repository.UserRepository;
import pe.parkeo.security.JwtTokenProvider;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtConfig jwtConfig;

    @InjectMocks
    AuthService authService;

    private Role userRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        userRole = Role.builder().id(1L).name(RoleName.ROLE_USER).build();
        testUser = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .passwordHash("$2a$12$hashedpassword")
                .isActive(true)
                .emailVerified(false)
                .build();
        testUser.getRoles().add(userRole);
    }

    @Test
    @DisplayName("register: debe registrar usuario nuevo correctamente")
    void register_shouldCreateNewUser() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("Password1")
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_USER)).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$12$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access.token.here");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtConfig.getExpiration()).thenReturn(86400000L);
        when(jwtConfig.getRefreshExpiration()).thenReturn(604800000L);

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.token.here");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser()).isNotNull();
        assertThat(response.getUser().getEmail()).isEqualTo("john@example.com");

        verify(userRepository).existsByEmail("john@example.com");
        verify(passwordEncoder).encode("Password1");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register: debe lanzar ConflictException cuando el email ya existe")
    void register_shouldThrowConflict_whenEmailAlreadyExists() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("existing@example.com")
                .password("Password1")
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("existing@example.com");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login: debe retornar AuthResponse con tokens válidos")
    void login_shouldReturnAuthResponse() {
        LoginRequest request = new LoginRequest("john@example.com", "Password1");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(new UsernamePasswordAuthenticationToken("john@example.com", null));
        when(userRepository.findByEmailWithRoles("john@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn("access.token");
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(jwtConfig.getExpiration()).thenReturn(86400000L);
        when(jwtConfig.getRefreshExpiration()).thenReturn(604800000L);

        AuthResponse response = authService.login(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access.token");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    @DisplayName("login: debe propagar BadCredentialsException cuando credenciales son incorrectas")
    void login_shouldPropagateBadCredentials() {
        LoginRequest request = new LoginRequest("john@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }
}
