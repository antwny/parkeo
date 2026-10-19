/*
 * PARKeo · Android
 * Archivo: `android/app/src/main/java/pe/parkeo/ui/theme/Dimens.kt`
 * Capa: sistema de diseño (dimensiones y espaciado).
 * Propósito: reunir los tamaños reutilizables de la interfaz para que márgenes, alturas, bordes
 *   e iconos no se escriban con números mágicos en cada pantalla. La escala favorece ritmo
 *   visual, accesibilidad táctil y consistencia entre componentes.
 * Conceptos clave: token de dimensión, `dp` (píxeles independientes de densidad), `sp` (texto
 *   escalable, usado en `Type.kt`), aliases semánticos, área táctil mínima y jerarquía espacial.
 * Flujo de uso: los componentes y pantallas importan `Dimens` y eligen un token (`Dimens.space16`,
 *   `Dimens.buttonHeight`, etc.) en lugar de un literal. `Theme.kt` no transforma estos valores;
 *   se consumen directamente.
 * Relación con pantallas/tema: las tarjetas usan `space*` para padding, los botones usan
 *   `buttonHeight*`, los campos `inputHeight`, y las barras/íconos `iconSize*`. `sp` pertenece a
 *   la tipografía, no a este objeto, porque escala con la preferencia de texto del usuario.
 * Guía: `docs/GUIA_MAESTRA_ESTUDIANTE.md`.
 */
package pe.parkeo.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Catálogo de dimensiones de PARKeo. Todos los valores son `Dp` y representan medidas de
 * diseño, no tamaños de fuente. La escala de espaciado avanza en pasos de 2, 4, 6, 8, 10, 12,
 * 14, 16, 20, 24, 32, 40 y 48 para cubrir desde separaciones finas hasta márgenes de pantalla.
 */
object Dimens {
    // Spacing
    /** Separación mínima de 2 dp: ajustes ópticos entre elementos muy pequeños. */
    val space2 = 2.dp
    /** Separación de 4 dp: espacio interno de badges y separaciones finas. */
    val space4 = 4.dp
    /** Separación de 6 dp: huecos entre icono y texto en etiquetas compactas. */
    val space6 = 6.dp
    /** Separación de 8 dp: ritmo base para agrupar elementos relacionados. */
    val space8 = 8.dp
    /** Separación de 10 dp: hueco entre icono inicial y texto en campos. */
    val space10 = 10.dp
    /** Separación de 12 dp: padding interno de tarjetas compactas y listas. */
    val space12 = 12.dp
    /** Separación de 14 dp: ajustes intermedios entre bloques. */
    val space14 = 14.dp
    /** Separación de 16 dp: padding estándar de tarjetas y contenido de pantalla. */
    val space16 = 16.dp
    /** Separación de 20 dp: separación entre bloques principales. */
    val space20 = 20.dp
    /** Separación de 24 dp: márgenes amplios y distancia antes de una acción. */
    val space24 = 24.dp
    /** Separación de 32 dp: margen de estados vacíos/error y secciones de nivel de pantalla. */
    val space32 = 32.dp
    /** Separación de 40 dp: márgenes grandes o separaciones de bloques hero. */
    val space40 = 40.dp
    /** Separación de 48 dp: espacio máximo de la escala, para composiciones amplias. */
    val space48 = 48.dp

    // Semantic Spacing Aliases
    /** Alias semántico de espacio extra pequeño: equivale a `space4` (4 dp). */
    val spacingXs = space4
    /** Alias semántico de espacio pequeño: equivale a `space8` (8 dp). */
    val spacingSm = space8
    /** Alias semántico de espacio medio: equivale a `space16` (16 dp). */
    val spacingMd = space16
    /** Alias semántico de espacio grande: equivale a `space20` (20 dp). */
    val spacingLg = space20
    /** Alias semántico de espacio extra grande: equivale a `space24` (24 dp). */
    val spacingXl = space24
    /** Alias semántico de espacio doble extra grande: equivale a `space32` (32 dp). */
    val spacingXxl = space32

    // Border strokes
    /**
     * Borde capilar de 0.75 dp: separadores y contornos muy finos. Puede redondearse a un
     * píxel físico según la densidad de la pantalla, por eso se usa en divisores discretos.
     */
    val borderHairline = 0.75.dp
    /** Borde estándar de 1 dp: contorno de tarjetas, badges y campos en reposo. */
    val borderThin = 1.dp
    /** Borde de 1.5 dp: refuerza el foco o el error de un campo sin duplicar el grosor. */
    val borderFocus = 1.5.dp
    /** Borde de 2 dp: énfasis máximo, por ejemplo un contorno seleccionado. */
    val borderThick = 2.dp

    // Component standard heights
    /**
     * Altura base de botón: 50 dp. Supera el mínimo táctil recomendado de 48 dp y deja espacio
     * para texto e iconos.
     */
    val buttonHeight = 50.dp
    /**
     * Alias explícito de la altura de botón por defecto (50 dp). Se conserva para que el código
     * pueda expresar “tamaño estándar” sin depender del nombre `buttonHeight`.
     */
    val buttonHeightDefault = 50.dp
    /** Altura de botón grande: 52 dp, para acciones principales que necesitan más presencia. */
    val buttonHeightLarge = 52.dp
    /** Altura de botón compacto: 38 dp, usado en estados vacíos y acciones secundarias densas. */
    val buttonHeightSmall = 38.dp
    /** Altura estándar de campo de entrada: 52 dp, alineada visualmente con los botones. */
    val inputHeight = 52.dp
    /** Icono pequeño: 16 dp, para metadatos y acciones dentro de etiquetas. */
    val iconSizeSmall = 16.dp
    /** Icono mediano: 20 dp, tamaño habitual en botones y campos. */
    val iconSizeMedium = 20.dp
    /** Icono grande: 24 dp, para barras de navegación y encabezados. */
    val iconSizeLarge = 24.dp
    /** Icono extra grande: 32 dp, para estados vacíos/error y elementos protagonistas. */
    val iconSizeXLarge = 32.dp
    /** Avatar circular: 44 dp, área suficiente para una imagen de perfil sin dominar la fila. */
    val avatarSize = 44.dp
}
