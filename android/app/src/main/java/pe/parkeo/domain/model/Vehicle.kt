/*
 * PARKeo · Android
 * Archivo: android/app/src/main/java/pe/parkeo/domain/model/Vehicle.kt
 * Capa: dominio (modelos de dominio).
 * Propósito: representar un vehículo registrado por el usuario y el catálogo de
 * tipos de vehículo (auto, moto, camioneta, etc.) que el backend usa para
 * tarifas y disponibilidad de espacios.
 * Conceptos clave:
 *   - Modelo de dominio: datos inmutables que describen el negocio sin
 *     depender de Retrofit, JSON, DataStore ni Compose.
 *   - DTO frente a modelo: `VehicleDto` incluye campos adicionales del JSON
 *     (`year`, `isActive`, `vehicleTypeId`, `vehicleTypeName` y el objeto
 *     `vehicleType`); este modelo conserva solo la placa, los datos opcionales
 *     de descripción y el tipo ya resuelto.
 *   - Nulabilidad: marca, modelo y color son opcionales; la placa y el tipo
 *     son obligatorios para operar.
 *   - Relación uno a muchos: un usuario puede tener varios vehículos; cada
 *     vehículo pertenece a un tipo del catálogo.
 * Flujo: backend -> JSON -> `VehicleDto`/`VehicleTypeDto` -> [mapeo previsto]
 * -> `Vehicle`/`VehicleType` -> ViewModel -> pantalla. En la versión actual
 * las interfaces de repositorio devuelven DTO y estos modelos no se usan fuera
 * de `domain/model`.
 * Relación: `VehicleRepositoryImpl` implementa las operaciones con
 * `VehicleApi`; `AppContainer` lo construye y `VehicleViewModel` consume el
 * resultado. La creación/edición de vehículos es requisito para reservar.
 * Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
 */
package pe.parkeo.domain.model

/**
 * Vehículo registrado por un usuario para identificarlo al reservar.
 *
 * La placa es el dato operativo principal: el backend la normaliza y evita
 * duplicados. Los campos descriptivos son opcionales y sirven para que el
 * usuario reconozca su vehículo en la lista. El tipo determina qué espacios y
 * tarifas puede usar. Todos los campos provienen del backend; este modelo no
 * agrega datos locales.
 *
 * @param id Identificador del vehículo asignado por el backend. Se usa en
 *   actualizaciones, eliminaciones y al crear una reserva.
 * @param licensePlate Placa del vehículo. Es obligatoria; el formato concreto
 *   (por ejemplo placas peruanas) lo valida el servidor.
 * @param brand Marca del vehículo, por ejemplo "Toyota". Nullable porque el
 *   formulario permite omitirla.
 * @param model Modelo o versión, por ejemplo "Corolla". Nullable.
 * @param color Color descriptivo, por ejemplo "Plata". Nullable.
 * @param vehicleType Tipo de vehículo resuelto como objeto; se usa para
 *   filtrar espacios compatibles y calcular la tarifa.
 */
data class Vehicle(
    val id: Long,
    val licensePlate: String,
    val brand: String?,
    val model: String?,
    val color: String?,
    val vehicleType: VehicleType
)

/**
 * Tipo de vehículo dentro del catálogo del backend.
 *
 * Los tipos permiten que un estacionamiento defina espacios y tarifas
 * distintas. El catálogo se obtiene con `VehicleRepository.getVehicleTypes()`
 * y su identificador se envía al crear o actualizar un vehículo. El DTO del
 * catálogo incluye además `description`, `icon` e `isActive`; este modelo
 * conserva solo lo necesario para mostrar y asociar el tipo.
 *
 * @param id Identificador del tipo en el backend. Se usa como `vehicleTypeId`
 *   en las peticiones.
 * @param name Nombre visible del tipo, por ejemplo "Auto" o "Moto".
 */
data class VehicleType(
    val id: Long,
    val name: String
)
