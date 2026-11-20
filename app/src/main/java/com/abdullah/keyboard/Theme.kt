package com.abdullah.keyboard

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color

data class Theme(
    val dark: Boolean, val glass: Boolean, val bgTop: Int, val bgBottom: Int,
    val key: Int, val special: Int, val pressed: Int, val accent: Int, val onAccent: Int,
    val text: Int, val hint: Int, val accentContainer: Int, val onAccentContainer: Int,
    val popup: Int, val popupText: Int, val popupStroke: Int, val keyStroke: Int
)

object Themes {
    private fun c(s: String) = Color.parseColor(s)
    private val dark by lazy { Theme(true,false,c("#202124"),c("#202124"),c("#3C4043"),c("#46494D"),c("#5F6368"),c("#A8C7FA"),c("#062E6F"),c("#E8EAED"),c("#9AA0A6"),c("#004A77"),c("#C2E7FF"),c("#5A5E63"),c("#FFFFFF"),c("#00000000"),c("#00000000")) }
    private val light by lazy { Theme(false,false,c("#E8EAED"),c("#E8EAED"),c("#FFFFFF"),c("#CDD1D6"),c("#BDC1C6"),c("#0B57D0"),c("#FFFFFF"),c("#1F1F1F"),c("#5F6368"),c("#D3E3FD"),c("#041E49"),c("#FFFFFF"),c("#1F1F1F"),c("#33000000"),c("#00000000")) }
    private val darkGlass by lazy { Theme(true,true,c("#CC24262E"),c("#CC101116"),c("#33FFFFFF"),c("#26FFFFFF"),c("#66FFFFFF"),c("#A8C7FA"),c("#062E6F"),c("#FFFFFF"),c("#B3FFFFFF"),c("#99004A77"),c("#C2E7FF"),c("#F0404450"),c("#FFFFFF"),c("#40FFFFFF"),c("#40FFFFFF")) }
    private val lightGlass by lazy { Theme(false,true,c("#CCFFFFFF"),c("#B3E6EAF2"),c("#A6FFFFFF"),c("#66FFFFFF"),c("#D9FFFFFF"),c("#0B57D0"),c("#FFFFFF"),c("#1C1C1E"),c("#99000000"),c("#99D3E3FD"),c("#041E49"),c("#F2FFFFFF"),c("#1C1C1E"),c("#66FFFFFF"),c("#99FFFFFF")) }

    fun resolve(ctx: Context): Theme {
        val isDark = when (Prefs.theme(ctx)) {
            "dark" -> true
            "light" -> false
            else -> (ctx.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
        val glass = Prefs.glass(ctx)
        return when { isDark && glass -> darkGlass; isDark -> dark; glass -> lightGlass; else -> light }
    }
}
