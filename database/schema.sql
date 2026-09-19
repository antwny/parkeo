-- =============================================================================
-- PARKEO - Schema de Base de Datos
-- MySQL 8.0 | UTF8MB4 | InnoDB
-- Versión: 1.0.0
-- Fecha: 2026-09-18
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- =============================================================================
-- 1. ROLES
-- =============================================================================
CREATE TABLE IF NOT EXISTS roles (
    id       TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name     VARCHAR(50)      NOT NULL,
    label    VARCHAR(100)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Roles del sistema: CLIENTE, OPERADOR, ADMIN';

-- =============================================================================
-- 2. USERS
-- =============================================================================
CREATE TABLE IF NOT EXISTS users (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    email           VARCHAR(255)     NOT NULL,
    password_hash   VARCHAR(255)     NOT NULL,
    first_name      VARCHAR(100)     NOT NULL,
    last_name       VARCHAR(100)     NOT NULL,
    phone           VARCHAR(20)      NULL,
    avatar_url      VARCHAR(500)     NULL,
    is_active       BOOLEAN          NOT NULL DEFAULT TRUE,
    email_verified  BOOLEAN          NOT NULL DEFAULT FALSE,
    role_id         TINYINT UNSIGNED NOT NULL,
    last_login      TIMESTAMP        NULL,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_role (role_id),
    KEY idx_users_active (is_active),
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Usuarios del sistema (clientes, operadores y administradores)';

-- =============================================================================
-- 3. REFRESH TOKENS (JWT)
-- =============================================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    token       VARCHAR(500)    NOT NULL,
    expires_at  TIMESTAMP       NOT NULL,
    is_revoked  BOOLEAN         NOT NULL DEFAULT FALSE,
    user_agent  VARCHAR(500)    NULL,
    ip_address  VARCHAR(45)     NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_tokens_token (token(255)),
    KEY idx_refresh_tokens_user (user_id),
    KEY idx_refresh_tokens_expires (expires_at),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tokens de refresco JWT para autenticación persistente';

-- =============================================================================
-- 4. VEHICLE TYPES
-- =============================================================================
CREATE TABLE IF NOT EXISTS vehicle_types (
    id          TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)      NOT NULL,
    label       VARCHAR(100)     NOT NULL,
    description VARCHAR(255)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_vehicle_types_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tipos de vehículo soportados';

-- =============================================================================
-- 5. VEHICLES (vehículos de usuarios)
-- =============================================================================
CREATE TABLE IF NOT EXISTS vehicles (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED  NOT NULL,
    vehicle_type_id TINYINT UNSIGNED NOT NULL,
    plate           VARCHAR(20)      NOT NULL,
    brand           VARCHAR(100)     NULL,
    model           VARCHAR(100)     NULL,
    color           VARCHAR(50)      NULL,
    year            YEAR             NULL,
    is_active       BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_vehicles_plate (plate),
    KEY idx_vehicles_user (user_id),
    KEY idx_vehicles_type (vehicle_type_id),
    CONSTRAINT fk_vehicles_user         FOREIGN KEY (user_id)         REFERENCES users (id)         ON DELETE CASCADE,
    CONSTRAINT fk_vehicles_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Vehículos registrados por los usuarios';

-- =============================================================================
-- 6. PARKING LOTS (estacionamientos)
-- =============================================================================
CREATE TABLE IF NOT EXISTS parking_lots (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    owner_id        BIGINT UNSIGNED NOT NULL  COMMENT 'Usuario con rol ADMIN u OPERADOR propietario del estacionamiento',
    name            VARCHAR(200)    NOT NULL,
    description     TEXT            NULL,
    address         VARCHAR(500)    NOT NULL,
    district        VARCHAR(100)    NULL,
    city            VARCHAR(100)    NULL,
    country         VARCHAR(100)    NOT NULL DEFAULT 'Perú',
    latitude        DECIMAL(10,8)   NOT NULL,
    longitude       DECIMAL(11,8)   NOT NULL,
    phone           VARCHAR(20)     NULL,
    email           VARCHAR(255)    NULL,
    total_capacity  INT UNSIGNED    NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    is_open         BOOLEAN         NOT NULL DEFAULT TRUE,
    image_url       VARCHAR(500)    NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_parking_lots_location   (latitude, longitude),
    KEY idx_parking_lots_active_open (is_active, is_open),
    KEY idx_parking_lots_district   (district),
    KEY idx_parking_lots_owner      (owner_id),
    CONSTRAINT fk_parking_lots_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Estacionamientos registrados en la plataforma';

-- =============================================================================
-- 7. PARKING SERVICES (catálogo de servicios)
-- =============================================================================
CREATE TABLE IF NOT EXISTS parking_services (
    id          TINYINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(100)     NOT NULL,
    label       VARCHAR(150)     NOT NULL,
    icon        VARCHAR(100)     NULL       COMMENT 'Nombre del ícono (ej: material-icons)',
    description VARCHAR(255)     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_parking_services_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Catálogo de servicios que puede ofrecer un estacionamiento';

-- =============================================================================
-- 8. PARKING LOT SERVICES (many-to-many)
-- =============================================================================
CREATE TABLE IF NOT EXISTS parking_lot_services (
    parking_lot_id  BIGINT UNSIGNED  NOT NULL,
    service_id      TINYINT UNSIGNED NOT NULL,
    PRIMARY KEY (parking_lot_id, service_id),
    CONSTRAINT fk_pls_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking_lots    (id) ON DELETE CASCADE,
    CONSTRAINT fk_pls_service     FOREIGN KEY (service_id)     REFERENCES parking_services (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Servicios disponibles en cada estacionamiento (N:M)';

-- =============================================================================
-- 9. PARKING SPACES (espacios individuales)
-- =============================================================================
CREATE TABLE IF NOT EXISTS parking_spaces (
    id              BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    parking_lot_id  BIGINT UNSIGNED  NOT NULL,
    vehicle_type_id TINYINT UNSIGNED NOT NULL,
    space_number    VARCHAR(20)      NOT NULL COMMENT 'Ej: A-01, B-12, M-05',
    status          ENUM('AVAILABLE','OCCUPIED','RESERVED','MAINTENANCE') NOT NULL DEFAULT 'AVAILABLE',
    floor_level     TINYINT          NOT NULL DEFAULT 1  COMMENT 'Nivel/piso del espacio',
    is_covered      BOOLEAN          NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_spaces_lot_number     (parking_lot_id, space_number),
    KEY idx_spaces_lot_status           (parking_lot_id, status),
    KEY idx_spaces_vehicle_type         (vehicle_type_id),
    CONSTRAINT fk_spaces_parking_lot   FOREIGN KEY (parking_lot_id)  REFERENCES parking_lots  (id) ON DELETE CASCADE,
    CONSTRAINT fk_spaces_vehicle_type  FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Espacios individuales dentro de cada estacionamiento';

-- =============================================================================
-- 10. SPACE STATUS HISTORY
-- =============================================================================
CREATE TABLE IF NOT EXISTS space_status_history (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    space_id        BIGINT UNSIGNED NOT NULL,
    previous_status ENUM('AVAILABLE','OCCUPIED','RESERVED','MAINTENANCE') NULL,
    new_status      ENUM('AVAILABLE','OCCUPIED','RESERVED','MAINTENANCE') NOT NULL,
    changed_by      BIGINT UNSIGNED NULL  COMMENT 'Usuario que realizó el cambio',
    reason          VARCHAR(255)    NULL,
    changed_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ssh_space    (space_id),
    KEY idx_ssh_changed  (changed_at),
    CONSTRAINT fk_ssh_space   FOREIGN KEY (space_id)   REFERENCES parking_spaces (id) ON DELETE CASCADE,
    CONSTRAINT fk_ssh_changed FOREIGN KEY (changed_by) REFERENCES users          (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Historial de cambios de estado de espacios de estacionamiento';

-- =============================================================================
-- 11. SCHEDULES (horarios por día)
-- =============================================================================
CREATE TABLE IF NOT EXISTS schedules (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    parking_lot_id  BIGINT UNSIGNED NOT NULL,
    day_of_week     ENUM('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY') NOT NULL,
    open_time       TIME            NULL,
    close_time      TIME            NULL,
    is_open         BOOLEAN         NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_schedules_lot_day (parking_lot_id, day_of_week),
    CONSTRAINT fk_schedules_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking_lots (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Horarios de apertura/cierre por día de semana para cada estacionamiento';

-- =============================================================================
-- 12. TARIFFS (tarifas)
-- =============================================================================
CREATE TABLE IF NOT EXISTS tariffs (
    id               BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    parking_lot_id   BIGINT UNSIGNED  NOT NULL,
    vehicle_type_id  TINYINT UNSIGNED NOT NULL,
    tariff_type      ENUM('HOURLY','DAILY','MONTHLY') NOT NULL,
    price            DECIMAL(8,2)     NOT NULL,
    minimum_minutes  INT UNSIGNED     NOT NULL DEFAULT 30,
    is_active        BOOLEAN          NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_tariffs_lot_type (parking_lot_id, vehicle_type_id, tariff_type),
    KEY idx_tariffs_active (is_active),
    CONSTRAINT fk_tariffs_parking_lot  FOREIGN KEY (parking_lot_id)  REFERENCES parking_lots  (id) ON DELETE CASCADE,
    CONSTRAINT fk_tariffs_vehicle_type FOREIGN KEY (vehicle_type_id) REFERENCES vehicle_types (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Tarifas por tipo de vehículo y modalidad (por hora, día o mes)';

-- =============================================================================
-- 13. RESERVATIONS (reservas)
-- =============================================================================
CREATE TABLE IF NOT EXISTS reservations (
    id                   BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id              BIGINT UNSIGNED NOT NULL,
    parking_space_id     BIGINT UNSIGNED NOT NULL,
    vehicle_id           BIGINT UNSIGNED NOT NULL,
    start_time           DATETIME        NOT NULL,
    end_time             DATETIME        NOT NULL,
    actual_start         DATETIME        NULL,
    actual_end           DATETIME        NULL,
    status               ENUM('PENDING','CONFIRMED','ACTIVE','COMPLETED','CANCELLED','NO_SHOW') NOT NULL DEFAULT 'PENDING',
    total_price          DECIMAL(10,2)   NULL,
    cancellation_reason  TEXT            NULL,
    notes                TEXT            NULL,
    created_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_user_reservations (user_id, status),
    KEY idx_space_time        (parking_space_id, start_time, end_time),
    KEY idx_status_time       (status, start_time),
    KEY idx_reservations_vehicle (vehicle_id),
    CONSTRAINT fk_reservations_user    FOREIGN KEY (user_id)          REFERENCES users          (id),
    CONSTRAINT fk_reservations_space   FOREIGN KEY (parking_space_id) REFERENCES parking_spaces (id),
    CONSTRAINT fk_reservations_vehicle FOREIGN KEY (vehicle_id)       REFERENCES vehicles       (id),
    CONSTRAINT chk_reservation_times   CHECK (end_time > start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Reservas de espacios de estacionamiento';

-- =============================================================================
-- 14. RESERVATION STATUS HISTORY
-- =============================================================================
CREATE TABLE IF NOT EXISTS reservation_status_history (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    reservation_id   BIGINT UNSIGNED NOT NULL,
    previous_status  ENUM('PENDING','CONFIRMED','ACTIVE','COMPLETED','CANCELLED','NO_SHOW') NULL,
    new_status       ENUM('PENDING','CONFIRMED','ACTIVE','COMPLETED','CANCELLED','NO_SHOW') NOT NULL,
    changed_by       BIGINT UNSIGNED NULL,
    notes            VARCHAR(500)    NULL,
    changed_at       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_rsh_reservation (reservation_id),
    CONSTRAINT fk_rsh_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (id) ON DELETE CASCADE,
    CONSTRAINT fk_rsh_changed_by  FOREIGN KEY (changed_by)     REFERENCES users        (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Historial de cambios de estado de reservas';

-- =============================================================================
-- 15. PARKING RATINGS (calificaciones)
-- =============================================================================
CREATE TABLE IF NOT EXISTS parking_ratings (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    reservation_id  BIGINT UNSIGNED NOT NULL,
    user_id         BIGINT UNSIGNED NOT NULL,
    parking_lot_id  BIGINT UNSIGNED NOT NULL,
    rating          TINYINT UNSIGNED NOT NULL COMMENT '1 a 5 estrellas',
    comment         TEXT            NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ratings_reservation (reservation_id),
    KEY idx_ratings_parking  (parking_lot_id),
    KEY idx_ratings_user     (user_id),
    CONSTRAINT fk_ratings_reservation FOREIGN KEY (reservation_id) REFERENCES reservations  (id),
    CONSTRAINT fk_ratings_user        FOREIGN KEY (user_id)        REFERENCES users         (id),
    CONSTRAINT fk_ratings_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking_lots  (id),
    CONSTRAINT chk_rating_range       CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Calificaciones de estacionamientos por reserva completada';

-- =============================================================================
-- 16. AUDIT LOG
-- =============================================================================
CREATE TABLE IF NOT EXISTS audit_log (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NULL,
    action      VARCHAR(100)    NOT NULL COMMENT 'CREATE, UPDATE, DELETE, LOGIN, etc.',
    entity      VARCHAR(100)    NOT NULL COMMENT 'Tabla/entidad afectada',
    entity_id   BIGINT UNSIGNED NULL,
    old_values  JSON            NULL,
    new_values  JSON            NULL,
    ip_address  VARCHAR(45)     NULL,
    user_agent  VARCHAR(500)    NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_audit_user   (user_id),
    KEY idx_audit_entity (entity, entity_id),
    KEY idx_audit_action (action),
    KEY idx_audit_time   (created_at),
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Registro de auditoría del sistema';

-- =============================================================================
-- 17. OPERATOR PARKING ASSIGNMENTS
-- =============================================================================
CREATE TABLE IF NOT EXISTS operator_parking_assignments (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    operator_id     BIGINT UNSIGNED NOT NULL,
    parking_lot_id  BIGINT UNSIGNED NOT NULL,
    assigned_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by     BIGINT UNSIGNED NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    UNIQUE KEY uq_opa_operator_lot (operator_id, parking_lot_id),
    KEY idx_opa_parking (parking_lot_id),
    CONSTRAINT fk_opa_operator    FOREIGN KEY (operator_id)    REFERENCES users        (id),
    CONSTRAINT fk_opa_parking_lot FOREIGN KEY (parking_lot_id) REFERENCES parking_lots (id) ON DELETE CASCADE,
    CONSTRAINT fk_opa_assigned_by FOREIGN KEY (assigned_by)    REFERENCES users        (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='Asignación de operadores a estacionamientos específicos';

SET FOREIGN_KEY_CHECKS = 1;
