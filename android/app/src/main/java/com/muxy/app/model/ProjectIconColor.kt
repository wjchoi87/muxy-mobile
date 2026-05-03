package com.muxy.app.model

object ProjectIconColor {
    data class Swatch(val id: String, val name: String, val hex: String) {
        val prefersDarkForeground: Boolean
            get() {
                val (r, g, b) = rgbFromHex(hex) ?: return false
                val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
                return luminance > 0.6
            }
    }

    val palette: List<Swatch> = listOf(
        Swatch("red", "Red", "#E5484D"),
        Swatch("orange", "Orange", "#F76B15"),
        Swatch("amber", "Amber", "#F5A623"),
        Swatch("yellow", "Yellow", "#EBCB00"),
        Swatch("lime", "Lime", "#9BCD1E"),
        Swatch("green", "Green", "#30A46C"),
        Swatch("teal", "Teal", "#12A594"),
        Swatch("cyan", "Cyan", "#05A2C2"),
        Swatch("blue", "Blue", "#3E63DD"),
        Swatch("indigo", "Indigo", "#5B5BD6"),
        Swatch("violet", "Violet", "#8E4EC6"),
        Swatch("pink", "Pink", "#D6409F"),
    )

    private val byId: Map<String, Swatch> = palette.associateBy { it.id }

    fun swatch(identifier: String?): Swatch? {
        if (identifier == null) return null
        byId[identifier]?.let { return it }
        return palette.firstOrNull { it.hex.equals(identifier, ignoreCase = true) }
    }

    fun rgbFromHex(hex: String): Triple<Double, Double, Double>? {
        var s = hex.trim()
        if (s.startsWith("#")) s = s.substring(1)
        if (s.length != 6) return null
        val v = s.toLongOrNull(16) ?: return null
        return Triple(
            ((v shr 16) and 0xFF) / 255.0,
            ((v shr 8) and 0xFF) / 255.0,
            (v and 0xFF) / 255.0,
        )
    }
}
