package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(qualifiers = "w411dp-h900dp")
@RunWith(RobolectricTestRunner::class)
class FastScrollerTest {
    @get:Rule val compose = createComposeRule()

    // A realistic library: A-Z plus some non-Latin headings, more entries than comfortably fit the strip.
    private val letters = ('A'..'Z').toList() + listOf('エ', 'ク', '機', '水', '竹', '重', '野', '鹿', '#')

    private fun indexOf(letter: Char) = letters.indexOf(letter) * 5

    private lateinit var firstVisible: () -> Int

    private fun launch(stripHeightDp: Int) {
        compose.setContent {
            MaterialTheme {
                val state = rememberLazyListState()
                firstVisible = { state.firstVisibleItemIndex }
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                        items((0 until letters.size * 5).toList()) { Text("row $it", Modifier.height(48.dp)) }
                    }
                    Box(Modifier.align(Alignment.CenterEnd).height(stripHeightDp.dp)) {
                        FastScroller(
                            lazyListState = state,
                            letters = letters,
                            indexOfLetter = ::indexOf,
                        )
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun tappingALetter_jumpsToThatLetter_whenTheStripIsCrowded() {
        launch(stripHeightDp = 420) // 35 letters do not fit at their natural height

        listOf('F', 'M', 'S', 'U').forEach { letter ->
            compose.onNodeWithText(letter.toString()).performClick()
            compose.waitForIdle()
            assertThat(firstVisible()).isEqualTo(indexOf(letter))
        }
    }

    @Test fun tappingALetter_jumpsToThatLetter_whenThereIsRoomToSpare() {
        launch(stripHeightDp = 800)

        listOf('B', 'N', 'Y').forEach { letter ->
            compose.onNodeWithText(letter.toString()).performClick()
            compose.waitForIdle()
            assertThat(firstVisible()).isEqualTo(indexOf(letter))
        }
    }
}
