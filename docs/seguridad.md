# PARKeo — Seguridad

## Modelo de seguridad

La seguridad de PARKeo está implementada en múltiples capas, siguiendo el principio de "Defense in Depth":

```
Base de datos → Backend → API REST → Android
```

---

## Autenticación — JWT

### Configuración de tokens

| Parámetro | Valor (desarrollo) | Descripción |
|-----------|-------------------|-------------|
| Algoritmo | HS256 | HMAC SHA-256 |
| Clave secreta | Mínimo 256 bits | Variable de entorno JWT_SECRET |
| Access token TTL | 24 horas | Configurable via JWT_EXPIRATION |
| Refresh token TTL | 7 días | Configurable via JWT_REFRESH_EXPIRATION |

### Claims en el JWT

```json
{
  "sub": "42",
  "email": "cliente@parkeo.pe",
  "role": "CLIENTE",
  "iat": 1750000000,
  "exp": 1750086400
}
```

### Flujo de refresh

1. Access token expirado → Android recibe 401
2. Android envía refresh token a `POST /api/auth/refresh`
3. Backend valida: token en BD, no revocado, no expirado
4. Backend emite nuevo access token + nuevo refresh token
5. Refresh token anterior se revoca (rotación)
6. Si el refresh token también expiró → redirigir a login

---

## Autorización — RBAC (Role-Based Access Control)

### Roles y permisos

| Recurso | CLIENTE | OPERADOR | ADMIN |
|---------|---------|----------|-------|
| Ver estacionamientos | ✅ | ✅ | ✅ |
| Ver disponibilidad | ✅ | ✅ | ✅ |
| Crear reserva | ✅ | ❌ | ✅ |
| Cancelar su reserva | ✅ | ❌ | ✅ |
| Ver sus vehículos | ✅ | ❌ | ✅ |
| Gestionar sus vehículos | ✅ | ❌ | ✅ |
| Ver estacionamientos asignados | ❌ | ✅ | ✅ |
| Actualizar estado de espacios | ❌ | ✅ | ✅ |
| Ver reservas de sus estac. | ❌ | ✅ | ✅ |
| Gestionar todos los usuarios | ❌ | ❌ | ✅ |
| Crear estacionamientos | ❌ | ❌ | ✅ |
| Ver estadísticas globales | ❌ | ❌ | ✅ |

### Implementación en Spring Security

```java
// SecurityConfig.java
http.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    .requestMatchers("/api/operator/**").hasAnyRole("ADMIN", "OPERADOR")
    .anyRequest().authenticated()
);
```

### Acceso horizontal (Horizontal Access Control)

Prevención de que un usuario acceda a recursos de otro:

```java
// VehicleService.java
public VehicleResponse getVehicle(Long vehicleId, Long authenticatedUserId) {
    Vehicle vehicle = vehicleRepository.findById(vehicleId)
        .orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado"));
    
    // Verificación de ownership
    if (!vehicle.getUser().getId().equals(authenticatedUserId)) {
        throw new UnauthorizedException("No tienes permiso para acceder a este recurso");
    }
    return mapper.toResponse(vehicle);
}
```

---

## Contraseñas — BCrypt

- **Factor de coste**: 10 (balance entre seguridad y rendimiento)
- Las contraseñas nunca se almacenan en texto plano
- Las contraseñas nunca se retornan en respuestas API
- Los logs nunca incluyen contraseñas

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
}
```

---

## Validación de entradas

### Backend (Bean Validation)

```java
public class RegisterRequest {
    @NotBlank @Email
    private String email;
    
    @NotBlank @Size(min = 8, max = 100)
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$",
             message = "La contraseña debe contener mayúsculas, minúsculas y números")
    private String password;
    
    @NotBlank @Size(min = 2, max = 100)
    private String firstName;
}
```

### Android (validación en cliente)

Los formularios en Android validan antes de enviar, pero **la validación del servidor es la autoritativa**. La validación en cliente es solo UX.

---

## Protección contra ataques comunes

| Ataque | Mitigación |
|--------|-----------|
| SQL Injection | JPA/Hibernate con queries parametrizados |
| XSS | API REST retorna JSON, no HTML; Spring Security headers |
| CSRF | Deshabilitado (API stateless con JWT, no cookies de sesión) |
| Brute Force | (Recomendado en producción: rate limiting con Redis) |
| Token theft | HTTPS en producción; tokens de corta duración |
| Broken Object Level Auth | Verificación de ownership en capa de servicio |
| Sensitive Data Exposure | BCrypt para contraseñas; no retornar password_hash; HTTPS |

---

## CORS

```java
// CorsConfig.java
@Configuration
public class CorsConfig {
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // En desarrollo: permitir el emulador Android
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
```

> ⚠️ En producción, `allowedOriginPatterns` debe restringirse al dominio específico.

---

## Gestión de secretos

### Variables de entorno requeridas

| Variable | Descripción | Ejemplo (NO usar en producción) |
|----------|-------------|--------------------------------|
| `DB_URL` | URL de conexión MySQL | `jdbc:mysql://localhost:3306/parkeo_db` |
| `DB_USERNAME` | Usuario MySQL | `antwny` |
| `DB_PASSWORD` | Contraseña MySQL | (usar variable real) |
| `JWT_SECRET` | Clave secreta JWT (mínimo 256 bits) | (generar aleatoriamente) |
| `GOOGLE_MAPS_API_KEY` | API Key de Google Maps | (obtener de Google Cloud Console) |

### Reglas

1. **NUNCA** hardcodear secretos en código fuente
2. **NUNCA** hacer commit de `.env`, `local.properties`, o archivos de credenciales
3. Los archivos `.gitignore` cubren todos los archivos sensibles
4. En producción, usar secretos gestionados (AWS Secrets Manager, GCP Secret Manager, etc.)

---

## Headers de seguridad HTTP

Spring Security agrega automáticamente:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `X-XSS-Protection: 0` (moderno)
- `Cache-Control: no-cache, no-store, max-age=0, must-revalidate`

---

## Auditoría

La tabla `audit_log` registra eventos sensibles:
- Login exitoso
- Intento de login fallido
- Creación/cancelación de reservas
- Cambios de contraseña
- Modificaciones de usuarios por admin

Los logs no incluyen contraseñas, tokens completos ni información de tarjetas.

---

## Checklist de seguridad para producción

- [ ] Cambiar JWT_SECRET a una clave aleatoria de 512 bits
- [ ] Configurar HTTPS con certificado válido (Let's Encrypt)
- [ ] Restringir CORS al dominio de producción
- [ ] Configurar rate limiting (Redis + Bucket4j)
- [ ] Revocar tokens de desarrollo
- [ ] Cambiar contraseñas de usuarios de prueba o desactivarlos
- [ ] Configurar firewall para que MySQL no sea accesible públicamente
- [ ] Habilitar binary logging de MySQL para auditoría
- [ ] Configurar alerts de seguridad
- [ ] Revisar permisos del usuario MySQL de producción
