# =============================================================================
# PARKeo · Android
# Archivo: app/proguard-rules.pro
# Tipo/capa: reglas de ofuscación, optimización y reducción de código (R8/ProGuard).
# Propósito: indicar a R8 qué clases, atributos y miembros deben conservarse
#            cuando se active la minificación de la variante release. Sin estas
#            reglas, la reflexión y la serialización podrían romperse al renombrar
#            clases o eliminar metadatos.
# Conceptos clave: R8 (motor actual de Android), ProGuard (formato de reglas),
#            `-keep`, `-keepattributes`, ofuscación, eliminación de código muerto
#            y preservación de anotaciones/firmas.
# Relación: `app/build.gradle.kts` referencia este archivo en `proguardFiles`.
#            Actualmente `isMinifyEnabled = false`, por lo que las reglas no se
#            aplican; están preparadas para cuando se habilite la reducción. Los
#            DTOs de red y los modelos de dominio son sensibles porque
#            kotlinx.serialization y Retrofit trabajan con ellos por reflexión/
#            metadatos.
# Guía: docs/GUIA_MAESTRA_ESTUDIANTE.md
# =============================================================================

# -----------------------------------------------------------------------------
# DTOs de la capa remota (`pe.parkeo.data.remote.dto`).
# Se conservan todas sus clases y miembros para que los nombres de propiedades
# sigan coincidiendo con las claves JSON del backend y para que los serializadores
# generados encuentren los campos. `{ *; }` incluye campos, métodos y clases
# internas del paquete.
# -----------------------------------------------------------------------------
-keep class pe.parkeo.data.remote.dto.** { *; }

# -----------------------------------------------------------------------------
# Modelos de dominio (`pe.parkeo.domain.model`).
# La UI y los repositorios los usan como datos tipados; conservarlos evita que R8
# elimine o renombre miembros que podrían mapearse desde JSON o exponerse en
# `StateFlow`. Es una regla amplia: si se activa la minificación conviene revisar
# si todos los modelos necesitan quedar intactos.
# -----------------------------------------------------------------------------
-keep class pe.parkeo.domain.model.** { *; }

# -----------------------------------------------------------------------------
# Metadatos que R8 no debe eliminar:
# - `*Annotation*`: anotaciones de Retrofit, kotlinx.serialization, Room, etc.
# - `Signature`: firmas genéricas usadas por conversores y reflexión.
# - `InnerClasses` y `EnclosingMethod`: relaciones entre clases internas que
#   algunas librerías consultan en tiempo de ejecución.
# -----------------------------------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# -----------------------------------------------------------------------------
# Reglas recomendadas si en el futuro se activa `isMinifyEnabled = true`:
# - Retrofit: conservar las interfaces de API y sus métodos anotados; las
#   versiones modernas incluyen reglas de consumidor, pero conviene verificarlas.
# - kotlinx.serialization: conservar los `Companion`/serializadores generados de
#   las clases `@Serializable` si no se usan las reglas de consumidor.
# - Google Maps y WorkManager: revisar sus reglas de consumidor y añadir
#   `-dontwarn` solo para dependencias opcionales que no se usen.
# - Añadir `-keepnames` para clases que se referencien por nombre desde XML,
#   `AndroidManifest.xml` o `PendingIntent`.
# No se añaden aquí porque hoy la minificación está desactivada y no deben
# introducirse reglas sin validar el comportamiento de la app release.
# -----------------------------------------------------------------------------
