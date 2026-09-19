# PARKeo

> **Encuentra, reserva y gestiona tu estacionamiento — en tiempo real.**

PARKeo es una aplicación móvil Android (con backend en Spring Boot) que conecta a conductores con estacionamientos disponibles en Lima, Perú. Permite consultar disponibilidad en tiempo real, hacer reservas, gestionar vehículos y navegar hasta el estacionamiento elegido — todo desde el smartphone.

---

## ¿Qué problema resuelve?

Encontrar estacionamiento en ciudades congestionadas es frustrante, ineficiente y costoso en tiempo. PARKeo digitaliza la operación de estacionamientos y empodera al conductor con información en tiempo real sobre disponibilidad, precios y ubicación.

---

## Características principales

| Característica | Descripción |
|---|---|
| 🗺️ Mapa interactivo | Mapa centrado en ubicación actual con marcadores de estacionamientos |
| 🅿️ Disponibilidad en tiempo real | Consulta espacios disponibles antes de ir |
| 📅 Reservas | Reserva un espacio con fecha, hora y duración |
| 🚗 Gestión de vehículos | Administra múltiples vehículos |
| 👤 Perfiles de usuario | Cliente, Operador, Administrador |
| 🔒 Seguridad | JWT + BCrypt + RBAC |
| 🌙 Dark/Light Mode | Soporte completo de tema oscuro y claro |
| 📍 Geolocalización | Permisos en tiempo de ejecución, manejo de errores |

---

## Arquitectura del sistema

```
┌─────────────────────────────────────────────┐
│            Android App (Kotlin)             │
│  Compose UI → ViewModel → UseCase →         │
│  Repository → Retrofit → REST API           │
└────────────────────┬────────────────────────┘
                     │ HTTPS / REST
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
- **Kotlin** + Jetpack Compose
- Material 3
- Navigation Compose
- ViewModel + StateFlow
- Retrofit + OkHttp
- Kotlin Serialization
- Google Maps SDK for Android
- Android Location Services
- DataStore (sesión local)
- Coroutines

### Backend
- **Java 21** + Spring Boot 3.3.x
- Spring Security + JWT (JJWT 0.12)
- BCrypt
- Spring Data JPA + Hibernate
- MySQL Driver
- Bean Validation
- SpringDoc OpenAPI (Swagger)
- Maven

### Base de datos
- **MySQL 8.0**
- Modelo relacional normalizado
- Índices para geolocalización y disponibilidad

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
│   ├── setup.sh       # Configuración inicial
│   ├── start-backend.sh
│   └── reset-db.sh
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
| Android SDK | API 34 | Android Studio |
| Git | 2.x | `git --version` |

---

## Instalación y configuración

### 1. Clonar / ubicar el proyecto

```bash
ls ~/Documentos/Parkeo/
```

### 2. Configurar base de datos

```bash
cd ~/Documentos/Parkeo
bash scripts/setup.sh
```

Esto creará la base de datos `parkeo_db`, aplicará el schema y cargará los datos de prueba.

**Credenciales de desarrollo (solo para desarrollo local):**

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
2. Habilita: **Maps SDK for Android** y **Geocoding API**
3. Agrega la clave a `android/local.properties`:

```properties
MAPS_API_KEY=AIza...tu_clave_aqui
```

> ⚠️ `local.properties` está en `.gitignore` y nunca se sube al repositorio.

---

## Ejecución

### Backend

```bash
cd ~/Documentos/Parkeo
bash scripts/start-backend.sh
```

Backend disponible en: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

### Android

1. Abre Android Studio
2. File → Open → `~/Documentos/Parkeo/android/`
3. Configura `local.properties` con tu Google Maps API Key
4. Run → Run 'app'

---

## API REST — Endpoints principales

```
POST   /api/auth/register       Registrar usuario
POST   /api/auth/login          Iniciar sesión
POST   /api/auth/refresh        Renovar token
POST   /api/auth/logout         Cerrar sesión

GET    /api/users/me            Perfil del usuario
PUT    /api/users/me            Actualizar perfil

GET    /api/vehicles            Mis vehículos
POST   /api/vehicles            Agregar vehículo
PUT    /api/vehicles/{id}       Actualizar vehículo
DELETE /api/vehicles/{id}       Eliminar vehículo

GET    /api/parking             Todos los estacionamientos
GET    /api/parking/{id}        Detalle del estacionamiento
GET    /api/parking/nearby      Cercanos por coordenadas
GET    /api/parking/search      Buscar por nombre/dirección
GET    /api/parking/{id}/availability  Disponibilidad

GET    /api/reservations        Mis reservas
POST   /api/reservations        Crear reserva
GET    /api/reservations/{id}   Detalle de reserva
PATCH  /api/reservations/{id}/cancel  Cancelar

GET    /api/admin/users         [ADMIN] Usuarios
GET    /api/admin/statistics    [ADMIN] Estadísticas
GET    /api/operator/parking-lots [OPERADOR] Mis estacionamientos
```

Documentación completa: `http://localhost:8080/swagger-ui.html`

---

## Seguridad

- **Autenticación**: JWT Bearer Token (expira en 24h)
- **Refresh Token**: 7 días, almacenado en BD, revocable
- **Contraseñas**: BCrypt (factor 10)
- **Roles**: CLIENTE, OPERADOR, ADMIN
- **Autorización**: basada en roles por endpoint
- **Acceso horizontal**: validado a nivel de servicio
- **Secretos**: variables de entorno, nunca en código

---

## Cuentas de prueba

> ⚠️ **Estas credenciales son SOLO para desarrollo local. Cámbialas en producción.**

| Email | Password | Rol |
|---|---|---|
| admin@parkeo.pe | Password123! | ADMIN |
| operador@parkeo.pe | Password123! | OPERADOR |
| cliente@parkeo.pe | Password123! | CLIENTE |
| juan.perez@parkeo.pe | Password123! | CLIENTE |

---

## Google Maps

La aplicación requiere una API Key de Google Maps SDK for Android.

Configura en `android/local.properties`:
```properties
MAPS_API_KEY=TU_API_KEY
```

APIs requeridas:
- Maps SDK for Android
- (Opcional) Geocoding API para búsqueda por texto

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
| Android — Reservas | ✅ Completo |
| Android — Perfil | ✅ Completo |
| Dark/Light Mode | ✅ Completo |
| Datos de prueba | ✅ Completo |
| Documentación | ✅ Completo |

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
