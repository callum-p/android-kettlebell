package com.kettlebell.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** A simple bar chart. [values] are non-negative; [labels] align 1:1 under the bars. */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    height: androidx.compose.ui.unit.Dp = 130.dp,
) {
    val max = (values.maxOrNull() ?: 0f).takeIf { it > 0f } ?: 1f
    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().height(height),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            values.forEach { value ->
                val fraction = (value / max).coerceIn(if (value > 0f) 0.03f else 0f, 1f)
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .background(barColor),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** A minimalist line chart (no axes) for a short trend series. */
@Composable
fun TrendLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (values.size < 2) return
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        val min = values.min()
        val max = values.max()
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = if (values.size > 1) size.width / (values.size - 1) else size.width
        fun pointY(v: Float) = size.height - ((v - min) / range) * size.height * 0.9f - size.height * 0.05f

        val path = Path()
        values.forEachIndexed { index, value ->
            val x = stepX * index
            val y = pointY(value)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
        values.forEachIndexed { index, value ->
            drawCircle(lineColor, radius = 7f, center = Offset(stepX * index, pointY(value)))
        }
    }
}

/** A GitHub-style activity heatmap: [weeks] columns of 7 days, coloured on active days. */
@Composable
fun FrequencyHeatmap(
    activeDays: Set<Long>,
    todayEpochDay: Long,
    weeks: Int,
    modifier: Modifier = Modifier,
) {
    val thisMonday = com.kettlebell.app.progress.Progress.weekStart(todayEpochDay)
    val activeColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (weekOffset in (weeks - 1) downTo 0) {
            val weekStart = thisMonday - weekOffset * 7L
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (dayOfWeek in 0..6) {
                    val day = weekStart + dayOfWeek
                    val color = when {
                        day > todayEpochDay -> Color.Transparent
                        day in activeDays -> activeColor
                        else -> emptyColor.copy(alpha = 0.5f)
                    }
                    Box(
                        Modifier
                            .size(13.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(color),
                    )
                }
            }
        }
    }
}
