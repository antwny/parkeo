# PARKeo — Flujos de Usuario

## Flujo Principal (Happy Path)

```
Splash (1.5s)
    ↓
    ¿Sesión válida?
    ├── SÍ → Mapa (pantalla principal)
    └── NO → Onboarding (1a vez) | Login
    
Onboarding (Puede omitirse)
    ↓
Login
    ↓
Mapa con ubicación actual + marcadores de estacionamientos
    ↓
Seleccionar estacionamiento en mapa | Buscar | Listar
    ↓
Detalle del estacionamiento
    ↓
[Reservar]
    ↓
Seleccionar fecha
    ↓
Seleccionar hora inicio + duración
    ↓
Seleccionar vehículo
    ↓
Resumen de reserva
    ↓
[Confirmar]
    ↓
Reserva creada ✅
    ↓
Mis Reservas
```

---

## Flujos Alternativos

### Permiso de ubicación rechazado

```
Solicitar permiso ACCESS_FINE_LOCATION
    ↓
    ├── Concedido → Centrar mapa en ubicación actual
    ├── Denegado (1a vez) → Explicación → Solicitar de nuevo
    └── Denegado permanentemente → 
            Explicar que el mapa aún funciona sin ubicación
            Mostrar mapa sin centrado automático
            Permitir buscar manualmente
```

### Token expirado durante uso

```
Request con access token expirado → 401
    ↓
Interceptor OkHttp detecta 401
    ↓
Intentar refresh: POST /api/auth/refresh
    ↓
    ├── Éxito → Guardar nuevos tokens → Reintentar request original
    └── Fallo (refresh expirado o revocado) → 
            Limpiar tokens de DataStore
            Navegar a Login
            Mostrar mensaje: "Tu sesión ha expirado"
```

### Estacionamiento lleno

```
Ver detalle de estacionamiento
    ↓
[Reservar]
    ↓
Verificar disponibilidad para la fecha/hora seleccionada
    ↓
    └── Sin espacios disponibles → 
            Mostrar mensaje: "No hay espacios disponibles"
            Mostrar otros estacionamientos cercanos
            Sugerir horarios alternativos si los hay
```

### Error de red

```
Cualquier request HTTP
    ↓
    └── Sin conexión / Timeout →
            Mostrar estado "Sin conexión"
            Mostrar datos en caché si disponibles
            Botón "Reintentar"
```

### Registro de nuevo usuario

```
Formulario de registro
    ↓
Validación en tiempo real (email, contraseña, nombre)
    ↓
[Registrarse]
    ↓
    ├── Éxito → Login automático → Mapa
    └── Email ya registrado → Mostrar error específico
    └── Error de validación → Marcar campos incorrectos
```

---

## Estados de UI

Cada pantalla debe manejar estos estados:

| Estado | UI |
|--------|-----|
| **Loading** | CircularProgressIndicator centrado o skeleton |
| **Success** | Contenido normal |
| **Empty** | Ilustración + mensaje + acción sugerida |
| **Error** | Mensaje descriptivo + botón "Reintentar" |
| **Offline** | Banner de sin conexión + datos en caché si aplica |
| **Unauthorized** | Redirigir a Login |

---

## Pantallas y componentes

### Splash
- Logo de PARKeo centrado
- Fondo en color primario (azul oscuro)
- Fade out suave
- Verificar token: si válido → Home, si no → Onboarding/Login

### Onboarding (3 páginas)
1. **Encuentra** — "Descubre estacionamientos cerca de ti"
2. **Consulta** — "Verifica disponibilidad en tiempo real"
3. **Reserva** — "Asegura tu espacio antes de salir"
- Paginador con dots indicator
- Botón "Omitir" (top-right)
- Botón "Siguiente" / "Comenzar"

### Login
- Logo pequeño de Parkeo
- Campo email (teclado email, validación en tiempo real)
- Campo contraseña (con toggle de visibilidad)
- Botón "Iniciar sesión" (loading state mientras hace request)
- Link "¿No tienes cuenta? Regístrate"
- Link "¿Olvidaste tu contraseña?"
- Error: "Correo o contraseña incorrectos" (mensaje genérico por seguridad)

### Registro
- Nombre
- Apellido
- Email
- Teléfono (opcional)
- Contraseña (con requisitos visuales: mayúscula, número, longitud)
- Confirmar contraseña
- Botón "Registrarse"
- Link "Ya tengo cuenta"

### Home / Mapa
- **90% del espacio**: GoogleMap
  - Marcadores de estacionamientos (icono personalizado de parking)
  - Color del marcador según disponibilidad: verde/amarillo/rojo/gris
  - Marcador de ubicación actual
- **Bottom Sheet** expandible:
  - Lista de estacionamientos cercanos en cards
  - Filtros: disponibilidad, precio, tipo, servicios
- Barra de búsqueda (top)
- Botón FAB: centrar en ubicación actual
- IconButton de perfil (top-right)

### Detalle del estacionamiento
- Hero section: imagen o gradiente con nombre
- Chips de servicios (Techado, 24h, Cámaras, etc.)
- Información: dirección, horario hoy, precio desde, distancia
- Disponibilidad visual: barra de progreso (X de Y espacios)
- Mapa pequeño estático con la ubicación
- Calificación (si implementada)
- Botones: [Ver en mapa] [Reservar]

### Flujo de reserva
1. **Seleccionar espacio/tipo**: tipo de vehículo disponible
2. **Fecha y hora**: DatePicker + TimePicker
3. **Duración**: Picker (1h, 2h, 3h, 4h, 8h, 12h, 24h)
4. **Seleccionar vehículo**: lista de vehículos del usuario
5. **Resumen**: estacionamiento, espacio, vehículo, fecha, hora, duración, precio total estimado
6. **Confirmación**: animación de éxito + detalle de la reserva

### Mis Reservas
- Pestañas: Próximas | Activas | Historial
- Card de reserva: estacionamiento, fecha/hora, vehículo, estado, precio
- Estado visual diferenciado: chip de color por estado
- Click → Detalle de reserva
- Opción cancelar (si aplica)

### Vehículos
- Lista de vehículos con icono por tipo
- FAB para agregar
- Swipe to delete (con confirmación)
- Formulario: tipo, placa, marca, modelo, color

### Perfil
- Avatar (iniciales si no hay foto)
- Nombre y email
- Opciones: Datos personales, Vehículos, Historial, Configuración
- Toggle Dark/Light mode
- Cerrar sesión (con confirmación)

---

## Estados de reserva

```
PENDING → CONFIRMED → ACTIVE → COMPLETED
    └→ CANCELLED
    └→ NO_SHOW
```

| Estado | Color | Icono | Descripción |
|--------|-------|-------|-------------|
| PENDING | Amarillo | ⏳ | Creada, esperando confirmación |
| CONFIRMED | Azul | ✅ | Confirmada |
| ACTIVE | Verde | 🅿️ | En curso |
| COMPLETED | Gris | ☑️ | Finalizada |
| CANCELLED | Rojo | ✕ | Cancelada |
| NO_SHOW | Naranja | ⚠️ | No se presentó |

---

## Estados de espacios

| Estado | Color | Descripción |
|--------|-------|-------------|
| AVAILABLE | Verde | Libre y disponible |
| RESERVED | Azul | Con reserva futura |
| OCCUPIED | Rojo | Ocupado en este momento |
| MAINTENANCE | Gris | Fuera de servicio |
