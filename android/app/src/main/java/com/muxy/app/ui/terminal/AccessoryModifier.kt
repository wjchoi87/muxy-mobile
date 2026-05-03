package com.muxy.app.ui.terminal

enum class AccessoryModifier(val title: String, val displayName: String, val glyph: String) {
    CTRL("ctrl", "Control", "⌃"),
    SHIFT("shift", "Shift", "⇧"),
    ALT("alt", "Option", "⌥"),
    CMD("cmd", "Command", "⌘"),
}
