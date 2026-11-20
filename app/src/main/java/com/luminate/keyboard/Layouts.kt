package com.abdullah.keyboard

enum class Layer { LETTERS, SYMBOLS }

enum class KeyType(val special: Boolean) {
    CHAR(false), SPACE(false), SHIFT(true), DEL(true), ENTER(true),
    MODE_SYM(true), MODE_ABC(true), GLOBE(false), SPACER(false)
}

data class Key(
    val label: String,
    val type: KeyType = KeyType.CHAR,
    val weight: Float = 1f,
    val hint: String? = null
)

object Layouts {
    private fun chars(s: String, hints: String? = null) =
        s.mapIndexed { i, ch -> Key(ch.toString(), hint = hints?.getOrNull(i)?.toString()) }

    private fun spacer(w: Float) = Key("", KeyType.SPACER, w)

    private val bottomLetters = listOf(
        Key("", KeyType.MODE_SYM, 1.5f), Key(","), Key("", KeyType.GLOBE, 1f),
        Key(" ", KeyType.SPACE, 4f), Key("."), Key("", KeyType.ENTER, 1.5f)
    )
    private val bottomSymbols = listOf(
        Key("", KeyType.MODE_ABC, 1.5f), Key(","), Key("", KeyType.GLOBE, 1f),
        Key(" ", KeyType.SPACE, 4f), Key("."), Key("", KeyType.ENTER, 1.5f)
    )

    private val letters = listOf(
        chars("qwertyuiop", "1234567890"),
        listOf(spacer(0.5f)) + chars("asdfghjkl") + spacer(0.5f),
        listOf(Key("", KeyType.SHIFT, 1.5f)) + chars("zxcvbnm") + Key("", KeyType.DEL, 1.5f),
        bottomLetters
    )

    private val symbols = listOf(
        chars("1234567890"),
        chars("@#\$%&-+()/"),
        listOf(spacer(1.5f)) + chars("*\"':;!?") + Key("", KeyType.DEL, 1.5f),
        bottomSymbols
    )

    fun rows(layer: Layer): List<List<Key>> = if (layer == Layer.LETTERS) letters else symbols
}
