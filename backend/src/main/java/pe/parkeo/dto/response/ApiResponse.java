// Guía maestra para el estudiante: [[docs/GUIA_MAESTRA_ESTUDIANTE.md]]. DTO serializable; representa datos que cruzan el contrato HTTP entre la app y el backend.
package pe.parkeo.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public ApiResponse() {
        this.timestamp = LocalDateTime.now();
    }

    /** Ejecuta la operación indicada por el nombre: ok. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("ok");
        response.setData(data);
        return response;
    }

    /** Ejecuta la operación indicada por el nombre: ok. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public static <T> ApiResponse<T> ok(String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        return response;
    }

    /** Respuesta para recursos creados (se usa junto con HTTP 201). */
    public static <T> ApiResponse<T> created(String message, T data) {
        return ok(message, data);
    }

    /** Ejecuta la operación indicada por el nombre: error. Consulte el catálogo de métodos y conceptos en la guía maestra. */
    public static <T> ApiResponse<T> error(String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setData(null);
        return response;
    }
}