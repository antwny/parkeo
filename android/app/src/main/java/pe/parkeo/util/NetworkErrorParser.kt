/*
 * PARKeo · Android
 * Archivo: util/NetworkErrorParser.kt
 * Capa: utilidades transversales (traducción de errores HTTP a mensajes).
 * Propósito: extraer un mensaje legible del cuerpo JSON de error que devuelve el backend
 * Spring Boot, en lugar de mostrar al usuario un JSON crudo o un mensaje genérico.
 * Conceptos clave: Retrofit `Response.errorBody()`, `ResponseBody`, JSON con `org.json`,
 *   contrato `ApiResponse`, errores de validación por campo, `Result.Error` y manejo
 *   defensivo de excepciones.
 * Flujo: un repositorio recibe una respuesta no exitosa, llama a `parseErrorMessage` con
 *   `response.errorBody()` y un texto de respaldo, y envuelve el resultado en
 *   `Result.Error(mensaje, código)`. El ViewModel publica ese mensaje y la pantalla lo
 *   muestra.
 * Relación: lo usan AuthRepositoryImpl y VehicleRepositoryImpl; otros repositorios aplican
 *   su propia extracción o un mapeo por código de estado.
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 6, 9 y 12).
 */
package pe.parkeo.util

import okhttp3.ResponseBody
import org.json.JSONObject

/**
 * Parser de mensajes de error del backend.
 *
 * Contrato esperado (Spring Boot `ApiResponse`):
 * ```
 * { "success": false, "message": "...", "data": { "campo": "mensaje" } }
 * ```
 * `data` puede ser un mapa de errores de validación por campo; en ese caso se concatenan
 * los mensajes. Si no hay mapa útil se usa `message`. Si el cuerpo está vacío, no es JSON o
 * no contiene un mensaje aceptable, se devuelve el texto de respaldo.
 *
 * Importante sobre las excepciones de red: esta utilidad **no** recibe ni clasifica
 * `HttpException`, `IOException` ni timeouts. En este proyecto los repositorios usan
 * `Response<T>` y revisan `isSuccessful`; los fallos de conexión se capturan con
 * `try/catch` y se convierten directamente en `Result.Error(e.message ?: ...)`. Solo se
 * llama aquí cuando ya existe un `errorBody` que se puede leer.
 *
 * Códigos HTTP en el flujo que rodea al parser:
 * - 400: normalmente trae errores de validación en `data`; se listan campo por campo.
 * - 401: el backend puede enviar "credenciales inválidas" o la sesión expiró; algunos
 *   repositorios lo traducen a un mensaje propio.
 * - 403: permiso insuficiente; suele venir con `message` del servidor.
 * - 404: recurso inexistente; se usa el mensaje del cuerpo o el texto de respaldo.
 * - 409: conflicto (por ejemplo, espacio u horario ya ocupado); ReservationRepositoryImpl
 *   tiene un mensaje específico para este código.
 * - 500: error interno; se prefiere el `message` si es útil y, si no, el texto de respaldo.
 * El parser no decide la semántica del código: esa decisión vive en cada repositorio.
 */
object NetworkErrorParser {
    /**
     * Interpreta el cuerpo de error y devuelve el mejor mensaje disponible.
     *
     * Orden de prioridad:
     * 1. Si `data` es un objeto con campos, concatena sus valores no vacíos separados por
     *    saltos de línea. Es el formato típico de validación de Spring.
     * 2. Si no hay mensajes de campo, usa `message` de nivel superior, salvo que sea nulo,
     *    la cadena literal `"null"` o el texto genérico `"Errores de validación"`.
     * 3. Si nada de lo anterior sirve, devuelve `defaultMessage`.
     *
     * El cuerpo se consume con `errorBody.string()`: un `ResponseBody` solo puede leerse
     * una vez, así que no debe reutilizarse después de esta llamada. Cualquier excepción de
     * parseo (JSON inválido, tipo inesperado, etc.) se captura y se responde con el texto de
     * respaldo; nunca se propaga a la UI.
     *
     * @param errorBody cuerpo de la respuesta HTTP fallida; puede ser `null`.
     * @param defaultMessage mensaje de respaldo definido por el repositorio según el
     *   contexto de la operación.
     * @return mensajes de validación unidos por `\n`, el `message` del backend o el
     *   `defaultMessage`. Siempre devuelve una cadena no vacía si el respaldo lo es.
     */
    fun parseErrorMessage(errorBody: ResponseBody?, defaultMessage: String): String {
        if (errorBody == null) return defaultMessage
        return try {
            val jsonString = errorBody.string()
            if (jsonString.isBlank()) return defaultMessage

            val json = JSONObject(jsonString)

            // 1. Si `data` es un objeto, se interpreta como mapa campo -> mensaje de validación.
            if (json.has("data") && !json.isNull("data")) {
                val dataObj = json.optJSONObject("data")
                if (dataObj != null && dataObj.length() > 0) {
                    val errorList = mutableListOf<String>()
                    val keys = dataObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val msg = dataObj.optString(key)
                        if (msg.isNotBlank()) {
                            errorList.add(msg)
                        }
                    }
                    if (errorList.isNotEmpty()) {
                        return errorList.joinToString("\n")
                    }
                }
            }

            // 2. Si no hubo errores por campo, se intenta el mensaje general del backend.
            val message = json.optString("message")
            if (message.isNotBlank() && !message.equals("null", ignoreCase = true) && !message.equals("Errores de validación", ignoreCase = true)) {
                return message
            }

            // 3. Respaldo del repositorio cuando el cuerpo no aporta un mensaje útil.
            defaultMessage
        } catch (_: Exception) {
            /* Cuerpo no JSON o estructura inesperada: se usa el respaldo sin romper el flujo. */
            defaultMessage
        }
    }
}
