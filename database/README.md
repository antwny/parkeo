# Parkeo — Base de Datos

Documentación técnica del esquema de base de datos MySQL 8.0 para la plataforma de reservas de estacionamientos **Parkeo**.

---

## Conexión

| Parámetro | Valor         |
|-----------|---------------|
| Host      | localhost     |
| Puerto    | 3306          |
| Base      | `parkeo_db`   |
| Charset   | utf8mb4       |
| Collation | utf8mb4_unicode_ci |

---

## Archivos

| Archivo      | Descripción                                     |
|--------------|-------------------------------------------------|
| `schema.sql` | DDL completo: tablas, índices y restricciones   |
| `data.sql`   | Datos de prueba (seed data)                     |
| `README.md`  | Esta documentación                              |

---

## Instalación

```bash
# 1. Crear la base de datos (si no existe)
mysql -u antwny -p198009 -e "CREATE DATABASE IF NOT EXISTS parkeo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. Aplicar el schema
mysql -u antwny -p198009 parkeo_db < schema.sql

# 3. Cargar los datos de prueba
mysql -u antwny -p198009 parkeo_db < data.sql

# 4. Verificar
mysql -u antwny -p198009 parkeo_db -e "SHOW TABLES;"
```

---

## Diagrama de Tablas

```
roles ──────────────────────────────────────┐
                                            │
users ──────────────┬───────────────────────┘
  │                 │
  ├── refresh_tokens│
  │                 │
  ├── vehicles ─────┤── vehicle_types
  │                 │
  └── parking_lots ─┴── owner_id
        │
        ├── parking_lot_services ── parking_services
        │
        ├── parking_spaces ──── vehicle_types
        │     └── space_status_history
        │
        ├── schedules
        │
        ├── tariffs ──────────── vehicle_types
        │
        └── operator_parking_assignments ── users (operador)

reservations ── users · parking_spaces · vehicles
  └── reservation_status_history
  └── parking_ratings

audit_log ── users
```

---

## Tablas

### `roles`
Roles del sistema.

| Columna | Tipo | Descripción |
|---------|------|-------------|
| id | TINYINT PK | |
| name | VARCHAR(50) UNIQUE | `CLIENTE`, `OPERADOR`, `ADMIN` |
| label | VARCHAR(100) | Etiqueta legible |

---

### `users`
Usuarios registrados en la plataforma.

| Columna | Tipo | Descripción |
|---------|------|-------------|
| id | BIGINT PK AI | |
| email | VARCHAR(255) UNIQUE | |
| password_hash | VARCHAR(255) | bcrypt hash |
| first_name | VARCHAR(100) | |
| last_name | VARCHAR(100) | |
| phone | VARCHAR(20) | |
| avatar_url | VARCHAR(500) | |
| is_active | BOOLEAN | Default: TRUE |
| email_verified | BOOLEAN | Default: FALSE |
| role_id | FK → roles.id | |
| last_login | TIMESTAMP NULL | |
| created_at / updated_at | TIMESTAMP | |

**Índices:** `uq_users_email`, `idx_users_role`, `idx_users_active`

---

### `refresh_tokens`
Tokens de refresco para autenticación JWT.

| Columna | Tipo | Descripción |
|---------|------|-------------|
| id | BIGINT PK AI | |
| user_id | FK → users.id | CASCADE DELETE |
| token | VARCHAR(500) UNIQUE | Token opaco |
| expires_at | TIMESTAMP | Expiración |
| is_revoked | BOOLEAN | |
| user_agent | VARCHAR(500) | |
| ip_address | VARCHAR(45) | IPv4/IPv6 |

---

### `vehicle_types`
Catálogo de tipos de vehículo.

| Nombre | Etiqueta |
|--------|----------|
| AUTOMOVIL | Automóvil |
| MOTO | Motocicleta |
| CAMIONETA | Camioneta/SUV |
| CAMION | Camión |
| BICICLETA | Bicicleta |

---

### `vehicles`
Vehículos registrados por los usuarios.

| Columna | Tipo | Descripción |
|---------|------|-------------|
| plate | VARCHAR(20) UNIQUE | Placa del vehículo |
| brand / model / color / year | | Datos del vehículo |
| is_active | BOOLEAN | |

---

### `parking_lots`
Estacionamientos disponibles en la plataforma.

| Columna | Tipo | Descripción |
|---------|------|-------------|
| owner_id | FK → users.id | Propietario/admin |
| latitude / longitude | DECIMAL | Coordenadas GPS |
| total_capacity | INT | Espacios totales |
| is_active | BOOLEAN | Visible en la app |
| is_open | BOOLEAN | Abierto en este momento |

**Índices:** `idx_parking_lots_location`, `idx_parking_lots_active_open`, `idx_parking_lots_district`

---

### `parking_services`
Catálogo de servicios (10 servicios disponibles):

`TECHADO`, `SEGURIDAD_24H`, `CAMARA_CCTV`, `LAVADO`, `CARGADOR_ELECTRICO`, `DISCAPACITADOS`, `WIFI`, `BICICLETAS`, `MOTOS`, `VALET`

---

### `parking_spaces`
Espacios individuales dentro de cada estacionamiento.

| Columna | Tipo | Descripción |
|---------|------|-------------|
| space_number | VARCHAR(20) | Ej: `A-01`, `M-05` |
| status | ENUM | `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE` |
| floor_level | TINYINT | Nivel/piso |
| is_covered | BOOLEAN | Techado |

**Restricción única:** `(parking_lot_id, space_number)`

---

### `schedules`
Horarios de apertura por día de semana.

- Restricción única: `(parking_lot_id, day_of_week)`
- Si `is_open = FALSE`, los campos `open_time` / `close_time` pueden ser NULL

---

### `tariffs`
Tarifas en **soles peruanos (S/)**.

| Tipo | Descripción |
|------|-------------|
| HOURLY | Por hora |
| DAILY | Por día |
| MONTHLY | Por mes |

Restricción única: `(parking_lot_id, vehicle_type_id, tariff_type)`

---

### `reservations`
Reservas de espacios de estacionamiento.

| Estado | Descripción |
|--------|-------------|
| PENDING | Creada, pendiente de confirmación |
| CONFIRMED | Confirmada por el sistema/operador |
| ACTIVE | Cliente dentro del estacionamiento |
| COMPLETED | Reserva finalizada exitosamente |
| CANCELLED | Cancelada por usuario o sistema |
| NO_SHOW | No se presentó |

**Constraint:** `end_time > start_time`

---

### `parking_ratings`
Calificaciones de 1 a 5 estrellas por reserva completada. Una calificación por reserva (`UNIQUE reservation_id`).

---

### `audit_log`
Registro de auditoría. Almacena valores JSON de cambios (`old_values`, `new_values`).

---

### `operator_parking_assignments`
Asignación de operadores a estacionamientos específicos.

---

## Datos de Prueba

### Usuarios

| Email | Contraseña | Rol |
|-------|-----------|-----|
| admin@parkeo.pe | Password123! | ADMIN |
| operador@parkeo.pe | Password123! | OPERADOR |
| cliente@parkeo.pe | Password123! | CLIENTE |
| juan.perez@parkeo.pe | Password123! | CLIENTE |
| sofia.mendez@parkeo.pe | Password123! | CLIENTE |
| roberto.silva@parkeo.pe | Password123! | CLIENTE |

### Estacionamientos

| # | Nombre | Distrito | Capacidad |
|---|--------|----------|-----------|
| 1 | Parkeo Miraflores Centro | Miraflores | 60 |
| 2 | Parkeo San Isidro Financiero | San Isidro | 80 |
| 3 | Parkeo Surco Chacarilla | Santiago de Surco | 100 |
| 4 | Parkeo Barranco Bohemio | Barranco | 35 |
| 5 | Parkeo Lince Express | Lince | 45 |
| 6 | Parkeo San Borja Premium | San Borja | 90 |

### Tarifas por hora (S/)

| Distrito | Auto | Moto | Camioneta |
|----------|------|------|-----------|
| Miraflores | S/5.00 | S/2.00 | S/7.00 |
| San Isidro | S/8.00 | S/3.00 | S/10.00 |
| Surco | S/6.00 | S/2.50 | S/8.00 |
| Barranco | S/4.00 | S/1.50 | — |
| Lince | S/3.00 | S/1.50 | S/5.00 |
| San Borja | S/7.00 | S/2.50 | S/9.00 |

---

## Consultas Útiles

```sql
-- Estacionamientos activos con disponibilidad
SELECT pl.name, pl.district,
       COUNT(ps.id) AS total_espacios,
       SUM(ps.status = 'AVAILABLE') AS disponibles
FROM parking_lots pl
JOIN parking_spaces ps ON ps.parking_lot_id = pl.id
WHERE pl.is_active = TRUE
GROUP BY pl.id;

-- Tarifas por hora de todos los estacionamientos
SELECT pl.name, vt.label AS vehiculo, t.price AS precio_hora
FROM tariffs t
JOIN parking_lots pl   ON pl.id = t.parking_lot_id
JOIN vehicle_types vt  ON vt.id = t.vehicle_type_id
WHERE t.tariff_type = 'HOURLY' AND t.is_active = TRUE
ORDER BY pl.district, vt.name;

-- Reservas activas con detalle
SELECT r.id, u.email, pl.name AS estacionamiento,
       ps.space_number, v.plate, r.start_time, r.end_time, r.total_price
FROM reservations r
JOIN users u           ON u.id = r.user_id
JOIN parking_spaces ps ON ps.id = r.parking_space_id
JOIN parking_lots pl   ON pl.id = ps.parking_lot_id
JOIN vehicles v        ON v.id = r.vehicle_id
WHERE r.status IN ('CONFIRMED','ACTIVE');

-- Rating promedio por estacionamiento
SELECT pl.name, pl.district,
       ROUND(AVG(pr.rating), 2) AS rating_promedio,
       COUNT(pr.id) AS total_ratings
FROM parking_lots pl
LEFT JOIN parking_ratings pr ON pr.parking_lot_id = pl.id
GROUP BY pl.id
ORDER BY rating_promedio DESC;
```
