package io.github.aceattacker77.nakedmusicplayer.ui

import kotlinx.serialization.Serializable

/** Type-safe navigation destinations. The first four are the top-level tabs. */
@Serializable data object Songs
@Serializable data object Albums
@Serializable data object Artists
@Serializable data object Playlists

@Serializable data class AlbumDetail(val id: Long)
@Serializable data class ArtistDetail(val id: Long)
@Serializable data class PlaylistDetail(val id: Long)
@Serializable data class SmartPlaylistDetail(val kind: String)

@Serializable data object Search
@Serializable data object Settings
@Serializable data object Equalizer
@Serializable data object Skins
