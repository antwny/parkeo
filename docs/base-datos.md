# PARKeo — Documentación de Base de Datos

## Motor de base de datos

- **Motor:** MySQL 8.0
- **Charset:** `utf8mb4`
- **Collation:** `utf8mb4_unicode_ci`
- **Base de datos:** `parkeo_db`
- **Scripts:** `database/schema.sql` (DDL) y `database/data.sql` (datos de prueba)
- **Sincronización JPA:** `spring.jpa.hibernate.ddl-auto=update`

El esquema está alineado con las entidades JPA del backend. Las columnas `operator_id`, `total_spaces`, `available_spaces`, `total_amount`, `checked_in_at`, `checked_out_at`, `is_closed`, `min_hours` y `max_hours` son las que utilizan las entidades.

---

## Modelo entidad-relación

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

### 1. `roles`

Catálogo de roles del sistema.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | TINYINT UNSIGNED PK AI | |
| `name` | VARCHAR(50) UNIQUE NOT NULL | `ROLE_USER`, `ROLE_OPERATOR`, `ROLE_ADMIN` |
| `label` | VARCHAR(100) NULL | Etiqueta legible |

### 2. `users`

Usuarios de la plataforma.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `email` | VARCHAR(150) UNIQUE NOT NULL | |
| `password_hash` | VARCHAR(255) NOT NULL | BCrypt |
| `first_name` | VARCHAR(100) NOT NULL | |
| `last_name` | VARCHAR(100) NOT NULL | |
| `phone` | VARCHAR(20) NULL | |
| `document_number` | VARCHAR(20) NULL | |
| `avatar_url` | VARCHAR(500) NULL | |
| `is_active` | BOOLEAN NOT NULL DEFAULT TRUE | |
| `email_verified` | BOOLEAN NOT NULL DEFAULT FALSE | |
| `last_login_at` | DATETIME(6) NULL | |
| `created_at` / `updated_at` | DATETIME(6) | |

Índices: `uq_users_email`, `idx_users_phone`, `idx_users_active`.

### 3. `user_roles`

Relación muchos a muchos entre usuarios y roles.

| Columna | Tipo |
|---|---|
| `user_id` | BIGINT UNSIGNED FK → `users.id` (CASCADE) |
| `role_id` | TINYINT UNSIGNED FK → `roles.id` |

PK compuesta: `(user_id, role_id)`.

### 4. `refresh_tokens`

Tokens de refresco JWT.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `user_id` | BIGINT UNSIGNED FK → `users.id` (CASCADE) | |
| `token` | VARCHAR(500) NOT NULL UNIQUE | |
| `expires_at` | TIMESTAMP NOT NULL | |
| `is_revoked` | BOOLEAN NOT NULL DEFAULT FALSE | |
| `user_agent` | VARCHAR(500) NULL | |
| `ip_address` | VARCHAR(45) NULL | |
| `created_at` | TIMESTAMP | |

### 5. `vehicle_types`

Tipos de vehículo soportados.

| `name` | `label` |
|---|---|
| `AUTOMOVIL` | Automóvil |
| `MOTO` | Motocicleta |
| `CAMIONETA` | Camioneta |
| `CAMION` | Camión |
| `BICICLETA` | Bicicleta |

Columnas: `id`, `name` (UNIQUE), `label`, `description`, `is_active`.

### 6. `vehicles`

Vehículos de los usuarios.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `user_id` | BIGINT UNSIGNED FK → `users.id` (CASCADE) | |
| `vehicle_type_id` | TINYINT UNSIGNED FK → `vehicle_types.id` | |
| `license_plate` | VARCHAR(20) UNIQUE NOT NULL | Formato peruano |
| `brand` / `model` | VARCHAR(100) | |
| `color` | VARCHAR(50) | |
| `year` | YEAR | |
| `is_active` | BOOLEAN DEFAULT TRUE | |
| `created_at` / `updated_at` | TIMESTAMP | |

### 7. `parking_lots`

Estacionamientos (cocheras) de la plataforma.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `operator_id` | BIGINT UNSIGNED NULL FK → `users.id` (SET NULL) | Operador asignado |
| `name` | VARCHAR(150) NOT NULL | |
| `description` | TEXT NULL | |
| `address` | VARCHAR(255) NOT NULL | |
| `district` | VARCHAR(100) NULL | |
| `city` | VARCHAR(100) NULL | |
| `country` | VARCHAR(100) NOT NULL DEFAULT 'Perú' | |
| `latitude` | DECIMAL(10,8) NOT NULL | |
| `longitude` | DECIMAL(11,8) NOT NULL | |
| `phone` | VARCHAR(20) NULL | |
| `email` | VARCHAR(150) NULL | |
| `total_spaces` | INT NOT NULL | Capacidad total |
| `available_spaces` | INT NOT NULL DEFAULT 0 | Disponibilidad actual |
| `is_active` | BOOLEAN NOT NULL DEFAULT TRUE | |
| `is_open` | BOOLEAN NOT NULL DEFAULT TRUE | |
| `image_url` | VARCHAR(500) NULL | |
| `created_at` / `updated_at` | TIMESTAMP | |

Índices: `idx_parking_lots_location`, `idx_parking_lots_active_open`, `idx_parking_lots_district`, `idx_parking_lots_operator`.

### 8. `parking_services`

Servicios/amenidades de cada estacionamiento.

| Columna | Tipo |
|---|---|
| `id` | BIGINT UNSIGNED PK AI |
| `parking_lot_id` | BIGINT UNSIGNED FK → `parking_lots.id` (CASCADE) |
| `name` | VARCHAR(100) NOT NULL |
| `description` | VARCHAR(255) NULL |
| `icon` | VARCHAR(100) NULL |

Valores usados en los datos de prueba: `TECHADO`, `SEGURIDAD_24H`, `CAMARA_CCTV`, `DISCAPACITADOS`, `WIFI`, `VALET`, `BICICLETAS`, `MOTOS`, `LAVADO`, `CARGADOR_ELECTRICO`.

### 9. `parking_spaces`

Espacios individuales de cada estacionamiento.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `parking_lot_id` | BIGINT UNSIGNED FK → `parking_lots.id` (CASCADE) | |
| `vehicle_type_id` | TINYINT UNSIGNED NULL FK → `vehicle_types.id` | |
| `space_number` | VARCHAR(20) NOT NULL | Ej. `A-01`, `M-05` |
| `floor` | VARCHAR(10) NULL | Nivel/piso |
| `section` | VARCHAR(20) NULL | Zona/sección |
| `status` | ENUM | `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE`, `INACTIVE` |
| `is_covered` | BOOLEAN DEFAULT FALSE | |
| `has_charging` | BOOLEAN DEFAULT FALSE | |
| `is_accessible` | BOOLEAN DEFAULT FALSE | |
| `notes` | VARCHAR(255) NULL | |
| `created_at` / `updated_at` | TIMESTAMP | |

Restricción única: `(parking_lot_id, space_number)`.

### 10. `schedules`

Horarios de atención por día.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `parking_lot_id` | BIGINT UNSIGNED FK → `parking_lots.id` (CASCADE) | |
| `day_of_week` | ENUM | `MONDAY` … `SUNDAY` |
| `open_time` | VARCHAR(8) NOT NULL | Ej. `07:00:00` |
| `close_time` | VARCHAR(8) NOT NULL | Ej. `23:00:00` |
| `is_closed` | BOOLEAN NOT NULL DEFAULT FALSE | |

Restricción única: `(parking_lot_id, day_of_week)`.

### 11. `tariffs`

Tarifas por tipo de vehículo y modalidad.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `parking_lot_id` | BIGINT UNSIGNED FK → `parking_lots.id` (CASCADE) | |
| `vehicle_type_id` | TINYINT UNSIGNED NULL FK → `vehicle_types.id` | |
| `tariff_type` | ENUM | `HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY`, `FIXED` |
| `price` | DECIMAL(10,2) NOT NULL | Soles peruanos |
| `min_hours` | INT NULL | |
| `max_hours` | INT NULL | |
| `is_active` | BOOLEAN DEFAULT TRUE | |
| `created_at` / `updated_at` | TIMESTAMP | |

Restricción única: `(parking_lot_id, vehicle_type_id, tariff_type)`.

### 12. `reservations`

Reservas de espacios.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `user_id` | BIGINT UNSIGNED FK → `users.id` | |
| `parking_space_id` | BIGINT UNSIGNED FK → `parking_spaces.id` | |
| `vehicle_id` | BIGINT UNSIGNED FK → `vehicles.id` | |
| `parking_lot_id` | BIGINT UNSIGNED FK → `parking_lots.id` | |
| `start_time` | DATETIME NOT NULL | |
| `end_time` | DATETIME NOT NULL | |
| `status` | ENUM | `PENDING`, `CONFIRMED`, `ACTIVE`, `COMPLETED`, `CANCELLED`, `NO_SHOW` |
| `total_amount` | DECIMAL(10,2) NOT NULL | |
| `currency` | VARCHAR(5) DEFAULT `PEN` | |
| `confirmation_code` | VARCHAR(20) UNIQUE NULL | Ej. `PKO-DEMO01` |
| `notes` | VARCHAR(500) NULL | |
| `cancelled_at` | DATETIME NULL | |
| `cancellation_reason` | VARCHAR(255) NULL | |
| `checked_in_at` | DATETIME NULL | |
| `checked_out_at` | DATETIME NULL | |
| `created_at` / `updated_at` | TIMESTAMP | |

Índices: `idx_user_reservations`, `idx_space_time`, `idx_status_time`, `idx_reservations_parking`.

### 13. `status_history`

Historial unificado de cambios de estado.

| Columna | Tipo |
|---|---|
| `id` | BIGINT UNSIGNED PK AI |
| `entity_type` | ENUM(`SPACE`, `RESERVATION`) |
| `entity_id` | BIGINT UNSIGNED |
| `previous_status` | VARCHAR(50) NULL |
| `new_status` | VARCHAR(50) NOT NULL |
| `changed_by` | BIGINT UNSIGNED NULL FK → `users.id` (SET NULL) |
| `notes` | VARCHAR(500) NULL |
| `changed_at` | TIMESTAMP |

### 14. `parking_ratings`

Calificaciones de reservas completadas.

| Columna | Tipo | Notas |
|---|---|---|
| `id` | BIGINT UNSIGNED PK AI | |
| `reservation_id` | BIGINT UNSIGNED UNIQUE FK → `reservations.id` | Una calificación por reserva |
| `user_id` | BIGINT UNSIGNED FK → `users.id` | |
| `parking_lot_id` | BIGINT UNSIGNED FK → `parking_lots.id` | |
| `rating` | TINYINT UNSIGNED | CHECK entre 1 y 5 |
| `comment` | TEXT NULL | |
| `created_at` | TIMESTAMP | |

### 15. `audit_log`

Registro de auditoría de acciones.

| Columna | Tipo |
|---|---|
| `id` | BIGINT UNSIGNED PK AI |
| `user_id` | BIGINT UNSIGNED NULL FK → `users.id` (SET NULL) |
| `action` | VARCHAR(100) NOT NULL |
| `entity` | VARCHAR(100) NOT NULL |
| `entity_id` | BIGINT UNSIGNED NULL |
| `old_values` / `new_values` | JSON NULL |
| `ip_address` | VARCHAR(45) NULL |
| `user_agent` | VARCHAR(500) NULL |
| `created_at` | TIMESTAMP |

> La tabla existe y tiene datos de ejemplo, pero el backend actual todavía no escribe auditoría de forma automática.

### 16. `operator_parking_assignments`

Asignación de operadores a estacionamientos.

| Columna | Tipo |
|---|---|
| `id` | BIGINT UNSIGNED PK AI |
| `operator_id` | BIGINT UNSIGNED FK → `users.id` |
| `parking_lot_id` | BIGINT UNSIGNED FK → `parking_lots.id` (CASCADE) |
| `assigned_at` | TIMESTAMP |
| `assigned_by` | BIGINT UNSIGNED NULL FK → `users.id` (SET NULL) |
| `is_active` | BOOLEAN DEFAULT TRUE |

Restricción única: `(operator_id, parking_lot_id)`.

---

## Datos de prueba

### Usuarios

| Email | Contraseña | Rol |
|---|---|---|
| admin@parkeo.pe | Password123! | `ROLE_ADMIN` |
| operador@parkeo.pe | Password123! | `ROLE_OPERATOR` |
| operador2@parkeo.pe | Password123! | `ROLE_OPERATOR` |
| cliente@parkeo.pe | Password123! | `ROLE_USER` |
| juan.perez@parkeo.pe | Password123! | `ROLE_USER` |
| sofia.mendez@parkeo.pe | Password123! | `ROLE_USER` |
| roberto.silva@parkeo.pe | Password123! | `ROLE_USER` |

### Estacionamientos

| # | Nombre | Distrito | Espacios |
|---|---|---|---|
| 1 | Parkeo Miraflores Centro | Miraflores | 60 |
| 2 | Parkeo San Isidro Financiero | San Isidro | 80 |
| 3 | Parkeo Surco Chacarilla | Santiago de Surco | 100 |
| 4 | Parkeo Barranco Bohemio | Barranco | 35 |
| 5 | Parkeo Lince Express | Lince | 45 |
| 6 | Parkeo San Borja Premium | San Borja | 90 |

Totales del seed: **6 lotes, 410 espacios, 42 horarios, 54 tarifas, 6 reservas y 7 usuarios**.

`available_spaces` se recalcula al final de `data.sql` contando los espacios en estado `AVAILABLE`.

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
JOIN parking_lots pl  ON pl.id = t.parking_lot_id
JOIN vehicle_types vt ON vt.id = t.vehicle_type_id
WHERE t.tariff_type = 'HOURLY' AND t.is_active = TRUE
ORDER BY pl.district, vt.name;
```

### Reservas activas

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
