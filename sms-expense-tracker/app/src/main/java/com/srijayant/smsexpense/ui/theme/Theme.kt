package com.srijayant.smsexpense.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF00897B)
private val TealDark = Color(0xFF4DB6AC)
private val Amber = Color(0xFFFFB300)

private val LightColors = lightColorScheme(
    primary = Teal,
    secondary = Amber,
    surfaceVariant = Color(0xFFE8F0EE)
)

private val DarkColors = darkColorScheme(
    primary = TealDark,
    secondary = Amber
)

val SpentRed = Color(0xFFE53935)
val ReceivedGreen = Color(0xFF43A047)

val CategoryColors = listOf(
    Color(0xFF26A69A), Color(0xFFEF5350), Color(0xFF42A5F5), Color(0xFFFFA726),
    Color(0xFFAB47BC), Color(0xFF66BB6A), Color(0xFFFF7043), Color(0xFF5C6BC0),
    Color(0xFFEC407A), Color(0xFF8D6E63), Color(0xFF78909C), Color(0xFFD4E157),
    Color(0xFF29B6F6), Color(0xFF9E9E9E)
)

@Composable
fun SmsExpenseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
