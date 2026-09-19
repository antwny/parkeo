# PARKeo — Guía de Instalación y Configuración

## Requisitos del sistema

| Herramienta | Versión mínima | Instalación |
|-------------|----------------|-------------|
| Java (Temurin/OpenJDK) | 21 LTS | SDKMAN, apt, dnf |
| Apache Maven | 3.9+ | SDKMAN |
| MySQL | 8.0 | apt, dnf, oficial |
| Android Studio | Ladybug 2024+ | Sitio oficial |
| Android SDK | API 34 (Android 14) | Desde Android Studio |
| Git | 2.x | apt, dnf |

---

## 1. Clonar / Obtener el proyecto

El proyecto está en:
```bash
~/Documentos/Parkeo/
```

---

## 2. Base de datos MySQL

### 2.1 Crear la base de datos

```bash
mysql -u antwny -p198009 -e "CREATE DATABASE IF NOT EXISTS parkeo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
```

### 2.2 Aplicar el schema

```bash
mysql -u antwny -p198009 parkeo_db < ~/Documentos/Parkeo/database/schema.sql
```

### 2.3 Cargar datos de prueba

```bash
mysql -u antwny -p198009 parkeo_db < ~/Documentos/Parkeo/database/data.sql
```

### 2.4 Verificar

```bash
mysql -u antwny -p198009 parkeo_db -e "SHOW TABLES;"
```

### 2.5 Reiniciar (si necesitas empezar de cero)

```bash
bash ~/Documentos/Parkeo/scripts/reset-db.sh
```

---

## 3. Backend Spring Boot

### 3.1 Variables de entorno

Configura las variables antes de ejecutar:

```bash
export DB_URL="jdbc:mysql://localhost:3306/parkeo_db?useSSL=false&serverTimezone=America/Lima&allowPublicKeyRetrieval=true"
export DB_USERNAME="antwny"
export DB_PASSWORD="198009"
export JWT_SECRET="parkeo-dev-secret-key-min-256-bits-0123456789abcdefghijklmnopqrstuvwxyz"
```

### 3.2 Compilar

```bash
cd ~/Documentos/Parkeo/backend
mvn clean package -DskipTests
```

### 3.3 Ejecutar

**Opción A — Script automatizado:**
```bash
bash ~/Documentos/Parkeo/scripts/start-backend.sh
```

**Opción B — Maven directamente:**
```bash
cd ~/Documentos/Parkeo/backend
mvn spring-boot:run
```

**Opción C — JAR compilado:**
```bash
cd ~/Documentos/Parkeo/backend
java -jar target/parkeo-backend-*.jar
```

### 3.4 Verificar que está funcionando

```bash
curl http://localhost:8080/actuator/health
# {"status":"UP"}
```

Swagger UI: http://localhost:8080/swagger-ui.html

---

## 4. Aplicación Android

### 4.1 Abrir en Android Studio

1. Abre Android Studio
2. **File → Open**
3. Navega a `~/Documentos/Parkeo/android/`
4. Click **OK**
5. Espera a que Gradle sincronice

### 4.2 Configurar Google Maps API Key

1. Obtén tu API Key en [Google Cloud Console](https://console.cloud.google.com/google/maps-apis/overview)
   - Habilita: **Maps SDK for Android**
2. Abre o crea el archivo `~/Documentos/Parkeo/android/local.properties`
3. Agrega:
   ```properties
   sdk.dir=/home/antwny/Android/Sdk
   MAPS_API_KEY=AIza...tu_clave_aqui
   ```

> ⚠️ `local.properties` está en `.gitignore` — nunca se sube al repositorio.

### 4.3 Configurar URL del backend

El archivo `android/app/src/main/res/values/network.xml` (o `BuildConfig`) define la URL del backend:

- **Emulador Android**: `http://10.0.2.2:8080` (mapea a localhost del host)
- **Dispositivo físico**: IP de tu máquina, ej: `http://192.168.1.x:8080`

En `android/app/build.gradle.kts`:
```kotlin
buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
```

### 4.4 Ejecutar

1. Conecta un dispositivo Android o inicia un emulador (API 34+)
2. Asegúrate de que el backend esté corriendo
3. Click **Run** (▶) en Android Studio

---

## 5. Setup automatizado (todo en uno)

```bash
bash ~/Documentos/Parkeo/scripts/setup.sh
```

Este script:
1. Verifica las herramientas instaladas
2. Crea la base de datos
3. Aplica el schema
4. Carga datos de prueba
5. Compila el backend

---

## 6. Cuentas de prueba

> ⚠️ Solo para desarrollo. No usar en producción.

| Email | Password | Rol |
|-------|----------|-----|
| admin@parkeo.pe | Password123! | ADMIN |
| operador@parkeo.pe | Password123! | OPERADOR |
| cliente@parkeo.pe | Password123! | CLIENTE |
| juan.perez@parkeo.pe | Password123! | CLIENTE |

---

## 7. Estructura de carpetas del proyecto

```
~/Documentos/Parkeo/
├── android/         → Proyecto Android Studio
├── backend/         → Proyecto Maven/Spring Boot
├── database/
│   ├── schema.sql   → DDL de la base de datos
│   ├── data.sql     → Datos de prueba
│   └── README.md
├── docs/            → Documentación técnica
├── scripts/
│   ├── setup.sh
│   ├── start-backend.sh
│   └── reset-db.sh
├── .gitignore
├── docker-compose.yml
└── README.md
```

---

## 8. Solución de problemas comunes

### Backend no inicia: "Cannot connect to MySQL"

```bash
# Verificar que MySQL está corriendo
sudo systemctl status mysql

# Iniciar si está detenido
sudo systemctl start mysql

# Verificar credenciales
mysql -u antwny -p198009 -e "SELECT 1"
```

### Android: Google Maps no muestra el mapa

1. Verificar que `MAPS_API_KEY` está en `local.properties`
2. Verificar que la API **Maps SDK for Android** está habilitada en Google Cloud Console
3. Verificar que la App ID / package name coincide con la restricción de la API Key

### Android: No puede conectar al backend (Connection refused)

1. Verificar que el backend está corriendo: `curl http://localhost:8080/actuator/health`
2. Para emulador: verificar que la URL usa `10.0.2.2` (no `localhost`)
3. Para dispositivo físico: verificar IP de la máquina host

### Error de compilación del backend: "ddl-auto=validate"

Si el schema aún no existe en la BD:
```bash
# Ejecutar el schema primero
mysql -u antwny -p198009 parkeo_db < ~/Documentos/Parkeo/database/schema.sql

# Luego intentar compilar/ejecutar de nuevo
```

---

## 9. Configuración para producción (referencias futuras)

1. Cambiar `spring.jpa.show-sql=false` (ya está así por defecto)
2. Configurar HTTPS en Spring Boot o detrás de Nginx
3. Cambiar `jwt.secret` a una clave de 512 bits generada aleatoriamente:
   ```bash
   openssl rand -base64 64
   ```
4. Configurar variables de entorno reales (no hardcodeadas)
5. Desactivar Swagger en producción:
   ```properties
   springdoc.swagger-ui.enabled=false
   ```
6. Configurar el dominio real en CORS
7. Usar una URL HTTPS en la app Android
