package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/**
 * A square switch: a 52 by 28 dp track with a 1 dp border and a 22 dp square thumb, `tertiary` when on. It is a real
 * toggleable with the switch role; the touch target is at least 48 dp, and a null [onCheckedChange] disables it.
 */
@Composable
fun GeoSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .testTag("geo-switch")
            .minimumInteractiveComponentSize()
            .toggleable(
                value = checked,
                enabled = onCheckedChange != null,
                role = Role.Switch,
                onValueChange = { onCheckedChange?.invoke(it) },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 28.dp)
                .border(1.dp, colors.outline)
                .background(if (checked) colors.tertiary else colors.surfaceVariant)
                .padding(3.dp),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(Modifier.size(22.dp).background(if (checked) colors.onTertiary else colors.outline))
        }
    }
}
