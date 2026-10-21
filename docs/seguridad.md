# PARKeo — Documentación de Seguridad

## Modelo de seguridad

PARKeo usa autenticación **stateless** con JWT y autorización basada en roles (RBAC).

```
Flujo de login (una vez por sesión):
Cliente → POST /api/auth/login
        ↓
Spring Security valida credenciales (BCrypt factor 12)
        ↓
Emite access token (JWT) + refresh token
        ↓
Cliente guarda ambos en DataStore

Flujo de cada petición (con cada request protegido):
Cliente envía Authorization: Bearer <accessToken>
        ↓
JwtAuthenticationFilter valida firma y expiración del JWT
        ↓
Carga el usuario desde la BD y sus roles activos
        ↓
SecurityContext con authorities ROLE_*
        ↓
@PreAuthorize / reglas de SecurityConfig autorizan el acceso

Flujo de renovación (cuando el access token expira):
POST /api/auth/refresh con el refresh token
        ↓
Backend valida que no esté revocado ni expirado
        ↓
Revoca el refresh token anterior (rotación) y emite un par nuevo
```

---

## Conceptos básicos: ¿qué son el JWT, el access token y el refresh token?

Si es tu primera vez con JWT, esta sección es el fundamento de todo lo demás.

### ¿Qué es un JWT?

**JWT (JSON Web Token)** es un estándar (RFC 7519) para crear tokens que *el servidor puede verificar sin consultar su base de datos*. Piénsalo como una **pulsera de un evento con un holograma anti-copias**:

- Trae información escrita encima (quién eres, qué te permite hacer, cuándo vence).
- Tiene una **firma criptográfica** que solo puede emitir quien conoce el secreto. Si alguien la modifica a mano, la firma ya no coincide y el servidor la rechaza.

Un JWT se ve así (tres bloques separados por puntos):

```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI0MiJ9.SflKxwRJSMeKKF2QT4fwpMeJf36...
     └─ header             └─ payload            └─ firma
```

| Parte | ¿Qué contiene? | ¿Cifrada? |
|---|---|---|
| **Header** | Algoritmo usado (en PARKeo: `HS256`) | ❌ Solo se codifica en Base64, no se cifra |
| **Payload** | Los *claims*: subject, email, role, `iat` (emitido), `exp` (expira) | ❌ Igual, legible por cualquiera |
| **Signature** | HMAC-SHA256 de las dos partes anteriores usando `JWT_SECRET` | ✅ Aquí vive el secreto |

> ⚠️ **Nunca guardes datos sensibles en el payload de un JWT.** Cualquiera que intercepte el token puede decodificarlo (Base64 es reversible). Lo que hace imposible falsificarlo es la firma, no el cifrado.

Ejemplo de payload tal como lo genera `JwtTokenProvider.generateAccessToken`:

```json
{
  "sub": "42",
  "email": "juan.perez@example.com",
  "role": "ROLE_USER",
  "iat": 1760900000,
  "exp": 1760986400
}
```

- `sub`: el ID del usuario (lo usa `getUserIdFromToken` para cargar al usuario).
- `exp`: marca de tiempo en la que el token deja de servir; el parser de jjwt rechaza tokens vencidos.

### ¿Qué es el access token?

Es el **"pasaporte" de corta vida que presenta en cada petición**. Ese JWT exacto es el que PARKeo emite en `/api/auth/login` y el que la app envía como `Authorization: Bearer <accessToken>`.

Características:

- **Corta vida** (en PARKeo: 24 h, en producción se recomienda menos): si lo roban, el daño tiene expiración.
- **Va en cada request**: el servidor lo valida en `JwtAuthenticationFilter`.
- **No se puede revocar fácilmente**: como es *stateless*, el servidor no lleva registro de los access tokens emitidos; solo puede confiar en la firma y la `exp`. Es el trade-off a cambio de no consultar BD para validar la firma.

> Detalle de la implementación de PARKeo: el filtro valida firma/expiración *sin tocar la BD*, pero después llama a `userDetailsService.loadUserById(userId)` para cargar al usuario y sus roles **desde la base de datos**. Consecuencias prácticas: (1) los permisos se actualizan tan pronto se cambia un rol en la BD, sin esperar a que el token expire, (2) una cuenta desactivada (`isActive=false`) deja de funcionar aunque su JWT aún sea válido.

### ¿Qué es el refresh token?

Es el **"carnet de socio" que sirve únicamente para pedir pasaportes nuevos**. NO es un JWT: en PARKeo es un **UUID aleatorio** (`UUID.randomUUID()`), un número sin significado que solo el servidor reconoce.

Características:

- **Vida más larga**: 7 días en PARKeo.
- **Se usa raramente**: solo va a `POST /api/auth/refresh` y `POST /api/auth/logout`.
- **Se puede revocar**: a diferencia del JWT, el servidor lo guarda en la tabla `refresh_tokens`, así que puede marcarlo como revocado (`is_revoked=true`) en cualquier momento. Esto es lo que hace posible el **logout real** y el cierre forzoso de todas las sesiones de un usuario (`revokeAllByUserId`).

### ¿Por qué dos tokens? (y no uno solo)

Si usaras un solo token de larga vida, al robarse tendrías acceso por semanas y no podrías revocarlo sin mantener una lista negra de cada token emitido. Si usas uno solo de corta vida, el usuario tendría que loguearse cada 15 minutos y odiaría tu app. El par access + refresh resuelve ambos problemas:

| Problema | Solución |
|---|---|
| Token largo = riesgo alto si se roba | **Access token corto** (minutos/horas): expira solo |
| Token corto = re-login constante | **Refresh token largo** que obtiene access tokens nuevos sin pedir la contraseña de nuevo |
| "Cerrar sesión" en JWT puro no es posible | **Refresh token en BD**: revocar = sesión muerta aunque el access token aún no expire |
| Cambiar el rol/estado del usuario | Los permisos reales se leen de la BD en cada request; el rol dentro del JWT solo es informativo |

### Analogía completa

> **Discoteca PARKeo**: entras y el vigilante revisa tu documento (login) y te da:
> 1. Una **pulsera del día** (access token): la muestras en cada zona (cada request), tiene un holograma verificable (firma) y a medianoche se apaga sola (`exp`).
> 2. Un **comprobante con tu foto** (refresh token): guardado en la caja fuerte del local (BD). Mañana (cuando tu pulsera ya se apagó) no vuelves a hacer fila ni sacas tu documento (no re-login), solo enseñas el comprobante en la caja fuerte y te dan una pulsera nueva.

### Ciclo de vida completo

```
Día 1
  Login ──────────────► accessToken (24h) + refreshToken (7d) guardados en DataStore
  App en uso ──────────► accessToken en cada header, el backend nunca pide nada más

Día 2 (el access token ya expiró)
  Request ─────────────► 401 (el JWT expiró)
  POST /auth/refresh ──► refreshToken válido ──► se revoca el viejo
                     ◄── accessToken nuevo + refreshToken NUEVO (rotación)

Día 6
  Otro POST /auth/refresh igual, el ciclo sigue vivo.

Día 7+1 (7 días sin renovar / logout)
  El refreshToken venció → hay que iniciar sesión de nuevo.

En cualquier momento
  POST /auth/logout ──► refreshToken marcado is_revoked=true ──►
  aunque el access token "sirva" unas horas más, el refresh ya no lo renovará.
```

> Si un atacante roba el refresh token y lo usa antes que tú, la **rotación** lo invalida de inmediato: tu siguiente refresh fallará con 401 y tendrás que iniciar sesión de nuevo. Es un mecanismo de detección de robo.

### HTTP para entender los errores

| Código HTTP | Significado | Causa típica con JWT |
|---|---|---|
| `401 Unauthorized` | **No sé quién eres, renueva tus credenciales** | Token inválido (firma, expirado, malformado) o inexistente |
| `403 Forbidden` | **Sé quién eres y no tienes permiso** | Token válido pero el rol no tiene permiso (ej. `ROLE_USER` tocando `/api/admin/**`) |

Regla de acción para el cliente: un `401` ⟶ intentar `refresh`; un `403` ⟶ mostrar "no tienes permisos".

---

## Roles y permisos

| Rol | Descripción |
|---|---|
| `ROLE_USER` | Cliente: consulta cocheras, gestiona vehículos y reservas |
| `ROLE_OPERATOR` | Operador: gestiona espacios y reservas de sus cocheras asignadas |
| `ROLE_ADMIN` | Administrador: gestión global de cocheras, operadores y usuarios |

### Matriz de acceso

| Recurso | Público | USER | OPERATOR | ADMIN |
|---|---|---|---|---|
| `POST /api/auth/**` | ✅ | ✅ | ✅ | ✅ |
| `GET /api/parking/**` | ✅ | ✅ | ✅ | ✅ |
| `POST /api/v1/availability/check` | ❌ | ✅ | ✅ | ✅ |
| `/api/users/me/**` | ❌ | ✅ | ✅ | ✅ |
| `/api/vehicles/**` | ❌ | ✅ | ✅ | ✅ |
| `/api/reservations/**` | ❌ | ✅ | ✅ | ✅ |
| `/api/operator/**` | ❌ | ❌ | ✅ | ✅ |
| `/api/admin/**` | ❌ | ❌ | ❌ | ✅ |
| Swagger / OpenAPI | ✅ | ✅ | ✅ | ✅ |
| `/actuator/health`, `/actuator/info` | ✅ | ✅ | ✅ | ✅ |

`@EnableMethodSecurity` está activo. `OperatorController` usa `@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_OPERATOR')")` y `AdminController` usa `@PreAuthorize("hasAuthority('ROLE_ADMIN')")`.

---

## Autenticación

### Contraseñas

- Hash con **BCrypt factor 12** (`new BCryptPasswordEncoder(12)`).
- Las contraseñas nunca se almacenan ni se devuelven en texto plano.
- Validación mínima: 8 caracteres.

### Access token (JWT)

| Atributo | Valor |
|---|---|
| Formato | JWT firmado en `JwtTokenProvider` |
| Algoritmo | HS256 |
| Subject | ID del usuario (`sub`) |
| Claims | `email`, `role`, `iat` (emitido), `exp` (expiración) |
| Vida útil | 24 horas (`JWT_EXPIRATION=86400000`) |
| Transporte | Header `Authorization: Bearer <token>` en cada request |

Claim `role` contiene el nombre completo del rol (`ROLE_USER`, `ROLE_OPERATOR` o `ROLE_ADMIN`).

> Interpretación clave: el JWT **no está cifrado**, solo firmado. Cualquiera con el token puede leer el email; lo que no puede hacer es alterar el payload sin invalidar la firma, porque la firma usa `JWT_SECRET`, que solo vive en el backend.

> Aclaración sobre este proyecto: el rol del claim `role` es **informativo**. La autorización real usa los roles leídos desde la BD (`UserDetailsServiceImpl.loadUserById`) dentro del filtro. Por eso un cambio de rol surte efecto de inmediato, sin esperar a que el access token expire.

### Refresh token

| Atributo | Valor |
|---|---|
| Formato | UUID v4 aleatorio, ej. `9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d` |
| Vida útil | 7 días (`JWT_REFRESH_EXPIRATION=604800000`) |
| Almacenamiento | Tabla `refresh_tokens` (`token`, `user_id`, `expires_at`, `is_revoked`) |
| Envío | Solo en el body de `POST /api/auth/refresh` y `POST /api/auth/logout` |
| Revocación | Al hacer logout o al renovar (rotación) |
| Limpieza | Job diario a las 2:00 AM borra tokens expirados/revocados |

Es un valor **sin significado**: a diferencia del JWT, no revela nada (ni siquiera el usuario al que pertenece). Solo la BD sabe qué usuario le corresponde; por eso el backend lo busca en `refresh_tokens` y carga la fila asociada (`RefreshToken.isValid()` verifica `!isRevoked && !isExpired()`).

> ⚠️ Actualmente se guarda en texto plano; antes de producción conviene almacenar solo un hash (ver "Riesgos y recomendaciones").

### ¿Qué devuelve /api/auth/login? (y /register, y /refresh)

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": { "id": 42, "email": "juan@example.com", "roles": ["ROLE_USER"], "...": "..." }
}
```

- `expiresIn` es en **segundos** (los 24 h) para que la app sepa cuándo anticipar el re-login/refresh.

Flujo de rotación (post `POST /api/auth/refresh`):

1. El cliente envía el refresh token (`RefreshTokenRequest`).
2. El servidor lo busca en `refresh_tokens`. Si no existe → `401`. Si `isRevoked` o `expiresAt` vencidos → `401`.
3. Marca el token anterior como revocado (`is_revoked = true`).
4. Emite un access token nuevo y un refresh token **nuevo diferente** (y guarda la nueva fila).
5. La app debe **reemplazar ambos** en DataStore; si guarda solo uno, la rotación se rompe.

Gracias a la rotación, cada refresh token usado queda marcado en BD: si uno robado (o muy antiguo) vuelve a usarse, el backend lo rechaza.

### Cierre de sesión

`POST /api/auth/logout` busca el refresh token y lo marca revocado (es idempotente: si ya no existe, simplemente no hace nada). El access token sigue siendo válido hasta su expiración (24 h), así que la app debe hacer `clearSession()` local **de todos modos**: eso evita que se siga enviando el token.

| Acción | ¿Qué se borra/revoca? | ¿Cuándo deja de funcionar el access token? |
|---|---|---|
| Logout | Refresh token (BD) | Cuando expire su `exp` (máx. 24 h). El backend no verifica access tokens revocados |
| Bloquear usuario (`isActive=false`) | Nada | De inmediato: el filtro carga al usuario y falla/no carga roles |
| `AuthService.logoutAll(userId)` (método de servicio) | Todos los refresh tokens del usuario | Ídem: se revoca toda renovación, pero no existe endpoint HTTP que lo exponga aún |

> Para un logout *inmediato* de un solo dispositivo sería necesario invalidar también el access token (lista de revocación en Redis o reducir su vida útil). Eso está pendiente en PARKeo (ver "Riesgos y recomendaciones").

---

## Autorización

### A nivel de endpoint

`SecurityConfig` establece:

```java
.requestMatchers(PUBLIC_PATHS).permitAll()
.requestMatchers(HttpMethod.GET, "/api/parking/**").permitAll()
.requestMatchers("/api/admin/**").hasAuthority("ROLE_ADMIN")
.requestMatchers("/api/operator/**").hasAnyAuthority("ROLE_ADMIN", "ROLE_OPERATOR")
.anyRequest().authenticated()
```

Requests autenticados que fallan (sin token, token inválido o expirado) no retornan genéricos: `SecurityConfig` define un `authenticationEntryPoint` que responde **`401` con header `WWW-Authenticate: Bearer`** y cuerpo `{"success":false,"message":"Sesión expirada o inválida."}`. Ese header es lo que permite al `TokenAuthenticator` de la app distinguir "renueva" de otros errores y refrescar automáticamente.

Rutas públicas:

```
/api/auth/**
/swagger-ui/**
/swagger-ui.html
/v3/api-docs/**
/api-docs/**
/api-docs.yaml
/actuator/health
/actuator/info
GET /api/parking/**
```

### A nivel de servicio (acceso horizontal)

Además del rol, el backend valida la propiedad del recurso:

- Un cliente solo puede ver y cancelar sus propias reservas.
- Un cliente solo puede modificar sus propios vehículos.
- Un operador solo accede a las cocheras que tiene asignadas (`operator_id`).
- El administrador puede acceder a todos los recursos.

Estas validaciones se aplican en la capa de servicio y devuelven `403` o `404` según corresponda.

---

## Sesión en Android (`data/local/SessionDataStore`, `data/remote/interceptor/AuthInterceptor`, `data/remote/interceptor/TokenAuthenticator`)

- Los tokens se guardan en **DataStore** (`parkeo_session`), no en `SharedPreferences` en texto plano.
- `AuthInterceptor` agrega `Authorization: Bearer <accessToken>` a cada request.
- **Renovación automática activa:** `TokenAuthenticator` implementa `okhttp3.Authenticator` y se dispara solo cuando el backend responde `401`. Su comportamiento:
  1. Interrumpe únicamente respuestas `401` y reintenta **una sola vez** por request (header `X-Auth-Retried` evita bucles infinitos).
  2. `synchronized(lock)`: un solo refresh concurrente (single-flight); si otro thread ya renovó, la fila de tokens guardados difiere del token que envió el request y se reenvía directamente con el token nuevo sin volver a llamar a `/api/auth/refresh`.
  3. Si el refresh falla (token vencido/revocado), limpia la sesión y emite un evento global (`SessionEvents.notifySessionExpired`) que lleva al usuario a Login.
- Clave para todo esto: `SecurityConfig` responde a requests **sin token o con token inválido con `401` + header `WWW-Authenticate: Bearer`** (mensaje "Sesión expirada o inválida."), lo que dispara al authenticator. Verificado en dispositivo: la app renueva sola el access token y sigue usando el backend sin pedir re-login.
- El `refreshToken` se envía solo a `/api/auth/refresh` y `/api/auth/logout` (nunca en el header de requests normales).
- `logout()` hace el `POST /api/auth/logout` y **siempre** llama a `clearSession()`, incluso si el servidor falla: la sesión local se limpia de todos modos.

---

## Protecciones implementadas

| Protección | Implementación |
|---|---|
| Inyección SQL | Spring Data JPA con consultas parametrizadas |
| CSRF | Deshabilitado por ser API stateless con JWT |
| Sesiones | `SessionCreationPolicy.STATELESS` |
| Fuerza bruta | BCrypt factor 12 (mitigación parcial) |
| Rate limiting en login | `LoginRateLimiter`: máx. **5 intentos fallidos por email en 15 minutos**; en bloqueo responde "Demasiados intentos, inténtalo más tarde". Ventana deslizante en memoria (`ConcurrentHashMap`, máx. 10 000 claves, se purga al superarla). El contador se limpia al acertar la contraseña. |
| Reservas vencidas | `ReservationExpiryService`: job cada 60 s que marca `NO_SHOW` las reservas `PENDING`/`CONFIRMED` cuya hora de inicio ya pasó con **15 min de gracia** (p. ej. `PKO-B26ZT7V` terminó en `NO_SHOW` automáticamente). |
| Expiración de datos de sesión | `RefreshTokenService`: limpieza diaria a las 2:00 AM de tokens expirados/revocados. |
| Auditoría | `AuditService` registra en `audit_log`: `RESERVATION_CHECK_IN`, `RESERVATION_CHECK_OUT`, `WALK_IN_CREATED`, `RESERVATION_CANCELLED` (usuario, entidad, notas). |
| Exposición de datos | DTOs de respuesta, sin entidades JPA directas |
| Validación de entrada | Bean Validation (`@Valid`, `@NotBlank`, `@Email`, etc.) |
| Errores | `GlobalExceptionHandler` con mensajes controlados |
| Secretos | Variables de entorno (`JWT_SECRET`, `DB_PASSWORD`) |
| CORS | Configurado en `CorsConfig` |
| Contraseñas | BCrypt, nunca en logs |

---

## Configuración de CORS

`CorsConfig` permite:

- Orígenes: `*` mediante `allowedOriginPatterns`.
- Métodos: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`.
- Headers: `Authorization`, `Content-Type`, `X-Requested-With`, `Accept`, `Origin`.
- Credenciales: `allowCredentials=true`.
- Cache: 3600 segundos.

> ⚠️ Para producción, restringe `allowedOriginPatterns` a los dominios reales. La combinación de `*` con credenciales es demasiado permisiva.

---

## Riesgos y recomendaciones

| Riesgo | Recomendación |
|---|---|
| CORS abierto | Restringir orígenes por entorno |
| Refresh token en texto plano en BD | Almacenar solo un hash (SHA-256) y comparar hashes |
| Access token de 24 h | Reducir a 15–60 minutos y usar refresh tokens |
| Rate limiting en memoria | Ya bloquea 5 intentos/15 min por email, pero es por instancia; migrar a Redis/Bucket4j si hay varios nodos |
| Sin HTTPS en desarrollo | Usar TLS en producción (Nginx + Let's Encrypt) |
| Sin verificación de email ni recuperación de contraseña | **Omitido por decisión de alcance del proyecto** (no es descuido). Para producción: flujo de verificación + restablecimiento con tokens de un solo uso |
| Sin revocación de access tokens | Lista negra en Redis o tokens de corta duración |
| Sin cierre de sesión global | Exponer `logoutAll` como endpoint (p. ej. al cambiar contraseña) |
| Swagger público | Proteger o deshabilitar en producción |
| `JWT_SECRET` de desarrollo | Generar secreto aleatorio de al menos 256 bits |
| Access token sin claim `iss`/`jti` | Agregar emisor e identificador de token para trazabilidad y depuración |

> La renovación automática de token en Android **ya está implementada y probada** (`TokenAuthenticator` + entry point `401`/`WWW-Authenticate`), por lo que ya no forma parte de los riesgos.

---

## Checklist para producción

- [ ] Configurar HTTPS y HSTS.
- [ ] Generar `JWT_SECRET` aleatorio y almacenarlo en un gestor de secretos.
- [ ] Restringir CORS a dominios conocidos.
- [ ] Cambiar todas las credenciales de prueba.
- [ ] Crear un usuario administrador propio y desactivar los de demo.
- [ ] Deshabilitar o proteger Swagger.
- [ ] Reducir la vida del access token.
- [ ] Mover el rate limiting de login a un almacenamiento distribuido (Redis/Bucket4j).
- [ ] Activar backups de MySQL.
- [ ] Configurar logs centralizados y alertas.
- [ ] Revisar dependencias con `mvn dependency-check:check`.
- [ ] Configurar firewall y acceso restringido a MySQL.
- [ ] Habilitar auditoría en `audit_log` para el resto de operaciones (registro de usuario, cambios de perfil, gestión de usuarios/cocheras).
