# PARKeo — Arquitectura del Sistema

## Visión general

PARKeo sigue una arquitectura cliente-servidor con separación de responsabilidades:

```
┌──────────────────────────────────────────┐
│          Android App (Cliente)           │
│                                          │
│  UI (Jetpack Compose)                    │
│      ↓                                   │
│  ViewModel (StateFlow, Coroutines)       │
│      ↓                                   │
│  Repository (interfaz de dominio)        │
│      ↓                                   │
│  Retrofit + OkHttp + JWT Interceptor     │
└──────────────┬───────────────────────────┘
               │
               │ HTTP/REST + JWT Bearer Token
               │ URL del backend: build lee BACKEND_URL de android/local.properties
               │   · física en la misma red → http://10.146.116.216:8080/ (actual)
               │   · emulador → http://10.0.2.2:8080/
               │
┌──────────────▼───────────────────────────┐
│         Spring Boot Backend              │
│                                          │
│  Controller (@RestController)            │
│      ↓                                   │
│  Service (@Service, @Transactional)      │
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

## Arquitectura Android

```
android/app/src/main/java/pe/parkeo/
│
├── ui/                        # Capa de presentación
│   ├── theme/                 # Material 3, colores, tipografía, dimensiones
│   ├── navigation/            # NavGraph y rutas
│   ├── screens/               # Pantallas Compose
│   │   ├── splash/
│   │   ├── onboarding/
│   │   ├── auth/
│   │   ├── main/              # Contenedor con navegación por rol
│   │   ├── home/              # Mapa y lista de estacionamientos
│   │   ├── parking/           # Detalle del estacionamiento
│   │   ├── reservation/       # Creación, listado y boarding pass QR
│   │   ├── vehicles/
│   │   ├── operator/          # Espacios y reservas del operador
│   │   ├── admin/             # Métricas, cocheras y usuarios
│   │   └── profile/
│   ├── components/            # Composables reutilizables
│   └── viewmodel/             # ViewModels (StateFlow)
│
├── domain/
│   ├── model/                 # Modelos de dominio
│   └── repository/            # Interfaces de repositorio
│
├── data/
│   ├── remote/
│   │   ├── api/               # Interfaces Retrofit
│   │   ├── dto/               # DTOs de la API
│   │   └── interceptor/       # AuthInterceptor (Bearer token) + TokenAuthenticator (renovación por 401)
│   ├── local/                 # DataStore (sesión y tokens)
│   └── repository/            # Implementaciones de repositorio
│
├── di/                        # Inyección manual con AppContainer
├── util/                      # Recordatorios, errores de red, resultados
├── MainActivity.kt
└── ParkeoApplication.kt
```

### Principios aplicados

> 📚 Cada concepto (Compose, ViewModel, StateFlow, coroutines, Retrofit/interceptores, DataStore, Navigation, WorkManager, DI manual) está explicado con código real de la app en **`docs/android.md`**.

- **Separación por capas**: UI, ViewModel, dominio y datos.
- **Repository Pattern**: las implementaciones de `data/repository` dependen de interfaces de `domain/repository`.
- **ViewModel + StateFlow**: estado de UI reactivo y persistente ante rotaciones.
- **Unidirectional Data Flow**: el estado fluye hacia la UI y los eventos hacia el ViewModel.
- **Navegación por rol**: `MainScreen` muestra pestañas distintas para cliente, operador y administrador.
- **Inyección manual**: `DefaultAppContainer` construye Retrofit, DataStore y repositorios.

### Navegación por rol

| Rol | Pestañas principales |
|---|---|
| Cliente | Explorar, Reservas, Vehículos, Perfil |
| Operador | Espacios, Reservas, Explorar, Perfil |
| Administrador | Métricas, Cocheras, Usuarios, Explorar, Perfil |

### Funciones locales

- Recordatorios de inicio y fin de reserva con **WorkManager** (`ReminderScheduler`).
- Notificaciones locales (Android 13+ requiere `POST_NOTIFICATIONS`).
- Sesión persistida en **DataStore** (tokens + preferencia de tema claro/oscuro/sistema).
- Boarding pass con código QR generado localmente a partir del `confirmationCode`; el código `PKO-XXXXXXXX` es la llave del flujo del operador (búsqueda por código y check-in).
- Tema claro/oscuro elegible desde Perfil (opción sincronizada en DataStore).

---

## Arquitectura Backend

```
pe.parkeo/
├── controller/       # Endpoints HTTP y autorización con @PreAuthorize
├── service/          # Lógica de negocio y transacciones
│   ├── ReservationService      # Crear/cancelar/check-in/check-out, walk-ins, ratings
│   ├── AvailabilityService     # Espacios libres por rango y cálculo de tarifas
│   ├── ParkingLotService       # Listado, búsqueda, cercanos, detalle, estadísticas
│   ├── AuthService             # Registro, login, refresh con rotación, logout
│   ├── LoginRateLimiter        # Bloqueo de login: 5 fallos / 15 min por email
│   ├── ReservationExpiryService # Job: PENDING/CONFIRMED vencidas → NO_SHOW
│   ├── RefreshTokenService     # Rotación y revocación de refresh tokens
│   └── AuditService            # Escritura en audit_log (check-in/out, walk-in, cancelación)
│                                  AuthService también tiene el cleanup diario a las 2:00 AM
├── repository/       # Acceso a datos con Spring Data JPA
├── entity/           # Entidades JPA mapeadas a MySQL
├── dto/
│   ├── request/      # DTOs de entrada con Bean Validation
│   └── response/     # DTOs de salida
├── security/         # JwtAuthenticationFilter, JwtTokenProvider, UserDetailsService
├── config/           # Spring Security, CORS, OpenAPI, JWT
├── exception/        # Excepciones y GlobalExceptionHandler
└── enums/            # Enumeraciones del dominio
```

### Seguridad en capas

| Capa | Responsabilidad |
|---|---|
| Controller | Validar entrada y aplicar `@PreAuthorize` |
| Service | Verificar acceso horizontal y reglas de negocio |
| Repository | Consultas JPA parametrizadas |
| Filter (JWT) | Validar token y cargar el `SecurityContext` |

---

## Flujo de autenticación

```
Android → POST /api/auth/login {email, password}
       ↓
Spring Security → UserDetailsServiceImpl.loadUserByUsername()
       ↓
BCryptPasswordEncoder.matches()
       ↓
JwtTokenProvider.generateAccessToken()
       ↓
Guardar RefreshToken en BD
       ↓
Android ← {accessToken, refreshToken, user}
       ↓
Android almacena tokens en DataStore
       ↓
Cada request: Authorization: Bearer {accessToken}
       ↓
JwtAuthenticationFilter valida el token → SecurityContext
```

### Claims del access token

```json
{
  "sub": "3",
  "email": "cliente@parkeo.pe",
  "role": "ROLE_USER",
  "iat": 1760000000,
  "exp": 1760086400
}
```

### Renovación de token

```
Access token expirado (401)
       ↓
Android → POST /api/auth/refresh {refreshToken}
       ↓
Validar refresh token en BD (no revocado, no expirado)
       ↓
Emitir nuevo access token + refresh token
       ↓
Revocar refresh token anterior
       ↓
Android ← nuevos tokens
```

---

## Lógica de disponibilidad

La disponibilidad se calcula con datos reales de la base de datos:

1. Se obtienen los espacios del estacionamiento filtrados por tipo de vehículo y estado `AVAILABLE`.
2. Se buscan reservas con estado `PENDING`, `CONFIRMED` o `ACTIVE` que se solapen con el rango solicitado.
3. Se excluyen los espacios con reservas superpuestas.
4. El precio se calcula en el servidor usando las tarifas activas (`HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY`, `FIXED`).
5. `parking_lots.available_spaces` se mantiene como contador de disponibilidad y se ajusta en check-in/check-out (se recalcula contando espacios `AVAILABLE`).

Condición de solapamiento:

```sql
:start_time < r.end_time AND :end_time > r.start_time
```

### Prevención de condición de carrera

La creación de reservas usa:

- `@Transactional(isolation = Isolation.READ_COMMITTED)`.
- Bloqueo pesimista del espacio con `SELECT ... FOR UPDATE` (`findByIdForUpdate`).
- Verificación de solapamiento dentro de la transacción.
- Restricción adicional a nivel de servicio para evitar dos reservas del mismo vehículo en el mismo horario.

---

## Ciclo de vida de una reserva

1. **Creación (cliente):** reserva en estado `CONFIRMED` con precio estimado por el servidor y espacio asignado; genera el `confirmationCode` (`PKO-XXXXXXXX`).
2. **Expiración automática:** un job cada 60 s marca `NO_SHOW` las reservas `PENDING`/`CONFIRMED` cuya hora de inicio pasó con 15 min de gracia.
3. **Cancelación (cliente):** `PENDING`/`CONFIRMED` → `CANCELLED` (asenta motivo y auditoría).
4. **Check-in (operador o admin):** `PENDING`/`CONFIRMED` → `ACTIVE`; espacio → `OCCUPIED`; `checkedInAt = ahora`.
5. **Check-out (operador o admin):** `ACTIVE` → `COMPLETED`; espacio → `AVAILABLE`; se recalcula el monto final con el tiempo real (`checkedInAt` → ahora) y la tarifa del tipo de vehículo (si el vehículo no tiene tipo —walk-ins— se usa el tipo del espacio).
6. **Walk-in (operador):** sin reserva previa se crea directamente una reserva `ACTIVE` con estado 0.00 y el vehículo marcador global `WALK-IN`; el pago real se cobra en el check-out.

---

## Decisiones de diseño

| Decisión | Justificación |
|---|---|
| MySQL 8.0 | Motor relacional disponible y adecuado para el dominio |
| Maven en backend | Estándar maduro para Spring Boot |
| Gradle en Android | Estándar de Android |
| JJWT 0.12 | API moderna y segura |
| `ddl-auto=update` | Hibernate sincroniza columnas nuevas; `schema.sql` se mantiene alineado con las entidades |
| Scripts SQL explícitos | Reproducibilidad del esquema y de los datos de prueba |
| Sin Flyway/Liquibase | Simplicidad para el alcance actual del proyecto |
| DECIMAL para coordenadas | Precisión suficiente para geolocalización |
| Haversine en JPQL | Búsqueda por radio sin dependencias externas |
| Inyección manual en Android | Menor complejidad que Hilt para el tamaño actual del proyecto |
| WorkManager para recordatorios | Recordatorios locales sin depender de un servidor de notificaciones |
| `vehicles.vehicle_type_id` **NULLABLE** + `VehicleType` LAZY | El vehículo marcador `WALK-IN` no tiene tipo; la carga LAZY evita serializar la FK en cada response (`VehicleService.mapToResponse` la trata con null-safety) |
| Checkout cobra por el tipo del espacio si el vehículo no tiene tipo | Los walk-ins siempre coinciden con el tipo que registró el operador al crear el ingreso |
| Rate limiting en memoria (no Redis) | Suficiente para una instancia de desarrollo; documentado el límite de migrar a Redis |
| 401 + `WWW-Authenticate: Bearer` como contrato de expiración | Permite al `TokenAuthenticator` de OkHttp detectar y refrescar tokens sin UI extra |
| Vehículo marcador `WALK-IN` único por placa | Todos los ingresos sin reserva comparten una sola entidad; cada reserva guarda su espacio, tiempo y monto real |

---

## Dependencias de producción (futuro)

- **Redis**: caché de disponibilidad y rate limiting.
- **Nginx**: reverse proxy y terminación SSL.
- **Let's Encrypt**: certificados SSL.
- **GitHub Actions / Jenkins**: CI/CD.
- **Spring Boot Actuator + Micrometer**: métricas y monitoreo.
- **Flyway**: migraciones versionadas.
- **Spring Cloud Config**: configuración centralizada.
- **Servicio de notificaciones push**: FCM para recordatorios en segundo plano.
