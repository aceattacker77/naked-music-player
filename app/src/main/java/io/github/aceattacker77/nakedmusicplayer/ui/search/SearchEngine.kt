package io.github.aceattacker77.nakedmusicplayer.ui.search

import io.github.aceattacker77.nakedmusicplayer.library.model.Album
import io.github.aceattacker77.nakedmusicplayer.library.model.Artist
import io.github.aceattacker77.nakedmusicplayer.library.model.Library
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import java.text.Normalizer
import java.util.Locale

data class SearchResults(
    val songs: List<Song>,
    val albums: List<Album>,
    val artists: List<Artist>,
) {
    companion object {
        val EMPTY = SearchResults(emptyList(), emptyList(), emptyList())
    }
}

/** Case- and diacritic-insensitive substring search over titles, artists and albums. */
object SearchEngine {
    private const val MAX_SONGS = 100
    private const val FIELD_SEPARATOR = '\u0000'
    private val combiningMarks = Regex("\\p{M}+")

    /** Normalised search keys, built once per library so each keystroke is just `contains` checks. */
    private class Index(val library: Library) {
        val songKeys = library.songs.map { key(it.title, it.artist, it.album) }
        val albumKeys = library.albums.map { key(it.title) }
        val artistKeys = library.artists.map { key(it.name) }
    }

    @Volatile
    private var cached: Index? = null

    fun search(library: Library, query: String): SearchResults {
        val needle = normalize(query.trim())
        if (needle.isEmpty()) return SearchResults.EMPTY
        val index = cached?.takeIf { it.library === library } ?: Index(library).also { cached = it }
        return SearchResults(
            songs = library.songs.filterIndexed { i, _ -> needle in index.songKeys[i] }.take(MAX_SONGS),
            albums = library.albums.filterIndexed { i, _ -> needle in index.albumKeys[i] },
            artists = library.artists.filterIndexed { i, _ -> needle in index.artistKeys[i] },
        )
    }

    // Fields are joined with a character no query can contain, so a match never spans two fields.
    private fun key(vararg fields: String): String = fields.joinToString(FIELD_SEPARATOR.toString()) { normalize(it) }

    private fun normalize(text: String): String =
        Normalizer.normalize(text, Normalizer.Form.NFD).replace(combiningMarks, "").lowercase(Locale.ROOT)
}
