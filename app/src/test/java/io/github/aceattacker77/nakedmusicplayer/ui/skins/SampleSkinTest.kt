package io.github.aceattacker77.nakedmusicplayer.ui.skins

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/** The sample skin shipped in docs/skins/geofront must keep importing as the format evolves. */
@RunWith(RobolectricTestRunner::class)
class SampleSkinTest {
    private val sample = File("../docs/skins/geofront/geofront.mskin")

    @Test fun geofront_importsAndKeepsOnlyWhatItReferences() {
        val reader = SkinArchiveReader(defaults = Skin.FALLBACK, imageProbe = { 1 to 1 }, fontProbe = { true })
        val result = sample.inputStream().use { reader.read(it) }
        val valid = result as? SkinImportResult.Valid ?: error("sample skin rejected: $result")
        assertThat(valid.skin.id).isEqualTo("com.nakedmusic.geofront")
        assertThat(valid.skin.cornerStyle).isEqualTo(CornerStyle.CHAMFER)
        assertThat(valid.skin.player.controls).isEqualTo(ControlsStyle.MIXED)
        assertThat(valid.skin.player.artPlaceholder).isEqualTo(ArtPlaceholder.HEXAGON)
        assertThat(valid.skin.player.artBorder).isTrue()
        assertThat(valid.skin.player.seekBar).isEqualTo(SeekBarStyle.SEGMENTED)
        assertThat(valid.skin.player.seekColor).isEqualTo(SeekColor.TERTIARY)
        assertThat(valid.skin.brackets).isTrue()
        assertThat(valid.skin.segmentedMeters).isTrue()
        assertThat(valid.skin.rowEdge).isTrue()
        assertThat(valid.skin.navStyle).isEqualTo(NavStyle.BLOCK)
        assertThat(valid.skin.statusTags).isTrue()
        assertThat(valid.skin.titleCards).isTrue()
        assertThat(valid.skin.panelHeader).isTrue()
        assertThat(valid.skin.squareSwitch).isTrue()
        assertThat(valid.skin.glow).isEqualTo(GlowMode.DARK)
        assertThat(valid.skin.strings["now_playing_status"]).isEqualTo(SkinString("Playing", "再生"))
        assertThat(valid.skin.strings["unplayable_tag"]).isEqualTo(SkinString("Unplayable", "否決"))
        assertThat(valid.skin.strings["eq_enabled_tag"]).isEqualTo(SkinString("Enabled", "稼働"))
        assertThat(valid.skin.strings["skin_active_tag"]).isEqualTo(SkinString("Active", "適用"))
        assertThat(valid.skin.strings["library_kicker"]).isEqualTo(SkinString("Library", "曲目"))
        assertThat(valid.skin.strings["eq_kicker"]).isEqualTo(SkinString("Audio", "音響"))
        assertThat(valid.entries.keys).containsExactly(
            "skin.json",
            "fonts/ArchivoNarrow-Regular.ttf",
            "fonts/ShipporiMinchoB1-ExtraBold.ttf",
            "fonts/IBMPlexMono-Regular.ttf",
        )
    }
}
