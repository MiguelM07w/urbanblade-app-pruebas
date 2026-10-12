package com.urbanblade.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.view.WindowCompat
import com.urbanblade.mobile.R
import com.urbanblade.mobile.core.network.AppContainer

/**
 * Los 4 temas de UrbanBlade, portados 1:1 desde los tokens CSS del
 * frontend web (frontend-urban/app/assets/css/main.css). El `key` es el
 * mismo valor que ese archivo usa en `data-theme="..."`.
 */
enum class UrbanTheme(val key: String, val label: String, val isLight: Boolean) {
    NOIR("noir", "Sastrería Nocturna", isLight = false),
    ACERO("acero", "Taller de Acero", isLight = false),
    SALON("salon", "Salón Inglés", isLight = false),
    LIBRETA("libreta", "Libreta de Barbero", isLight = true),
}

/**
 * Mismo mapeo tema→mascota que useBrandMascots.ts en frontend-urban:
 * "salon" → Nava, "libreta" → Bruno, el resto → Bladebot.
 */
val UrbanTheme.mascotDrawable: Int
    get() = when (this) {
        UrbanTheme.SALON -> R.drawable.mascot_nava
        UrbanTheme.LIBRETA -> R.drawable.mascot_bruno
        UrbanTheme.NOIR, UrbanTheme.ACERO -> R.drawable.mascot_bladebot
    }

/**
 * Fondos y acentos de marca compartidos con la web. UrbanColors adapta la tinta de
 * acento y los tonos semánticos para que también sean legibles en el tema claro.
 */
private data class UrbanPalette(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardAlt: Color,
    val line: Color,
    val gold: Color,
    val goldDim: Color,
    val ink: Color,
    val muted: Color,
    val gradientTop: Color,
    val gradientBottom: Color,
)

/** Fondo y acento de un tema, para pintar su vista previa en el selector de apariencia. */
fun UrbanTheme.previewColors(): Pair<Color, Color> = palettes.getValue(this).let { it.background to it.gold }

private val palettes = mapOf(
    UrbanTheme.NOIR to UrbanPalette(
        background = Color(0xFF0A0A0A),
        surface = Color(0xFF111111),
        card = Color(0xFF161616),
        cardAlt = Color(0xFF1F1F1F),
        line = Color(0xFF2C2C2C),
        gold = Color(0xFFD4AF37),
        goldDim = Color(0xFFA8842C),
        ink = Color(0xFFF2F2F2),
        muted = Color(0xFF9C9C9C),
        gradientTop = Color(0xFF151515),
        gradientBottom = Color(0xFF0A0A0A),
    ),
    UrbanTheme.ACERO to UrbanPalette(
        background = Color(0xFF111317),
        surface = Color(0xFF14171B),
        card = Color(0xFF1A1D22),
        cardAlt = Color(0xFF22262C),
        line = Color(0xFF2C3038),
        gold = Color(0xFFC1703D),
        goldDim = Color(0xFF9C5A30),
        ink = Color(0xFFEEF0F2),
        muted = Color(0xFF8F95A0),
        gradientTop = Color(0xFF191C21),
        gradientBottom = Color(0xFF0F1114),
    ),
    UrbanTheme.SALON to UrbanPalette(
        background = Color(0xFF0B1210),
        surface = Color(0xFF0F1614),
        card = Color(0xFF141C19),
        cardAlt = Color(0xFF1C2621),
        line = Color(0xFF26332C),
        gold = Color(0xFFC9A24A),
        goldDim = Color(0xFFA1813A),
        ink = Color(0xFFEEF2EE),
        muted = Color(0xFF93A49B),
        gradientTop = Color(0xFF131C18),
        gradientBottom = Color(0xFF0A0F0D),
    ),
    UrbanTheme.LIBRETA to UrbanPalette(
        background = Color(0xFFF3EDE0),
        surface = Color(0xFFF7F1E4),
        card = Color(0xFFFFFBF3),
        cardAlt = Color(0xFFECE2CC),
        line = Color(0xFFE2D5B8),
        gold = Color(0xFFB8860B),
        goldDim = Color(0xFF8A6608),
        ink = Color(0xFF241E18),
        muted = Color(0xFF685E50),
        gradientTop = Color(0xFFFBF6EA),
        gradientBottom = Color(0xFFEFE6D2),
    ),
)

/**
 * Singleton reactivo: sus campos son mutableStateOf, así que Compose
 * recompone automáticamente cualquier pantalla que los lea (UrbanColors.Gold,
 * etc. -- 126 sitios repartidos en 9 archivos) en cuanto applyTheme()
 * cambie el tema activo, sin tener que tocar esos 126 sitios ni convertir
 * esto a un CompositionLocal.
 */
object UrbanColors {
    private val default = palettes.getValue(UrbanTheme.NOIR)

    var current: UrbanTheme = UrbanTheme.NOIR
        private set

    var Background by mutableStateOf(default.background)
        private set
    var Surface by mutableStateOf(default.surface)
        private set
    var Card by mutableStateOf(default.card)
        private set
    var CardAlt by mutableStateOf(default.cardAlt)
        private set
    var Line by mutableStateOf(default.line)
        private set
    var Gold by mutableStateOf(default.gold)
        private set
    var GoldDim by mutableStateOf(default.goldDim)
        private set
    var Ink by mutableStateOf(default.ink)
        private set
    var Muted by mutableStateOf(default.muted)
        private set
    var GradientTop by mutableStateOf(default.gradientTop)
        private set
    var GradientBottom by mutableStateOf(default.gradientBottom)
        private set

    // Los fondos conservan el acento de marca; el texto y los estados se oscurecen en Libreta.
    var GoldText by mutableStateOf(default.gold)
        private set
    var Success by mutableStateOf(Color(0xFF4BB983))
        private set
    var Warning by mutableStateOf(Color(0xFFE2A84D))
        private set
    var Danger by mutableStateOf(Color(0xFFE36D6D))
        private set

    /** Texto/ícono sobre fondos dorados (botones, chips seleccionados): oscuro en los 4 temas. */
    val OnGold = Color(0xFF080808)
    var Info by mutableStateOf(Color(0xFF72A5D8))
        private set

    fun applyTheme(theme: UrbanTheme) {
        val p = palettes.getValue(theme)
        current = theme
        Background = p.background
        Surface = p.surface
        Card = p.card
        CardAlt = p.cardAlt
        Line = p.line
        Gold = p.gold
        GoldDim = p.goldDim
        GoldText = when (theme) {
            UrbanTheme.LIBRETA -> Color(0xFF795806)
            UrbanTheme.ACERO -> Color(0xFFD28B5A)
            else -> p.gold
        }
        Success = if (theme.isLight) Color(0xFF236541) else Color(0xFF4BB983)
        Warning = if (theme.isLight) Color(0xFF80520D) else Color(0xFFE2A84D)
        Danger = if (theme.isLight) Color(0xFFAA3030) else Color(0xFFE36D6D)
        Info = if (theme.isLight) Color(0xFF285C8F) else Color(0xFF72A5D8)
        Ink = p.ink
        Muted = p.muted
        GradientTop = p.gradientTop
        GradientBottom = p.gradientBottom
    }
}

private val UrbanTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5f).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 31.sp,
        lineHeight = 37.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 27.sp,
        lineHeight = 33.sp
    ),
    // Paso intermedio de la familia serif: encabezados de módulos y pantallas utilitarias
    // (auditoría P2-2: el título de 27 sp es para las pantallas raíz, no para todo).
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 29.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp
    )
)

private val UrbanShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    // Escala de la auditoría (P0-5): 8 / 12 / 16 / 24. Antes 18 y 30, que no encajaban con ningún paso.
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun UrbanBladeTheme(content: @Composable () -> Unit) {
    // Restaura el tema guardado al arrancar -- mismo patrón que
    // AuthViewModel.bootstrap() usa para restaurar la sesión desde DataStore.
    LaunchedEffect(Unit) {
        val savedKey = AppContainer.sessionManager.currentTheme()
        val saved = UrbanTheme.entries.find { it.key == savedKey }
        if (saved != null && saved != UrbanColors.current) {
            UrbanColors.applyTheme(saved)
        }
    }

    val colorScheme = if (UrbanColors.current.isLight) {
        lightColorScheme(
            primary = UrbanColors.GoldText,
            onPrimary = Color(0xFFFFFBF3),
            primaryContainer = Color(0xFFF0DFB0),
            onPrimaryContainer = Color(0xFF4A3B08),
            secondary = UrbanColors.GoldDim,
            onSecondary = Color(0xFFFFFBF3),
            // Material usa secondaryContainer para el filtro (FilterChip) seleccionado: en dorado,
            // igual que las pestañas y la barra inferior, en todas las pantallas sin tocar cada una.
            secondaryContainer = UrbanColors.Gold,
            onSecondaryContainer = UrbanColors.OnGold,
            tertiary = Color(0xFFB8860B),
            background = UrbanColors.Background,
            onBackground = UrbanColors.Ink,
            surface = UrbanColors.Surface,
            onSurface = UrbanColors.Ink,
            surfaceVariant = UrbanColors.CardAlt,
            onSurfaceVariant = UrbanColors.Muted,
            outline = UrbanColors.Line,
            outlineVariant = Color(0xFFEDE4D0),
            error = UrbanColors.Danger,
            errorContainer = Color(0xFFF6DCDC),
            onErrorContainer = Color(0xFF5A1010)
        )
    } else {
        darkColorScheme(
            primary = UrbanColors.GoldText,
            onPrimary = Color(0xFF0A0A0A),
            primaryContainer = Color(0xFF332B12),
            onPrimaryContainer = Color(0xFFF4D978),
            secondary = UrbanColors.GoldDim,
            onSecondary = Color(0xFF111111),
            secondaryContainer = UrbanColors.Gold,
            onSecondaryContainer = UrbanColors.OnGold,
            tertiary = Color(0xFFC1703D),
            background = UrbanColors.Background,
            onBackground = UrbanColors.Ink,
            surface = UrbanColors.Surface,
            onSurface = UrbanColors.Ink,
            surfaceVariant = UrbanColors.CardAlt,
            onSurfaceVariant = UrbanColors.Muted,
            outline = UrbanColors.Line,
            outlineVariant = Color(0xFF222222),
            error = UrbanColors.Danger,
            errorContainer = Color(0xFF3A1D1D),
            onErrorContainer = Color(0xFFFFC9C9)
        )
    }

    // "libreta" es el único tema claro -- sin esto los íconos de la barra
    // de estado (hora, batería) se ven mal (claros sobre fondo claro).
    val view = LocalView.current
    if (!view.isInEditMode) {
        val isLight = UrbanColors.current.isLight
        SideEffect {
            val window = (view.context as? android.app.Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = UrbanTypography,
        shapes = UrbanShapes,
        content = content
    )
}
