package dev.pinaki.localnotes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

val Ink = Color(0xFF171717)
val Paper = Color(0xFFFFF9ED)
val ElectricBlue = Color(0xFF7DD3FC)
val AcidYellow = Color(0xFFFDE047)
val HotPink = Color(0xFFF9A8D4)
val Mint = Color(0xFF86EFAC)
val Coral = Color(0xFFFF8A65)
val MutedInk = Color(0xFF4A4A4A)

private val LightColors = lightColorScheme(
    primary = ElectricBlue, onPrimary = Ink,
    primaryContainer = AcidYellow, onPrimaryContainer = Ink,
    secondary = HotPink, onSecondary = Ink,
    secondaryContainer = Mint, onSecondaryContainer = Ink,
    background = Paper, onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFFFF1C7), onSurfaceVariant = MutedInk,
    error = Coral, onError = Ink, outline = Ink,
)

private val DarkColors = darkColorScheme(
    primary = ElectricBlue, onPrimary = Ink,
    primaryContainer = AcidYellow, onPrimaryContainer = Ink,
    secondary = HotPink, onSecondary = Ink,
    secondaryContainer = Mint, onSecondaryContainer = Ink,
    background = Color(0xFF202020), onBackground = Paper,
    surface = Color(0xFF303030), onSurface = Paper,
    surfaceVariant = Color(0xFF414141), onSurfaceVariant = Color(0xFFE5E5E5),
    error = Coral, onError = Ink, outline = Paper,
)

private val NeoTypography = Typography(
    displaySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 40.sp, lineHeight = 40.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 28.sp, lineHeight = 30.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 21.sp, lineHeight = 25.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = .5.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Black, fontSize = 14.sp, letterSpacing = .8.sp),
)

@Composable
fun LocalNotesTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colors.background.toArgb()
            window.navigationBarColor = colors.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = NeoTypography, content = content)
}
