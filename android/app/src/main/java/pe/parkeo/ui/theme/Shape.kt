/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/theme/Shape.kt`
 * Capa: sistema de diseño (formas y radios de esquina).
 * Propósito: centralizar los radios de esquina que Material 3 expone mediante `Shapes` y añadir
 *   formas especializadas de PARKeo para tarjetas, campos, botones, etiquetas, hojas y píldoras.
 * Conceptos clave: `Shapes`, `RoundedCornerShape`, `dp` (píxeles independientes de densidad),
 *   jerarquía visual, consistencia entre componentes y esquinas asimétricas.
 * Flujo de uso: `Theme.kt` entrega `Shapes` a `MaterialTheme.shapes`; los componentes usan las
 *   formas especializadas (`ParkeoCardShape`, `ParkeoInputShape`, etc.) para mantener el mismo
 *   lenguaje visual aunque no pasen por un control Material.
 * Relación con pantallas/tema: las tarjetas de estacionamiento, los formularios y las hojas de
 *   detalle comparten estos radios. Cambiar un valor aquí redondea de forma coherente todos los
 *   componentes que lo referencian.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Escala estándar de formas de Material 3 adaptada a PARKeo. Los radios crecen con la jerarquía
 * del componente: los controles pequeños son casi rectos y los contenedores grandes son más
 * redondeados. Todos los valores están en `dp` para verse igual en distintas densidades.
 */
val Shapes = Shapes(
    // 4 dp: indicadores, miniaturas y controles muy pequeños.
    extraSmall = RoundedCornerShape(4.dp),
    // 8 dp: chips, etiquetas y contenedores compactos.
    small = RoundedCornerShape(8.dp),
    // 12 dp: controles estándar y agrupaciones medianas.
    medium = RoundedCornerShape(12.dp),
    // 16 dp: tarjetas y superficies principales.
    large = RoundedCornerShape(16.dp),
    // 24 dp: hojas, diálogos y contenedores de máxima jerarquía.
    extraLarge = RoundedCornerShape(24.dp)
)

// Specialized Parkeo shapes
/**
 * Forma de etiquetas y badges: 6 dp. Es más contenida que `Shapes.small` para que una etiqueta
 * pequeña no parezca una píldora y conserve una lectura de “chip” compacto.
 */
val ParkeoTagShape = RoundedCornerShape(6.dp)
/**
 * Forma de tarjeta de contenido: 14 dp. Un radio intermedio entre `medium` y `large` que suaviza
 * las tarjetas sin llegar al aspecto de hoja.
 */
val ParkeoCardShape = RoundedCornerShape(14.dp)
/**
 * Forma de campos de entrada: 10 dp. Se comparte entre el contenedor y el borde para que ambos
 * curven exactamente igual.
 */
val ParkeoInputShape = RoundedCornerShape(10.dp)
/**
 * Forma de botones: 10 dp. Mantiene la misma familia visual que los campos y evita que el botón
 * parezca una píldora completa.
 */
val ParkeoButtonShape = RoundedCornerShape(10.dp)
/**
 * Forma de hoja inferior (bottom sheet): solo las esquinas superiores se redondean a 20 dp; las
 * inferiores quedan a 0 dp porque la hoja nace desde el borde de la pantalla.
 */
val ParkeoSheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
/**
 * Forma de píldora: `RoundedCornerShape(50)` interpreta el argumento como porcentaje, no como
 * `dp`, y produce extremos totalmente redondeados. Se usa en indicadores y fondos de selección.
 */
val ParkeoPillShape = RoundedCornerShape(50)
