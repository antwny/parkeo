# PARKeo — Documentación de la API REST

La API de PARKeo es una API RESTful desarrollada con **Spring Boot 3.3.x**, asegurada con **Spring Security** y tokens **JWT**.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

---

## Formato estándar de respuesta

### Éxito

```json
{
  "success": true,
  "message": "ok",
  "data": { },
  "timestamp": "2026-10-19T19:28:01"
}
```

### Error

```json
{
  "success": false,
  "message": "Descripción clara del error",
  "data": null,
  "timestamp": "2026-10-19T19:28:01"
}
```

### Paginación

Los endpoints que devuelven páginas usan el formato de Spring Data:

```json
{
  "content": [],
  "pageable": { "pageNumber": 0, "pageSize": 20 },
  "totalElements": 6,
  "totalPages": 1,
  "last": true
}
```

---

## Autenticación y autorización

Los endpoints protegidos requieren:

```http
Authorization: Bearer <JWT_ACCESS_TOKEN>
```

Roles disponibles: `ROLE_USER`, `ROLE_OPERATOR`, `ROLE_ADMIN`.

Son públicos:

- `/api/auth/**`
- `GET /api/parking/**`
- Swagger, OpenAPI y `/actuator/health`

---

## Catálogo de endpoints

### 1. Autenticación (`/api/auth`)

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/register` | Público | Registrar nuevo usuario cliente |
| `POST` | `/api/auth/login` | Público | Iniciar sesión y obtener tokens |
| `POST` | `/api/auth/refresh` | Público | Renovar tokens con refresh token |
| `POST` | `/api/auth/logout` | Autenticado | Revocar refresh token y cerrar sesión |

#### Registro

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "cliente@parkeo.pe",
  "password": "Password123!",
  "firstName": "Juan",
  "lastName": "Pérez",
  "phone": "+51987654321"
}
```

El registro asigna automáticamente el rol `ROLE_USER`.

#### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "cliente@parkeo.pe",
  "password": "Password123!"
}
```

La respuesta incluye `accessToken`, `refreshToken` y los datos del usuario con sus roles.

#### Refresh

```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "<REFRESH_TOKEN>"
}
```

El refresh token anterior se revoca y se emite un nuevo par de tokens.

---

### 2. Usuarios (`/api/users`)

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/users/me` | Autenticado | Perfil del usuario autenticado |
| `PUT` | `/api/users/me` | Autenticado | Actualizar nombres, teléfono y documento |
| `PATCH` | `/api/users/me/password` | Autenticado | Cambiar contraseña |

#### Actualizar perfil

```http
PUT /api/users/me
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "firstName": "Juan",
  "lastName": "Pérez",
  "phone": "+51987654321",
  "documentNumber": "12345678"
}
```

#### Cambiar contraseña

```http
PATCH /api/users/me/password
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "currentPassword": "Password123!",
  "newPassword": "NuevaPassword123!"
}
```

La nueva contraseña debe tener al menos 8 caracteres.

---

### 3. Vehículos (`/api/vehicles`)

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/vehicles` | Autenticado | Vehículos del usuario |
| `POST` | `/api/vehicles` | Autenticado | Registrar vehículo |
| `PUT` | `/api/vehicles/{id}` | Autenticado | Actualizar vehículo propio |
| `DELETE` | `/api/vehicles/{id}` | Autenticado | Eliminar vehículo propio |
| `GET` | `/api/vehicles/types` | Autenticado | Catálogo de tipos de vehículo |

#### Registrar vehículo

```http
POST /api/vehicles
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "licensePlate": "ABC-123",
  "brand": "Toyota",
  "model": "Corolla",
  "color": "Negro",
  "year": 2021,
  "vehicleTypeId": 1
}
```

La placa debe cumplir el formato peruano `ABC-123` o `ABC123`.

---

### 4. Estacionamientos (`/api/parking`)

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/parking` | Público | Estacionamientos activos con paginación |
| `GET` | `/api/parking/{id}` | Público | Detalle completo del estacionamiento |
| `GET` | `/api/parking/nearby` | Público | Búsqueda por radio geográfico |
| `GET` | `/api/parking/search` | Público | Búsqueda por nombre o dirección |
| `GET` | `/api/parking/{id}/schedules` | Público | Horarios del estacionamiento |
| `GET` | `/api/parking/{id}/tariffs` | Público | Tarifas activas |
| `GET` | `/api/parking/{id}/availability` | Público | Espacios disponibles |
| `GET` | `/api/parking/{id}/spaces/available` | Público | Alias del endpoint anterior |
| `POST` | `/api/v1/availability/check` | Autenticado | Disponibilidad y precio por rango de fechas |

#### Búsqueda por proximidad

```http
GET /api/parking/nearby?lat=-12.1215&lon=-77.0298&radius=5.0
```

#### Disponibilidad

```http
GET /api/parking/1/availability?vehicleTypeId=1
```

#### Consulta avanzada de disponibilidad

```http
POST /api/v1/availability/check
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "parkingLotId": 1,
  "vehicleTypeId": 1,
  "startTime": "2026-10-20T10:00:00",
  "endTime": "2026-10-20T12:00:00"
}
```

La respuesta incluye `isAvailable`, `totalHours`, `calculatedPrice`, `currency` y la lista de espacios disponibles.

---

### 5. Reservas (`/api/reservations`)

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/reservations` | Autenticado | Mis reservas (paginado) |
| `GET` | `/api/reservations/{id}` | Autenticado | Detalle de una reserva propia |
| `POST` | `/api/reservations` | Autenticado | Crear reserva con verificación de cupo |
| `PATCH` | `/api/reservations/{id}/cancel` | Autenticado | Cancelar reserva pendiente o confirmada |

#### Listar reservas

```http
GET /api/reservations?status=PENDING,CONFIRMED&page=0&size=10
```

El parámetro `status` acepta uno o varios estados separados por coma: `PENDING`, `CONFIRMED`, `ACTIVE`, `COMPLETED`, `CANCELLED`, `NO_SHOW`.

#### Crear reserva

```http
POST /api/reservations
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "parkingLotId": 1,
  "vehicleId": 2,
  "startTime": "2026-10-20T10:00:00",
  "endTime": "2026-10-20T12:00:00",
  "notes": "Llegaré puntual"
}
```

Se puede enviar `parkingSpaceId` en lugar de `parkingLotId` para elegir un espacio específico. Si solo se envía `parkingLotId`, el servidor asigna automáticamente un espacio libre compatible con el tipo de vehículo.

Reglas principales:

- Duración mínima: 15 minutos.
- Duración máxima: 24 horas.
- Reserva hasta con 30 días de anticipación.
- El precio se calcula en el servidor a partir de las tarifas activas.
- Se evita el solapamiento de reservas para un mismo espacio y vehículo.

#### Cancelar reserva

```http
PATCH /api/reservations/1/cancel
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "reason": "Cambio de planes"
}
```

---

### 6. Panel Operador (`/api/operator`)

Requiere `ROLE_OPERATOR` o `ROLE_ADMIN`.

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/operator/parking-lots` | Estacionamientos asignados al operador |
| `GET` | `/api/operator/parking-lots/{id}/spaces` | Espacios del estacionamiento |
| `PATCH` | `/api/operator/spaces/{spaceId}/status` | Cambiar estado de un espacio |
| `GET` | `/api/operator/reservations` | Reservas de un estacionamiento |
| `PATCH` | `/api/operator/reservations/{id}/check-in` | Registrar ingreso |
| `PATCH` | `/api/operator/reservations/{id}/check-out` | Registrar salida |

#### Cambiar estado de espacio

```http
PATCH /api/operator/spaces/10/status
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "status": "MAINTENANCE",
  "notes": "Limpieza programada"
}
```

Estados válidos: `AVAILABLE`, `OCCUPIED`, `RESERVED`, `MAINTENANCE`, `INACTIVE`.

#### Reservas del estacionamiento

```http
GET /api/operator/reservations?parkingLotId=1&status=ACTIVE&page=0&size=20
```

El parámetro `parkingLotId` es obligatorio. El operador solo puede acceder a estacionamientos asignados; el administrador puede acceder a todos.

#### Check-in / check-out

```http
PATCH /api/operator/reservations/6/check-in
PATCH /api/operator/reservations/6/check-out
```

El check-in pasa la reserva de `PENDING`/`CONFIRMED` a `ACTIVE`, marca el espacio como `OCCUPIED` y descuenta un espacio disponible. El check-out pasa la reserva a `COMPLETED`, libera el espacio y devuelve el cupo.

---

### 7. Panel Administrador (`/api/admin`)

Requiere `ROLE_ADMIN`.

| Método | Endpoint | Descripción |
|---|---|---|
| `GET` | `/api/admin/users` | Listar todos los usuarios |
| `PUT` | `/api/admin/users/{id}/status` | Activar o desactivar usuario |
| `GET` | `/api/admin/parking-lots` | Listar todos los estacionamientos (incluye inactivos) |
| `PUT` | `/api/admin/parking-lots/{id}/status` | Abrir/cerrar o activar/desactivar cochera |
| `PUT` | `/api/admin/parking-lots/{id}/operator` | Asignar operador |
| `DELETE` | `/api/admin/parking-lots/{id}` | Desactivar cochera (soft delete) |
| `GET` | `/api/admin/statistics` | Estadísticas globales |

#### Activar / desactivar usuario

```http
PUT /api/admin/users/3/status
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "isActive": false
}
```

#### Estado de cochera

```http
PUT /api/admin/parking-lots/1/status
Content-Type: application/json

{
  "isOpen": true,
  "isActive": true
}
```

#### Asignar operador

```http
PUT /api/admin/parking-lots/1/operator
Content-Type: application/json

{
  "operatorId": 2
}
```

Enviar `operatorId: null` desasigna al operador.

#### Estadísticas

La respuesta incluye:

- `totalParkingLots`
- `totalReservations`
- `pendingReservations`
- `confirmedReservations`
- `activeReservations`
- `completedReservations`
- `cancelledReservations`

---

## Códigos HTTP usuales

| Código | Significado |
|---|---|
| `200` | Operación exitosa |
| `201` | Recurso creado |
| `400` | Datos inválidos o faltantes |
| `401` | Token ausente, inválido o expirado |
| `403` | Sin permisos para el recurso |
| `404` | Recurso no encontrado |
| `409` | Conflicto (solapamiento de reserva, placa duplicada, etc.) |
