package com.muxy.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.muxy.app.data.SessionRepository

data class Palette(
    val background: Color,
    val foreground: Color,
)

object MuxyTheme {
    fun from(theme: SessionRepository.DeviceTheme?): Palette {
        if (theme == null) return Palette(background = Color(0xFF0E0E0E), foreground = Color(0xFFE6E6E6))
        return Palette(background = rgb(theme.bg), foreground = rgb(theme.fg))
    }

    private fun rgb(value: Long): Color {
        val r = ((value shr 16) and 0xFF) / 255f
        val g = ((value shr 8) and 0xFF) / 255f
        val b = (value and 0xFF) / 255f
        return Color(r, g, b)
    }
}
