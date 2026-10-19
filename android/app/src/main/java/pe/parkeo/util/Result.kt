/*
 * PARKeo · Android
 * Archivo: util/Result.kt
 * Capa: utilidades transversales (modelo de resultado).
 * Propósito: representar el resultado de una operación (sobre todo de red) como un valor
 * explícito con éxito, error o carga, en lugar de lanzar excepciones hasta la UI.
 * Conceptos clave: clase sellada (`sealed class`), genericidad, varianza `out T`,
 *   `data class`, `object`, exhaustividad del `when`, funciones inline y encadenamiento.
 * Flujo: los repositorios envuelven las respuestas HTTP en `Result.Success` o
 *   `Result.Error`; los ViewModels hacen `when` sobre ese valor y publican estado; las
 *   pantallas nunca reciben una excepción de Retrofit directamente.
 * Relación: lo usan todos los repositorios de dominio, todos los ViewModels y la utilidad
 *   NetworkErrorParser (a través del mensaje que producen los repositorios).
 * Guía de estudio: docs/GUIA_MAESTRA_ESTUDIANTE.md (secciones 5, 6 y 12).
 */
package pe.parkeo.util

/**
 * Resultado tipado de una operación.
 *
 * Ventajas frente a dejar escapar excepciones:
 * - La firma de la función suspendida declara que puede fallar; el llamador no lo olvida.
 * - El `when` sobre una clase sellada es exhaustivo: el compilador obliga a considerar
 *   todas las variantes.
 * - El error viaja como dato (mensaje y código), no como una traza que la UI deba atrapar.
 * - Se puede transformar o encadenar sin `try/catch` repetidos.
 *
 * `out T` permite usar `Result<Subclase>` donde se espera `Result<Superclase>`. Las
 * variantes que no transportan datos usan `Nothing`, que es subtipo de cualquier tipo.
 *
 * @param T tipo del dato exitoso. Es covariante (`out`), por lo que solo se produce, no se
 *   consume, dentro de esta jerarquía.
 */
sealed class Result<out T> {
    /**
     * Operación exitosa.
     *
     * @param data dato producido por el repositorio (DTO, lista, `Unit`, etc.).
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Operación fallida con un mensaje apto para el usuario.
     *
     * @param message texto ya normalizado por el repositorio o `NetworkErrorParser`.
     * @param code código HTTP cuando la fuente fue una respuesta de red; `null` si el fallo
     *   ocurrió antes de obtener una respuesta (por ejemplo, un error de conexión).
     */
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()

    /**
     * Estado intermedio de carga.
     *
     * Los repositorios de este proyecto no lo emiten: los ViewModels publican su propio
     * `isLoading`. Se conserva para que el modelo sea completo y los `when` incluyan la
     * rama `else`, aunque hoy quede vacía.
     */
    object Loading : Result<Nothing>()
}

/**
 * Ejecuta un efecto secundario solo si el resultado es exitoso y devuelve el mismo
 * resultado para poder encadenar otras llamadas.
 *
 * Es `inline` para que el bloque no cree un objeto adicional. No transforma el valor; para
 * eso haría falta un `map`. Tampoco captura excepciones del bloque.
 *
 * @param block acción que recibe el dato exitoso.
 * @return este mismo `Result`, con éxito, error o carga.
 */
inline fun <T> Result<T>.onSuccess(block: (T) -> Unit): Result<T> {
    if (this is Result.Success) block(data)
    return this
}

/**
 * Ejecuta un efecto secundario solo si el resultado es un error y devuelve el mismo
 * resultado para encadenar.
 *
 * @param block acción que recibe el mensaje de error; no recibe el código HTTP.
 * @return este mismo `Result`, con éxito, error o carga.
 */
inline fun <T> Result<T>.onError(block: (String) -> Unit): Result<T> {
    if (this is Result.Error) block(message)
    return this
}
