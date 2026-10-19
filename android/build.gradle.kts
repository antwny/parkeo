// =============================================================================
// PARKeo · Android
// Archivo: build.gradle.kts (raíz de `android/`)
// Tipo/capa: configuración de build Gradle a nivel de proyecto.
// Propósito: declarar los plugins que estarán disponibles para los módulos y
//            fijar sus versiones en un único lugar, sin aplicarlos al proyecto
//            raíz. La configuración real de Android vive en `app/build.gradle.kts`.
// Conceptos clave: plugins con `apply false`, catálogo de versiones
//            (`libs.plugins.*`), separación entre resolución y aplicación de
//            plugins, y build multi-módulo.
// Relación: las versiones se resuelven desde `gradle/libs.versions.toml`; el
//            módulo `:app` vuelve a referenciar estos alias, esta vez sin
//            `apply false`, para activarlos. Este archivo no contiene código de
//            la app ni dependencias de ejecución.
// Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
// =============================================================================

plugins {
    // Plugin de Android para aplicaciones (AGP). Aporta el bloque `android { }`,
    // las tareas de compilación, empaquetado, recursos y variantes. Se declara
    // con `apply false` porque aquí solo se registra la versión; aplicarlo en la
    // raíz afectaría a un proyecto que no es un módulo Android.
    alias(libs.plugins.android.application) apply false

    // Plugin oficial de Compose Compiler para Kotlin. Habilita el compilador de
    // composables y debe aplicarse en el mismo módulo donde se activa
    // `buildFeatures.compose`. También se difiere al módulo `:app`.
    alias(libs.plugins.kotlin.compose) apply false

    // Plugin de serialización de Kotlin. Genera serializadores para las clases
    // anotadas con `@Serializable`, usados por kotlinx.serialization y por el
    // conversor de Retrofit. Se declara aquí para compartir la versión de Kotlin.
    alias(libs.plugins.kotlin.serialization) apply false
}
