package com.abdullah.keyboard

import android.content.Context
import android.content.SharedPreferences

object Prefs {
    const val K_THEME = "theme"       // system | light | dark
    const val K_GLASS = "glass"       // liquid glass on/off
    const val K_ONEHAND = "onehand"   // off | left | right
    const val K_SCALE = "scale"       // keyboard height scale

    fun sp(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences("kb_prefs", Context.MODE_PRIVATE)

    fun theme(c: Context): String = sp(c).getString(K_THEME, "system") ?: "system"
    fun setTheme(c: Context, v: String) = sp(c).edit().putString(K_THEME, v).apply()

    fun glass(c: Context): Boolean = sp(c).getBoolean(K_GLASS, false)
    fun setGlass(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_GLASS, v).apply()

    fun oneHand(c: Context): String = sp(c).getString(K_ONEHAND, "off") ?: "off"
    fun setOneHand(c: Context, v: String) = sp(c).edit().putString(K_ONEHAND, v).apply()

    fun scale(c: Context): Float = sp(c).getFloat(K_SCALE, 1f)
    fun setScale(c: Context, v: Float) = sp(c).edit().putFloat(K_SCALE, v).apply()
}
