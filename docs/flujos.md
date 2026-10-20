# PARKeo — Flujos de Usuario

## Roles del sistema

| Rol | Pantallas principales |
|---|---|
| `ROLE_USER` (cliente) | Explorar, Reservas, Vehículos, Perfil |
| `ROLE_OPERATOR` | Espacios, Reservas, Explorar, Perfil |
| `ROLE_ADMIN` | Métricas, Cocheras, Usuarios, Explorar, Perfil |

La app determina el menú inferior a partir del rol devuelto por el backend.

---

## Flujo principal del cliente

```
Splash
    ↓
¿Sesión válida?
├── SÍ → Home (mapa)
└── NO → Onboarding (primera vez) | Login

Login / Registro
    ↓
Home: mapa + lista de estacionamientos
    ↓
Seleccionar estacionamiento (mapa, búsqueda o lista)
    ↓
Detalle del estacionamiento
    ↓
[Reservar]
    ↓
Seleccionar fecha, hora y duración
    ↓
Seleccionar vehículo
    ↓
Resumen con precio calculado por el servidor
    ↓
[Confirmar reserva]
    ↓
Reserva creada (PENDING)
    ↓
Mis Reservas
    ↓
Boarding pass QR + recordatorios de inicio/fin
```

---

## Flujos alternativos

### Permiso de ubicación rechazado

```
Solicitar ACCESS_FINE_LOCATION
    ↓
├── Concedido → Centrar mapa en la ubicación actual
├── Denegado → Explicación y reintento
└── Denegado permanentemente →
        El mapa sigue funcionando sin centrado automático
        El usuario puede buscar manualmente
```

### Token expirado durante el uso

```
Request con access token expirado → 401
    ↓
AuthInterceptor intenta POST /api/auth/refresh
    ↓
├── Éxito → Guardar tokens nuevos → Reintentar request original
└── Fallo → Limpiar sesión → Navegar a Login
```

### Estacionamiento sin disponibilidad

```
Ver detalle
    ↓
[Reservar]
    ↓
Consultar disponibilidad para fecha/hora seleccionada
    ↓
└── Sin espacios → "No hay espacios disponibles"
                   Mostrar otros estacionamientos cercanos
```

### Error de red

```
Cualquier request HTTP
    ↓
└── Sin conexión / timeout →
        Mostrar estado de error
        Botón "Reintentar"
```

### Registro de nuevo usuario

```
Formulario de registro
    ↓
Validación en tiempo real
    ↓
[Registrarse]
    ↓
├── Éxito → Sesión iniciada → Home
└── Email ya registrado → Error específico
```

---

## Flujo del operador

```
Login como ROLE_OPERATOR
    ↓
Pestaña "Espacios"
    ↓
Seleccionar cochera asignada
    ↓
Ver espacios (AVAILABLE, OCCUPIED, RESERVED, MAINTENANCE, INACTIVE)
    ↓
Cambiar estado de un espacio
    ↓
Pestaña "Reservas"
    ↓
Ver reservas de la cochera (filtros por estado)
    ↓
[Registrar ingreso]  → PENDING/CONFIRMED → ACTIVE
    ↓                  Espacio → OCCUPIED
    ↓                  available_spaces - 1
[Registrar salida]   → ACTIVE → COMPLETED
                       Espacio → AVAILABLE
                       available_spaces + 1
```

El operador solo puede acceder a los estacionamientos que tiene asignados. El administrador puede operar sobre todos.

---

## Flujo del administrador

```
Login como ROLE_ADMIN
    ↓
Pestaña "Métricas"
    ↓
Ver total de cocheras, reservas y reservas por estado
    ↓
Pestaña "Cocheras"
    ↓
Listar todas las cocheras (activas e inactivas)
    ↓
Abrir/cerrar cochera, activar/desactivar o asignar operador
    ↓
Pestaña "Usuarios"
    ↓
Listar usuarios y activar/desactivar cuentas
```

---

## Estados de UI

| Estado | UI |
|---|---|
| Loading | Indicador de carga o skeleton |
| Success | Contenido normal |
| Empty | Ilustración/mensaje + acción sugerida |
| Error | Mensaje descriptivo + botón "Reintentar" |
| Unauthorized | Redirigir a Login |

---

## Pantallas principales

### Splash
- Logo y fondo de marca.
- Verificación de sesión guardada.
- Redirección a Onboarding/Login o Home.

### Onboarding
- Presentación de las funciones principales.
- Botón para omitir y botón de avance.

### Login / Registro
- Validación de email y contraseña.
- Registro de cliente con nombre, apellido, email, teléfono y contraseña.
- Manejo de errores del backend.

### Home / Mapa
- Google Maps con marcadores de estacionamientos.
- Lista de cocheras cercanas.
- Búsqueda por nombre/dirección.
- Acceso a perfil y reservas.

### Detalle del estacionamiento
- Nombre, dirección, horarios, servicios y tarifas.
- Disponibilidad y calificación.
- Botón para reservar.

### Creación de reserva
- Fecha, hora de inicio y duración.
- Selección de vehículo.
- Cálculo de precio en servidor.
- Confirmación y creación de la reserva.

### Mis reservas
- Pestañas/filtros por estado.
- Detalle de la reserva.
- Cancelación de reservas `PENDING` o `CONFIRMED`.
- Boarding pass con código QR para reservas confirmadas/activas.
- Recordatorios locales de inicio y fin.

### Vehículos
- Lista de vehículos con tipo y placa.
- Alta, edición y eliminación.
- Validación de placa peruana.

### Perfil
- Datos del usuario y rol.
- Acceso a vehículos, reservas y configuración.
- Cierre de sesión.

### Operador — Espacios
- Selector de cochera asignada.
- Grilla de espacios con filtros por estado.
- Cambio de estado con notas.

### Operador — Reservas
- Reservas de la cochera seleccionada.
- Búsqueda y filtros por estado.
- Check-in y check-out.

### Administrador — Métricas
- KPIs de cocheras y reservas.
- Accesos rápidos a cocheras y usuarios.

### Administrador — Cocheras
- Listado completo de cocheras.
- Estado activo/abierto.
- Asignación de operador.

### Administrador — Usuarios
- Listado de usuarios.
- Activación/desactivación de cuentas.

---

## Estados de reserva

```
PENDING → CONFIRMED → ACTIVE → COMPLETED
    └→ CANCELLED
    └→ NO_SHOW
```

| Estado | Descripción |
|---|---|
| `PENDING` | Creada, pendiente de confirmación |
| `CONFIRMED` | Confirmada |
| `ACTIVE` | Cliente dentro del estacionamiento |
| `COMPLETED` | Reserva finalizada |
| `CANCELLED` | Cancelada por el cliente o el sistema |
| `NO_SHOW` | El cliente no se presentó |

---

## Estados de espacio

| Estado | Descripción |
|---|---|
| `AVAILABLE` | Libre y disponible |
| `RESERVED` | Con reserva futura |
| `OCCUPIED` | Ocupado en este momento |
| `MAINTENANCE` | Fuera de servicio temporalmente |
| `INACTIVE` | Deshabilitado |
