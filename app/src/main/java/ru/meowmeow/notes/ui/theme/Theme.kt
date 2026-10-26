package ru.meowmeow.notes.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val Mint = Color(0xFFDCE7E1)
    val Amber = Color(0xFFB8792B)
    val AmberSoft = Color(0xFFF6E7CD)
    val Danger = Color(0xFFA63A2C)

    val Diagnosis = Color(0xFF5B6C7D)
    val DiagnosisSoft = Color(0xFFE4EAF0)
    val Ordered = Color(0xFF40548F)
    val OrderedSoft = Color(0xFFE2E7F5)
    val Ready = Color(0xFF3D7A55)
    val ReadySoft = Color(0xFFDDEBE1)
}

private val MeowColors = lightColorScheme(
    primary = Meow.Brand,
    onPrimary = Color.White,
    primaryContainer = Meow.Mint,
    onPrimaryContainer = Meow.BrandDeep,
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
fun MeowTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MeowColors,
        typography = MeowTypography,
        shapes = MeowShapes,
        content = content,
    )
}

/** Цвет полосы/текста статуса. */
fun OrderStatus.tint(): Color = when (this) {
    OrderStatus.DIAGNOSIS -> Meow.Diagnosis
    OrderStatus.IN_PROGRESS -> Meow.Amber
    OrderStatus.PART_ORDERED -> Meow.Ordered
    OrderStatus.DONE -> Meow.Ready
}

/** Подложка пилюли статуса. */
fun OrderStatus.soft(): Color = when (this) {
    OrderStatus.DIAGNOSIS -> Meow.DiagnosisSoft
    OrderStatus.IN_PROGRESS -> Meow.AmberSoft
    OrderStatus.PART_ORDERED -> Meow.OrderedSoft
    OrderStatus.DONE -> Meow.ReadySoft
}
