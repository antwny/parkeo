// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Controlador REST; recibe solicitudes HTTP, delega al servicio y define la respuesta del endpoint.
package pe.parkeo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.parkeo.dto.request.LoginRequest;
import pe.parkeo.dto.request.RefreshTokenRequest;
import pe.parkeo.dto.request.RegisterRequest;
import pe.parkeo.dto.response.ApiResponse;
import pe.parkeo.dto.response.AuthResponse;
import pe.parkeo.service.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Endpoints de registro, login y gestión de tokens")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Registrar nuevo usuario")
    /** Registra y devuelve: register. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Usuario registrado exitosamente", response));
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión")
    /** Autentica las credenciales y actualiza el estado de sesión: login. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login exitoso", response));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar access token usando refresh token")
    /** Ejecuta la operación indicada por el nombre: refresh. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token renovado exitosamente", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Cerrar sesión y revocar refresh token")
    /** Cierra la sesión y limpia el estado local/remoto correspondiente: logout. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Sesión cerrada exitosamente", null));
    }
}
