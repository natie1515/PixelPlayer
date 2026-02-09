package com.theveloper.pixelplay.presentation.components.scoped

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal data class CastSheetState(
    val showCastSheet: Boolean,
    val castSheetOpenFraction: Float,
    val openCastSheet: () -> Unit,
    val dismissCastSheet: () -> Unit,
    val onCastExpansionChanged: (Float) -> Unit
)

@Composable
internal fun rememberCastSheetState(): CastSheetState {
    var showCastSheet by remember { mutableStateOf(false) }
    var castSheetOpenFraction by remember { mutableFloatStateOf(0f) }

    val openCastSheet = remember {
        { showCastSheet = true }
    }
    val dismissCastSheet = remember {
        {
            castSheetOpenFraction = 0f
            showCastSheet = false
        }
    }
    val onCastExpansionChanged = remember {
        { fraction: Float ->
            castSheetOpenFraction = fraction
        }
    }

    return CastSheetState(
        showCastSheet = showCastSheet,
        castSheetOpenFraction = castSheetOpenFraction,
        openCastSheet = openCastSheet,
        dismissCastSheet = dismissCastSheet,
        onCastExpansionChanged = onCastExpansionChanged
    )
}
