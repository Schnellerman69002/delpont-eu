package eu.delpont.morphee.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Thème nuit à contraste élevé : fond noir pur, texte blanc, accent ambre
 * (faible lumière bleue, adapté à une utilisation nocturne).
 */
private val NightColorScheme = darkColorScheme(
    primary = Color(0xFFFFB300),
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF3A2A00),
    onPrimaryContainer = Color(0xFFFFD54F),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF10312E),
    onSecondaryContainer = Color(0xFFB2DFDB),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF0A0A0A),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF181818),
    onSurfaceVariant = Color(0xFFE0E0E0),
    surfaceContainer = Color(0xFF101010),
    surfaceContainerHigh = Color(0xFF1A1A1A),
    surfaceContainerHighest = Color(0xFF222222),
    outline = Color(0xFF666666),
    error = Color(0xFFFF8A80),
    onError = Color.Black,
)

private val NightTypography = Typography(
    headlineLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 42.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp),
    titleMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    labelLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 24.sp),
)

@Composable
fun MorpheeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NightColorScheme,
        typography = NightTypography,
        content = content,
    )
}
