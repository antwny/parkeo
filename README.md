# PARKeo

> **Encuentra, reserva y gestiona tu estacionamiento — en tiempo real.**

PARKeo es una aplicación móvil Android con backend Spring Boot que conecta a conductores con estacionamientos disponibles en Lima, Perú. Permite consultar disponibilidad, reservar espacios, gestionar vehículos, mostrar un boarding pass con código QR y operar la cochera desde paneles para operadores y administradores.

---

## ¿Qué problema resuelve?

Encontrar estacionamiento en ciudades congestionadas es frustrante, ineficiente y costoso en tiempo. PARKeo digitaliza la operación de estacionamientos y entrega al conductor información en tiempo real sobre disponibilidad, precios y ubicación.

---

## Características principales

| Característica | Descripción |
|---|---|
| Mapa interactivo | Google Maps con marcadores de estacionamientos y ubicación actual |
| Disponibilidad en tiempo real | Espacios libres por estacionamiento y tipo de vehículo |
| Reservas | Creación con fecha, hora, duración y cálculo de precio en servidor |
| Boarding pass QR | Código QR generado en la app a partir del código de confirmación |
| Recordatorios | Notificaciones locales de inicio y fin de reserva con WorkManager |
| Gestión de vehículos | Registro, edición y eliminación de vehículos del cliente |
| Panel de operador | Control de espacios y ciclo de reservas (check-in / check-out) |
| Panel de administrador | Métricas globales, cocheras, asignación de operador y usuarios |
| Seguridad | JWT + BCrypt + RBAC con roles `ROLE_USER`, `ROLE_OPERATOR`, `ROLE_ADMIN` |
| Dark/Light Mode | Tema claro y oscuro completo |
| Geolocalización | Permisos en tiempo de ejecución y búsqueda por proximidad |

---

## Arquitectura del sistema

```
┌─────────────────────────────────────────────┐
│            Android App (Kotlin)             │
│  Compose UI → ViewModel → Repository →      │
│  Retrofit → REST API                        │
└────────────────────┬────────────────────────┘
                     │ HTTP/REST + JWT
┌────────────────────▼────────────────────────┐
│         Spring Boot Backend (Java 21)       │
│  Controller → Service → Repository →        │
│  JPA Entity → MySQL                         │
└────────────────────┬────────────────────────┘
                     │ JDBC / JPA
┌────────────────────▼────────────────────────┐
│              MySQL 8.0                      │
│  parkeo_db — modelo relacional normalizado  │
└─────────────────────────────────────────────┘
```

---

## Stack tecnológico

### Android
- **Kotlin** + Jetpack Compose + Material 3
- Navigation Compose
- ViewModel + StateFlow + Coroutines
- Retrofit + OkHttp + Kotlin Serialization
- Google Maps SDK / Maps Compose
- Android Location Services
- DataStore (sesión local)
- WorkManager (recordatorios de reservas)
- Inyección de dependencias manual mediante `AppContainer`

### Backend
- **Java 21** + Spring Boot 3.3.x
- Spring Security + JWT (JJWT 0.12)
- BCrypt (factor 12)
- Spring Data JPA + Hibernate
- MySQL Driver
- Bean Validation
- SpringDoc OpenAPI (Swagger)
- Maven
- JUnit 5 + Mockito

### Base de datos
- **MySQL 8.0**
- Modelo relacional normalizado
- Índices para geolocalización, disponibilidad y reservas
- `schema.sql` y `data.sql` alineados con las entidades JPA

---

## Estructura del proyecto

```
Parkeo/
├── android/           # Aplicación Android (Kotlin/Compose)
├── backend/           # API REST (Spring Boot)
├── database/
│   ├── schema.sql     # DDL completo
│   ├── data.sql       # Datos de prueba
│   └── README.md
├── docs/              # Documentación técnica
│   ├── arquitectura.md
│   ├── base-datos.md
│   ├── api.md
│   ├── seguridad.md
│   ├── flujos.md
│   └── instalacion.md
├── scripts/
│   ├── setup.sh
│   ├── start-backend.sh
│   ├── reset-db.sh
│   ├── demo_flow.sh
│   └── levantar-celular.sh
├── .gitignore
├── docker-compose.yml
└── README.md
```

---

## Requisitos previos

| Herramienta | Versión mínima | Verificación |
|---|---|---|
| Java (Temurin) | 21 | `java -version` |
| Maven | 3.9+ | `mvn --version` |
| MySQL | 8.0 | `mysql --version` |
| Android Studio | Ladybug+ | IDE |
| Android SDK | API 35 | Android Studio |
| Git | 2.x | `git --version` |

La app Android tiene `minSdk 26`, `targetSdk 35` y `compileSdk 35`.

---

## Instalación y configuración

### 1. Clonar / ubicar el proyecto

```bash
cd ~/Documentos/parkeo
```

### 2. Configurar la base de datos

```bash
bash scripts/setup.sh
```

Esto crea `parkeo_db`, aplica `database/schema.sql` y carga `database/data.sql`.

Para reiniciar la base desde cero:

```bash
bash scripts/reset-db.sh
```

> Si el backend estaba corriendo durante el reset, reinícialo para que Hibernate vuelva a validar la conexión y el esquema.

**Credenciales de desarrollo local:**

```
Host: localhost
Puerto: 3306
Base de datos: parkeo_db
Usuario: antwny
```

> ⚠️ **Nunca uses estas credenciales en producción.**

### 3. Configurar variables de entorno del backend

```bash
export DB_URL="jdbc:mysql://localhost:3306/parkeo_db?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true"
export DB_USERNAME="antwny"
export DB_PASSWORD="TU_PASSWORD"
export JWT_SECRET="tu-clave-secreta-minimo-256-bits"
```

O usa el script:

```bash
bash scripts/start-backend.sh
```

### 4. Configurar Google Maps API Key

1. Obtén una API Key en [Google Cloud Console](https://console.cloud.google.com/)
2. Habilita **Maps SDK for Android**
3. Agrega la clave en `android/local.properties`:

```properties
MAPS_API_KEY=AIza...tu_clave_aqui
BACKEND_URL=http://10.0.2.2:8080/
```

> ⚠️ `local.properties` está en `.gitignore` y nunca se sube al repositorio.

---

## Ejecución

### Backend

```bash
cd ~/Documentos/parkeo
bash scripts/start-backend.sh
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

### Android

1. Abre Android Studio
2. File → Open → `~/Documentos/parkeo/android/`
3. Configura `local.properties` con `MAPS_API_KEY` y `BACKEND_URL`
4. Run → Run 'app'

---

## API REST — Endpoints principales

```
POST   /api/auth/register        Registro de cliente
POST   /api/auth/login           Inicio de sesión
POST   /api/auth/refresh         Renovar tokens
POST   /api/auth/logout          Cerrar sesión

GET    /api/users/me             Perfil del usuario
PUT    /api/users/me             Actualizar perfil
PATCH  /api/users/me/password    Cambiar contraseña

GET    /api/vehicles             Mis vehículos
POST   /api/vehicles             Agregar vehículo
PUT    /api/vehicles/{id}        Actualizar vehículo
DELETE /api/vehicles/{id}        Eliminar vehículo
GET    /api/vehicles/types       Catálogo de tipos

GET    /api/parking              Estacionamientos activos (paginado)
GET    /api/parking/{id}         Detalle del estacionamiento
GET    /api/parking/nearby       Cercanos por coordenadas
GET    /api/parking/search       Buscar por nombre/dirección
GET    /api/parking/{id}/schedules
GET    /api/parking/{id}/tariffs
GET    /api/parking/{id}/availability
POST   /api/v1/availability/check

GET    /api/reservations         Mis reservas
POST   /api/reservations         Crear reserva
GET    /api/reservations/{id}    Detalle de reserva
PATCH  /api/reservations/{id}/cancel

GET    /api/operator/parking-lots            [OPERADOR/ADMIN]
GET    /api/operator/parking-lots/{id}/spaces
PATCH  /api/operator/spaces/{id}/status
GET    /api/operator/reservations
PATCH  /api/operator/reservations/{id}/check-in
PATCH  /api/operator/reservations/{id}/check-out

GET    /api/admin/users                      [ADMIN]
PUT    /api/admin/users/{id}/status
GET    /api/admin/parking-lots
PUT    /api/admin/parking-lots/{id}/status
PUT    /api/admin/parking-lots/{id}/operator
DELETE /api/admin/parking-lots/{id}
GET    /api/admin/statistics
```

Documentación completa: `http://localhost:8080/swagger-ui.html`

---

## Seguridad

- **Autenticación**: JWT Bearer Token (24 h por defecto)
- **Refresh Token**: 7 días, almacenado en BD, revocable
- **Contraseñas**: BCrypt con factor 12
- **Roles**: `ROLE_USER`, `ROLE_OPERATOR`, `ROLE_ADMIN`
- **Autorización**: por endpoint con `@PreAuthorize` y reglas de Spring Security
- **Acceso horizontal**: validado en la capa de servicio
- **Secretos**: variables de entorno, nunca en código

---

## Cuentas de prueba

> ⚠️ **Estas credenciales son SOLO para desarrollo local. Cámbialas en producción.**

| Email | Password | Rol |
|---|---|---|
| admin@parkeo.pe | Password123! | ADMIN |
| operador@parkeo.pe | Password123! | OPERADOR |
| operador2@parkeo.pe | Password123! | OPERADOR |
| cliente@parkeo.pe | Password123! | CLIENTE |
| juan.perez@parkeo.pe | Password123! | CLIENTE |
| sofia.mendez@parkeo.pe | Password123! | CLIENTE |
| roberto.silva@parkeo.pe | Password123! | CLIENTE |

---

## Datos de prueba

- 6 estacionamientos en Lima (Miraflores, San Isidro, Surco, Barranco, Lince y San Borja).
- 410 espacios distribuidos entre los 6 estacionamientos.
- 42 horarios semanales (una fila por día y estacionamiento).
- 54 tarifas (por hora, diaria y mensual por tipo de vehículo).
- 6 reservas de ejemplo en distintos estados.
- 2 calificaciones y 8 registros de auditoría.
- `available_spaces` se calcula a partir del estado real de los espacios.

---

## Google Maps

La aplicación requiere una API Key de Google Maps SDK for Android.

Configura en `android/local.properties`:

```properties
MAPS_API_KEY=TU_API_KEY
BACKEND_URL=http://10.0.2.2:8080/
```

APIs requeridas:
- Maps SDK for Android

---

## Estado del proyecto

| Módulo | Estado |
|---|---|
| Base de datos (MySQL) | ✅ Completo |
| Backend (Spring Boot) | ✅ Completo |
| Seguridad (JWT + RBAC) | ✅ Completo |
| API REST | ✅ Completo |
| Swagger / OpenAPI | ✅ Completo |
| Android — Autenticación | ✅ Completo |
| Android — Mapa | ✅ Completo (requiere API Key) |
| Android — Estacionamientos | ✅ Completo |
| Android — Vehículos | ✅ Completo |
| Android — Reservas + QR | ✅ Completo |
| Android — Panel Operador | ✅ Completo |
| Android — Panel Administrador | ✅ Completo |
| Recordatorios locales | ✅ Completo |
| Dark/Light Mode | ✅ Completo |
| Datos de prueba | ✅ Completo |
| Pruebas backend | ✅ JUnit/Mockito |
| Documentación | ✅ Actualizada |

---

## Documentación adicional

- [Arquitectura](docs/arquitectura.md)
- [Base de datos](docs/base-datos.md)
- [API REST](docs/api.md)
- [Seguridad](docs/seguridad.md)
- [Flujos de usuario](docs/flujos.md)
- [Guía de instalación](docs/instalacion.md)

---

## Licencia

Proyecto privado — PARKeo © 2025. Todos los derechos reservados.
