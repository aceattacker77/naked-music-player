package io.github.aceattacker77.nakedmusicplayer.ui.skins

import android.content.res.AssetManager

/** The four skins shipped in `assets/skins/`, parsed by the same loader as imported skins. */
object BuiltInSkins {
    const val DEFAULT_ID = "builtin.default"
    private const val PREFIX = "builtin."

    val IDS: List<String> = listOf(DEFAULT_ID, "builtin.vinyl", "builtin.minimal", "builtin.amoled")

    /**
     * Default is parsed against [Skin.FALLBACK]; every other built-in inherits from the parsed Default,
     * exactly as an imported partial skin would.
     */
    fun load(assets: AssetManager, parse: (json: String, defaults: Skin) -> SkinParseResult): List<Skin> {
        val default = parseAsset(assets, DEFAULT_ID, parse, Skin.FALLBACK)
        return IDS.map { id -> if (id == DEFAULT_ID) default else parseAsset(assets, id, parse, default) }
    }

    private fun parseAsset(
        assets: AssetManager,
        id: String,
        parse: (String, Skin) -> SkinParseResult,
        defaults: Skin,
    ): Skin {
        val json = assets.open("skins/${id.removePrefix(PREFIX)}/skin.json").use { it.readBytes().toString(Charsets.UTF_8) }
        return when (val result = parse(json, defaults)) {
            is SkinParseResult.Ok -> result.skin
            is SkinParseResult.Error -> error("built-in skin '$id' is invalid: ${result.message}")
        }
    }
}
