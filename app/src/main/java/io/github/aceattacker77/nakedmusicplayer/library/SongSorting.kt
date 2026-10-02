package io.github.aceattacker77.nakedmusicplayer.library

import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import java.text.Collator

enum class SongSort { TITLE, ARTIST, ALBUM, DATE_ADDED }

private val collator: Collator = Collator.getInstance().apply { strength = Collator.PRIMARY }

private val byTitle = Comparator<Song> { a, b -> collator.compare(a.title, b.title) }
private val byArtist = Comparator<Song> { a, b -> collator.compare(a.artist, b.artist) }
private val byAlbum = Comparator<Song> { a, b -> collator.compare(a.album, b.album) }
private val byTrack = compareBy<Song>({ it.discNumber }, { it.trackNumber })

fun List<Song>.sortedWith(sort: SongSort): List<Song> = when (sort) {
    SongSort.TITLE -> sortedWith(byTitle)
    SongSort.ARTIST -> sortedWith(byArtist.then(byAlbum).then(byTrack).then(byTitle))
    SongSort.ALBUM -> sortedWith(byAlbum.then(byTrack).then(byTitle))
    SongSort.DATE_ADDED -> sortedByDescending { it.dateAddedSec }
}
