-- =============================================================================
-- PARKEO - Datos de Prueba (Seed Data)
-- MySQL 8.0
-- Versión: 1.0.0
-- Fecha: 2026-09-18
-- NOTA: Las contraseñas son bcrypt hash de 'Password123!'
-- =============================================================================

SET FOREIGN_KEY_CHECKS = 0;
SET SQL_MODE = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- =============================================================================
-- ROLES
-- =============================================================================
INSERT INTO roles (id, name, label) VALUES
(1, 'CLIENTE',   'Cliente'),
(2, 'OPERADOR',  'Operador'),
(3, 'ADMIN',     'Administrador')
ON DUPLICATE KEY UPDATE label = VALUES(label);

-- =============================================================================
-- VEHICLE TYPES
-- =============================================================================
INSERT INTO vehicle_types (id, name, label, description) VALUES
(1, 'AUTOMOVIL',   'Automóvil',   'Vehículo de pasajeros estándar'),
(2, 'MOTO',        'Motocicleta', 'Motocicleta o scooter'),
(3, 'CAMIONETA',   'Camioneta',   'SUV, pick-up o camioneta'),
(4, 'CAMION',      'Camión',      'Vehículo de carga pesada'),
(5, 'BICICLETA',   'Bicicleta',   'Bicicleta convencional o eléctrica')
ON DUPLICATE KEY UPDATE label = VALUES(label);

-- =============================================================================
-- PARKING SERVICES (catálogo)
-- =============================================================================
INSERT INTO parking_services (id, name, label, icon, description) VALUES
(1,  'TECHADO',              'Techado',                  'roofing',           'Estacionamiento con techo cubierto'),
(2,  'SEGURIDAD_24H',        'Seguridad 24 horas',        'security',          'Personal de seguridad las 24 horas'),
(3,  'CAMARA_CCTV',          'Cámaras CCTV',              'videocam',          'Sistema de cámaras de vigilancia'),
(4,  'LAVADO',               'Servicio de lavado',        'local_car_wash',    'Lavado de vehículos en el local'),
(5,  'CARGADOR_ELECTRICO',   'Cargador eléctrico',        'ev_station',        'Estación de carga para vehículos eléctricos'),
(6,  'DISCAPACITADOS',       'Acceso discapacitados',     'accessible',        'Espacios y accesos para personas con discapacidad'),
(7,  'WIFI',                 'Wi-Fi gratuito',            'wifi',              'Acceso Wi-Fi en las instalaciones'),
(8,  'BICICLETAS',           'Estacionamiento bicis',     'pedal_bike',        'Espacios para bicicletas'),
(9,  'MOTOS',                'Zona de motos',             'two_wheeler',       'Zona exclusiva para motocicletas'),
(10, 'VALET',                'Servicio Valet',            'directions_car',    'Estacionamiento con servicio valet')
ON DUPLICATE KEY UPDATE label = VALUES(label);

-- =============================================================================
-- USERS  (password_hash = bcrypt('Password123!', cost=10))
-- =============================================================================
INSERT INTO users (id, email, password_hash, first_name, last_name, phone, is_active, email_verified, role_id) VALUES
-- ADMIN
(1, 'admin@parkeo.pe',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhrO',
    'Admin', 'Parkeo', '+51 999 000 001', TRUE, TRUE, 3),
-- OPERADOR
(2, 'operador@parkeo.pe',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhrO',
    'Carlos', 'Ramos', '+51 999 000 002', TRUE, TRUE, 2),
-- CLIENTES
(3, 'cliente@parkeo.pe',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhrO',
    'María', 'González', '+51 987 654 321', TRUE, TRUE, 1),
(4, 'juan.perez@parkeo.pe',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhrO',
    'Juan', 'Pérez', '+51 987 123 456', TRUE, TRUE, 1),
-- CLIENTES ADICIONALES
(5, 'sofia.mendez@parkeo.pe',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhrO',
    'Sofía', 'Méndez', '+51 987 222 333', TRUE, TRUE, 1),
(6, 'roberto.silva@parkeo.pe',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhrO',
    'Roberto', 'Silva', '+51 987 444 555', TRUE, FALSE, 1)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- VEHICLES
-- =============================================================================
INSERT INTO vehicles (id, user_id, vehicle_type_id, plate, brand, model, color, year) VALUES
-- Vehículos de María (cliente id=3)
(1,  3, 1, 'ABC-123', 'Toyota',    'Corolla',    'Blanco',  2021),
(2,  3, 2, 'MO-456',  'Honda',     'CB190R',     'Negro',   2022),
-- Vehículos de Juan (cliente id=4)
(3,  4, 1, 'DEF-789', 'Hyundai',   'Tucson',     'Plata',   2020),
(4,  4, 3, 'GHI-012', 'Ford',      'Ranger',     'Rojo',    2019),
-- Vehículos de Sofía (cliente id=5)
(5,  5, 1, 'JKL-345', 'Kia',       'Sportage',   'Azul',    2023),
(6,  5, 2, 'MO-678',  'Yamaha',    'FZ25',       'Negro',   2021),
-- Vehículos de Roberto (cliente id=6)
(7,  6, 1, 'MNO-901', 'Nissan',    'Sentra',     'Gris',    2020),
(8,  6, 3, 'PQR-234', 'Toyota',    'Hilux',      'Blanco',  2022)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- PARKING LOTS (estacionamientos en Lima, Perú)
-- =============================================================================
INSERT INTO parking_lots
    (id, owner_id, name, description, address, district, city, country,
     latitude, longitude, phone, email, total_capacity, is_active, is_open)
VALUES
-- 1. Miraflores
(1, 1,
 'Parkeo Miraflores Centro',
 'Estacionamiento moderno en el corazón de Miraflores, a pasos del Parque Kennedy. Contamos con seguridad 24 horas y cámaras CCTV.',
 'Av. Larco 1150, Miraflores', 'Miraflores', 'Lima', 'Perú',
 -12.11900000, -77.03100000,
 '+51 1 234-5678', 'miraflores@parkeo.pe', 60, TRUE, TRUE),

-- 2. San Isidro
(2, 1,
 'Parkeo San Isidro Financiero',
 'Ubicado en el distrito financiero de San Isidro. Ideal para ejecutivos y empresas. Servicio valet disponible.',
 'Calle Los Libertadores 456, San Isidro', 'San Isidro', 'Lima', 'Perú',
 -12.09700000, -77.03600000,
 '+51 1 234-9012', 'sanisidro@parkeo.pe', 80, TRUE, TRUE),

-- 3. Surco
(3, 2,
 'Parkeo Surco Chacarilla',
 'Amplio estacionamiento en el exclusivo barrio de Chacarilla. Techado y con acceso para personas con discapacidad.',
 'Av. El Polo 670, Santiago de Surco', 'Santiago de Surco', 'Lima', 'Perú',
 -12.12300000, -76.99000000,
 '+51 1 345-6789', 'surco@parkeo.pe', 100, TRUE, TRUE),

-- 4. Barranco
(4, 2,
 'Parkeo Barranco Bohemio',
 'En el distrito bohemio de Barranco, cerca de la Bajada de los Baños. Perfecto para visitas turísticas y restaurantes.',
 'Jr. Junín 280, Barranco', 'Barranco', 'Lima', 'Perú',
 -12.14800000, -77.02100000,
 '+51 1 456-7890', 'barranco@parkeo.pe', 35, TRUE, TRUE),

-- 5. Lince
(5, 1,
 'Parkeo Lince Express',
 'Estacionamiento rápido en Lince, cerca del Mercado de Surquillo. Tarifas accesibles y rotación alta.',
 'Av. Arequipa 3200, Lince', 'Lince', 'Lima', 'Perú',
 -12.08500000, -77.03300000,
 '+51 1 567-8901', 'lince@parkeo.pe', 45, TRUE, TRUE),

-- 6. San Borja
(6, 1,
 'Parkeo San Borja Premium',
 'Estacionamiento premium en San Borja con cargadores eléctricos y servicio de lavado. Ideal para residentes y visitantes al Jockey Plaza.',
 'Av. Aviación 2850, San Borja', 'San Borja', 'Lima', 'Perú',
 -12.10200000, -76.99700000,
 '+51 1 678-9012', 'sanborja@parkeo.pe', 90, TRUE, TRUE)
ON DUPLICATE KEY UPDATE updated_at = CURRENT_TIMESTAMP;

-- =============================================================================
-- PARKING LOT SERVICES
-- =============================================================================
INSERT INTO parking_lot_services (parking_lot_id, service_id) VALUES
-- Miraflores: techado, seguridad 24h, CCTV, discapacitados, wifi
(1,1),(1,2),(1,3),(1,6),(1,7),
-- San Isidro: techado, seguridad 24h, CCTV, valet, wifi, discapacitados
(2,1),(2,2),(2,3),(2,6),(2,7),(2,10),
-- Surco: techado, seguridad 24h, CCTV, discapacitados, bicicletas, motos
(3,1),(3,2),(3,3),(3,6),(3,8),(3,9),
-- Barranco: CCTV, motos, bicicletas, wifi
(4,3),(4,7),(4,8),(4,9),
-- Lince: seguridad 24h, CCTV, motos
(5,2),(5,3),(5,9),
-- San Borja: techado, seguridad 24h, CCTV, lavado, cargador eléctrico, discapacitados, valet
(6,1),(6,2),(6,3),(6,4),(6,5),(6,6),(6,10)
ON DUPLICATE KEY UPDATE parking_lot_id = parking_lot_id;

-- =============================================================================
-- PARKING SPACES
-- Distribución por estacionamiento
-- =============================================================================

-- ----- Estacionamiento 1: Miraflores (60 espacios) -----
-- 40 automóviles (A-01..A-40), 12 camionetas (C-01..C-12), 8 motos (M-01..M-08)
INSERT INTO parking_spaces (parking_lot_id, vehicle_type_id, space_number, status, floor_level, is_covered) VALUES
(1,1,'A-01','AVAILABLE',1,TRUE),(1,1,'A-02','AVAILABLE',1,TRUE),(1,1,'A-03','AVAILABLE',1,TRUE),
(1,1,'A-04','AVAILABLE',1,TRUE),(1,1,'A-05','RESERVED',1,TRUE),(1,1,'A-06','AVAILABLE',1,TRUE),
(1,1,'A-07','AVAILABLE',1,TRUE),(1,1,'A-08','OCCUPIED',1,TRUE),(1,1,'A-09','AVAILABLE',1,TRUE),
(1,1,'A-10','AVAILABLE',1,TRUE),(1,1,'A-11','AVAILABLE',1,TRUE),(1,1,'A-12','AVAILABLE',1,TRUE),
(1,1,'A-13','AVAILABLE',1,TRUE),(1,1,'A-14','AVAILABLE',1,TRUE),(1,1,'A-15','AVAILABLE',1,TRUE),
(1,1,'A-16','AVAILABLE',2,TRUE),(1,1,'A-17','AVAILABLE',2,TRUE),(1,1,'A-18','AVAILABLE',2,TRUE),
(1,1,'A-19','AVAILABLE',2,TRUE),(1,1,'A-20','AVAILABLE',2,TRUE),(1,1,'A-21','AVAILABLE',2,TRUE),
(1,1,'A-22','AVAILABLE',2,TRUE),(1,1,'A-23','OCCUPIED',2,TRUE),(1,1,'A-24','AVAILABLE',2,TRUE),
(1,1,'A-25','AVAILABLE',2,TRUE),(1,1,'A-26','AVAILABLE',2,TRUE),(1,1,'A-27','AVAILABLE',2,TRUE),
(1,1,'A-28','AVAILABLE',2,TRUE),(1,1,'A-29','AVAILABLE',2,TRUE),(1,1,'A-30','AVAILABLE',2,TRUE),
(1,1,'A-31','AVAILABLE',3,TRUE),(1,1,'A-32','AVAILABLE',3,TRUE),(1,1,'A-33','AVAILABLE',3,TRUE),
(1,1,'A-34','AVAILABLE',3,TRUE),(1,1,'A-35','AVAILABLE',3,TRUE),(1,1,'A-36','MAINTENANCE',3,TRUE),
(1,1,'A-37','AVAILABLE',3,TRUE),(1,1,'A-38','AVAILABLE',3,TRUE),(1,1,'A-39','AVAILABLE',3,TRUE),
(1,1,'A-40','AVAILABLE',3,TRUE),
(1,3,'C-01','AVAILABLE',1,TRUE),(1,3,'C-02','AVAILABLE',1,TRUE),(1,3,'C-03','OCCUPIED',1,TRUE),
(1,3,'C-04','AVAILABLE',1,TRUE),(1,3,'C-05','AVAILABLE',1,TRUE),(1,3,'C-06','AVAILABLE',1,TRUE),
(1,3,'C-07','AVAILABLE',2,TRUE),(1,3,'C-08','AVAILABLE',2,TRUE),(1,3,'C-09','AVAILABLE',2,TRUE),
(1,3,'C-10','AVAILABLE',2,TRUE),(1,3,'C-11','AVAILABLE',2,TRUE),(1,3,'C-12','AVAILABLE',2,TRUE),
(1,2,'M-01','AVAILABLE',1,FALSE),(1,2,'M-02','AVAILABLE',1,FALSE),(1,2,'M-03','OCCUPIED',1,FALSE),
(1,2,'M-04','AVAILABLE',1,FALSE),(1,2,'M-05','AVAILABLE',1,FALSE),(1,2,'M-06','AVAILABLE',1,FALSE),
(1,2,'M-07','AVAILABLE',1,FALSE),(1,2,'M-08','AVAILABLE',1,FALSE);

-- ----- Estacionamiento 2: San Isidro (80 espacios) -----
-- 50 automóviles, 20 camionetas, 10 motos
INSERT INTO parking_spaces (parking_lot_id, vehicle_type_id, space_number, status, floor_level, is_covered) VALUES
(2,1,'A-01','AVAILABLE',1,TRUE),(2,1,'A-02','AVAILABLE',1,TRUE),(2,1,'A-03','RESERVED',1,TRUE),
(2,1,'A-04','AVAILABLE',1,TRUE),(2,1,'A-05','OCCUPIED',1,TRUE),(2,1,'A-06','AVAILABLE',1,TRUE),
(2,1,'A-07','AVAILABLE',1,TRUE),(2,1,'A-08','AVAILABLE',1,TRUE),(2,1,'A-09','AVAILABLE',1,TRUE),
(2,1,'A-10','AVAILABLE',1,TRUE),(2,1,'A-11','AVAILABLE',1,TRUE),(2,1,'A-12','AVAILABLE',1,TRUE),
(2,1,'A-13','AVAILABLE',2,TRUE),(2,1,'A-14','AVAILABLE',2,TRUE),(2,1,'A-15','AVAILABLE',2,TRUE),
(2,1,'A-16','AVAILABLE',2,TRUE),(2,1,'A-17','OCCUPIED',2,TRUE),(2,1,'A-18','AVAILABLE',2,TRUE),
(2,1,'A-19','AVAILABLE',2,TRUE),(2,1,'A-20','AVAILABLE',2,TRUE),(2,1,'A-21','AVAILABLE',2,TRUE),
(2,1,'A-22','AVAILABLE',2,TRUE),(2,1,'A-23','AVAILABLE',2,TRUE),(2,1,'A-24','AVAILABLE',2,TRUE),
(2,1,'A-25','AVAILABLE',3,TRUE),(2,1,'A-26','AVAILABLE',3,TRUE),(2,1,'A-27','RESERVED',3,TRUE),
(2,1,'A-28','AVAILABLE',3,TRUE),(2,1,'A-29','AVAILABLE',3,TRUE),(2,1,'A-30','AVAILABLE',3,TRUE),
(2,1,'A-31','AVAILABLE',3,TRUE),(2,1,'A-32','AVAILABLE',3,TRUE),(2,1,'A-33','AVAILABLE',3,TRUE),
(2,1,'A-34','AVAILABLE',4,TRUE),(2,1,'A-35','AVAILABLE',4,TRUE),(2,1,'A-36','AVAILABLE',4,TRUE),
(2,1,'A-37','MAINTENANCE',4,TRUE),(2,1,'A-38','AVAILABLE',4,TRUE),(2,1,'A-39','AVAILABLE',4,TRUE),
(2,1,'A-40','AVAILABLE',4,TRUE),(2,1,'A-41','AVAILABLE',4,TRUE),(2,1,'A-42','AVAILABLE',4,TRUE),
(2,1,'A-43','AVAILABLE',4,TRUE),(2,1,'A-44','AVAILABLE',4,TRUE),(2,1,'A-45','AVAILABLE',4,TRUE),
(2,1,'A-46','AVAILABLE',4,TRUE),(2,1,'A-47','AVAILABLE',4,TRUE),(2,1,'A-48','AVAILABLE',4,TRUE),
(2,1,'A-49','AVAILABLE',4,TRUE),(2,1,'A-50','AVAILABLE',4,TRUE),
(2,3,'C-01','AVAILABLE',1,TRUE),(2,3,'C-02','AVAILABLE',1,TRUE),(2,3,'C-03','OCCUPIED',1,TRUE),
(2,3,'C-04','AVAILABLE',1,TRUE),(2,3,'C-05','AVAILABLE',1,TRUE),(2,3,'C-06','AVAILABLE',1,TRUE),
(2,3,'C-07','AVAILABLE',2,TRUE),(2,3,'C-08','AVAILABLE',2,TRUE),(2,3,'C-09','AVAILABLE',2,TRUE),
(2,3,'C-10','AVAILABLE',2,TRUE),(2,3,'C-11','AVAILABLE',2,TRUE),(2,3,'C-12','AVAILABLE',2,TRUE),
(2,3,'C-13','AVAILABLE',3,TRUE),(2,3,'C-14','AVAILABLE',3,TRUE),(2,3,'C-15','AVAILABLE',3,TRUE),
(2,3,'C-16','AVAILABLE',3,TRUE),(2,3,'C-17','AVAILABLE',3,TRUE),(2,3,'C-18','AVAILABLE',3,TRUE),
(2,3,'C-19','AVAILABLE',3,TRUE),(2,3,'C-20','AVAILABLE',3,TRUE),
(2,2,'M-01','AVAILABLE',1,TRUE),(2,2,'M-02','AVAILABLE',1,TRUE),(2,2,'M-03','AVAILABLE',1,TRUE),
(2,2,'M-04','OCCUPIED',1,TRUE),(2,2,'M-05','AVAILABLE',1,TRUE),(2,2,'M-06','AVAILABLE',1,TRUE),
(2,2,'M-07','AVAILABLE',1,TRUE),(2,2,'M-08','AVAILABLE',1,TRUE),(2,2,'M-09','AVAILABLE',1,TRUE),
(2,2,'M-10','AVAILABLE',1,TRUE);

-- ----- Estacionamiento 3: Surco (100 espacios) -----
-- 60 automóviles, 25 camionetas, 10 motos, 5 bicicletas
INSERT INTO parking_spaces (parking_lot_id, vehicle_type_id, space_number, status, floor_level, is_covered) VALUES
(3,1,'A-01','AVAILABLE',1,TRUE),(3,1,'A-02','AVAILABLE',1,TRUE),(3,1,'A-03','AVAILABLE',1,TRUE),
(3,1,'A-04','OCCUPIED',1,TRUE),(3,1,'A-05','AVAILABLE',1,TRUE),(3,1,'A-06','RESERVED',1,TRUE),
(3,1,'A-07','AVAILABLE',1,TRUE),(3,1,'A-08','AVAILABLE',1,TRUE),(3,1,'A-09','AVAILABLE',1,TRUE),
(3,1,'A-10','AVAILABLE',1,TRUE),(3,1,'A-11','AVAILABLE',1,TRUE),(3,1,'A-12','AVAILABLE',1,TRUE),
(3,1,'A-13','AVAILABLE',2,TRUE),(3,1,'A-14','AVAILABLE',2,TRUE),(3,1,'A-15','AVAILABLE',2,TRUE),
(3,1,'A-16','AVAILABLE',2,TRUE),(3,1,'A-17','AVAILABLE',2,TRUE),(3,1,'A-18','OCCUPIED',2,TRUE),
(3,1,'A-19','AVAILABLE',2,TRUE),(3,1,'A-20','AVAILABLE',2,TRUE),(3,1,'A-21','AVAILABLE',2,TRUE),
(3,1,'A-22','AVAILABLE',2,TRUE),(3,1,'A-23','AVAILABLE',2,TRUE),(3,1,'A-24','AVAILABLE',2,TRUE),
(3,1,'A-25','AVAILABLE',3,TRUE),(3,1,'A-26','AVAILABLE',3,TRUE),(3,1,'A-27','AVAILABLE',3,TRUE),
(3,1,'A-28','AVAILABLE',3,TRUE),(3,1,'A-29','RESERVED',3,TRUE),(3,1,'A-30','AVAILABLE',3,TRUE),
(3,1,'A-31','AVAILABLE',3,TRUE),(3,1,'A-32','AVAILABLE',3,TRUE),(3,1,'A-33','AVAILABLE',3,TRUE),
(3,1,'A-34','AVAILABLE',3,TRUE),(3,1,'A-35','AVAILABLE',3,TRUE),(3,1,'A-36','AVAILABLE',3,TRUE),
(3,1,'A-37','MAINTENANCE',4,TRUE),(3,1,'A-38','AVAILABLE',4,TRUE),(3,1,'A-39','AVAILABLE',4,TRUE),
(3,1,'A-40','AVAILABLE',4,TRUE),(3,1,'A-41','AVAILABLE',4,TRUE),(3,1,'A-42','AVAILABLE',4,TRUE),
(3,1,'A-43','AVAILABLE',4,TRUE),(3,1,'A-44','AVAILABLE',4,TRUE),(3,1,'A-45','AVAILABLE',4,TRUE),
(3,1,'A-46','AVAILABLE',4,TRUE),(3,1,'A-47','AVAILABLE',4,TRUE),(3,1,'A-48','AVAILABLE',4,TRUE),
(3,1,'A-49','AVAILABLE',4,TRUE),(3,1,'A-50','AVAILABLE',4,TRUE),(3,1,'A-51','AVAILABLE',5,TRUE),
(3,1,'A-52','AVAILABLE',5,TRUE),(3,1,'A-53','AVAILABLE',5,TRUE),(3,1,'A-54','AVAILABLE',5,TRUE),
(3,1,'A-55','AVAILABLE',5,TRUE),(3,1,'A-56','AVAILABLE',5,TRUE),(3,1,'A-57','AVAILABLE',5,TRUE),
(3,1,'A-58','AVAILABLE',5,TRUE),(3,1,'A-59','AVAILABLE',5,TRUE),(3,1,'A-60','AVAILABLE',5,TRUE),
(3,3,'C-01','AVAILABLE',1,TRUE),(3,3,'C-02','AVAILABLE',1,TRUE),(3,3,'C-03','OCCUPIED',1,TRUE),
(3,3,'C-04','AVAILABLE',1,TRUE),(3,3,'C-05','AVAILABLE',1,TRUE),(3,3,'C-06','AVAILABLE',1,TRUE),
(3,3,'C-07','AVAILABLE',2,TRUE),(3,3,'C-08','AVAILABLE',2,TRUE),(3,3,'C-09','AVAILABLE',2,TRUE),
(3,3,'C-10','AVAILABLE',2,TRUE),(3,3,'C-11','AVAILABLE',2,TRUE),(3,3,'C-12','AVAILABLE',2,TRUE),
(3,3,'C-13','AVAILABLE',3,TRUE),(3,3,'C-14','AVAILABLE',3,TRUE),(3,3,'C-15','AVAILABLE',3,TRUE),
(3,3,'C-16','AVAILABLE',3,TRUE),(3,3,'C-17','AVAILABLE',3,TRUE),(3,3,'C-18','AVAILABLE',3,TRUE),
(3,3,'C-19','AVAILABLE',3,TRUE),(3,3,'C-20','AVAILABLE',3,TRUE),(3,3,'C-21','AVAILABLE',4,TRUE),
(3,3,'C-22','AVAILABLE',4,TRUE),(3,3,'C-23','RESERVED',4,TRUE),(3,3,'C-24','AVAILABLE',4,TRUE),
(3,3,'C-25','AVAILABLE',4,TRUE),
(3,2,'M-01','AVAILABLE',1,FALSE),(3,2,'M-02','AVAILABLE',1,FALSE),(3,2,'M-03','AVAILABLE',1,FALSE),
(3,2,'M-04','OCCUPIED',1,FALSE),(3,2,'M-05','AVAILABLE',1,FALSE),(3,2,'M-06','AVAILABLE',1,FALSE),
(3,2,'M-07','AVAILABLE',1,FALSE),(3,2,'M-08','AVAILABLE',1,FALSE),(3,2,'M-09','AVAILABLE',1,FALSE),
(3,2,'M-10','AVAILABLE',1,FALSE),
(3,5,'B-01','AVAILABLE',1,FALSE),(3,5,'B-02','AVAILABLE',1,FALSE),(3,5,'B-03','AVAILABLE',1,FALSE),
(3,5,'B-04','AVAILABLE',1,FALSE),(3,5,'B-05','AVAILABLE',1,FALSE);

-- ----- Estacionamiento 4: Barranco (35 espacios) -----
-- 20 automóviles, 8 motos, 7 bicicletas
INSERT INTO parking_spaces (parking_lot_id, vehicle_type_id, space_number, status, floor_level, is_covered) VALUES
(4,1,'A-01','AVAILABLE',1,FALSE),(4,1,'A-02','OCCUPIED',1,FALSE),(4,1,'A-03','AVAILABLE',1,FALSE),
(4,1,'A-04','AVAILABLE',1,FALSE),(4,1,'A-05','AVAILABLE',1,FALSE),(4,1,'A-06','RESERVED',1,FALSE),
(4,1,'A-07','AVAILABLE',1,FALSE),(4,1,'A-08','AVAILABLE',1,FALSE),(4,1,'A-09','AVAILABLE',1,FALSE),
(4,1,'A-10','AVAILABLE',1,FALSE),(4,1,'A-11','AVAILABLE',1,FALSE),(4,1,'A-12','MAINTENANCE',1,FALSE),
(4,1,'A-13','AVAILABLE',1,FALSE),(4,1,'A-14','AVAILABLE',1,FALSE),(4,1,'A-15','AVAILABLE',1,FALSE),
(4,1,'A-16','AVAILABLE',1,FALSE),(4,1,'A-17','AVAILABLE',1,FALSE),(4,1,'A-18','AVAILABLE',1,FALSE),
(4,1,'A-19','AVAILABLE',1,FALSE),(4,1,'A-20','AVAILABLE',1,FALSE),
(4,2,'M-01','AVAILABLE',1,FALSE),(4,2,'M-02','AVAILABLE',1,FALSE),(4,2,'M-03','OCCUPIED',1,FALSE),
(4,2,'M-04','AVAILABLE',1,FALSE),(4,2,'M-05','AVAILABLE',1,FALSE),(4,2,'M-06','AVAILABLE',1,FALSE),
(4,2,'M-07','AVAILABLE',1,FALSE),(4,2,'M-08','AVAILABLE',1,FALSE),
(4,5,'B-01','AVAILABLE',1,FALSE),(4,5,'B-02','AVAILABLE',1,FALSE),(4,5,'B-03','AVAILABLE',1,FALSE),
(4,5,'B-04','AVAILABLE',1,FALSE),(4,5,'B-05','AVAILABLE',1,FALSE),(4,5,'B-06','AVAILABLE',1,FALSE),
(4,5,'B-07','AVAILABLE',1,FALSE);

-- ----- Estacionamiento 5: Lince (45 espacios) -----
-- 30 automóviles, 10 camionetas, 5 motos
INSERT INTO parking_spaces (parking_lot_id, vehicle_type_id, space_number, status, floor_level, is_covered) VALUES
(5,1,'A-01','AVAILABLE',1,FALSE),(5,1,'A-02','AVAILABLE',1,FALSE),(5,1,'A-03','OCCUPIED',1,FALSE),
(5,1,'A-04','AVAILABLE',1,FALSE),(5,1,'A-05','AVAILABLE',1,FALSE),(5,1,'A-06','AVAILABLE',1,FALSE),
(5,1,'A-07','RESERVED',1,FALSE),(5,1,'A-08','AVAILABLE',1,FALSE),(5,1,'A-09','AVAILABLE',1,FALSE),
(5,1,'A-10','AVAILABLE',1,FALSE),(5,1,'A-11','AVAILABLE',1,FALSE),(5,1,'A-12','AVAILABLE',1,FALSE),
(5,1,'A-13','AVAILABLE',2,FALSE),(5,1,'A-14','AVAILABLE',2,FALSE),(5,1,'A-15','AVAILABLE',2,FALSE),
(5,1,'A-16','AVAILABLE',2,FALSE),(5,1,'A-17','AVAILABLE',2,FALSE),(5,1,'A-18','OCCUPIED',2,FALSE),
(5,1,'A-19','AVAILABLE',2,FALSE),(5,1,'A-20','AVAILABLE',2,FALSE),(5,1,'A-21','AVAILABLE',2,FALSE),
(5,1,'A-22','AVAILABLE',2,FALSE),(5,1,'A-23','AVAILABLE',2,FALSE),(5,1,'A-24','AVAILABLE',2,FALSE),
(5,1,'A-25','MAINTENANCE',2,FALSE),(5,1,'A-26','AVAILABLE',2,FALSE),(5,1,'A-27','AVAILABLE',2,FALSE),
(5,1,'A-28','AVAILABLE',2,FALSE),(5,1,'A-29','AVAILABLE',2,FALSE),(5,1,'A-30','AVAILABLE',2,FALSE),
(5,3,'C-01','AVAILABLE',1,FALSE),(5,3,'C-02','AVAILABLE',1,FALSE),(5,3,'C-03','OCCUPIED',1,FALSE),
(5,3,'C-04','AVAILABLE',1,FALSE),(5,3,'C-05','AVAILABLE',1,FALSE),(5,3,'C-06','AVAILABLE',1,FALSE),
(5,3,'C-07','AVAILABLE',1,FALSE),(5,3,'C-08','AVAILABLE',1,FALSE),(5,3,'C-09','AVAILABLE',1,FALSE),
(5,3,'C-10','AVAILABLE',1,FALSE),
(5,2,'M-01','AVAILABLE',1,FALSE),(5,2,'M-02','AVAILABLE',1,FALSE),(5,2,'M-03','AVAILABLE',1,FALSE),
(5,2,'M-04','AVAILABLE',1,FALSE),(5,2,'M-05','OCCUPIED',1,FALSE);

-- ----- Estacionamiento 6: San Borja (90 espacios) -----
-- 55 automóviles, 25 camionetas, 10 motos
INSERT INTO parking_spaces (parking_lot_id, vehicle_type_id, space_number, status, floor_level, is_covered) VALUES
(6,1,'A-01','AVAILABLE',1,TRUE),(6,1,'A-02','AVAILABLE',1,TRUE),(6,1,'A-03','AVAILABLE',1,TRUE),
(6,1,'A-04','RESERVED',1,TRUE),(6,1,'A-05','AVAILABLE',1,TRUE),(6,1,'A-06','OCCUPIED',1,TRUE),
(6,1,'A-07','AVAILABLE',1,TRUE),(6,1,'A-08','AVAILABLE',1,TRUE),(6,1,'A-09','AVAILABLE',1,TRUE),
(6,1,'A-10','AVAILABLE',1,TRUE),(6,1,'A-11','AVAILABLE',1,TRUE),(6,1,'A-12','AVAILABLE',1,TRUE),
(6,1,'A-13','AVAILABLE',2,TRUE),(6,1,'A-14','AVAILABLE',2,TRUE),(6,1,'A-15','OCCUPIED',2,TRUE),
(6,1,'A-16','AVAILABLE',2,TRUE),(6,1,'A-17','AVAILABLE',2,TRUE),(6,1,'A-18','AVAILABLE',2,TRUE),
(6,1,'A-19','AVAILABLE',2,TRUE),(6,1,'A-20','AVAILABLE',2,TRUE),(6,1,'A-21','RESERVED',2,TRUE),
(6,1,'A-22','AVAILABLE',2,TRUE),(6,1,'A-23','AVAILABLE',2,TRUE),(6,1,'A-24','AVAILABLE',2,TRUE),
(6,1,'A-25','AVAILABLE',3,TRUE),(6,1,'A-26','AVAILABLE',3,TRUE),(6,1,'A-27','AVAILABLE',3,TRUE),
(6,1,'A-28','AVAILABLE',3,TRUE),(6,1,'A-29','AVAILABLE',3,TRUE),(6,1,'A-30','MAINTENANCE',3,TRUE),
(6,1,'A-31','AVAILABLE',3,TRUE),(6,1,'A-32','AVAILABLE',3,TRUE),(6,1,'A-33','AVAILABLE',3,TRUE),
(6,1,'A-34','AVAILABLE',3,TRUE),(6,1,'A-35','AVAILABLE',3,TRUE),(6,1,'A-36','AVAILABLE',3,TRUE),
(6,1,'A-37','AVAILABLE',4,TRUE),(6,1,'A-38','AVAILABLE',4,TRUE),(6,1,'A-39','AVAILABLE',4,TRUE),
(6,1,'A-40','AVAILABLE',4,TRUE),(6,1,'A-41','AVAILABLE',4,TRUE),(6,1,'A-42','AVAILABLE',4,TRUE),
(6,1,'A-43','AVAILABLE',4,TRUE),(6,1,'A-44','AVAILABLE',4,TRUE),(6,1,'A-45','AVAILABLE',4,TRUE),
(6,1,'A-46','AVAILABLE',4,TRUE),(6,1,'A-47','AVAILABLE',4,TRUE),(6,1,'A-48','AVAILABLE',4,TRUE),
(6,1,'A-49','AVAILABLE',5,TRUE),(6,1,'A-50','AVAILABLE',5,TRUE),(6,1,'A-51','AVAILABLE',5,TRUE),
(6,1,'A-52','AVAILABLE',5,TRUE),(6,1,'A-53','AVAILABLE',5,TRUE),(6,1,'A-54','AVAILABLE',5,TRUE),
(6,1,'A-55','AVAILABLE',5,TRUE),
(6,3,'C-01','AVAILABLE',1,TRUE),(6,3,'C-02','AVAILABLE',1,TRUE),(6,3,'C-03','OCCUPIED',1,TRUE),
(6,3,'C-04','AVAILABLE',1,TRUE),(6,3,'C-05','AVAILABLE',1,TRUE),(6,3,'C-06','AVAILABLE',1,TRUE),
(6,3,'C-07','RESERVED',1,TRUE),(6,3,'C-08','AVAILABLE',1,TRUE),(6,3,'C-09','AVAILABLE',1,TRUE),
(6,3,'C-10','AVAILABLE',2,TRUE),(6,3,'C-11','AVAILABLE',2,TRUE),(6,3,'C-12','AVAILABLE',2,TRUE),
(6,3,'C-13','AVAILABLE',2,TRUE),(6,3,'C-14','AVAILABLE',2,TRUE),(6,3,'C-15','AVAILABLE',2,TRUE),
(6,3,'C-16','AVAILABLE',2,TRUE),(6,3,'C-17','AVAILABLE',2,TRUE),(6,3,'C-18','AVAILABLE',2,TRUE),
(6,3,'C-19','AVAILABLE',3,TRUE),(6,3,'C-20','AVAILABLE',3,TRUE),(6,3,'C-21','AVAILABLE',3,TRUE),
(6,3,'C-22','AVAILABLE',3,TRUE),(6,3,'C-23','AVAILABLE',3,TRUE),(6,3,'C-24','AVAILABLE',3,TRUE),
(6,3,'C-25','AVAILABLE',3,TRUE),
(6,2,'M-01','AVAILABLE',1,TRUE),(6,2,'M-02','AVAILABLE',1,TRUE),(6,2,'M-03','AVAILABLE',1,TRUE),
(6,2,'M-04','OCCUPIED',1,TRUE),(6,2,'M-05','AVAILABLE',1,TRUE),(6,2,'M-06','AVAILABLE',1,TRUE),
(6,2,'M-07','AVAILABLE',1,TRUE),(6,2,'M-08','AVAILABLE',1,TRUE),(6,2,'M-09','AVAILABLE',1,TRUE),
(6,2,'M-10','AVAILABLE',1,TRUE);

-- =============================================================================
-- SCHEDULES (horarios lunes-domingo para cada estacionamiento)
-- =============================================================================
INSERT INTO schedules (parking_lot_id, day_of_week, open_time, close_time, is_open) VALUES
-- Miraflores: L-V 07:00-23:00, S 07:00-24:00, D 08:00-22:00
(1,'MONDAY',    '07:00:00','23:00:00',TRUE),
(1,'TUESDAY',   '07:00:00','23:00:00',TRUE),
(1,'WEDNESDAY', '07:00:00','23:00:00',TRUE),
(1,'THURSDAY',  '07:00:00','23:00:00',TRUE),
(1,'FRIDAY',    '07:00:00','23:59:00',TRUE),
(1,'SATURDAY',  '07:00:00','23:59:00',TRUE),
(1,'SUNDAY',    '08:00:00','22:00:00',TRUE),
-- San Isidro: L-V 06:00-22:00, S 07:00-20:00, D cerrado
(2,'MONDAY',    '06:00:00','22:00:00',TRUE),
(2,'TUESDAY',   '06:00:00','22:00:00',TRUE),
(2,'WEDNESDAY', '06:00:00','22:00:00',TRUE),
(2,'THURSDAY',  '06:00:00','22:00:00',TRUE),
(2,'FRIDAY',    '06:00:00','22:00:00',TRUE),
(2,'SATURDAY',  '07:00:00','20:00:00',TRUE),
(2,'SUNDAY',    NULL,       NULL,      FALSE),
-- Surco: L-D 00:00-23:59 (24h)
(3,'MONDAY',    '00:00:00','23:59:00',TRUE),
(3,'TUESDAY',   '00:00:00','23:59:00',TRUE),
(3,'WEDNESDAY', '00:00:00','23:59:00',TRUE),
(3,'THURSDAY',  '00:00:00','23:59:00',TRUE),
(3,'FRIDAY',    '00:00:00','23:59:00',TRUE),
(3,'SATURDAY',  '00:00:00','23:59:00',TRUE),
(3,'SUNDAY',    '00:00:00','23:59:00',TRUE),
-- Barranco: L-S 08:00-22:00, D 09:00-20:00
(4,'MONDAY',    '08:00:00','22:00:00',TRUE),
(4,'TUESDAY',   '08:00:00','22:00:00',TRUE),
(4,'WEDNESDAY', '08:00:00','22:00:00',TRUE),
(4,'THURSDAY',  '08:00:00','22:00:00',TRUE),
(4,'FRIDAY',    '08:00:00','22:00:00',TRUE),
(4,'SATURDAY',  '08:00:00','22:00:00',TRUE),
(4,'SUNDAY',    '09:00:00','20:00:00',TRUE),
-- Lince: L-V 07:00-21:00, S 08:00-18:00, D cerrado
(5,'MONDAY',    '07:00:00','21:00:00',TRUE),
(5,'TUESDAY',   '07:00:00','21:00:00',TRUE),
(5,'WEDNESDAY', '07:00:00','21:00:00',TRUE),
(5,'THURSDAY',  '07:00:00','21:00:00',TRUE),
(5,'FRIDAY',    '07:00:00','21:00:00',TRUE),
(5,'SATURDAY',  '08:00:00','18:00:00',TRUE),
(5,'SUNDAY',    NULL,       NULL,      FALSE),
-- San Borja: L-D 06:00-23:59
(6,'MONDAY',    '06:00:00','23:59:00',TRUE),
(6,'TUESDAY',   '06:00:00','23:59:00',TRUE),
(6,'WEDNESDAY', '06:00:00','23:59:00',TRUE),
(6,'THURSDAY',  '06:00:00','23:59:00',TRUE),
(6,'FRIDAY',    '06:00:00','23:59:00',TRUE),
(6,'SATURDAY',  '06:00:00','23:59:00',TRUE),
(6,'SUNDAY',    '07:00:00','22:00:00',TRUE)
ON DUPLICATE KEY UPDATE open_time=VALUES(open_time), close_time=VALUES(close_time), is_open=VALUES(is_open);

-- =============================================================================
-- TARIFFS (tarifas en soles peruanos)
-- =============================================================================
INSERT INTO tariffs (parking_lot_id, vehicle_type_id, tariff_type, price, minimum_minutes) VALUES
-- ---- Miraflores ----
(1, 1, 'HOURLY',  5.00, 30),(1, 1, 'DAILY',  40.00, 60),(1, 1, 'MONTHLY', 300.00, 60),
(1, 2, 'HOURLY',  2.00, 30),(1, 2, 'DAILY',  15.00, 30),(1, 2, 'MONTHLY', 120.00, 30),
(1, 3, 'HOURLY',  7.00, 30),(1, 3, 'DAILY',  55.00, 60),(1, 3, 'MONTHLY', 420.00, 60),
-- ---- San Isidro ----
(2, 1, 'HOURLY',  8.00, 30),(2, 1, 'DAILY',  60.00, 60),(2, 1, 'MONTHLY', 450.00, 60),
(2, 2, 'HOURLY',  3.00, 30),(2, 2, 'DAILY',  20.00, 30),(2, 2, 'MONTHLY', 150.00, 30),
(2, 3, 'HOURLY', 10.00, 30),(2, 3, 'DAILY',  80.00, 60),(2, 3, 'MONTHLY', 600.00, 60),
-- ---- Surco ----
(3, 1, 'HOURLY',  6.00, 30),(3, 1, 'DAILY',  45.00, 60),(3, 1, 'MONTHLY', 350.00, 60),
(3, 2, 'HOURLY',  2.50, 30),(3, 2, 'DAILY',  18.00, 30),(3, 2, 'MONTHLY', 130.00, 30),
(3, 3, 'HOURLY',  8.00, 30),(3, 3, 'DAILY',  60.00, 60),(3, 3, 'MONTHLY', 480.00, 60),
-- ---- Barranco ----
(4, 1, 'HOURLY',  4.00, 30),(4, 1, 'DAILY',  30.00, 60),(4, 1, 'MONTHLY', 220.00, 60),
(4, 2, 'HOURLY',  1.50, 30),(4, 2, 'DAILY',  12.00, 30),(4, 2, 'MONTHLY',  90.00, 30),
-- ---- Lince ----
(5, 1, 'HOURLY',  3.00, 30),(5, 1, 'DAILY',  22.00, 60),(5, 1, 'MONTHLY', 180.00, 60),
(5, 2, 'HOURLY',  1.50, 30),(5, 2, 'DAILY',  10.00, 30),(5, 2, 'MONTHLY',  80.00, 30),
(5, 3, 'HOURLY',  5.00, 30),(5, 3, 'DAILY',  35.00, 60),(5, 3, 'MONTHLY', 280.00, 60),
-- ---- San Borja ----
(6, 1, 'HOURLY',  7.00, 30),(6, 1, 'DAILY',  50.00, 60),(6, 1, 'MONTHLY', 400.00, 60),
(6, 2, 'HOURLY',  2.50, 30),(6, 2, 'DAILY',  18.00, 30),(6, 2, 'MONTHLY', 140.00, 30),
(6, 3, 'HOURLY',  9.00, 30),(6, 3, 'DAILY',  70.00, 60),(6, 3, 'MONTHLY', 550.00, 60)
ON DUPLICATE KEY UPDATE price=VALUES(price);

-- =============================================================================
-- OPERATOR PARKING ASSIGNMENTS
-- =============================================================================
INSERT INTO operator_parking_assignments (operator_id, parking_lot_id, assigned_by, is_active) VALUES
(2, 3, 1, TRUE),
(2, 4, 1, TRUE)
ON DUPLICATE KEY UPDATE is_active=VALUES(is_active);

-- =============================================================================
-- RESERVATIONS (muestra de reservas con distintos estados)
-- =============================================================================
-- Nota: Los IDs de parking_space se refieren a los IDs auto-generados.
-- Usamos subconsultas para obtener el space_id correcto por estacionamiento y número.

INSERT INTO reservations (user_id, parking_space_id, vehicle_id, start_time, end_time, actual_start, actual_end, status, total_price)
SELECT 3, ps.id, 1,
    '2026-09-15 09:00:00', '2026-09-15 11:00:00',
    '2026-09-15 09:05:00', '2026-09-15 11:10:00',
    'COMPLETED', 10.00
FROM parking_spaces ps
WHERE ps.parking_lot_id = 1 AND ps.space_number = 'A-01' LIMIT 1;

INSERT INTO reservations (user_id, parking_space_id, vehicle_id, start_time, end_time, actual_start, actual_end, status, total_price)
SELECT 4, ps.id, 3,
    '2026-09-16 14:00:00', '2026-09-16 17:00:00',
    '2026-09-16 14:02:00', '2026-09-16 17:15:00',
    'COMPLETED', 24.00
FROM parking_spaces ps
WHERE ps.parking_lot_id = 2 AND ps.space_number = 'A-04' LIMIT 1;

INSERT INTO reservations (user_id, parking_space_id, vehicle_id, start_time, end_time, status, total_price)
SELECT 5, ps.id, 5,
    '2026-09-19 10:00:00', '2026-09-19 12:00:00',
    'CONFIRMED', 12.00
FROM parking_spaces ps
WHERE ps.parking_lot_id = 3 AND ps.space_number = 'A-05' LIMIT 1;

INSERT INTO reservations (user_id, parking_space_id, vehicle_id, start_time, end_time, status, total_price)
SELECT 3, ps.id, 2,
    '2026-09-20 08:00:00', '2026-09-20 10:00:00',
    'PENDING', 4.00
FROM parking_spaces ps
WHERE ps.parking_lot_id = 4 AND ps.space_number = 'M-01' LIMIT 1;

INSERT INTO reservations (user_id, parking_space_id, vehicle_id, start_time, end_time, status, total_price, cancellation_reason)
SELECT 6, ps.id, 7,
    '2026-09-17 11:00:00', '2026-09-17 14:00:00',
    'CANCELLED', 9.00, 'Cambio de planes del usuario'
FROM parking_spaces ps
WHERE ps.parking_lot_id = 5 AND ps.space_number = 'A-01' LIMIT 1;

INSERT INTO reservations (user_id, parking_space_id, vehicle_id, start_time, end_time, actual_start, status, total_price)
SELECT 4, ps.id, 4,
    '2026-09-18 09:00:00', '2026-09-18 12:00:00',
    '2026-09-18 09:03:00',
    'ACTIVE', 27.00
FROM parking_spaces ps
WHERE ps.parking_lot_id = 6 AND ps.space_number = 'C-01' LIMIT 1;

-- =============================================================================
-- RESERVATION STATUS HISTORY
-- =============================================================================
INSERT INTO reservation_status_history (reservation_id, previous_status, new_status, changed_by, notes) VALUES
(1, NULL,        'PENDING',   3, 'Reserva creada'),
(1, 'PENDING',   'CONFIRMED', 1, 'Confirmada automáticamente'),
(1, 'CONFIRMED', 'ACTIVE',    3, 'Cliente ingresó al estacionamiento'),
(1, 'ACTIVE',    'COMPLETED', 3, 'Cliente salió del estacionamiento'),
(2, NULL,        'PENDING',   4, 'Reserva creada'),
(2, 'PENDING',   'CONFIRMED', 1, 'Confirmada automáticamente'),
(2, 'CONFIRMED', 'ACTIVE',    4, 'Cliente ingresó'),
(2, 'ACTIVE',    'COMPLETED', 4, 'Completada'),
(3, NULL,        'PENDING',   5, 'Reserva creada'),
(3, 'PENDING',   'CONFIRMED', 1, 'Confirmada'),
(4, NULL,        'PENDING',   3, 'Reserva creada'),
(5, NULL,        'PENDING',   6, 'Reserva creada'),
(5, 'PENDING',   'CANCELLED', 6, 'Cancelada por el cliente'),
(6, NULL,        'PENDING',   4, 'Reserva creada'),
(6, 'PENDING',   'CONFIRMED', 1, 'Confirmada'),
(6, 'CONFIRMED', 'ACTIVE',    4, 'Cliente ingresó');

-- =============================================================================
-- PARKING RATINGS (para reservas completadas)
-- =============================================================================
INSERT INTO parking_ratings (reservation_id, user_id, parking_lot_id, rating, comment) VALUES
(1, 3, 1, 5, 'Excelente servicio. Muy limpio y bien organizado. El personal fue muy amable.'),
(2, 4, 2, 4, 'Muy buen estacionamiento. El servicio valet funcionó perfecto. Solo tardó un poco en la entrada.');

-- =============================================================================
-- AUDIT LOG (entradas iniciales)
-- =============================================================================
INSERT INTO audit_log (user_id, action, entity, entity_id, new_values, ip_address) VALUES
(1, 'CREATE', 'parking_lots', 1, '{"name":"Parkeo Miraflores Centro","district":"Miraflores"}', '127.0.0.1'),
(1, 'CREATE', 'parking_lots', 2, '{"name":"Parkeo San Isidro Financiero","district":"San Isidro"}',  '127.0.0.1'),
(2, 'CREATE', 'parking_lots', 3, '{"name":"Parkeo Surco Chacarilla","district":"Santiago de Surco"}', '127.0.0.1'),
(2, 'CREATE', 'parking_lots', 4, '{"name":"Parkeo Barranco Bohemio","district":"Barranco"}', '127.0.0.1'),
(1, 'CREATE', 'parking_lots', 5, '{"name":"Parkeo Lince Express","district":"Lince"}', '127.0.0.1'),
(1, 'CREATE', 'parking_lots', 6, '{"name":"Parkeo San Borja Premium","district":"San Borja"}', '127.0.0.1'),
(3, 'LOGIN',  'users',        3, '{"email":"cliente@parkeo.pe"}', '192.168.1.10'),
(4, 'LOGIN',  'users',        4, '{"email":"juan.perez@parkeo.pe"}', '192.168.1.11');

SET FOREIGN_KEY_CHECKS = 1;
