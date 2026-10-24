package com.d4rkwell.workouttracker.ui.components

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * Drag state for one reorderable list. [itemHeight] must be the fixed
 * height of each row (including the spacing between rows) in dp, since
 * touch drag distance is translated into "how many rows did this move"
 * by dividing by that height — this only works cleanly for fixed-height
 * rows, which every reorderable list in this app uses.
 *
 * [onMove] is called live as the dragged item crosses into a neighboring
 * row's position (so the list visually reorders as you drag, not just
 * once you release), and [onDragEnd] once you lift your finger, to
 * persist the final order.
 */
class DragReorderState(
    private val itemCount: () -> Int,
    private val itemHeightPx: Float,
    private val onMove: (from: Int, to: Int) -> Unit,
    private val onDragEnd: () -> Unit
) {
    var draggedIndex by mutableStateOf(-1)
        private set
    var dragOffset by mutableStateOf(0f)
        private set

    fun start(index: Int) {
        draggedIndex = index
        dragOffset = 0f
    }

    fun drag(delta: Float) {
        if (draggedIndex < 0) return
        dragOffset += delta
        val moveBy = (dragOffset / itemHeightPx).roundToInt()
        if (moveBy != 0) {
            val newIndex = (draggedIndex + moveBy).coerceIn(0, itemCount() - 1)
            if (newIndex != draggedIndex) {
                onMove(draggedIndex, newIndex)
                draggedIndex = newIndex
                dragOffset -= moveBy * itemHeightPx
            }
        }
    }

    fun end() {
        draggedIndex = -1
        dragOffset = 0f
        onDragEnd()
    }
}

@Composable
fun rememberDragReorderState(
    itemCount: Int,
    itemHeight: Dp = 72.dp,
    onMove: (from: Int, to: Int) -> Unit,
    onDragEnd: () -> Unit
): DragReorderState {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeightPx = with(density) { itemHeight.toPx() }
    return remember(itemCount, itemHeightPx) {
        DragReorderState(
            itemCount = { itemCount },
            itemHeightPx = itemHeightPx,
            onMove = onMove,
            onDragEnd = onDragEnd
        )
    }
}

/** Applies the visual drag offset + elevation to a row while it's being dragged. */
fun Modifier.dragReorderOffset(state: DragReorderState, index: Int): Modifier {
    return if (state.draggedIndex == index) {
        this
            .zIndex(1f)
            .graphicsLayer { translationY = state.dragOffset }
    } else {
        this
    }
}

/**
 * A small grab handle: long-press and drag vertically to reorder. Placed
 * at the start of a row alongside [dragReorderOffset] on that row's
 * outer modifier.
 */
@Composable
fun DragHandleGrip(state: DragReorderState, index: Int, modifier: Modifier = Modifier) {
    Icon(
        Icons.Filled.DragHandle,
        contentDescription = "Drag to reorder",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .pointerInput(index) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { state.start(index) },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        state.drag(dragAmount.y)
                    },
                    onDragEnd = { state.end() },
                    onDragCancel = { state.end() }
                )
            }
    )
}
