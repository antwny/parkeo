// =============================================================================
// PARKeo · Android
// Archivo: settings.gradle.kts
// Tipo/capa: configuración de Gradle a nivel de proyecto (build raíz).
// Propósito: indicar de dónde se resuelven los plugins y las dependencias, y
//            declarar los módulos que participan en la compilación. Gradle lee
//            este archivo antes que cualquier build.gradle.kts.
// Conceptos clave: `pluginManagement`, `dependencyResolutionManagement`,
//            `RepositoriesMode.FAIL_ON_PROJECT_REPOS`, catálogo de versiones
//            (`gradle/libs.versions.toml`) e inclusión de módulos.
// Relación: el único módulo, `:app`, aplica los plugins resueltos aquí y consume
//            las dependencias versionadas en el catálogo. Sobre ese módulo se
//            apoya la arquitectura UI Compose -> ViewModels -> repositorios ->
//            Retrofit/backend Spring Boot descrita en la guía.
// Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
// =============================================================================

// -----------------------------------------------------------------------------
// Gestión de plugins del build.
// Los plugins (AGP, Compose Compiler, serialización) se buscan primero en estos
// repositorios. `google()` publica AGP y AndroidX; `mavenCentral()` aloja
// Kotlin y librerías JVM; `gradlePluginPortal()` es el portal oficial de
// plugins de Gradle. Centralizar aquí los repositorios evita repetirlos en cada
// módulo y garantiza que todos resuelvan las mismas coordenadas.
// -----------------------------------------------------------------------------
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// -----------------------------------------------------------------------------
// Resolución de dependencias de todos los módulos.
// `FAIL_ON_PROJECT_REPOS` obliga a que las dependencias se busquen únicamente
// en los repositorios declarados en este bloque: si un build.gradle.kts de
// módulo intenta añadir su propio `repositories { ... }`, el build falla. Esto
// evita resoluciones distintas entre módulos y facilita la reproducibilidad.
// -----------------------------------------------------------------------------
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Nombre lógico del build raíz; se usa en el IDE, en informes y en la ruta de
// algunos artefactos. No es el `applicationId` ni el nombre visible de la app.
rootProject.name = "Parkeo"

// Declara el módulo Android que contiene el código, los recursos y el
// AndroidManifest de PARKeo. La ruta física es `android/app` y su configuración
// vive en `app/build.gradle.kts`.
include(":app")
