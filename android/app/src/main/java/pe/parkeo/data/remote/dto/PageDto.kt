/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/data/remote/dto/PageDto.kt
 * Capa: datos/red · contrato (DTO de paginación).
 * Propósito: representar la forma paginada que devuelve Spring Data (`Page<T>`) dentro
 * del sobre `ApiResponseDto<T>`, sin acoplarse a la clase Java del backend.
 * Contrato JSON: `{"content":[...],"totalElements":120,"totalPages":6,"size":20,
 * "number":0,"first":true,"last":false,"empty":false}`.
 * Endpoints: `GET /api/parking`, `GET /api/parking/search`,
 * `GET /api/reservations`, `GET /api/admin/parking-lots`,
 * `GET /api/operator/reservations` (y cualquier otro que devuelva `Page<T>`).
 * Conceptos clave: Kotlin Serialization, `@Serializable`, genéricos, paginación base
 * 0, `ignoreUnknownKeys`, valores por defecto.
 * Flujo resumido: Retrofit deserializa `ApiResponseDto<PageDto<T>>`; el repositorio
 * toma `data.content` y la UI usa los metadatos para saber si hay más páginas.
 * Relación: lo usan ParkingApi, ReservationApi, AdminApi y OperatorApi. El backend
 * envía además `pageable`, `sort`, `numberOfElements`, etc.; se ignoran.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Página genérica de resultados.
 *
 * Los índices son base 0: `number = 0` es la primera página. `content` puede llegar
 * vacío en una búsqueda sin resultados (o si falta el campo, por el valor por
 * defecto). `totalElements` es `Long` porque el backend usa `long`; los demás
 * metadatos son `Int`. Con `ignoreUnknownKeys` se descartan los campos de Spring que
 * no interesan (`pageable`, `sort`, `numberOfElements`, `offset`, etc.).
 *
 * @param T tipo de cada elemento de `content`.
 * @property content elementos de la página; por defecto lista vacía.
 * @property totalElements cantidad total de elementos en todas las páginas.
 * @property totalPages cantidad total de páginas.
 * @property size tamaño de página solicitado/efectivo.
 * @property number índice de la página actual, base 0.
 * @property first indica si es la primera página; por defecto `true`.
 * @property last indica si es la última página; por defecto `true`.
 * @property empty indica si la página no tiene elementos; por defecto `false`.
 */
@Serializable
data class PageDto<T>(
    val content: List<T> = emptyList(),
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val size: Int = 0,
    val number: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true,
    val empty: Boolean = false
)
