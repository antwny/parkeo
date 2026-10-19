/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/theme/Color.kt`
 * Capa: sistema de diseño (tokens de color).
 * Propósito: definir la paleta cruda de PARKeo y sus alias heredados. Estos valores son la
 *   fuente de verdad que `Theme.kt` traduce a `ColorScheme` de Material 3 y a
 *   `ParkeoExtendedColors`; los componentes no deberían inventar colores fuera de este archivo.
 * Conceptos clave: token de color, `Color`, `ColorScheme`, `MaterialTheme`, tema claro/oscuro,
 *   contraste, canales alfa, superficies por capas y color semántico de estado.
 * Flujo de uso: este archivo declara valores -> `Theme.kt` los agrupa en esquemas claro/oscuro ->
 *   `MaterialTheme.colorScheme` y `ParkeoTheme.colors` los exponen -> las pantallas y los
 *   componentes los consumen. Cambiar un token aquí afecta a toda la app de forma coherente.
 * Relación con pantallas/tema: las pantallas usan `ParkeoTheme.colors` para tokens extendidos
 *   (acento lima, superficies, bordes, textos) y `MaterialTheme.colorScheme` para controles
 *   Material. Los colores de estado se reutilizan en badges, tarjetas y barras.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.theme

import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════════════════════════
// Parkeo DESIGN SYSTEM — CORE PALETTE
// Signature Lime + Obsidian Dark Surfaces + Crisp Titanium Light Surfaces
// ════════════════════════════════════════════════════════════════════════════════

/**
 * Acento de marca principal: lima eléctrico. En tema oscuro es el color de las acciones
 * primarias (botones, foco, indicadores); en tema claro se usa sobre todo como relleno de
 * contenedores y detalles, porque el texto lima sobre fondo claro perdería contraste.
 */
val ParkeoLime           = Color(0xFFD4FF00) // Hero action color
/**
 * Variante más luminosa del lima. Se emplea como `onPrimaryContainer` en tema oscuro y como
 * color de realce cuando el acento necesita destacar sobre una superficie ya teñida de lima.
 */
val ParkeoLimeBright     = Color(0xFFDDFF24)
/**
 * Lima atenuado para tema claro. Al reducir la luminosidad mantiene la identidad de marca pero
 * mejora el contraste del texto oscuro que se coloca encima.
 */
val ParkeoLimeDim        = Color(0xFFA6CC00)
/**
 * Lima translúcido al 20 % de alfa (`0x33`). Se usa para halos, sombras de color o fondos
 * suaves que deben dejar ver la superficie inferior.
 */
val ParkeoLimeGlow       = Color(0x33D4FF00) // 20% alpha
/**
 * Lima translúcido al 10 % de alfa (`0x1A`). Es el contenedor estándar del acento: fondos de
 * chips, tarjetas resaltadas y estados seleccionados sin competir con el contenido.
 */
val ParkeoLimeContainer  = Color(0x1AD4FF00) // 10% alpha
/**
 * Texto e iconos casi negros que se colocan encima del lima. El contraste alto es intencional:
 * el lima es muy luminoso y un texto claro resultaría ilegible.
 */
val OnParkeoLime         = Color(0xFF0A0A0A) // Ultra high-contrast dark text/icon on lime

// Obsidian Layered Surfaces (Dark Theme Core)
/**
 * Negro profundo base del tema oscuro. Funciona como fondo absoluto y como `background` del
 * `ColorScheme` oscuro.
 */
val ParkeoObsidian       = Color(0xFF060606) // Absolute deep void
/**
 * Superficie de nivel 0 del tema oscuro: fondo de scaffolds y contenedores de pantalla completa.
 * Es apenas más clara que `ParkeoObsidian` para separar la estructura del fondo absoluto.
 */
val ParkeoDarkSurface0   = Color(0xFF0C0C0C) // Scaffolds
/**
 * Superficie de nivel 1 del tema oscuro: tarjetas y hojas (sheets). Es la elevación visual
 * estándar para agrupar contenido sobre el scaffold.
 */
val ParkeoDarkSurface1   = Color(0xFF131313) // Cards, Sheets
/**
 * Superficie de nivel 2 del tema oscuro: campos de entrada y contenedores internos. Al ser más
 * clara que la tarjeta, crea profundidad sin necesidad de sombras.
 */
val ParkeoDarkSurface2   = Color(0xFF1A1A1A) // Inputs, inner containers
/**
 * Superficie de nivel 3 del tema oscuro: estados hover/activos, chips seleccionados y diálogos.
 * Es el nivel más elevado de la escala de superficies oscuras.
 */
val ParkeoDarkSurface3   = Color(0xFF242424) // Hover, active chips, dialogs
/**
 * Borde principal del tema oscuro. Un gris muy cercano al fondo que dibuja límites nítidos sin
 * resultar pesado.
 */
val ParkeoDarkBorder     = Color(0xFF262626) // Crisp hairline border
/**
 * Borde secundario del tema oscuro para separadores y contornos de menor jerarquía, por ejemplo
 * el divisor inferior de una barra.
 */
val ParkeoDarkBorderSubtle = Color(0xFF1A1A1A) // Subtle separators

// Titanium Layered Surfaces (Light Theme Core)
/**
 * Fondo base del tema claro: un blanco roto con matiz frío (titánico) en lugar de blanco puro.
 * Evita el aspecto clínico y permite que las tarjetas blancas destaquen.
 */
val ParkeoTitaniumBg     = Color(0xFFF4F5F8) // Subtle off-white base (not plain white)
/**
 * Superficie de nivel 1 del tema claro: blanco puro para tarjetas y planos de contenido. Es el
 * mayor contraste disponible sobre el fondo titanio.
 */
val ParkeoLightSurface1  = Color(0xFFFFFFFF) // Pure white card planes
/**
 * Superficie de nivel 2 del tema claro: campos de entrada y pistas de controles segmentados.
 * Un gris azulado muy suave que se distingue del blanco de la tarjeta.
 */
val ParkeoLightSurface2  = Color(0xFFEBEFF4) // Inputs, segmented tracks
/**
 * Superficie de nivel 3 del tema claro: estados hover y chips. Es el gris más marcado de la
 * escala clara y se usa con moderación.
 */
val ParkeoLightSurface3  = Color(0xFFE0E5ED) // Hover, chips
/**
 * Borde principal del tema claro. Gris frío que delimita tarjetas y campos con una apariencia
 * arquitectónica y limpia.
 */
val ParkeoLightBorder    = Color(0xFFD4D9E2) // Architectural card border
/**
 * Borde secundario del tema claro para separadores de baja jerarquía.
 */
val ParkeoLightBorderSubtle = Color(0xFFE8ECF2)

// Typography & Content Neutral Contrast
/**
 * Texto primario del tema oscuro: blanco cálido casi puro. Se usa para títulos y contenido
 * principal sobre superficies obsidiana.
 */
val ParkeoTextWhite      = Color(0xFFF8F9FA) // 98% white
/**
 * Texto secundario del tema oscuro: gris claro. Aporta jerarquía en descripciones, metadatos y
 * etiquetas sin competir con el texto primario.
 */
val ParkeoTextGrayLight  = Color(0xFFA6ACB6) // Secondary dark mode
/**
 * Texto terciario del tema oscuro: gris medio. Se reserva para pistas, placeholders y datos de
 * menor importancia.
 */
val ParkeoTextGrayMuted  = Color(0xFF6B7280) // Tertiary dark mode
/**
 * Texto primario del tema claro: negro azulado. Máximo contraste sobre fondos claros.
 */
val ParkeoTextBlack      = Color(0xFF0D0E12) // Primary light mode
/**
 * Texto secundario del tema claro: gris pizarra. Se usa en descripciones y metadatos.
 */
val ParkeoTextSlate      = Color(0xFF4B5563) // Secondary light mode
/**
 * Texto terciario del tema claro: gris medio claro. Para placeholders y textos auxiliares.
 */
val ParkeoTextMutedLight = Color(0xFF8B94A2) // Tertiary light mode

// Semantic States (Mobility, Availability, Signals)
/**
 * Verde de estado disponible o éxito. Se usa en badges de disponibilidad, confirmaciones y
 * cualquier señal positiva del dominio de estacionamientos.
 */
val ParkeoGreen500       = Color(0xFF22C55E) // Disponible / Éxito
/**
 * Fondo translúcido al 10 % derivado del verde de éxito. Permite construir badges y avisos
 * positivos sin rellenar toda la superficie con color saturado.
 */
val ParkeoGreenContainer = Color(0x1A22C55E)
/**
 * Rojo de estado ocupado, error o acción destructiva. Es también el color de `error` en ambos
 * `ColorScheme`.
 */
val ParkeoRed500         = Color(0xFFFF3B30) // Ocupado / Error / Destructivo
/**
 * Fondo translúcido al 10 % del rojo de error, usado en badges y contenedores de alerta.
 */
val ParkeoRedContainer   = Color(0x1AFF3B30)
/**
 * Ámbar de estado reservado, pendiente o advertencia. Comunica una condición intermedia que
 * requiere atención pero no es un fallo.
 */
val ParkeoAmber500       = Color(0xFFFFB020) // Reservado / Pendiente / Advertencia
/**
 * Fondo translúcido al 10 % del ámbar de advertencia.
 */
val ParkeoAmberContainer = Color(0x1AFFB020)
/**
 * Gris de estado mantenimiento, cerrado o deshabilitado. Neutraliza la señal cuando no hay una
 * acción disponible.
 */
val ParkeoGray500        = Color(0xFF71717A) // Mantenimiento / Deshabilitado
/**
 * Fondo translúcido al 10 % del gris neutro.
 */
val ParkeoGrayContainer  = Color(0x1A71717A)
/**
 * Cian tecnológico usado como acento secundario en el tema oscuro y en badges de rol cliente.
 * Aporta una señal fría y moderna sin competir con el lima de marca.
 */
val ParkeoCyan500        = Color(0xFF00E5FF) // Tech mobility accent

// Pure Universal Neutrals
/**
 * Blanco puro universal. Se usa cuando se necesita contraste absoluto, por ejemplo texto sobre
 * un botón destructivo o sobre el azul oscuro.
 */
val White = Color(0xFFFFFFFF)
/**
 * Negro puro universal. Disponible para casos que requieran el extremo absoluto de la escala;
 * el sistema prefiere `ParkeoObsidian` y `ParkeoTextBlack` para mantener el matiz de marca.
 */
val Black = Color(0xFF000000)
/**
 * Alias del fondo claro. Facilita cambiar la base del tema claro sin recorrer todas las
 * referencias del proyecto.
 */
val SurfaceLight = ParkeoTitaniumBg
/**
 * Alias del fondo oscuro. Facilita cambiar la base del tema oscuro en un único punto.
 */
val SurfaceDark  = ParkeoObsidian

// ════════════════════════════════════════════════════════════════════════════════
// BACKWARD-COMPATIBLE FALLBACK ALIASES
// ════════════════════════════════════════════════════════════════════════════════
/**
 * Alias heredado (azul 900) conservado para pantallas antiguas. No forma parte de la paleta
 * nueva; si se crea UI desde cero, debe preferirse `ParkeoObsidian` o `ParkeoDarkSurface0`.
 */
val ParkeoBlue900 = Color(0xFF0D121F)
/**
 * Alias heredado (azul 800). Se mantiene por compatibilidad con código existente.
 */
val ParkeoBlue800 = Color(0xFF121A2C)
/**
 * Alias heredado que redirige las acciones primarias al lima de marca. Evita que pantallas
 * antiguas sigan pintando azul cuando el sistema ya adoptó el lima.
 */
val ParkeoBlue700 = ParkeoLime       // Primary actions map to signature Lime
/**
 * Alias heredado del lima brillante, usado por componentes anteriores al rediseño.
 */
val ParkeoBlue600 = ParkeoLimeBright
/**
 * Alias heredado de un azul cielo. Se conserva para compatibilidad; el acento secundario actual
 * es `ParkeoCyan500`.
 */
val ParkeoBlue500 = Color(0xFF38BDF8)
/**
 * Alias heredado de un azul cielo claro.
 */
val ParkeoBlue400 = Color(0xFF7DD3FC)
/**
 * Alias heredado que apunta al contenedor lima actual.
 */
val ParkeoBlue100 = ParkeoLimeContainer
/**
 * Alias heredado de un lima casi transparente (5 % de alfa). Se mantiene por compatibilidad.
 */
val ParkeoBlue50  = Color(0x0DD4FF00)
/**
 * Alias heredado de cian claro. Preferir `ParkeoCyan500` en código nuevo.
 */
val ParkeoCyan400 = Color(0xFF38E1FF)
/**
 * Alias heredado del cian translúcido al 10 %.
 */
val ParkeoCyan100 = Color(0x1A00E5FF)
/**
 * Alias heredado que apunta a la superficie oscura de nivel 2.
 */
val ParkeoGray800 = ParkeoDarkSurface2
/**
 * Alias heredado que apunta a la superficie oscura de nivel 1.
 */
val ParkeoGray900 = ParkeoDarkSurface1
