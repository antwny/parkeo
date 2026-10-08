-- =============================================================================
-- PARKEO - Datos de prueba (compatible con schema v1.1 - tablas reducidas)
-- MySQL 8.0 | UTF-8 sin BOM
-- Se puede ejecutar varias veces: primero vacía las tablas y luego inserta.
-- Contraseña de TODAS las cuentas de prueba: Password123!
-- =============================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
SET SQL_SAFE_UPDATES = 0;

-- =============================================================================
-- LIMPIEZA (resetea también los AUTO_INCREMENT)
-- =============================================================================
TRUNCATE TABLE audit_log;
TRUNCATE TABLE parking_ratings;
TRUNCATE TABLE status_history;
TRUNCATE TABLE reservations;
TRUNCATE TABLE operator_parking_assignments;
TRUNCATE TABLE tariffs;
TRUNCATE TABLE schedules;
TRUNCATE TABLE parking_spaces;
TRUNCATE TABLE parking_services;
TRUNCATE TABLE parking_lots;
TRUNCATE TABLE vehicles;
TRUNCATE TABLE refresh_tokens;
TRUNCATE TABLE user_roles;
TRUNCATE TABLE users;
TRUNCATE TABLE vehicle_types;
TRUNCATE TABLE roles;

-- =============================================================================
-- ROLES (nombres que usa el backend: RoleName.ROLE_*)
-- =============================================================================
INSERT INTO roles (id, name, label) VALUES
(1, 'ROLE_USER',     'Cliente'),
(2, 'ROLE_OPERATOR', 'Operador'),
(3, 'ROLE_ADMIN',    'Administrador');

-- =============================================================================
-- VEHICLE TYPES
-- =============================================================================
INSERT INTO vehicle_types (id, name, label, description, is_active) VALUES
(1, 'AUTOMOVIL', 'Automóvil',   'Vehículo de pasajeros estándar',     TRUE),
(2, 'MOTO',      'Motocicleta', 'Motocicleta o scooter',              TRUE),
(3, 'CAMIONETA', 'Camioneta',   'SUV, pick-up o camioneta',           TRUE),
(4, 'CAMION',    'Camión',      'Vehículo de carga pesada',           TRUE),
(5, 'BICICLETA', 'Bicicleta',   'Bicicleta convencional o eléctrica', TRUE);

-- =============================================================================
-- USERS (password_hash = bcrypt de 'Password123!')
-- El rol va en la tabla user_roles (no hay columna role_id en users)
-- =============================================================================
INSERT INTO users (id, email, password_hash, first_name, last_name, phone, is_active, email_verified) VALUES
(1, 'admin@parkeo.pe',          '$2a$10$lsgskzIZh6LpCawrQIgQmOOkTKAssUshrUw3ahlu1CV1u1HybIsSq', 'Admin',   'Parkeo',   '+51 999 000 001', TRUE, TRUE),
(2, 'operador@parkeo.pe',       '$2a$10$lsgskzIZh6LpCawrQIgQmOOkTKAssUshrUw3ahlu1CV1u1HybIsSq', 'Carlos',  'Ramos',    '+51 999 000 002', TRUE, TRUE),
(3, 'cliente@parkeo.pe',        '$2a$10$lsgskzIZh6LpCawrQIgQmOOkTKAssUshrUw3ahlu1CV1u1HybIsSq', 'María',   'González', '+51 987 654 321', TRUE, TRUE),
(4, 'juan.perez@parkeo.pe',     '$2a$10$lsgskzIZh6LpCawrQIgQmOOkTKAssUshrUw3ahlu1CV1u1HybIsSq', 'Juan',    'Pérez',    '+51 987 123 456', TRUE, TRUE),
(5, 'sofia.mendez@parkeo.pe',   '$2a$10$lsgskzIZh6LpCawrQIgQmOOkTKAssUshrUw3ahlu1CV1u1HybIsSq', 'Sofía',   'Méndez',   '+51 987 222 333', TRUE, TRUE),
(6, 'roberto.silva@parkeo.pe',  '$2a$10$lsgskzIZh6LpCawrQIgQmOOkTKAssUshrUw3ahlu1CV1u1HybIsSq', 'Roberto', 'Silva',    '+51 987 444 555', TRUE, FALSE);

INSERT INTO user_roles (user_id, role_id) VALUES
(1, 3),
(2, 2),
(3, 1),
(4, 1),
(5, 1),
(6, 1);

-- =============================================================================
-- VEHICLES
-- =============================================================================
INSERT INTO vehicles (id, user_id, vehicle_type_id, license_plate, brand, model, color, year) VALUES
(1, 3, 1, 'ABC-123', 'Toyota',  'Corolla',  'Blanco', 2021),
(2, 3, 2, 'MO-456',  'Honda',   'CB190R',   'Negro',  2022),
(3, 4, 1, 'DEF-789', 'Hyundai', 'Tucson',   'Plata',  2020),
(4, 4, 3, 'GHI-012', 'Ford',    'Ranger',   'Rojo',   2019),
(5, 5, 1, 'JKL-345', 'Kia',     'Sportage', 'Gris',   2023),
(6, 5, 2, 'MO-789',  'Yamaha',  'FZ25',     'Azul',   2021),
(7, 6, 1, 'MNO-901', 'Nissan',  'Sentra',   'Gris',   2020),
(8, 6, 3, 'PQR-234', 'Toyota',  'Hilux',    'Blanco', 2022);

-- =============================================================================
-- PARKING LOTS
-- =============================================================================
INSERT INTO parking_lots
    (id, owner_id, name, description, address, district, city, country,
     latitude, longitude, phone, email, total_capacity, is_active, is_open)
VALUES
(1, 1, 'Parkeo Miraflores Centro',
 'Estacionamiento moderno en el corazón de Miraflores, a pasos del Parque Kennedy. Contamos con seguridad 24 horas y cámaras CCTV.',
 'Av. Larco 1150, Miraflores', 'Miraflores', 'Lima', 'Perú',
 -12.11900000, -77.03100000, '+51 1 234-5678', 'miraflores@parkeo.pe', 60, TRUE, TRUE),
(2, 1, 'Parkeo San Isidro Financiero',
 'Ubicado en el distrito financiero de San Isidro. Ideal para ejecutivos y empresas. Servicio valet disponible.',
 'Calle Los Libertadores 456, San Isidro', 'San Isidro', 'Lima', 'Perú',
 -12.09700000, -77.03600000, '+51 1 234-9012', 'sanisidro@parkeo.pe', 80, TRUE, TRUE),
(3, 2, 'Parkeo Surco Chacarilla',
 'Amplio estacionamiento en el exclusivo barrio de Chacarilla. Techado y con acceso para personas con discapacidad.',
 'Av. El Polo 670, Santiago de Surco', 'Santiago de Surco', 'Lima', 'Perú',
 -12.12300000, -76.99000000, '+51 1 345-6789', 'surco@parkeo.pe', 100, TRUE, TRUE),
(4, 2, 'Parkeo Barranco Bohemio',
 'En el distrito bohemio de Barranco, cerca de la Bajada de los Baños. Perfecto para visitas turísticas y restaurantes.',
 'Jr. Junín 280, Barranco', 'Barranco', 'Lima', 'Perú',
 -12.14800000, -77.02100000, '+51 1 456-7890', 'barranco@parkeo.pe', 35, TRUE, TRUE),
(5, 1, 'Parkeo Lince Express',
 'Estacionamiento rápido en Lince, cerca del Mercado de Surquillo. Tarifas accesibles y rotación alta.',
 'Av. Arequipa 3200, Lince', 'Lince', 'Lima', 'Perú',
 -12.08500000, -77.03300000, '+51 1 567-8901', 'lince@parkeo.pe', 45, TRUE, TRUE),
(6, 1, 'Parkeo San Borja Premium',
 'Estacionamiento premium en San Borja con cargadores eléctricos y servicio de lavado. Ideal para residentes y visitantes al Jockey Plaza.',
 'Av. Aviación 2850, San Borja', 'San Borja', 'Lima', 'Perú',
 -12.10200000, -76.99700000, '+51 1 678-9012', 'sanborja@parkeo.pe', 90, TRUE, TRUE);

-- =============================================================================
-- PARKING SERVICES
-- =============================================================================
INSERT INTO parking_services (parking_lot_id, name, description, icon) VALUES
-- Miraflores
(1, 'TECHADO',             'Estacionamiento con techo cubierto',          'roofing'),
(1, 'SEGURIDAD_24H',       'Personal de seguridad las 24 horas',          'security'),
(1, 'CAMARA_CCTV',         'Sistema de cámaras de vigilancia',            'videocam'),
(1, 'DISCAPACITADOS',      'Acceso para personas con discapacidad',       'accessible'),
(1, 'WIFI',                'Acceso Wi-Fi en las instalaciones',           'wifi'),
-- San Isidro
(2, 'TECHADO',             'Estacionamiento con techo cubierto',          'roofing'),
(2, 'SEGURIDAD_24H',       'Personal de seguridad las 24 horas',          'security'),
(2, 'CAMARA_CCTV',         'Sistema de cámaras de vigilancia',            'videocam'),
(2, 'VALET',               'Servicio Valet',                              'directions_car'),
(2, 'WIFI',                'Acceso Wi-Fi en las instalaciones',           'wifi'),
(2, 'DISCAPACITADOS',      'Acceso para personas con discapacidad',       'accessible'),
-- Surco
(3, 'TECHADO',             'Estacionamiento con techo cubierto',          'roofing'),
(3, 'SEGURIDAD_24H',       'Personal de seguridad las 24 horas',          'security'),
(3, 'CAMARA_CCTV',         'Sistema de cámaras de vigilancia',            'videocam'),
(3, 'DISCAPACITADOS',      'Acceso para personas con discapacidad',       'accessible'),
(3, 'BICICLETAS',          'Estacionamiento para bicicletas',             'pedal_bike'),
(3, 'MOTOS',               'Zona exclusiva para motocicletas',            'two_wheeler'),
-- Barranco
(4, 'CAMARA_CCTV',         'Sistema de cámaras de vigilancia',            'videocam'),
(4, 'WIFI',                'Acceso Wi-Fi en las instalaciones',           'wifi'),
(4, 'BICICLETAS',          'Estacionamiento para bicicletas',             'pedal_bike'),
(4, 'MOTOS',               'Zona exclusiva para motocicletas',            'two_wheeler'),
-- Lince
(5, 'SEGURIDAD_24H',       'Personal de seguridad las 24 horas',          'security'),
(5, 'CAMARA_CCTV',         'Sistema de cámaras de vigilancia',            'videocam'),
(5, 'MOTOS',               'Zona exclusiva para motocicletas',            'two_wheeler'),
-- San Borja
(6, 'TECHADO',             'Estacionamiento con techo cubierto',          'roofing'),
(6, 'SEGURIDAD_24H',       'Personal de seguridad las 24 horas',          'security'),
(6, 'CAMARA_CCTV',         'Sistema de cámaras de vigilancia',            'videocam'),
(6, 'LAVADO',              'Servicio de lavado de vehículos',             'local_car_wash'),
(6, 'CARGADOR_ELECTRICO',  'Estación de carga para vehículos eléctricos', 'ev_station'),
(6, 'DISCAPACITADOS',      'Acceso para personas con discapacidad',       'accessible'),
(6, 'VALET',               'Servicio Valet',                              'directions_car');

-- =============================================================================
-- PARKING SPACES (generados; total por local = total_capacity)
-- spec: lot, tipo vehículo, prefijo, cantidad, espacios por piso, techado
-- =============================================================================
INSERT INTO parking_spaces (parking_lot_id, vehicle_type_id, space_number, status, floor_level, is_covered)
WITH RECURSIVE n AS (
    SELECT 1 AS i
    UNION ALL
    SELECT i + 1 FROM n WHERE i < 60
),
spec AS (
    -- Miraflores (60)
    SELECT 1 AS lot, 1 AS vt, 'A' AS p, 40 AS cnt, 15 AS per, TRUE  AS cov UNION ALL
    SELECT 1, 3, 'C', 12, 6,  TRUE  UNION ALL
    SELECT 1, 2, 'M', 8,  8,  FALSE UNION ALL
    -- San Isidro (80)
    SELECT 2, 1, 'A', 50, 12, TRUE  UNION ALL
    SELECT 2, 3, 'C', 20, 6,  TRUE  UNION ALL
    SELECT 2, 2, 'M', 10, 10, TRUE  UNION ALL
    -- Surco (100)
    SELECT 3, 1, 'A', 60, 12, TRUE  UNION ALL
    SELECT 3, 3, 'C', 25, 6,  TRUE  UNION ALL
    SELECT 3, 2, 'M', 10, 10, FALSE UNION ALL
    SELECT 3, 5, 'B', 5,  5,  FALSE UNION ALL
    -- Barranco (35)
    SELECT 4, 1, 'A', 20, 20, FALSE UNION ALL
    SELECT 4, 2, 'M', 8,  8,  FALSE UNION ALL
    SELECT 4, 5, 'B', 7,  7,  FALSE UNION ALL
    -- Lince (45)
    SELECT 5, 1, 'A', 30, 12, FALSE UNION ALL
    SELECT 5, 3, 'C', 10, 10, FALSE UNION ALL
    SELECT 5, 2, 'M', 5,  5,  FALSE UNION ALL
    -- San Borja (90)
    SELECT 6, 1, 'A', 55, 12, TRUE  UNION ALL
    SELECT 6, 3, 'C', 25, 9,  TRUE  UNION ALL
    SELECT 6, 2, 'M', 10, 10, TRUE
)
SELECT s.lot, s.vt, CONCAT(s.p, '-', LPAD(n.i, 2, '0')), 'AVAILABLE', CEIL(n.i / s.per), s.cov
FROM spec s
JOIN n ON n.i <= s.cnt;

-- Algunos espacios con otros estados (para que el mapa muestre variedad)
UPDATE parking_spaces SET status = 'OCCUPIED' WHERE
    (parking_lot_id = 1 AND space_number IN ('A-08','A-23','C-03','M-03')) OR
    (parking_lot_id = 2 AND space_number IN ('A-05','A-17','C-03','M-04')) OR
    (parking_lot_id = 3 AND space_number IN ('A-04','A-18','C-03','M-04')) OR
    (parking_lot_id = 4 AND space_number IN ('A-02','M-03')) OR
    (parking_lot_id = 5 AND space_number IN ('A-03','A-18','C-03','M-05')) OR
    (parking_lot_id = 6 AND space_number IN ('A-06','A-15','C-03','M-04'));

UPDATE parking_spaces SET status = 'RESERVED' WHERE
    (parking_lot_id = 1 AND space_number IN ('A-05')) OR
    (parking_lot_id = 2 AND space_number IN ('A-03','A-27')) OR
    (parking_lot_id = 3 AND space_number IN ('A-06','A-29','C-23')) OR
    (parking_lot_id = 4 AND space_number IN ('A-06')) OR
    (parking_lot_id = 5 AND space_number IN ('A-07')) OR
    (parking_lot_id = 6 AND space_number IN ('A-04','A-21','C-07'));

UPDATE parking_spaces SET status = 'MAINTENANCE' WHERE
    (parking_lot_id = 1 AND space_number = 'A-36') OR
    (parking_lot_id = 2 AND space_number = 'A-37') OR
    (parking_lot_id = 3 AND space_number = 'A-37') OR
    (parking_lot_id = 4 AND space_number = 'A-12') OR
    (parking_lot_id = 5 AND space_number = 'A-25') OR
    (parking_lot_id = 6 AND space_number = 'A-30');

-- =============================================================================
-- SCHEDULES
-- =============================================================================
INSERT INTO schedules (parking_lot_id, day_of_week, open_time, close_time, is_open) VALUES
-- Miraflores
(1,'MONDAY','07:00:00','23:00:00',TRUE),(1,'TUESDAY','07:00:00','23:00:00',TRUE),
(1,'WEDNESDAY','07:00:00','23:00:00',TRUE),(1,'THURSDAY','07:00:00','23:00:00',TRUE),
(1,'FRIDAY','07:00:00','23:59:00',TRUE),(1,'SATURDAY','07:00:00','23:59:00',TRUE),
(1,'SUNDAY','08:00:00','22:00:00',TRUE),
-- San Isidro
(2,'MONDAY','06:00:00','22:00:00',TRUE),(2,'TUESDAY','06:00:00','22:00:00',TRUE),
(2,'WEDNESDAY','06:00:00','22:00:00',TRUE),(2,'THURSDAY','06:00:00','22:00:00',TRUE),
(2,'FRIDAY','06:00:00','22:00:00',TRUE),(2,'SATURDAY','07:00:00','20:00:00',TRUE),
(2,'SUNDAY',NULL,NULL,FALSE),
-- Surco (24h)
(3,'MONDAY','00:00:00','23:59:00',TRUE),(3,'TUESDAY','00:00:00','23:59:00',TRUE),
(3,'WEDNESDAY','00:00:00','23:59:00',TRUE),(3,'THURSDAY','00:00:00','23:59:00',TRUE),
(3,'FRIDAY','00:00:00','23:59:00',TRUE),(3,'SATURDAY','00:00:00','23:59:00',TRUE),
(3,'SUNDAY','00:00:00','23:59:00',TRUE),
-- Barranco
(4,'MONDAY','08:00:00','22:00:00',TRUE),(4,'TUESDAY','08:00:00','22:00:00',TRUE),
(4,'WEDNESDAY','08:00:00','22:00:00',TRUE),(4,'THURSDAY','08:00:00','22:00:00',TRUE),
(4,'FRIDAY','08:00:00','22:00:00',TRUE),(4,'SATURDAY','08:00:00','22:00:00',TRUE),
(4,'SUNDAY','09:00:00','20:00:00',TRUE),
-- Lince
(5,'MONDAY','07:00:00','21:00:00',TRUE),(5,'TUESDAY','07:00:00','21:00:00',TRUE),
(5,'WEDNESDAY','07:00:00','21:00:00',TRUE),(5,'THURSDAY','07:00:00','21:00:00',TRUE),
(5,'FRIDAY','07:00:00','21:00:00',TRUE),(5,'SATURDAY','08:00:00','18:00:00',TRUE),
(5,'SUNDAY',NULL,NULL,FALSE),
-- San Borja
(6,'MONDAY','06:00:00','23:59:00',TRUE),(6,'TUESDAY','06:00:00','23:59:00',TRUE),
(6,'WEDNESDAY','06:00:00','23:59:00',TRUE),(6,'THURSDAY','06:00:00','23:59:00',TRUE),
(6,'FRIDAY','06:00:00','23:59:00',TRUE),(6,'SATURDAY','06:00:00','23:59:00',TRUE),
(6,'SUNDAY','07:00:00','22:00:00',TRUE);

-- =============================================================================
-- TARIFFS (soles peruanos)
-- =============================================================================
INSERT INTO tariffs (parking_lot_id, vehicle_type_id, tariff_type, price, minimum_minutes) VALUES
-- Miraflores
(1,1,'HOURLY',5.00,30),(1,1,'DAILY',40.00,60),(1,1,'MONTHLY',300.00,60),
(1,2,'HOURLY',2.00,30),(1,2,'DAILY',15.00,30),(1,2,'MONTHLY',120.00,30),
(1,3,'HOURLY',7.00,30),(1,3,'DAILY',55.00,60),(1,3,'MONTHLY',420.00,60),
-- San Isidro
(2,1,'HOURLY',8.00,30),(2,1,'DAILY',60.00,60),(2,1,'MONTHLY',450.00,60),
(2,2,'HOURLY',3.00,30),(2,2,'DAILY',20.00,30),(2,2,'MONTHLY',150.00,30),
(2,3,'HOURLY',10.00,30),(2,3,'DAILY',80.00,60),(2,3,'MONTHLY',600.00,60),
-- Surco
(3,1,'HOURLY',6.00,30),(3,1,'DAILY',45.00,60),(3,1,'MONTHLY',350.00,60),
(3,2,'HOURLY',2.50,30),(3,2,'DAILY',18.00,30),(3,2,'MONTHLY',130.00,30),
(3,3,'HOURLY',8.00,30),(3,3,'DAILY',60.00,60),(3,3,'MONTHLY',480.00,60),
-- Barranco
(4,1,'HOURLY',4.00,30),(4,1,'DAILY',30.00,60),(4,1,'MONTHLY',220.00,60),
(4,2,'HOURLY',1.50,30),(4,2,'DAILY',12.00,30),(4,2,'MONTHLY',90.00,30),
(4,3,'HOURLY',6.00,30),(4,3,'DAILY',45.00,60),(4,3,'MONTHLY',320.00,60),
-- Lince
(5,1,'HOURLY',3.00,30),(5,1,'DAILY',22.00,60),(5,1,'MONTHLY',180.00,60),
(5,2,'HOURLY',1.50,30),(5,2,'DAILY',10.00,30),(5,2,'MONTHLY',80.00,30),
(5,3,'HOURLY',5.00,30),(5,3,'DAILY',35.00,60),(5,3,'MONTHLY',280.00,60),
-- San Borja
(6,1,'HOURLY',7.00,30),(6,1,'DAILY',50.00,60),(6,1,'MONTHLY',400.00,60),
(6,2,'HOURLY',2.50,30),(6,2,'DAILY',18.00,30),(6,2,'MONTHLY',140.00,30),
(6,3,'HOURLY',9.00,30),(6,3,'DAILY',70.00,60),(6,3,'MONTHLY',550.00,60);

-- =============================================================================
-- OPERATOR PARKING ASSIGNMENTS
-- =============================================================================
INSERT INTO operator_parking_assignments (operator_id, parking_lot_id, assigned_by, is_active) VALUES
(2, 1, 1, TRUE),
(2, 3, 1, TRUE),
(2, 4, 1, TRUE);

-- =============================================================================
-- RESERVATIONS (6 reservas con distintos estados -> ids 1 a 6)
-- =============================================================================
INSERT INTO reservations (user_id, parking_space_id, vehicle_id, parking_lot_id, start_time, end_time, actual_start, actual_end, status, total_price)
SELECT 3, ps.id, 1, ps.parking_lot_id, '2026-09-15 09:00:00', '2026-09-15 11:00:00', '2026-09-15 09:05:00', '2026-09-15 11:10:00', 'COMPLETED', 10.00
FROM parking_spaces ps WHERE ps.parking_lot_id = 1 AND ps.space_number = 'A-01';

INSERT INTO reservations (user_id, parking_space_id, vehicle_id, parking_lot_id, start_time, end_time, actual_start, actual_end, status, total_price)
SELECT 4, ps.id, 3, ps.parking_lot_id, '2026-09-16 14:00:00', '2026-09-16 17:00:00', '2026-09-16 14:02:00', '2026-09-16 17:15:00', 'COMPLETED', 24.00
FROM parking_spaces ps WHERE ps.parking_lot_id = 2 AND ps.space_number = 'A-04';

INSERT INTO reservations (user_id, parking_space_id, vehicle_id, parking_lot_id, start_time, end_time, status, total_price)
SELECT 5, ps.id, 5, ps.parking_lot_id, '2026-09-19 10:00:00', '2026-09-19 12:00:00', 'CONFIRMED', 12.00
FROM parking_spaces ps WHERE ps.parking_lot_id = 3 AND ps.space_number = 'A-05';

INSERT INTO reservations (user_id, parking_space_id, vehicle_id, parking_lot_id, start_time, end_time, status, total_price)
SELECT 3, ps.id, 2, ps.parking_lot_id, '2026-09-20 08:00:00', '2026-09-20 10:00:00', 'PENDING', 4.00
FROM parking_spaces ps WHERE ps.parking_lot_id = 4 AND ps.space_number = 'M-01';

INSERT INTO reservations (user_id, parking_space_id, vehicle_id, parking_lot_id, start_time, end_time, status, total_price, cancellation_reason)
SELECT 6, ps.id, 7, ps.parking_lot_id, '2026-09-17 11:00:00', '2026-09-17 14:00:00', 'CANCELLED', 9.00, 'Cambio de planes del usuario'
FROM parking_spaces ps WHERE ps.parking_lot_id = 5 AND ps.space_number = 'A-01';

INSERT INTO reservations (user_id, parking_space_id, vehicle_id, parking_lot_id, start_time, end_time, actual_start, status, total_price)
SELECT 4, ps.id, 4, ps.parking_lot_id, '2026-09-18 09:00:00', '2026-09-18 12:00:00', '2026-09-18 09:03:00', 'ACTIVE', 27.00
FROM parking_spaces ps WHERE ps.parking_lot_id = 6 AND ps.space_number = 'C-01';

-- =============================================================================
-- STATUS HISTORY
-- =============================================================================
INSERT INTO status_history (entity_type, entity_id, previous_status, new_status, changed_by, notes) VALUES
('RESERVATION', 1, NULL,        'PENDING',   3, 'Reserva creada'),
('RESERVATION', 1, 'PENDING',   'CONFIRMED', 1, 'Confirmada automáticamente'),
('RESERVATION', 1, 'CONFIRMED', 'ACTIVE',    3, 'Cliente ingresó al estacionamiento'),
('RESERVATION', 1, 'ACTIVE',    'COMPLETED', 3, 'Cliente salió del estacionamiento'),
('RESERVATION', 2, NULL,        'PENDING',   4, 'Reserva creada'),
('RESERVATION', 2, 'PENDING',   'CONFIRMED', 1, 'Confirmada automáticamente'),
('RESERVATION', 2, 'CONFIRMED', 'ACTIVE',    4, 'Cliente ingresó'),
('RESERVATION', 2, 'ACTIVE',    'COMPLETED', 4, 'Completada'),
('RESERVATION', 3, NULL,        'PENDING',   5, 'Reserva creada'),
('RESERVATION', 3, 'PENDING',   'CONFIRMED', 1, 'Confirmada'),
('RESERVATION', 4, NULL,        'PENDING',   3, 'Reserva creada'),
('RESERVATION', 5, NULL,        'PENDING',   6, 'Reserva creada'),
('RESERVATION', 5, 'PENDING',   'CANCELLED', 6, 'Cancelada por el cliente'),
('RESERVATION', 6, NULL,        'PENDING',   4, 'Reserva creada'),
('RESERVATION', 6, 'PENDING',   'CONFIRMED', 1, 'Confirmada'),
('RESERVATION', 6, 'CONFIRMED', 'ACTIVE',    4, 'Cliente ingresó');

-- =============================================================================
-- PARKING RATINGS (solo reservas completadas: 1 y 2)
-- =============================================================================
INSERT INTO parking_ratings (reservation_id, user_id, parking_lot_id, rating, comment) VALUES
(1, 3, 1, 5, 'Excelente servicio. Muy limpio y bien organizado. El personal fue muy amable.'),
(2, 4, 2, 4, 'Muy buen estacionamiento. El servicio valet funcionó perfecto. Solo tardó un poco en la entrada.');

-- =============================================================================
-- AUDIT LOG
-- =============================================================================
INSERT INTO audit_log (user_id, action, entity, entity_id, new_values, ip_address) VALUES
(1, 'CREATE', 'parking_lots', 1, '{"name":"Parkeo Miraflores Centro","district":"Miraflores"}',          '127.0.0.1'),
(1, 'CREATE', 'parking_lots', 2, '{"name":"Parkeo San Isidro Financiero","district":"San Isidro"}',       '127.0.0.1'),
(2, 'CREATE', 'parking_lots', 3, '{"name":"Parkeo Surco Chacarilla","district":"Santiago de Surco"}',    '127.0.0.1'),
(2, 'CREATE', 'parking_lots', 4, '{"name":"Parkeo Barranco Bohemio","district":"Barranco"}',             '127.0.0.1'),
(1, 'CREATE', 'parking_lots', 5, '{"name":"Parkeo Lince Express","district":"Lince"}',                   '127.0.0.1'),
(1, 'CREATE', 'parking_lots', 6, '{"name":"Parkeo San Borja Premium","district":"San Borja"}',            '127.0.0.1'),
(3, 'LOGIN',  'users',        3, '{"email":"cliente@parkeo.pe"}',                                        '192.168.1.10'),
(4, 'LOGIN',  'users',        4, '{"email":"juan.perez@parkeo.pe"}',                                     '192.168.1.11');

SET FOREIGN_KEY_CHECKS = 1;
SET SQL_SAFE_UPDATES = 1;

-- Verificación rápida (debe dar: 6 lotes, 410 espacios, 6 reservas, 6 usuarios)
SELECT (SELECT COUNT(*) FROM parking_lots)  AS lotes,
       (SELECT COUNT(*) FROM parking_spaces) AS espacios,
       (SELECT COUNT(*) FROM reservations)   AS reservas,
       (SELECT COUNT(*) FROM users)          AS usuarios;