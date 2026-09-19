# PARKeo — Arquitectura del Sistema

## Visión general

PARKeo sigue una arquitectura cliente-servidor clásica pero moderna, con separación clara de responsabilidades:

```
┌──────────────────────────────────────────┐
│          Android App (Cliente)           │
│                                          │
│  Capa UI (Jetpack Compose)               │
│      ↓                                   │
│  ViewModel (StateFlow, Coroutines)       │
│      ↓                                   │
│  Use Cases (lógica de presentación)      │
│      ↓                                   │
│  Repository (abstracción de datos)       │
│      ↓                                   │
│  Remote Data Source (Retrofit + OkHttp)  │
└──────────────┬───────────────────────────┘
               │
               │ HTTPS + JWT Bearer Token
               │ (localhost:8080 en desarrollo)
               │
┌──────────────▼───────────────────────────┐
│         Spring Boot Backend              │
│                                          │
│  Controller (@RestController)            │
│      ↓                                   │
│  Service (@Service)                      │
│      ↓                                   │
│  Repository (Spring Data JPA)            │
│      ↓                                   │
│  Entity (JPA / Hibernate)                │
└──────────────┬───────────────────────────┘
               │
               │ JDBC / JPA
               │
┌──────────────▼───────────────────────────┐
│           MySQL 8.0                      │
│        parkeo_db                         │
│   (modelo relacional normalizado)        │
└──────────────────────────────────────────┘
```

---

## Arquitectura Android — Clean Architecture

```
android/app/src/main/java/pe/parkeo/
│
├── ui/                        # Capa de presentación
│   ├── theme/                 # Material 3, colores, tipografía
│   ├── navigation/            # NavGraph, rutas
│   ├── screens/               # Pantallas Compose
│   │   ├── splash/
│   │   ├── onboarding/
│   │   ├── auth/
│   │   ├── home/              # Mapa principal
│   │   ├── parking/           # Detalle de estacionamiento
│   │   ├── reservation/       # Flujo de reserva
│   │   ├── vehicles/          # Gestión de vehículos
│   │   └── profile/
│   └── components/            # Composables reutilizables
│
├── viewmodel/                 # ViewModels (AAC)
├── domain/
│   ├── model/                 # Modelos de dominio
│   ├── repository/            # Interfaces de repositorio
│   └── usecase/               # Casos de uso
│
├── data/
│   ├── remote/                # Retrofit, API, DTOs
│   │   ├── api/               # Interfaces Retrofit
│   │   ├── dto/               # Data Transfer Objects
│   │   └── interceptor/       # Auth interceptor
│   ├── local/                 # DataStore (sesión)
│   └── repository/            # Implementaciones de repositorio
│
└── di/                        # Hilt dependency injection
```

### Principios aplicados

- **Single Responsibility**: Cada clase tiene una única responsabilidad
- **Dependency Inversion**: Las capas dependen de abstracciones (interfaces), no de implementaciones
- **Repository Pattern**: La fuente de datos es intercambiable
- **ViewModel**: Maneja el estado de UI y sobrevive rotaciones de pantalla
- **StateFlow**: Estado reactivo, predecible y testeable
- **Unidirectional Data Flow**: Estado fluye hacia abajo, eventos hacia arriba

---

## Arquitectura Backend — Layered Architecture

```
pe.parkeo/
├── controller/       # HTTP handlers, validación de entrada, autorización
├── service/          # Lógica de negocio, transacciones
├── repository/       # Acceso a datos (Spring Data JPA)
├── entity/           # Entidades JPA mapeadas a tablas MySQL
├── dto/
│   ├── request/      # DTOs de entrada (validados con Bean Validation)
│   └── response/     # DTOs de salida (nunca exponer entidades directamente)
├── security/         # JWT filter, UserDetailsService
├── config/           # Spring Security, CORS, OpenAPI
├── exception/        # Excepciones personalizadas, GlobalExceptionHandler
└── enums/            # Enumeraciones del dominio
```

### Principios de seguridad en capas

| Capa | Responsabilidad de seguridad |
|------|------------------------------|
| Controller | Validar entrada, aplicar @PreAuthorize |
| Service | Verificar acceso horizontal (¿este recurso pertenece al usuario?) |
| Repository | Consultas parametrizadas (JPA evita SQL injection) |
| Filter (JWT) | Extraer y validar token, cargar SecurityContext |

---

## Flujo de autenticación

```
Android → POST /api/auth/login {email, password}
       ↓
Spring Security → UserDetailsService.loadUserByUsername()
       ↓
BCryptPasswordEncoder.matches()
       ↓
JwtTokenProvider.generateAccessToken() + generateRefreshToken()
       ↓
Guardar RefreshToken en BD
       ↓
Android ← {accessToken, refreshToken, user}
       ↓
Android almacena tokens en DataStore (cifrado)
       ↓
Cada request: Authorization: Bearer {accessToken}
       ↓
JwtAuthenticationFilter valida token → SecurityContext
```

### Renovación de token

```
AccessToken expirado (401)
       ↓
Android → POST /api/auth/refresh {refreshToken}
       ↓
Validar refreshToken en BD (no revocado, no expirado)
       ↓
Emitir nuevo accessToken + refreshToken
       ↓
Revocar refreshToken anterior
       ↓
Android ← {nuevos tokens}
```

---

## Lógica de disponibilidad

La disponibilidad se calcula dinámicamente, no como un contador simple:

```
Para un período [startTime, endTime] y tipo de vehículo:

1. Obtener TODOS los espacios del estacionamiento compatibles con el tipo de vehículo
2. Para cada espacio, verificar si existe alguna reserva ACTIVA que solape:
   SELECT COUNT(*) FROM reservations
   WHERE parking_space_id = :spaceId
   AND status IN ('PENDING', 'CONFIRMED', 'ACTIVE')
   AND (:startTime < end_time AND :endTime > start_time)
3. Los espacios sin solapamiento = DISPONIBLES
4. available_count = total_compatible_spaces - occupied_spaces
```

### Prevención de condición de carrera

La creación de reservas usa `@Transactional` con `SELECT FOR UPDATE` implícito via JPA + `Isolation.SERIALIZABLE` para el método crítico, garantizando que dos usuarios no puedan reservar simultáneamente el mismo espacio.

---

## Decisiones de diseño

| Decisión | Justificación |
|----------|---------------|
| MySQL sobre PostgreSQL | El usuario tiene MySQL 8.0 instalado y configurado |
| Maven sobre Gradle (backend) | Maven está disponible via SDKMAN, más maduro para Spring |
| Gradle (Android) | Estándar de Android, mejor soporte en Android Studio |
| JJWT 0.12.x | API más limpia y segura que versiones anteriores |
| ddl-auto=validate | El schema se gestiona con scripts SQL explícitos, no auto-generado |
| No usar Flyway/Liquibase | Simplicidad para el proyecto inicial; fácil de añadir después |
| Coordenadas DECIMAL(10,8) y (11,8) | Precisión de ~1.1mm, suficiente para geolocalización |
| Haversine en JPQL | Sin dependencias adicionales para búsqueda por radio |

---

## Dependencias de producción (futuro)

Para evolucionar a producción, se necesitaría añadir:

- **Redis**: Cache de disponibilidad, rate limiting
- **Nginx**: Reverse proxy, terminación SSL
- **Let's Encrypt**: Certificados SSL gratuitos
- **GitHub Actions / Jenkins**: CI/CD pipeline
- **Spring Boot Actuator + Micrometer**: Métricas y monitoreo
- **Flyway**: Migraciones de base de datos versionadas
- **Spring Cloud Config**: Configuración centralizada
