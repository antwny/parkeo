# PARKeo — Documentación de Seguridad

## Modelo de seguridad

PARKeo usa autenticación **stateless** con JWT y autorización basada en roles (RBAC).

```
Cliente → POST /api/auth/login
        ↓
Spring Security valida credenciales (BCrypt factor 12)
        ↓
Emite access token (JWT) + refresh token
        ↓
Cliente envía Authorization: Bearer <accessToken>
        ↓
JwtAuthenticationFilter valida firma y expiración
        ↓
SecurityContext con authorities ROLE_*
        ↓
@PreAuthorize / reglas de SecurityConfig autorizan el acceso
```

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
| Algoritmo | HS256 |
| Emisor | `parkeo-backend` |
| Subject | ID del usuario |
| Claims | `email`, `role` |
| Vida útil | 24 horas (`JWT_EXPIRATION=86400000`) |
| Transporte | `Authorization: Bearer <token>` |

Claim `role` contiene el nombre completo del rol (`ROLE_USER`, `ROLE_OPERATOR` o `ROLE_ADMIN`).

### Refresh token

| Atributo | Valor |
|---|---|
| Formato | UUID aleatorio |
| Vida útil | 7 días (`JWT_REFRESH_EXPIRATION=604800000`) |
| Almacenamiento | Tabla `refresh_tokens` |
| Revocación | Al hacer logout o al renovar (rotación) |
| Limpieza | Job diario a las 2:00 AM para tokens expirados/revocados |

Flujo de rotación:

1. El cliente envía el refresh token a `POST /api/auth/refresh`.
2. El servidor valida que no esté revocado ni expirado.
3. Revoca el token anterior.
4. Emite un nuevo access token y un nuevo refresh token.

### Cierre de sesión

`POST /api/auth/logout` revoca el refresh token enviado. El access token sigue siendo válido hasta su expiración (24 h), por lo que en producción se recomienda una duración menor o una lista de revocación.

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

## Sesión en Android

- Los tokens se guardan en **DataStore**, no en `SharedPreferences` en texto plano.
- `AuthInterceptor` agrega `Authorization: Bearer` a cada request.
- Ante un `401`, el interceptor intenta renovar el token con `/api/auth/refresh`.
- Si la renovación falla, se limpia la sesión y se redirige a Login.
- El `refreshToken` se envía solo a `/api/auth/refresh` y `/api/auth/logout`.

---

## Protecciones implementadas

| Protección | Implementación |
|---|---|
| Inyección SQL | Spring Data JPA con consultas parametrizadas |
| CSRF | Deshabilitado por ser API stateless con JWT |
| Sesiones | `SessionCreationPolicy.STATELESS` |
| Fuerza bruta | BCrypt factor 12 (mitigación parcial) |
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
| Sin rate limiting | Agregar límite en login/refresh (Redis, Bucket4j o API Gateway) |
| Sin bloqueo por intentos fallidos | Bloquear cuenta temporalmente tras N intentos |
| Sin HTTPS en desarrollo | Usar TLS en producción (Nginx + Let's Encrypt) |
| Sin auditoría automática | Integrar escritura en `audit_log` desde servicios o AOP |
| Swagger público | Proteger o deshabilitar en producción |
| `JWT_SECRET` de desarrollo | Generar secreto aleatorio de al menos 256 bits |
| Sin verificación de email | Implementar flujo de verificación y recuperación de contraseña |
| Sin revocación de access tokens | Lista negra en Redis o tokens de corta duración |

---

## Checklist para producción

- [ ] Configurar HTTPS y HSTS.
- [ ] Generar `JWT_SECRET` aleatorio y almacenarlo en un gestor de secretos.
- [ ] Restringir CORS a dominios conocidos.
- [ ] Cambiar todas las credenciales de prueba.
- [ ] Crear un usuario administrador propio y desactivar los de demo.
- [ ] Deshabilitar o proteger Swagger.
- [ ] Reducir la vida del access token.
- [ ] Agregar rate limiting en autenticación.
- [ ] Activar backups de MySQL.
- [ ] Configurar logs centralizados y alertas.
- [ ] Revisar dependencias con `mvn dependency-check:check`.
- [ ] Configurar firewall y acceso restringido a MySQL.
- [ ] Habilitar auditoría en `audit_log`.
