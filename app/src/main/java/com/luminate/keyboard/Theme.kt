package com.abdullah.keyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

data class Theme(
    val dark: Boolean,
    val glass: Boolean,
    val bgTop: Int,
    val bgBottom: Int,
    val key: Int,
    val special: Int,
    val pressed: Int,
    val accent: Int,
    val onAccent: Int,
    val text: Int,
    val hint: Int,
    val accentContainer: Int,
    val onAccentContainer: Int,
    val popup: Int,
    val popupText: Int,
    val popupStroke: Int,
    val keyStroke: Int
)

object Themes {
    private fun c(s: String) = Color.parseColor(s)

    private val dark by lazy {
        Theme(
            dark = true, glass = false,
            bgTop = c("#202124"), bgBottom = c("#202124"),
            key = c("#3C4043"), special = c("#46494D"), pressed = c("#5F6368"),
            accent = c("#A8C7FA"), onAccent = c("#062E6F"),
            text = c("#E8EAED"), hint = c("#9AA0A6"),
            accentContainer = c("#004A77"), onAccentContainer = c("#C2E7FF"),
            popup = c("#5A5E63"), popupText = c("#FFFFFF"),
            popupStroke = c("#00000000"), keyStroke = c("#00000000")
        )
    }

    private val light by lazy {
        Theme(
            dark = false, glass = false,
            bgTop = c("#E8EAED"), bgBottom = c("#E8EAED"),
            key = c("#FFFFFF"), special = c("#CDD1D6"), pressed = c("#BDC1C6"),
            accent = c("#0B57D0"), onAccent = c("#FFFFFF"),
            text = c("#1F1F1F"), hint = c("#5F6368"),
            accentContainer = c("#D3E3FD"), onAccentContainer = c("#041E49"),
            popup = c("#FFFFFF"), popupText = c("#1F1F1F"),
            popupStroke = c("#33000000"), keyStroke = c("#00000000")
        )
    }

    private val darkGlass by lazy {
        Theme(
            dark = true, glass = true,
            bgTop = c("#CC24262E"), bgBottom = c("#CC101116"),
            key = c("#33FFFFFF"), special = c("#26FFFFFF"), pressed = c("#66FFFFFF"),
            accent = c("#A8C7FA"), onAccent = c("#062E6F"),
            text = c("#FFFFFF"), hint = c("#B3FFFFFF"),
            accentContainer = c("#99004A77"), onAccentContainer = c("#C2E7FF"),
            popup = c("#F0404450"), popupText = c("#FFFFFF"),
            popupStroke = c("#40FFFFFF"), keyStroke = c("#40FFFFFF")
        )
    }

    private val lightGlass by lazy {
        Theme(
            dark = false, glass = true,
            bgTop = c("#CCFFFFFF"), bgBottom = c("#B3E6EAF2"),
            key = c("#A6FFFFFF"), special = c("#66FFFFFF"), pressed = c("#D9FFFFFF"),
            accent = c("#0B57D0"), onAccent = c("#FFFFFF"),
            text = c("#1C1C1E"), hint = c("#99000000"),
            accentContainer = c("#99D3E3FD"), onAccentContainer = c("#041E49"),
            popup = c("#F2FFFFFF"), popupText = c("#1C1C1E"),
            popupStroke = c("#66FFFFFF"), keyStroke = c("#99FFFFFF")
        )
    }

    fun resolve(ctx: Context): Theme {
        val isDark = when (Prefs.theme(ctx)) {
            "dark" -> true
            "light" -> false
            else -> (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }
        val glass = Prefs.glass(ctx)
        return when {
            isDark && glass -> darkGlass
            isDark -> dark
            glass -> lightGlass
            else -> light
        }
    }
}
