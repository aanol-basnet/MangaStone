package com.personal.mangastone.ui.components

import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.personal.mangastone.ui.theme.Primary
import com.personal.mangastone.ui.theme.Surface

@Composable
fun GenreFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        modifier = modifier.height(32.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Surface,
            labelColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = Primary,
            selectedLabelColor = Color.White
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) Primary else Color.Transparent,
            selectedBorderColor = Primary
        )
    )
}
