# PARKeo — Documentación de la API REST

La API de PARKeo es una API RESTful desarrollada con **Spring Boot 3.3.x**, asegurada con **Spring Security** y tokens **JWT**.
Documentación interactiva disponible en Swagger UI:
`http://localhost:8080/swagger-ui.html`
Definición OpenAPI (JSON):
`http://localhost:8080/api-docs`

---

## Formato Estándar de Respuesta

### Éxito
```json
{
  "success": true,
  "message": "Operación exitosa",
  "data": { ... },
  "timestamp": "2026-09-18T20:00:00"
}
```

### Error
```json
{
  "success": false,
  "message": "Descripción clara del error",
  "data": null,
  "timestamp": "2026-09-18T20:00:00"
}
```

---

## Autenticación y Autorización
Los endpoints protegidos requieren el encabezado:
```http
Authorization: Bearer <JWT_ACCESS_TOKEN>
```

---

## Catálogo de Endpoints

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

#### Login
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "cliente@parkeo.pe",
  "password": "Password123!"
}
```

---

### 2. Estacionamientos (`/api/parking`)

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/parking` | Público | Listar estacionamientos con paginación |
| `GET` | `/api/parking/{id}` | Público | Obtener detalle completo de estacionamiento |
| `GET` | `/api/parking/nearby` | Público | Buscar estacionamientos cercanos por radio |
| `GET` | `/api/parking/search` | Público | Búsqueda por texto (nombre, distrito) |
| `GET` | `/api/parking/{id}/availability` | Público | Consultar disponibilidad en tiempo real |
| `GET` | `/api/parking/{id}/spaces/available` | Público | Espacios disponibles para un rango de fechas |

#### Búsqueda por proximidad
```http
GET /api/parking/nearby?lat=-12.1215&lon=-77.0298&radius=5.0
```

---

### 3. Vehículos (`/api/vehicles`)

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/vehicles` | Autenticado | Obtener vehículos del usuario autenticado |
| `POST` | `/api/vehicles` | Autenticado | Registrar nuevo vehículo |
| `PUT` | `/api/vehicles/{id}` | Autenticado | Actualizar vehículo propio |
| `DELETE` | `/api/vehicles/{id}` | Autenticado | Eliminar vehículo propio |
| `GET` | `/api/vehicles/types` | Público | Catálogo de tipos de vehículo |

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
  "vehicleTypeId": 1
}
```

---

### 4. Reservas (`/api/reservations`)

| Método | Endpoint | Acceso | Descripción |
|---|---|---|---|
| `GET` | `/api/reservations` | Autenticado | Listar mis reservas (filtrable por status) |
| `GET` | `/api/reservations/{id}` | Autenticado | Obtener detalle de una reserva propia |
| `POST` | `/api/reservations` | Autenticado | Crear nueva reserva con verificación de cupo |
| `PATCH` | `/api/reservations/{id}/cancel` | Autenticado | Cancelar una reserva pendiente o confirmada |

#### Crear reserva
```http
POST /api/reservations
Authorization: Bearer <TOKEN>
Content-Type: application/json

{
  "parkingSpaceId": 1,
  "vehicleId": 2,
  "startTime": "2026-09-19T10:00:00",
  "endTime": "2026-09-19T12:00:00"
}
```

---

### 5. Panel Operador (`/api/operator`)

| Método | Endpoint | Rol Requerido | Descripción |
|---|---|---|---|
| `GET` | `/api/operator/parking-lots` | OPERADOR, ADMIN | Ver estacionamientos asignados |
| `GET` | `/api/operator/parking-lots/{id}/spaces` | OPERADOR, ADMIN | Consultar estado de los espacios |
| `PATCH` | `/api/operator/spaces/{id}/status` | OPERADOR, ADMIN | Cambiar estado de espacio (OCCUPIED, AVAILABLE) |
| `GET` | `/api/operator/reservations` | OPERADOR, ADMIN | Ver reservas de sus estacionamientos |

---

### 6. Panel Administrador (`/api/admin`)

| Método | Endpoint | Rol Requerido | Descripción |
|---|---|---|---|
| `GET` | `/api/admin/users` | ADMIN | Listar todos los usuarios del sistema |
| `PUT` | `/api/admin/users/{id}/status` | ADMIN | Activar / desactivar usuario |
| `GET` | `/api/admin/parking-lots` | ADMIN | Gestionar todos los estacionamientos |
| `POST` | `/api/admin/parking-lots` | ADMIN | Crear nuevo estacionamiento |
| `PUT` | `/api/admin/parking-lots/{id}` | ADMIN | Modificar estacionamiento |
| `DELETE` | `/api/admin/parking-lots/{id}` | ADMIN | Desactivar estacionamiento |
| `GET` | `/api/admin/statistics` | ADMIN | Estadísticas globales del sistema |
