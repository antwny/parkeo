# PARKeo — Guía de Instalación

## Requisitos previos

| Herramienta | Versión mínima | Verificación |
|---|---|---|
| Java (Temurin) | 21 | `java -version` |
| Maven | 3.9+ | `mvn --version` |
| MySQL | 8.0 | `mysql --version` |
| Android Studio | Ladybug+ | IDE |
| Android SDK | API 35 | Android Studio |
| Git | 2.x | `git --version` |

La app Android usa `compileSdk 35`, `targetSdk 35` y `minSdk 26`.

---

## 1. Ubicar el proyecto

```bash
cd ~/Documentos/parkeo
```

---

## 2. Configurar MySQL

### Opción A — MySQL local (recomendado para desarrollo)

Asegúrate de que el servicio esté activo:

```bash
sudo systemctl status mysql
sudo systemctl start mysql
```

Configura la base de datos con el script de setup:

```bash
bash scripts/setup.sh
```

El script:

1. Verifica Java, MySQL, Git y Maven.
2. Crea `parkeo_db` con `utf8mb4`.
3. Aplica `database/schema.sql`.
4. Carga `database/data.sql`.
5. Compila el backend (`mvn clean package -DskipTests`).

### Opción B — Docker Compose

```bash
docker compose up -d
```

- MySQL queda expuesto en el puerto **3307**.
- Base de datos: `parkeo_db`.
- Usuario: `parkeo_user`.
- Contraseña: valor de `MYSQL_PASSWORD` (por defecto `parkeo_password_change_me`).
- El backend se construye desde `backend/Dockerfile` y escucha en `8080`.

> Los scripts `schema.sql` y `data.sql` se montan en `/docker-entrypoint-initdb.d`, por lo que solo se ejecutan la primera vez que se crea el volumen. Para recargarlos, elimina el volumen con `docker compose down -v`.

### Credenciales locales usadas por los scripts

```
Host: localhost
Puerto: 3306
Base de datos: parkeo_db
Usuario: antwny
```

> ⚠️ Estas credenciales son de desarrollo. No las uses en producción.

### Reiniciar la base de datos

```bash
bash scripts/reset-db.sh
```

El script pide confirmación escribiendo `SI`, elimina `parkeo_db`, la recrea, aplica `schema.sql` y carga `data.sql`.

Si el backend estaba corriendo durante el reset, reinícialo para restablecer el pool de conexiones y el estado de Hibernate.

---

## 3. Variables de entorno del backend

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/parkeo_db?...` | URL JDBC |
| `DB_USERNAME` | `root` | Usuario MySQL |
| `DB_PASSWORD` | `mysql` | Contraseña MySQL |
| `JWT_SECRET` | secreto de desarrollo | Clave HMAC de al menos 256 bits |
| `JWT_EXPIRATION` | `86400000` | Vida del access token (24 h) |
| `JWT_REFRESH_EXPIRATION` | `604800000` | Vida del refresh token (7 días) |
| `SPRING_PROFILES_ACTIVE` | `dev` | Perfil de Spring |

Ejemplo:

```bash
export DB_URL="jdbc:mysql://localhost:3306/parkeo_db?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true"
export DB_USERNAME="antwny"
export DB_PASSWORD="TU_PASSWORD"
export JWT_SECRET="tu-clave-secreta-minimo-256-bits"
export JWT_EXPIRATION="86400000"
export JWT_REFRESH_EXPIRATION="604800000"
```

> En producción usa un `JWT_SECRET` aleatorio de al menos 32 bytes y credenciales distintas a las de desarrollo.

El backend lee `application.properties`:

- `spring.jpa.hibernate.ddl-auto=update`
- Puerto `8080`
- Swagger en `/swagger-ui.html`
- OpenAPI en `/api-docs`
- Actuator: `/actuator/health`

---

## 4. Iniciar el backend

### Con el script

```bash
bash scripts/start-backend.sh
```

### Manualmente

```bash
cd backend
mvn spring-boot:run
```

### Verificar

```bash
curl http://localhost:8080/actuator/health
curl "http://localhost:8080/api/parking?page=0&size=3"
```

Debe devolver `success: true` y una lista de estacionamientos con `availableSpaces` mayor que cero.

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

---

## 5. Configurar Android

### 5.1 Abrir el proyecto

Android Studio → File → Open → `~/Documentos/parkeo/android/`.

### 5.2 Crear `android/local.properties`

```properties
sdk.dir=/home/TU_USUARIO/Android/Sdk
MAPS_API_KEY=AIza...tu_clave
BACKEND_URL=http://10.0.2.2:8080/
```

> `local.properties` está en `.gitignore`.

### 5.3 Google Maps API Key

1. Entra a [Google Cloud Console](https://console.cloud.google.com/).
2. Crea o selecciona un proyecto.
3. Habilita **Maps SDK for Android**.
4. Crea una API Key y restríngela al paquete `pe.parkeo`.
5. Copia la clave en `MAPS_API_KEY`.

Sin una API Key válida, el mapa no carga y la app muestra un mensaje de error.

### 5.4 URL del backend según el dispositivo

| Dispositivo | `BACKEND_URL` |
|---|---|
| Emulador Android | `http://10.0.2.2:8080/` |
| Celular físico con `adb reverse` | `http://localhost:8080/` |
| Celular físico en la misma red | `http://IP_DE_TU_PC:8080/` |

Para un celular conectado por USB:

```bash
bash scripts/levantar-celular.sh
```

El script ejecuta `adb reverse tcp:8080 tcp:8080` y abre la app con `adb shell am start -n pe.parkeo/.MainActivity`.

Asegúrate de que `BACKEND_URL` en `android/local.properties` apunte a `http://localhost:8080/` cuando uses `adb reverse`.

### 5.5 Compilar y ejecutar

```bash
cd android
./gradlew assembleDebug
./gradlew installDebug
```

O usa el botón **Run** de Android Studio.

---

## 6. Cuentas de prueba

| Email | Password | Rol |
|---|---|---|
| admin@parkeo.pe | Password123! | ADMIN |
| operador@parkeo.pe | Password123! | OPERADOR |
| operador2@parkeo.pe | Password123! | OPERADOR |
| cliente@parkeo.pe | Password123! | CLIENTE |
| juan.perez@parkeo.pe | Password123! | CLIENTE |
| sofia.mendez@parkeo.pe | Password123! | CLIENTE |
| roberto.silva@parkeo.pe | Password123! | CLIENTE |

---

## 7. Verificación rápida

### Base de datos

```bash
mysql -u antwny -p parkeo_db -e "
SELECT COUNT(*) AS lotes FROM parking_lots;
SELECT COUNT(*) AS espacios FROM parking_spaces;
SELECT COUNT(*) AS reservas FROM reservations;
SELECT COUNT(*) AS usuarios FROM users;
"
```

Resultado esperado:

```
lotes: 6
espacios: 410
reservas: 6
usuarios: 7
```

### API

```bash
curl "http://localhost:8080/api/parking?page=0&size=3"
```

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"cliente@parkeo.pe","password":"Password123!"}'
```

Debe devolver `accessToken`, `refreshToken` y el usuario con rol `ROLE_USER`.

### Pruebas del backend

```bash
cd backend
mvn test
```

---

## 8. Solución de problemas

### `Unknown column 'operator_id'` al cargar datos

Causa: `schema.sql` desactualizado respecto a las entidades JPA.

Solución:

```bash
git pull origin main
bash scripts/reset-db.sh
```

Reinicia el backend después del reset.

### Los estacionamientos muestran 0 espacios disponibles

El contador `available_spaces` no coincide con los espacios reales. Verifica:

```bash
mysql -u antwny -p parkeo_db -e "
SELECT pl.name, pl.total_spaces, pl.available_spaces,
       SUM(ps.status = 'AVAILABLE') AS reales
FROM parking_lots pl
JOIN parking_spaces ps ON ps.parking_lot_id = pl.id
GROUP BY pl.id;
"
```

Recarga los datos de prueba con `bash scripts/reset-db.sh`.

### El backend no conecta a MySQL

- Verifica que MySQL esté activo: `sudo systemctl status mysql`.
- Si usas Docker, el puerto es **3307**, no 3306.
- Revisa `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

### El mapa aparece vacío

- Verifica `MAPS_API_KEY` en `local.properties`.
- Confirma que **Maps SDK for Android** esté habilitado.
- Revisa que la API Key no tenga restricciones incorrectas.

### La app no conecta al backend

- Emulador: usa `http://10.0.2.2:8080/`.
- Celular físico: ejecuta `adb reverse tcp:8080 tcp:8080` y usa `http://localhost:8080/`.
- Verifica que el backend esté escuchando: `curl http://localhost:8080/actuator/health`.

### Error 401 después de un tiempo

El access token expiró. La app intenta renovarlo automáticamente con el refresh token. Si el refresh token también expiró o fue revocado, vuelve a iniciar sesión.

### `local.properties` no existe

Créalo en `android/local.properties` con `sdk.dir`, `MAPS_API_KEY` y `BACKEND_URL`.

---

## 9. Comandos útiles

```bash
# Backend
cd backend && mvn spring-boot:run
cd backend && mvn test
cd backend && mvn clean package -DskipTests

# Base de datos
bash scripts/setup.sh
bash scripts/reset-db.sh

# Android
cd android && ./gradlew assembleDebug
cd android && ./gradlew installDebug

# Celular físico
bash scripts/levantar-celular.sh

# Flujo de demostración
bash scripts/demo_flow.sh
```
