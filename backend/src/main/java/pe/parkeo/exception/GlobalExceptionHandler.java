// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. Tipo de error de dominio o manejador HTTP para comunicar fallos de manera consistente.
package pe.parkeo.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.parkeo.dto.response.ApiResponse;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    /** Ejecuta la operación indicada por el nombre: handle validation errors. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        ApiResponse<Map<String, String>> response = ApiResponse.error("Errores de validación");
        response.setData(errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    /** Ejecuta la operación indicada por el nombre: handle resource not found. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    /** Ejecuta la operación indicada por el nombre: handle unauthorized. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(UnauthorizedException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ConflictException.class)
    /** Ejecuta la operación indicada por el nombre: handle conflict. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    /** Ejecuta la operación indicada por el nombre: handle business validation. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<Void>> handleBusinessValidation(ValidationException ex) {
        log.warn("Business validation error: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    /** Ejecuta la operación indicada por el nombre: handle bad credentials. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Email o contraseña incorrectos"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    /** Ejecuta la operación indicada por el nombre: handle access denied. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("No tienes permiso para acceder a este recurso"));
    }

    @ExceptionHandler(Exception.class)
    /** Ejecuta la operación indicada por el nombre: handle generic exception. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Ha ocurrido un error interno. Por favor intente más tarde."));
    }
}
