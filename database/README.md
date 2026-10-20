# Parkeo — Base de Datos

Documentación técnica del esquema MySQL 8.0 para la plataforma de reservas de estacionamientos **Parkeo**.

---

## Conexión

| Parámetro | Valor |
|---|---|
| Host | localhost |
| Puerto | 3306 |
| Base | `parkeo_db` |
| Charset | `utf8mb4` |
| Collation | `utf8mb4_unicode_ci` |
| Usuario local | `antwny` |

> Con Docker Compose el puerto expuesto es **3307** y el usuario es `parkeo_user`.

---

## Archivos

| Archivo | Descripción |
|---|---|
| `schema.sql` | DDL completo: tablas, índices y restricciones |
| `data.sql` | Datos de prueba (seed data) |
| `README.md` | Esta documentación |

Los scripts SQL no contienen comentarios y están alineados con las entidades JPA del backend.

---

## Instalación

```bash
mysql -u antwny -p -e "DROP DATABASE IF EXISTS parkeo_db;"
mysql -u antwny -p -e "CREATE DATABASE parkeo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u antwny -p parkeo_db < schema.sql
mysql -u antwny -p parkeo_db < data.sql
mysql -u antwny -p parkeo_db -e "SHOW TABLES;"
```

O desde la raíz del proyecto:

```bash
bash scripts/reset-db.sh
```

---

## Diagrama de tablas

```
roles ───< user_roles >─── users
                              │
                              ├──< refresh_tokens
                              ├──< vehicles >─── vehicle_types
                              ├──< parking_lots >─── operator_id (users)
                              │        │
                              │        ├──< parking_services
                              │        ├──< parking_spaces >─── vehicle_types
                              │        ├──< schedules
                              │        ├──< tariffs >─── vehicle_types
                              │        └──< operator_parking_assignments
                              │
                              ├──< reservations >─── parking_spaces
                              │         │             vehicles
                              │         │             parking_lots
                              │         └──< parking_ratings
                              │
                              ├──< status_history
                              └──< audit_log
```

---

## Tablas

### `roles`

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | TINYINT UNSIGNED PK | |
| `name` | VARCHAR(50) UNIQUE | `ROLE_USER`, `ROLE_OPERATOR`, `ROLE_ADMIN` |
| `label` | VARCHAR(100) | Etiqueta legible |

### `users`

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `email` | VARCHAR(150) UNIQUE | |
| `password_hash` | VARCHAR(255) | Hash BCrypt |
| `first_name`, `last_name` | VARCHAR(100) | |
| `phone` | VARCHAR(20) | |
| `document_number` | VARCHAR(20) | |
| `avatar_url` | VARCHAR(500) | |
| `is_active` | BOOLEAN | Default `TRUE` |
| `email_verified` | BOOLEAN | Default `FALSE` |
| `last_login_at` | DATETIME(6) | |
| `created_at`, `updated_at` | DATETIME(6) | |

Índices: `uq_users_email`, `idx_users_phone`, `idx_users_active`.

### `user_roles`

| Columna | Tipo |
|---|---|
| `user_id` | BIGINT UNSIGNED FK → `users.id` |
| `role_id` | TINYINT UNSIGNED FK → `roles.id` |

PK compuesta: `(user_id, role_id)`.

### `refresh_tokens`

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `user_id` | BIGINT UNSIGNED FK → `users.id` | CASCADE |
| `token` | VARCHAR(500) UNIQUE | Token opaco |
| `expires_at` | TIMESTAMP | Expiración |
| `is_revoked` | BOOLEAN | |
| `user_agent` | VARCHAR(500) | |
| `ip_address` | VARCHAR(45) | IPv4/IPv6 |
| `created_at` | TIMESTAMP | |

### `vehicle_types`

| Nombre | Etiqueta |
|---|---|
| `AUTOMOVIL` | Automóvil |
| `MOTO` | Motocicleta |
| `CAMIONETA` | Camioneta/SUV |
| `CAMION` | Camión |
| `BICICLETA` | Bicicleta |

### `vehicles`

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `user_id` | FK → `users.id` | |
| `vehicle_type_id` | FK → `vehicle_types.id` | |
| `license_plate` | VARCHAR(20) UNIQUE | Placa del vehículo |
| `brand`, `model`, `color`, `year` | | Datos del vehículo |
| `is_active` | BOOLEAN | |

### `parking_lots`

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `operator_id` | FK → `users.id` (NULL) | Operador asignado |
| `name`, `description`, `address` | | Datos del estacionamiento |
| `district`, `city`, `country` | | Ubicación administrativa |
| `latitude`, `longitude` | DECIMAL | Coordenadas GPS |
| `phone`, `email`, `image_url` | | Contacto e imagen |
| `total_spaces` | INT | Espacios totales |
| `available_spaces` | INT | Espacios disponibles |
| `is_active` | BOOLEAN | Visible en la app |
| `is_open` | BOOLEAN | Abierto en este momento |
| `created_at`, `updated_at` | TIMESTAMP | |

Índices: `idx_parking_lots_location`, `idx_parking_lots_active_open`, `idx_parking_lots_district`, `idx_parking_lots_operator`.

### `parking_services`

| Columna | Tipo |
|---|---|
| `id` | BIGINT UNSIGNED PK AI |
| `parking_lot_id` | FK → `parking_lots.id` (CASCADE) |
| `name` | VARCHAR(100) |
| `description` | VARCHAR(255) |
| `icon` | VARCHAR(100) |

Servicios usados: `TECHADO`, `SEGURIDAD_24H`, `CAMARA_CCTV`, `DISCAPACITADOS`, `WIFI`, `VALET`, `BICICLETAS`, `MOTOS`, `LAVADO`, `CARGADOR_ELECTRICO`.

### `parking_spaces`

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `parking_lot_id` | FK → `parking_lots.id` | CASCADE |
| `vehicle_type_id` | FK → `vehicle_types.id` | |
| `space_number` | VARCHAR(20) | Ej: `A-01`, `M-05` |
| `floor` | VARCHAR(10) | Nivel/piso |
| `section` | VARCHAR(20) | Zona/sección |
| `status` | ENUM | `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE`, `INACTIVE` |
| `is_covered` | BOOLEAN | Techado |
| `has_charging` | BOOLEAN | Punto de carga |
| `is_accessible` | BOOLEAN | Accesible |
| `notes` | VARCHAR(255) | |

Restricción única: `(parking_lot_id, space_number)`.

### `schedules`

| Columna | Tipo | Descripción |
|---|---|---|
| `parking_lot_id` | FK → `parking_lots.id` | CASCADE |
| `day_of_week` | ENUM | `MONDAY` … `SUNDAY` |
| `open_time` | VARCHAR(8) | Ej. `07:00:00` |
| `close_time` | VARCHAR(8) | Ej. `23:00:00` |
| `is_closed` | BOOLEAN | Día cerrado |

Restricción única: `(parking_lot_id, day_of_week)`.

### `tariffs`

| Columna | Tipo | Descripción |
|---|---|---|
| `parking_lot_id` | FK → `parking_lots.id` | CASCADE |
| `vehicle_type_id` | FK → `vehicle_types.id` | |
| `tariff_type` | ENUM | `HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY`, `FIXED` |
| `price` | DECIMAL(10,2) | Soles peruanos |
| `min_hours` | INT | Horas mínimas |
| `max_hours` | INT | Horas máximas |
| `is_active` | BOOLEAN | |

Restricción única: `(parking_lot_id, vehicle_type_id, tariff_type)`.

### `reservations`

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `user_id` | FK → `users.id` | |
| `parking_space_id` | FK → `parking_spaces.id` | |
| `vehicle_id` | FK → `vehicles.id` | |
| `parking_lot_id` | FK → `parking_lots.id` | |
| `start_time`, `end_time` | DATETIME | Rango de la reserva |
| `status` | ENUM | `PENDING`, `CONFIRMED`, `ACTIVE`, `COMPLETED`, `CANCELLED`, `NO_SHOW` |
| `total_amount` | DECIMAL(10,2) | Monto calculado |
| `currency` | VARCHAR(5) | Default `PEN` |
| `confirmation_code` | VARCHAR(20) UNIQUE | Ej. `PKO-DEMO01` |
| `notes` | VARCHAR(500) | |
| `cancelled_at`, `cancellation_reason` | | Cancelación |
| `checked_in_at`, `checked_out_at` | | Check-in / check-out |
| `created_at`, `updated_at` | TIMESTAMP | |

Índices: `idx_user_reservations`, `idx_space_time`, `idx_status_time`, `idx_reservations_parking`.

### `status_history`

Historial unificado de cambios de estado para espacios y reservas.

| Columna | Tipo |
|---|---|
| `entity_type` | ENUM(`SPACE`, `RESERVATION`) |
| `entity_id` | BIGINT UNSIGNED |
| `previous_status` | VARCHAR(50) |
| `new_status` | VARCHAR(50) |
| `changed_by` | FK → `users.id` |
| `notes` | VARCHAR(500) |
| `changed_at` | TIMESTAMP |

### `parking_ratings`

Calificaciones de 1 a 5 estrellas por reserva completada. Una calificación por reserva (`UNIQUE reservation_id`).

### `audit_log`

Registro de auditoría con valores JSON de cambios (`old_values`, `new_values`). El backend actual todavía no escribe auditoría automáticamente.

### `operator_parking_assignments`

Asignación de operadores a estacionamientos específicos. Restricción única: `(operator_id, parking_lot_id)`.

---

## Datos de prueba

### Usuarios

| Email | Contraseña | Rol |
|---|---|---|
| admin@parkeo.pe | Password123! | ADMIN |
| operador@parkeo.pe | Password123! | OPERADOR |
| operador2@parkeo.pe | Password123! | OPERADOR |
| cliente@parkeo.pe | Password123! | CLIENTE |
| juan.perez@parkeo.pe | Password123! | CLIENTE |
| sofia.mendez@parkeo.pe | Password123! | CLIENTE |
| roberto.silva@parkeo.pe | Password123! | CLIENTE |

### Estacionamientos

| # | Nombre | Distrito | Capacidad |
|---|---|---|---|
| 1 | Parkeo Miraflores Centro | Miraflores | 60 |
| 2 | Parkeo San Isidro Financiero | San Isidro | 80 |
| 3 | Parkeo Surco Chacarilla | Santiago de Surco | 100 |
| 4 | Parkeo Barranco Bohemio | Barranco | 35 |
| 5 | Parkeo Lince Express | Lince | 45 |
| 6 | Parkeo San Borja Premium | San Borja | 90 |

Totales: **410 espacios, 42 horarios, 54 tarifas, 6 reservas y 7 usuarios**.

### Tarifas por hora (S/)

| Distrito | Auto | Moto | Camioneta |
|---|---|---|---|
| Miraflores | S/5.00 | S/2.00 | S/7.00 |
| San Isidro | S/8.00 | S/3.00 | S/10.00 |
| Surco | S/6.00 | S/2.50 | S/8.00 |
| Barranco | S/4.00 | S/1.50 | S/6.00 |
| Lince | S/3.00 | S/1.50 | S/5.00 |
| San Borja | S/7.00 | S/2.50 | S/9.00 |

---

## Consultas útiles

### Estacionamientos activos con disponibilidad

```sql
SELECT pl.name, pl.district,
       pl.total_spaces,
       pl.available_spaces
FROM parking_lots pl
WHERE pl.is_active = TRUE
ORDER BY pl.name;
```

### Disponibilidad calculada desde los espacios

```sql
SELECT pl.name,
       COUNT(ps.id) AS total_espacios,
       SUM(ps.status = 'AVAILABLE') AS disponibles
FROM parking_lots pl
JOIN parking_spaces ps ON ps.parking_lot_id = pl.id
WHERE pl.is_active = TRUE
GROUP BY pl.id;
```

### Tarifas por hora

```sql
SELECT pl.name, vt.label AS vehiculo, t.price AS precio_hora
FROM tariffs t
JOIN parking_lots pl   ON pl.id = t.parking_lot_id
JOIN vehicle_types vt  ON vt.id = t.vehicle_type_id
WHERE t.tariff_type = 'HOURLY' AND t.is_active = TRUE
ORDER BY pl.district, vt.name;
```

### Reservas activas con detalle

```sql
SELECT r.id, r.confirmation_code, u.email, pl.name AS estacionamiento,
       ps.space_number, v.license_plate, r.start_time, r.end_time, r.total_amount
FROM reservations r
JOIN users u           ON u.id = r.user_id
JOIN parking_spaces ps ON ps.id = r.parking_space_id
JOIN parking_lots pl   ON pl.id = r.parking_lot_id
JOIN vehicles v        ON v.id = r.vehicle_id
WHERE r.status IN ('CONFIRMED', 'ACTIVE');
```

### Rating promedio por estacionamiento

```sql
SELECT pl.name, pl.district,
       ROUND(AVG(pr.rating), 2) AS rating_promedio,
       COUNT(pr.id) AS total_ratings
FROM parking_lots pl
LEFT JOIN parking_ratings pr ON pr.parking_lot_id = pl.id
GROUP BY pl.id
ORDER BY rating_promedio DESC;
```
