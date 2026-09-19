package pe.parkeo.ui.theme

import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════════════════════════
// Parkeo DESIGN SYSTEM — CORE PALETTE
// Signature Lime + Obsidian Dark Surfaces + Crisp Titanium Light Surfaces
// ════════════════════════════════════════════════════════════════════════════════

// Signature Accent — Verde Lima Eléctrico
val ParkeoLime           = Color(0xFFD4FF00) // Hero action color
val ParkeoLimeBright     = Color(0xFFDDFF24)
val ParkeoLimeDim        = Color(0xFFA6CC00)
val ParkeoLimeGlow       = Color(0x33D4FF00) // 20% alpha
val ParkeoLimeContainer  = Color(0x1AD4FF00) // 10% alpha
val OnParkeoLime         = Color(0xFF0A0A0A) // Ultra high-contrast dark text/icon on lime

// Obsidian Layered Surfaces (Dark Theme Core)
val ParkeoObsidian       = Color(0xFF060606) // Absolute deep void
val ParkeoDarkSurface0   = Color(0xFF0C0C0C) // Scaffolds
val ParkeoDarkSurface1   = Color(0xFF131313) // Cards, Sheets
val ParkeoDarkSurface2   = Color(0xFF1A1A1A) // Inputs, inner containers
val ParkeoDarkSurface3   = Color(0xFF242424) // Hover, active chips, dialogs
val ParkeoDarkBorder     = Color(0xFF262626) // Crisp hairline border
val ParkeoDarkBorderSubtle = Color(0xFF1A1A1A) // Subtle separators

// Titanium Layered Surfaces (Light Theme Core)
val ParkeoTitaniumBg     = Color(0xFFF4F5F8) // Subtle off-white base (not plain white)
val ParkeoLightSurface1  = Color(0xFFFFFFFF) // Pure white card planes
val ParkeoLightSurface2  = Color(0xFFEBEFF4) // Inputs, segmented tracks
val ParkeoLightSurface3  = Color(0xFFE0E5ED) // Hover, chips
val ParkeoLightBorder    = Color(0xFFD4D9E2) // Architectural card border
val ParkeoLightBorderSubtle = Color(0xFFE8ECF2)

// Typography & Content Neutral Contrast
val ParkeoTextWhite      = Color(0xFFF8F9FA) // 98% white
val ParkeoTextGrayLight  = Color(0xFFA6ACB6) // Secondary dark mode
val ParkeoTextGrayMuted  = Color(0xFF6B7280) // Tertiary dark mode
val ParkeoTextBlack      = Color(0xFF0D0E12) // Primary light mode
val ParkeoTextSlate      = Color(0xFF4B5563) // Secondary light mode
val ParkeoTextMutedLight = Color(0xFF8B94A2) // Tertiary light mode

// Semantic States (Mobility, Availability, Signals)
val ParkeoGreen500       = Color(0xFF22C55E) // Disponible / Éxito
val ParkeoGreenContainer = Color(0x1A22C55E)
val ParkeoRed500         = Color(0xFFFF3B30) // Ocupado / Error / Destructivo
val ParkeoRedContainer   = Color(0x1AFF3B30)
val ParkeoAmber500       = Color(0xFFFFB020) // Reservado / Pendiente / Advertencia
val ParkeoAmberContainer = Color(0x1AFFB020)
val ParkeoGray500        = Color(0xFF71717A) // Mantenimiento / Deshabilitado
val ParkeoGrayContainer  = Color(0x1A71717A)
val ParkeoCyan500        = Color(0xFF00E5FF) // Tech mobility accent

// Pure Universal Neutrals
val White = Color(0xFFFFFFFF)
val Black = Color(0xFF000000)
val SurfaceLight = ParkeoTitaniumBg
val SurfaceDark  = ParkeoObsidian

// ════════════════════════════════════════════════════════════════════════════════
// BACKWARD-COMPATIBLE FALLBACK ALIASES
// ════════════════════════════════════════════════════════════════════════════════
val ParkeoBlue900 = Color(0xFF0D121F)
val ParkeoBlue800 = Color(0xFF121A2C)
val ParkeoBlue700 = ParkeoLime       // Primary actions map to signature Lime
val ParkeoBlue600 = ParkeoLimeBright
val ParkeoBlue500 = Color(0xFF38BDF8)
val ParkeoBlue400 = Color(0xFF7DD3FC)
val ParkeoBlue100 = ParkeoLimeContainer
val ParkeoBlue50  = Color(0x0DD4FF00)
val ParkeoCyan400 = Color(0xFF38E1FF)
val ParkeoCyan100 = Color(0x1A00E5FF)
val ParkeoGray800 = ParkeoDarkSurface2
val ParkeoGray900 = ParkeoDarkSurface1
