// =============================================================================
// PARKeo · Android
// Archivo: app/build.gradle.kts
// Tipo/capa: configuración del módulo `:app` (build, variantes y dependencias).
// Propósito: describir cómo se compila y empaqueta la aplicación Android:
//            identidad, niveles de SDK, versión, claves de configuración,
//            variantes debug/release, Java 17, Compose y todas las librerías
//            que sostienen la arquitectura UI -> ViewModels -> repositorios.
// Conceptos clave: `namespace` frente a `applicationId`, `compileSdk`/`minSdk`/
//            `targetSdk`, `versionCode`/`versionName`, `manifestPlaceholders`,
//            `buildConfigField`, `buildTypes`, R8/ProGuard, `compileOptions`,
//            `buildFeatures`, BOM de Compose y alcances de dependencias
//            (`implementation`, `testImplementation`, `androidTestImplementation`,
//            `debugImplementation`).
// Relación: consume el catálogo `gradle/libs.versions.toml`, los plugins
//            declarados en el build raíz y los valores de `local.properties`.
//            Genera `BuildConfig.BASE_URL`, que `AppContainer` usa para Retrofit,
//            y el placeholder `MAPS_API_KEY`, que el AndroidManifest inyecta en
//            Google Maps. La app se organiza en `pe.parkeo` con DI manual,
//            Navigation Compose y sesión en DataStore, tal como explica la guía.
// Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
// =============================================================================

// `java.util.Properties` permite leer pares clave-valor de `local.properties`
// sin dependencias externas. Se usa para no versionar secretos ni URLs locales.
import java.util.Properties

// -----------------------------------------------------------------------------
// Plugins del módulo. Aquí sí se aplican (sin `apply false`): AGP aporta el
// bloque `android`, el plugin de Compose habilita el compilador de composables y
// el de serialización genera los serializadores de Kotlin usados en la red.
// -----------------------------------------------------------------------------
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// -----------------------------------------------------------------------------
// Lectura de `local.properties`.
// Es un archivo local, normalmente ignorado por Git, que contiene la clave de
// Google Maps y la URL del backend. Si no existe, el bloque no falla: los
// valores por defecto se aplican más abajo. Se lee en tiempo de configuración
// para poder inyectar los valores en `BuildConfig` y en el manifiesto.
// -----------------------------------------------------------------------------
val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        load(localPropertiesFile.inputStream())
    }
}

android {
    // `namespace` define el paquete de la clase `R` y de `BuildConfig` generados.
    // No es lo mismo que `applicationId`: este último identifica la app ante el
    // sistema y las tiendas. Aquí ambos coinciden en `pe.parkeo`.
    namespace = "pe.parkeo"

    // SDK con el que se compila el código: Android 15 (API 35). Da acceso a las
    // APIs y reglas de recursos de esa versión; no implica que la app solo
    // funcione en Android 15.
    compileSdk = 35

    defaultConfig {
        // Identificador único de la aplicación en el dispositivo y en Google
        // Play. Cambiarlo crea una app distinta y afecta a las claves de Maps.
        applicationId = "pe.parkeo"

        // Android 8.0 (API 26) es el mínimo soportado. Permite usar APIs
        // modernas y reducir ramas de compatibilidad; el proyecto no necesita
        // soportar versiones anteriores.
        minSdk = 26

        // SDK objetivo: Android 15 (API 35). Indica al sistema qué reglas de
        // comportamiento y permisos aplicar; debe ser coherente con `compileSdk`.
        targetSdk = 35

        // `versionCode` es un entero interno que debe crecer con cada publicación;
        // `versionName` es la cadena visible para el usuario. Para una entrega
        // nueva se incrementa el primero y, si corresponde, se actualiza el segundo.
        versionCode = 1
        versionName = "1.0.0"

        // Runner de JUnit para pruebas instrumentadas. AndroidX Test lo usa para
        // ejecutar los tests en un dispositivo o emulador.
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // `manifestPlaceholders` sustituye `${MAPS_API_KEY}` en el manifiesto.
        // La clave real se lee de `local.properties`; si falta, se inyecta una
        // cadena vacía para que el build no falle, aunque el mapa no podrá
        // autenticarse. Nunca debe escribirse una clave real en el repositorio.
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY") ?: ""

        // URL base del backend para los build types que no la sobrescriban
        // (principalmente debug). El valor de `local.properties` permite apuntar
        // a la IP de la máquina de desarrollo; si falta, se usa una IP de LAN de
        // ejemplo. Retrofit exige que termine en `/`.
        val backendUrl = localProperties.getProperty("BACKEND_URL") ?: "http://192.168.18.8:8080/"

        // Crea `BuildConfig.BASE_URL`. Las comillas escapadas son necesarias para
        // que el valor generado sea un `String` de Kotlin/Java válido. `AppContainer`
        // lo pasa a `Retrofit.Builder().baseUrl(...)`.
        buildConfigField("String", "BASE_URL", "\"$backendUrl\"")
    }

    // -------------------------------------------------------------------------
    // Variantes de compilación.
    // `debug` existe de forma implícita con depuración activada. Solo se
    // personaliza `release`, que es la variante distribuible.
    // -------------------------------------------------------------------------
    buildTypes {
        release {
            // R8/ProGuard desactivado por ahora: no se elimina código ni recursos.
            // Las reglas de `proguard-rules.pro` quedan preparadas para cuando se
            // active la minificación; con `false` no se aplican.
            isMinifyEnabled = false

            // Reglas de optimización por defecto de Android más las reglas
            // propias del proyecto. `getDefaultProguardFile` apunta al archivo
            // estándar de AGP que conoce las APIs de Android.
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            // En release se fuerza la URL HTTPS de producción, independientemente
            // de `local.properties`. Así una build publicada no apunta al backend
            // local ni viaja con tráfico claro.
            buildConfigField("String", "BASE_URL", "\"https://api.parkeo.pe/\"")
        }
    }

    // -------------------------------------------------------------------------
    // Nivel de Java del bytecode.
    // Se fija en 17 tanto para el código fuente como para el bytecode generado.
    // AGP 9 y las versiones recientes de AndroidX/Kotlin requieren una JVM
    // moderna; además, Java 17 es el nivel con el que se ejecuta el toolchain
    // del proyecto. Esto no cambia el `minSdk` ni el lenguaje Kotlin.
    // -------------------------------------------------------------------------
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // -------------------------------------------------------------------------
    // Características del build.
    // `compose = true` activa el soporte de Compose y se complementa con el
    // plugin `kotlin-compose`. `buildConfig = true` genera la clase `BuildConfig`;
    // AGP moderno la desactiva por defecto, y aquí se necesita para leer
    // `BuildConfig.BASE_URL` y `BuildConfig.DEBUG`.
    // -------------------------------------------------------------------------
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// -----------------------------------------------------------------------------
// Dependencias del módulo.
// El orden está agrupado por responsabilidad para facilitar la lectura; las
// versiones se resuelven en `gradle/libs.versions.toml`. `implementation` no
// expone la librería a otros módulos, lo que mantiene encapsulada la app.
// -----------------------------------------------------------------------------
dependencies {
    // Núcleo, ciclo de vida y Activity: utilidades Kotlin de AndroidX, soporte de
    // ciclo de vida con corrutinas, ViewModels observables desde Compose y el
    // `setContent`/edge-to-edge de `MainActivity`.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Compose: la BOM fija las versiones del conjunto y evita incompatibilidades.
    // `ui` y `ui-graphics` aportan el motor y el dibujo; `tooling-preview` permite
    // previsualizar; `material3` implementa los componentes; los iconos ampliados
    // cubren la iconografía de la app. `ui-tooling` completo solo entra en debug.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended)

    // Navegación: `NavHost`, rutas y transiciones entre las pantallas de cliente,
    // operador y administrador.
    implementation(libs.androidx.navigation.compose)

    // Persistencia local de la sesión: DataStore Preferences guarda token y datos
    // básicos del usuario de forma asíncrona, sin base de datos relacional.
    implementation(libs.androidx.datastore.preferences)

    // Red: Retrofit define las APIs tipadas, OkHttp ejecuta las peticiones y su
    // interceptor registra el tráfico en debug; el conversor de kotlinx.serialization
    // transforma el JSON del backend Spring Boot. `kotlinx-serialization-json`
    // proporciona el motor de serialización y los DTOs anotados.
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Mapas y ubicación: SDK nativo de Google Maps, composable `GoogleMap` y
    // `FusedLocationProviderClient` para obtener la posición del usuario.
    implementation(libs.google.maps)
    implementation(libs.maps.compose)
    implementation(libs.google.location)

    // Corrutinas y carga de imágenes: los dispatchers sostienen las llamadas de
    // red y los `StateFlow`; Coil descarga y cachea imágenes en Compose.
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)

    // Pruebas locales en la JVM con JUnit 4.
    testImplementation(libs.junit)

    // Pruebas instrumentadas en dispositivo/emulador: AndroidX JUnit como base,
    // Espresso para UI clásica, la BOM de Compose para alinear versiones y
    // `ui-test-junit4` para las reglas y finders de Compose.
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Herramientas exclusivas de debug: inspección de composables y manifiesto
    // auxiliar para tests de Compose. No se incluyen en la build release.
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // WorkManager: programa los recordatorios de reserva que ejecuta
    // `ReservationReminderWorker`, incluso si la app no está en primer plano.
    implementation(libs.androidx.work.runtime.ktx)
}
