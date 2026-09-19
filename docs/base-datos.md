# PARKeo — Documentación de Base de Datos

## Motor de Base de Datos
- **Motor:** MySQL 8.0
- **Juego de caracteres:** `utf8mb4`
- **Collation:** `utf8mb4_unicode_ci`
- **Base de datos:** `parkeo_db`

---

## Modelo Entidad-Relación

El modelo de datos está normalizado en 3FN y optimizado para operaciones de alta concurrencia (consultas de disponibilidad, búsqueda geográfica por proximidad y reservas transaccionales).

```
[roles] ───< [users] ───< [vehicles] >─── [vehicle_types]
                │             │
                │             v
                ├───< [reservations] >─── [parking_spaces] >─── [parking_lots]
                │             │                                        │
                ├───< [refresh_tokens]                                ├───< [schedules]
                │                                                      ├───< [tariffs]
                ├───< [parking_ratings] >──────────────────────────────┤
                │                                                      └───< [parking_lot_services] >─── [parking_services]
                └───< [operator_parking_assignments] >─────────────────┘
```

---

## Tablas y Estructura

### 1. `roles`
Catálogo de roles del sistema.
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `name` (VARCHAR(50), UNIQUE, NOT NULL): `ROLE_CLIENT`, `ROLE_OPERATOR`, `ROLE_ADMIN`
- `description` (VARCHAR(255))

### 2. `users`
Usuarios del sistema (clientes, operadores, administradores).
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `email` (VARCHAR(255), UNIQUE, NOT NULL, INDEX)
- `password_hash` (VARCHAR(255), NOT NULL) — BCrypt (factor 10)
- `first_name` (VARCHAR(100), NOT NULL)
- `last_name` (VARCHAR(100), NOT NULL)
- `phone` (VARCHAR(20))
- `avatar_url` (VARCHAR(500))
- `is_active` (BOOLEAN, DEFAULT TRUE)
- `email_verified` (BOOLEAN, DEFAULT FALSE)
- `role_id` (BIGINT, FK -> roles.id, NOT NULL)
- `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
- `updated_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP ON UPDATE)
- `last_login` (TIMESTAMP, NULL)

### 3. `refresh_tokens`
Tokens para renovación segura de sesiones JWT.
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `user_id` (BIGINT, FK -> users.id, NOT NULL)
- `token` (VARCHAR(500), UNIQUE, NOT NULL, INDEX)
- `expires_at` (TIMESTAMP, NOT NULL)
- `is_revoked` (BOOLEAN, DEFAULT FALSE)
- `created_at` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
- `user_agent` (VARCHAR(500))
- `ip_address` (VARCHAR(45))

### 4. `vehicle_types`
Tipos de vehículos soportados.
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `name` (VARCHAR(50), UNIQUE, NOT NULL): `AUTOMOVIL`, `MOTO`, `CAMIONETA`, `CAMION`, `BICICLETA`
- `description` (VARCHAR(255))
- `icon` (VARCHAR(100))

### 5. `vehicles`
Vehículos registrados por los usuarios.
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `user_id` (BIGINT, FK -> users.id, NOT NULL, INDEX)
- `vehicle_type_id` (BIGINT, FK -> vehicle_types.id, NOT NULL)
- `license_plate` (VARCHAR(20), NOT NULL, INDEX)
- `brand` (VARCHAR(100))
- `model` (VARCHAR(100))
- `color` (VARCHAR(50))
- `created_at`, `updated_at` (TIMESTAMP)
- UNIQUE constraint: `(user_id, license_plate)`

### 6. `parking_lots`
Estacionamientos o sedes.
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `name` (VARCHAR(200), NOT NULL)
- `description` (TEXT)
- `address` (VARCHAR(500), NOT NULL)
- `district` (VARCHAR(100))
- `city` (VARCHAR(100))
- `country` (VARCHAR(100), DEFAULT 'Perú')
- `latitude` (DECIMAL(10,8), NOT NULL)
- `longitude` (DECIMAL(11,8), NOT NULL)
- `phone` (VARCHAR(20))
- `email` (VARCHAR(255))
- `total_capacity` (INT, NOT NULL)
- `is_active` (BOOLEAN, DEFAULT TRUE)
- `is_open` (BOOLEAN, DEFAULT TRUE)
- `image_url` (VARCHAR(500))
- `owner_id` (BIGINT, FK -> users.id, NULL)
- `created_at`, `updated_at` (TIMESTAMP)
- **Índices:** `idx_parking_location (latitude, longitude)`, `idx_parking_active_open (is_active, is_open)`

### 7. `parking_services`
Catálogo de amenidades y servicios.
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `name` (VARCHAR(100), UNIQUE, NOT NULL): `TECHADO`, `SEGURIDAD_24H`, `CAMARA_CCTV`, `LAVADO`, `CARGADOR_ELECTRICO`, `VALET_PARKING`, `ACCESO_DISCAPACITADOS`, `INFLADO_LLANTAS`, `PAGO_DIGITAL`, `BAÑOS`
- `icon` (VARCHAR(100))

### 8. `parking_lot_services`
Relación muchos a muchos entre estacionamientos y servicios.
- `parking_lot_id` (BIGINT, FK -> parking_lots.id)
- `parking_service_id` (BIGINT, FK -> parking_services.id)
- PK compuesto: `(parking_lot_id, parking_service_id)`

### 9. `parking_spaces`
Espacios individuales numerados dentro de cada estacionamiento.
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `parking_lot_id` (BIGINT, FK -> parking_lots.id, NOT NULL)
- `space_number` (VARCHAR(20), NOT NULL)
- `vehicle_type_id` (BIGINT, FK -> vehicle_types.id, NOT NULL)
- `status` (ENUM('AVAILABLE', 'OCCUPIED', 'RESERVED', 'MAINTENANCE'), DEFAULT 'AVAILABLE')
- `floor_level` (INT, DEFAULT 1)
- `is_covered` (BOOLEAN, DEFAULT FALSE)
- `created_at`, `updated_at` (TIMESTAMP)
- UNIQUE constraint: `(parking_lot_id, space_number)`
- **Índice:** `idx_spaces_lot_status (parking_lot_id, status)`

### 10. `schedules`
Horarios de atención por día de la semana.
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `parking_lot_id` (BIGINT, FK -> parking_lots.id, NOT NULL)
- `day_of_week` (ENUM('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'))
- `open_time` (TIME, NOT NULL)
- `close_time` (TIME, NOT NULL)
- `is_open` (BOOLEAN, DEFAULT TRUE)
- UNIQUE: `(parking_lot_id, day_of_week)`

### 11. `tariffs`
Tarifas por tipo de vehículo y modalidad de cobro.
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `parking_lot_id` (BIGINT, FK -> parking_lots.id, NOT NULL)
- `vehicle_type_id` (BIGINT, FK -> vehicle_types.id, NOT NULL)
- `tariff_type` (ENUM('HOURLY', 'DAILY', 'MONTHLY'), NOT NULL)
- `price` (DECIMAL(8,2), NOT NULL)
- `minimum_minutes` (INT, DEFAULT 30)
- `is_active` (BOOLEAN, DEFAULT TRUE)
- UNIQUE: `(parking_lot_id, vehicle_type_id, tariff_type)`

### 12. `reservations`
Reservas de espacios de estacionamiento.
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `user_id` (BIGINT, FK -> users.id, NOT NULL)
- `parking_space_id` (BIGINT, FK -> parking_spaces.id, NOT NULL)
- `vehicle_id` (BIGINT, FK -> vehicles.id, NOT NULL)
- `start_time` (DATETIME, NOT NULL)
- `end_time` (DATETIME, NOT NULL)
- `actual_start` (DATETIME, NULL)
- `actual_end` (DATETIME, NULL)
- `status` (ENUM('PENDING', 'CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED', 'NO_SHOW'), DEFAULT 'PENDING')
- `total_price` (DECIMAL(10,2), NULL)
- `cancellation_reason` (TEXT)
- `notes` (TEXT)
- `created_at`, `updated_at` (TIMESTAMP)
- **Índices:**
  - `idx_reservations_user_status (user_id, status)`
  - `idx_reservations_space_time (parking_space_id, start_time, end_time)`
  - `idx_reservations_status_time (status, start_time)`

### 13. `parking_ratings`
Calificaciones y reseñas de clientes.
- `id` (BIGINT, PK, AUTO_INCREMENT)
- `user_id` (BIGINT, FK -> users.id, NOT NULL)
- `parking_lot_id` (BIGINT, FK -> parking_lots.id, NOT NULL)
- `reservation_id` (BIGINT, FK -> reservations.id, NULL)
- `rating` (INT, CHECK 1-5, NOT NULL)
- `comment` (TEXT)
- `created_at` (TIMESTAMP)

### 14. `operator_parking_assignments`
Asignación de operadores a estacionamientos específicos para control de acceso.
- `operator_id` (BIGINT, FK -> users.id)
- `parking_lot_id` (BIGINT, FK -> parking_lots.id)
- `assigned_at` (TIMESTAMP)
- PK compuesto: `(operator_id, parking_lot_id)`

---

## Consultas Críticas y Optimización

### 1. Búsqueda por Radio Geográfico (Fórmula Haversine)
```sql
SELECT pl.*,
  (6371 * acos(cos(radians(:lat)) * cos(radians(pl.latitude)) *
   cos(radians(pl.longitude) - radians(:lon)) +
   sin(radians(:lat)) * sin(radians(pl.latitude)))) AS distance_km
FROM parking_lots pl
WHERE pl.is_active = true AND pl.is_open = true
HAVING distance_km < :radius_km
ORDER BY distance_km ASC;
```

### 2. Comprobación de Disponibilidad sin Solapamiento
```sql
SELECT COUNT(*) FROM reservations r
WHERE r.parking_space_id = :space_id
  AND r.status IN ('PENDING', 'CONFIRMED', 'ACTIVE')
  AND (:start_time < r.end_time AND :end_time > r.start_time);
```
