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
Nueva reserva: fecha, hora de entrada y duración
    (pills "Inmediato / 30 min / 1 hora / 2 horas / Personalizada")
    ↓
Seleccionar vehículo
    ↓
Resumen con precio calculado por el servidor
    ↓
[Confirmar reserva]
    ↓
Reserva creada CONFIRMED
    ↓
Mis Reservas
    ↓
Boarding pass QR + recordatorios de inicio/fin
    ↓
Al completarse la reserva → calificación (1-5 estrellas)
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
Request con access token expirado → 401 + WWW-Authenticate: Bearer
    ↓
TokenAuthenticator (OkHttp) detecta el 401
    ↓
POST /api/auth/refresh (una sola vez, con lock anti-concurrencia)
    ↓
├── Éxito → Guardar tokens nuevos → Reintentar request original
└── Fallo → Limpiar sesión → Evento de sesión expirada → Login
```

### Reserva vencida sin check-in

```
Reserva PENDING/CONFIRMED cuya hora de inicio ya pasó
    ↓
Job del backend (cada 60 s, 15 min de gracia)
    ↓
└── Marca NO_SHOW automáticamente
    La reserva deja de consumir el espacio y ya no admite check-in
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
┌────────── Búsqueda por código ───────────┐
│ El cliente muestra su boarding pass      │
│ Código PKO-XXXXXXXX (sufijo de 4+        │
│ caracteres) → detalle → [Registrar       │
│ ingreso]                                 │
└──────────────────────────────────────────┘
Registro de ingreso   → PENDING/CONFIRMED → ACTIVE
                        Espacio → OCCUPIED
                        available_spaces - 1
Registro de salida    → ACTIVE → COMPLETED
                        Espacio → AVAILABLE
                        Cobro final = tarifas × tiempo real
                        (check-in → check-out)
```

### Walk-in: cliente sin reserva

```
Cliente llega sin reserva
    ↓
Pestaña "Reservas" → [Registrar walk-in]
    ↓
Elegir tipo de vehículo (y espacio, opcional)
    ↓
Backend crea reserva ACTIVE con vehículo marcador WALK-IN
    startTime = ahora · endTime = +1 h · monto 0.00
    Espacio → OCCUPIED
    ↓
Al retirarse: [Registrar salida] sobre esa reserva
    ↓
Cobro real con el tipo del espacio asignado
    → COMPLETED · monto final > 0
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
    (incluye NO_SHOW)
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

Accesos adicionales del admin:
- Reservas de todo el sistema (lista con filtro por estado,
  por ejemplo "En curso" para ACTIVE) y detalle/boarding pass
  de cualquier reserva.
- Pestaña "Explorar": mismo mapa de cocheras que el cliente.
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

### Home / Mapa (Explorar)
- Google Maps con marcadores de estacionamientos y lista ("Lista (N)") syncronizada.
- Buscador compacto (una línea, "Buscar cochera…", botón de limpiar) que filtra por nombre/distrito en vivo contra el backend.
- Al tocar un marcador: tarjeta inferior con asa deslizable, badge de disponibilidad ("N libres", "Completo", "Cerrado"), chips de capacidad/distancia/estado y botón primario a ancho completo "Ver detalles y reservar".

### Detalle del estacionamiento
- Nombre, dirección, horarios, servicios y tarifas.
- Disponibilidad por tipo de vehículo (chips de espacios libres).
- Botón para reservar.

### Creación de reserva
- Fecha, y **hora de entrada** con pills deslizables (`⚡ Inmediato`, `30 min`, `1 hora`, `2 horas`, `Personalizada`); "Personalizada" abre día/hora manual y ajuste fino ±15 min.
- La hora exacta de ingreso se muestra en la cabecera de la sección y recalcula el precio.
- Selección de vehículo (calcula disponibilidad por espacio y tipo).
- Cálculo de precio en servidor y selección de espacio (chips).
- Confirmación: reserva `CONFIRMED` + boarding pass QR.

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
- Calificación (1-5 estrellas + comentario) de reservas `COMPLETED`.

### Vehículos
- Lista de vehículos con tipo y placa.
- Alta, edición y eliminación.
- La placa se normaliza en el servidor (mayúsculas y formato `ABC-123`).

### Perfil
- Datos del usuario y rol.
- Selección de tema: **claro/oscuro/sistema**.
- Acceso a vehículos, reservas y configuración.
- Cierre de sesión.

### Operador — Espacios
- Selector de cochera asignada.
- Grilla de espacios con filtros por estado.
- Cambio de estado con notas.

### Operador — Reservas
- Reservas de la cochera seleccionada.
- Búsqueda y filtros por estado.
- **Búsqueda por código PKO-** (sufijo de 4-8 caracteres) desde el boarding pass del cliente.
- Botón "Registrar walk-in" (tipo de vehículo y espacio opcional).
- Check-in y check-out (el check-out muestra el monto final recalculado).

### Administrador — Métricas
- KPIs de cocheras y reservas (incluye `NO_SHOW`).
- Accesos rápidos a cocheras y usuarios.

### Administrador — Cocheras
- Listado completo de cocheras.
- Estado activo/abierto.
- Asignación de operador.

### Administrador — Usuarios
- Listado de usuarios.
- Activación/desactivación de cuentas.

### Administrador — Reservas
- Reservas de todas las cocheras con filtro por estado ("En curso" = ACTIVE).
- Detalle y boarding pass de cualquier reserva.

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
| `CONFIRMED` | Confirmada (estado con el que la app crea las reservas) |
| `ACTIVE` | Cliente dentro del estacionamiento (así nacen también los walk-ins) |
| `COMPLETED` | Reserva finalizada (con monto final recalculado) |
| `CANCELLED` | Cancelada por el cliente |
| `NO_SHOW` | El cliente no se presentó (asignado por el job de expiración; 15 min de gracia) |

---

## Estados de espacio

| Estado | Descripción |
|---|---|
| `AVAILABLE` | Libre y disponible |
| `RESERVED` | Con reserva futura |
| `OCCUPIED` | Ocupado en este momento |
| `MAINTENANCE` | Fuera de servicio temporalmente |
| `INACTIVE` | Deshabilitado |
