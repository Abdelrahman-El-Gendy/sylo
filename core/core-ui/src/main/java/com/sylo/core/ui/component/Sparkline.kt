package com.sylo.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sylo.core.ui.theme.SyloBrandCyan

/** A cyan area sparkline for a list of normalised (0..1) points. */
@Composable
fun Sparkline(points: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        if (points.size < 2) return@Canvas
        val stepX = size.width / (points.size - 1)
        fun y(v: Float) = size.height * (1f - v)
        val line = Path().apply {
            moveTo(0f, y(points.first()))
            points.forEachIndexed { i, v -> lineTo(i * stepX, y(v)) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            area,
            brush = Brush.verticalGradient(
                listOf(SyloBrandCyan.copy(alpha = 0.35f), SyloBrandCyan.copy(alpha = 0f)),
            ),
        )
        drawPath(line, color = SyloBrandCyan, style = Stroke(width = 3f))
    }
}
