/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/domain/model/ParkingLot.kt
 * Capa: dominio (modelo de dominio).
 * Propósito: representar un estacionamiento del negocio con los datos que una
 * pantalla necesita para listarlo, buscarlo, mostrarlo en un mapa o abrir su
 * detalle. Este archivo no realiza peticiones HTTP ni conoce Android: solo
 * describe información y dos reglas derivadas de presentación.
 * Conceptos clave:
 *   - Modelo de dominio: vocabulario estable del problema, independiente de
 *     Retrofit, JSON, DataStore o Compose.
 *   - DTO frente a modelo: `ParkingLotDto` (capa de datos) refleja el JSON y
 *     tolera variantes del backend; `ParkingLot` expresa lo que la app necesita.
 *     El mapeo DTO -> modelo debería concentrarse en la capa de datos: así un
 *     cambio del contrato HTTP no obliga a modificar las pantallas.
 *   - Nulabilidad: los campos `String?` y `Double?` pueden faltar en la
 *     respuesta del backend y obligan a la UI a decidir cómo mostrarlos.
 *   - Estado derivado: `isAvailable` y `occupancyPercent` se calculan en
 *     memoria y no se persisten ni se envían al servidor.
 * Flujo: backend -> JSON -> `ParkingLotDto` (Retrofit) -> [mapeo previsto] ->
 * `ParkingLot` -> ViewModel -> pantalla Compose. En el estado actual del
 * proyecto las interfaces de repositorio devuelven DTO y este modelo no se
 * referencia fuera de `domain/model`; se conserva como vocabulario de negocio.
 * Relación: lo consume la capa de dominio/presentación; las implementaciones
 * `ParkingRepositoryImpl` y `AdminRepositoryImpl` obtienen los datos remotos
 * que alimentarían este modelo. `AppContainer` construye esas dependencias y
 * `ParkingApi` define las rutas HTTP del backend Spring.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.domain.model

/**
 * Estacionamiento o cochera que el usuario puede descubrir y reservar.
 *
 * Es un `data class`: el compilador genera `equals`, `hashCode`, `toString`,
 * `copy` y destructuración a partir de las propiedades del constructor. La
 * inmutabilidad de las referencias (`val`) no vuelve inmutables las listas:
 * `services` es una `List` de solo lectura, pero su contenido depende de quien
 * la construya.
 *
 * Todos los campos provienen, directa o indirectamente, de la respuesta del
 * backend; los dos últimos (`isAvailable` y `occupancyPercent`) son cálculos
 * locales definidos en el cuerpo de la clase.
 *
 * @param id Identificador interno del estacionamiento. Lo asigna el backend
 *   (clave primaria) y se usa para consultar detalle, disponibilidad y crear
 *   reservas.
 * @param name Nombre comercial visible para el usuario, por ejemplo
 *   "Estacionamiento Larcomar".
 * @param address Dirección textual mostrada en tarjetas y detalle.
 * @param district Distrito donde se ubica. Es nullable porque el backend puede
 *   no informarlo; la UI debe evitar concatenar `null`.
 * @param latitude Latitud geográfica en grados decimales (sistema WGS84).
 *   Se usa para mapas y para la búsqueda por cercanía.
 * @param longitude Longitud geográfica en grados decimales (WGS84).
 * @param totalCapacity Cantidad total de espacios físicos del estacionamiento.
 *   Es un entero no negativo; si llega en cero no debe usarse como divisor.
 * @param availableSpaces Espacios libres en el momento de la consulta. Es un
 *   dato fotográfico: puede quedar desactualizado hasta la próxima llamada.
 * @param isOpen Indica si el estacionamiento está abierto según su horario o
 *   una decisión operativa del backend. No significa que tenga espacios libres.
 * @param imageUrl URL remota de una imagen ilustrativa. Es nullable y puede
 *   apuntar a un recurso que no cargue; la UI debe ofrecer un marcador visual.
 * @param services Nombres de servicios o comodidades (por ejemplo "Wi-Fi",
 *   "Seguridad"). Lista vacía si el backend no envía servicios. En el listado
 *   el DTO ya trae textos; en el detalle llegan objetos `ParkingServiceDto`,
 *   así que el mapeo debe extraer el campo `name`.
 * @param distance Distancia en kilómetros respecto de la ubicación consultada.
 *   Es nullable porque solo tiene sentido en búsquedas por cercanía; en un
 *   listado general puede no venir.
 * @param rating Calificación promedio, normalmente en una escala de 0 a 5.
 *   Es nullable cuando el estacionamiento aún no tiene valoraciones.
 */
data class ParkingLot(
    val id: Long,
    val name: String,
    val address: String,
    val district: String?,
    val latitude: Double,
    val longitude: Double,
    val totalCapacity: Int,
    val availableSpaces: Int,
    val isOpen: Boolean,
    val imageUrl: String?,
    val services: List<String>,
    val distance: Double?,
    val rating: Double?
) {
    /**
     * Indica si hay al menos un espacio libre.
     *
     * Es un estado derivado de `availableSpaces`; no consulta al backend ni
     * toma en cuenta `isOpen`. Un estacionamiento cerrado con espacios libres
     * seguiría dando `true`, por lo que la UI debe combinar ambas condiciones
     * si desea mostrar "disponible para reservar ahora".
     *
     * @return `true` si `availableSpaces` es mayor que cero; `false` en caso
     *   contrario (incluido un valor negativo inesperado).
     */
    val isAvailable: Boolean get() = availableSpaces > 0

    /**
     * Calcula la ocupación como fracción de la capacidad total.
     *
     * La fórmula es `(totalCapacity - availableSpaces) / totalCapacity`. El
     * resultado está en el rango de 0.0 a 1.0, **no** en porcentaje 0..100:
     * a pesar del nombre, quien lo muestre debe multiplicar por 100. Si
     * `availableSpaces` supera la capacidad, o si hay valores negativos por
     * datos inconsistentes, el resultado puede salir del rango esperado.
     * La guarda `totalCapacity > 0` evita una división entre cero y devuelve
     * `0f` cuando no hay capacidad conocida.
     *
     * @return Fracción de ocupación como `Float`; `0f` si `totalCapacity` no
     *   es positivo.
     */
    val occupancyPercent: Float get() = if (totalCapacity > 0)
        (totalCapacity - availableSpaces).toFloat() / totalCapacity else 0f
}
