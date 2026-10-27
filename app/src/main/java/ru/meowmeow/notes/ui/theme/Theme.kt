package ru.meowmeow.notes.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.meowmeow.notes.data.AccentColor
import ru.meowmeow.notes.data.AppSettings
import ru.meowmeow.notes.data.ThemeMode
import ru.meowmeow.notes.domain.OrderStatus

/**
 * Палитра «мастерская»: тёплая бумага вместо белого, графитовый зелёный вместо
 * корпоративного синего, янтарный акцент для того, что ждёт действия.
 */
object Meow {
    val Paper = Color(0xFFF4EFE6)
    val Card = Color(0xFFFFFDF8)
    val Ink = Color(0xFF1F2421)
    val Muted = Color(0xFF63675E)
    val Line = Color(0xFFE2D9C9)
    val Brand = Color(0xFF2C4A45)
    val BrandDeep = Color(0xFF1C322E)
    val Amber = Color(0xFFB8792B)
    val AmberSoft = Color(0xFFF6E7CD)
    val Blue = Color(0xFF40548F)
    val BlueSoft = Color(0xFFE2E7F5)
    val Danger = Color(0xFFA63A2C)

    // Тёмная тема
    val NightBackground = Color(0xFF101513)
    val NightSurface = Color(0xFF171E1B)
    val NightSurfaceVariant = Color(0xFF232D28)
    val NightText = Color(0xFFEFE9DF)
    val NightMuted = Color(0xFFA7B2AA)
    val NightLine = Color(0xFF2C3731)
}

/** Цвета статусов, зависящие от темы. */
@Immutable
data class MeowPalette(
    val diagnosis: Color,
    val diagnosisSoft: Color,
    val inProgress: Color,
    val inProgressSoft: Color,
    val ordered: Color,
    val orderedSoft: Color,
    val done: Color,
    val doneSoft: Color,
    val refused: Color,
    val refusedSoft: Color,
)

private val LightPalette = MeowPalette(
    diagnosis = Color(0xFF5B6C7D),
    diagnosisSoft = Color(0xFFE4EAF0),
    inProgress = Color(0xFF8A5A1E),
    inProgressSoft = Color(0xFFF6E7CD),
    ordered = Color(0xFF40548F),
    orderedSoft = Color(0xFFE2E7F5),
    done = Color(0xFF3D7A55),
    doneSoft = Color(0xFFDDEBE1),
    refused = Color(0xFF8A4B45),
    refusedSoft = Color(0xFFF3DFDC),
)

private val DarkPalette = MeowPalette(
    diagnosis = Color(0xFFA9C0D3),
    diagnosisSoft = Color(0xFF24313B),
    inProgress = Color(0xFFE3B36F),
    inProgressSoft = Color(0xFF3B2E1C),
    ordered = Color(0xFFAEBEEA),
    orderedSoft = Color(0xFF26314C),
    done = Color(0xFF93C9A9),
    doneSoft = Color(0xFF1F3527),
    refused = Color(0xFFE5A9A2),
    refusedSoft = Color(0xFF3B2522),
)

val LocalMeowPalette = staticCompositionLocalOf { LightPalette }

private fun accentPrimary(accent: AccentColor, dark: Boolean): Color = when (accent) {
    AccentColor.GREEN -> if (dark) Color(0xFF9AC7B6) else Meow.Brand
    AccentColor.AMBER -> if (dark) Color(0xFFE0A45A) else Color(0xFFA9671F)
    AccentColor.BLUE -> if (dark) Color(0xFFA9BCEA) else Color(0xFF3A4E86)
}

private fun accentContainer(accent: AccentColor, dark: Boolean): Color = when (accent) {
    AccentColor.GREEN -> if (dark) Color(0xFF2A403A) else Color(0xFFDCE7E1)
    AccentColor.AMBER -> if (dark) Color(0xFF4A3720) else Meow.AmberSoft
    AccentColor.BLUE -> if (dark) Color(0xFF2C3A5C) else Meow.BlueSoft
}

private fun accentOnContainer(accent: AccentColor, dark: Boolean): Color = when (accent) {
    AccentColor.GREEN -> if (dark) Color(0xFFD7EDE3) else Meow.BrandDeep
    AccentColor.AMBER -> if (dark) Color(0xFFF7DFC0) else Color(0xFF4A2E08)
    AccentColor.BLUE -> if (dark) Color(0xFFDDE5F8) else Color(0xFF1A2544)
}

/** Цвет кружка в настройках оформления — одинаковый в обеих темах. */
fun AccentColor.swatch(): Color = when (this) {
    AccentColor.GREEN -> Meow.Brand
    AccentColor.AMBER -> Meow.Amber
    AccentColor.BLUE -> Meow.Blue
}

private fun lightScheme(accent: AccentColor) = lightColorScheme(
    primary = accentPrimary(accent, dark = false),
    onPrimary = Color.White,
    primaryContainer = accentContainer(accent, dark = false),
    onPrimaryContainer = accentOnContainer(accent, dark = false),
    secondary = Meow.Amber,
    onSecondary = Color.White,
    secondaryContainer = Meow.AmberSoft,
    onSecondaryContainer = Color(0xFF4A2E08),
    background = Meow.Paper,
    onBackground = Meow.Ink,
    surface = Meow.Card,
    onSurface = Meow.Ink,
    surfaceVariant = Color(0xFFEDE6D8),
    onSurfaceVariant = Meow.Muted,
    outline = Color(0xFFB9AE9C),
    outlineVariant = Meow.Line,
    error = Meow.Danger,
    onError = Color.White,
    errorContainer = Color(0xFFF6DCD7),
    onErrorContainer = Color(0xFF41120B),
)

private fun darkScheme(accent: AccentColor) = darkColorScheme(
    primary = accentPrimary(accent, dark = true),
    onPrimary = Color(0xFF10201B),
    primaryContainer = accentContainer(accent, dark = true),
    onPrimaryContainer = accentOnContainer(accent, dark = true),
    secondary = Color(0xFFE0B27A),
    onSecondary = Color(0xFF2A1A06),
    secondaryContainer = Color(0xFF3B2E1C),
    onSecondaryContainer = Color(0xFFF7DFC0),
    background = Meow.NightBackground,
    onBackground = Meow.NightText,
    surface = Meow.NightSurface,
    onSurface = Meow.NightText,
    surfaceVariant = Meow.NightSurfaceVariant,
    onSurfaceVariant = Meow.NightMuted,
    outline = Color(0xFF6E7A73),
    outlineVariant = Meow.NightLine,
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF3B0A08),
    errorContainer = Color(0xFF5A2622),
    onErrorContainer = Color(0xFFF9DEDC),
)

private val MeowTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp,
    ),
)

private val MeowShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun MeowTheme(
    settings: AppSettings = AppSettings(),
    content: @Composable () -> Unit,
) {
    val dark = when (settings.mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val scheme = if (dark) darkScheme(settings.accent) else lightScheme(settings.accent)
    val palette = if (dark) DarkPalette else LightPalette

    CompositionLocalProvider(LocalMeowPalette provides palette) {
        MaterialTheme(
            colorScheme = scheme,
            typography = MeowTypography,
            shapes = MeowShapes,
            content = content,
        )
    }
}

/** Цвет полосы/текста статуса. */
@Composable
fun OrderStatus.tint(): Color {
    val palette = LocalMeowPalette.current
    return when (this) {
        OrderStatus.DIAGNOSIS -> palette.diagnosis
        OrderStatus.IN_PROGRESS -> palette.inProgress
        OrderStatus.PART_ORDERED -> palette.ordered
        OrderStatus.DONE -> palette.done
        OrderStatus.REFUSED -> palette.refused
    }
}

/** Подложка пилюли статуса. */
@Composable
fun OrderStatus.soft(): Color {
    val palette = LocalMeowPalette.current
    return when (this) {
        OrderStatus.DIAGNOSIS -> palette.diagnosisSoft
        OrderStatus.IN_PROGRESS -> palette.inProgressSoft
        OrderStatus.PART_ORDERED -> palette.orderedSoft
        OrderStatus.DONE -> palette.doneSoft
        OrderStatus.REFUSED -> palette.refusedSoft
    }
}
