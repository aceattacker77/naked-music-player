package io.github.aceattacker77.nakedmusicplayer.ui.components

import io.github.aceattacker77.nakedmusicplayer.ui.theme.originalLabel
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinLabel

/** One destination of [GeoNavigationBar]. */
data class GeoTab(val iconRes: Int, val label: String, val tag: String)

/**
 * The compact bottom bar for skins that ask for `navStyle: block`: the selected tab is a block in the
 * theme's medium shape (a chamfer in a chamfer skin) holding an `onPrimary` icon and label.
 */
@Composable
fun GeoNavigationBar(
    tabs: List<GeoTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .selectableGroup()
            .testTag("geo-nav"),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier
                    .weight(1f)
                    .testTag(tab.tag)
                    .clip(shape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .heightIn(min = 56.dp)
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(painterResource(tab.iconRes), contentDescription = null, tint = content)
                Text(
                    text = skinLabel(tab.label),
                    modifier = Modifier.originalLabel(tab.label),
                    style = MaterialTheme.typography.labelSmall,
                    color = content,
                    maxLines = 1,
                    // Tracked caps in a fixed-width cell: shrink toward a floor instead of clipping the word.
                    autoSize = TextAutoSize.StepBased(minFontSize = 8.sp, maxFontSize = 12.sp),
                )
            }
        }
    }
}
