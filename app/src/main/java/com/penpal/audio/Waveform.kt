package com.penpal.audio

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val animatedOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = modifier
            .height(100.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            val barWidth = 4.dp.toPx()
            val barSpacing = 4.dp.toPx()
            val barCount = (size.width / (barWidth + barSpacing)).toInt()
            val centerY = size.height / 2

if (isRecording) {
                for (i in 0 until barCount) {
                    val offset = (i + animatedOffset * barCount) % barCount
                    val normalizedAmp = if (amplitude > 0) {
                        amplitude * (0.3f + 0.7f * kotlin.math.sin(offset * 0.5f + animatedOffset * 6.28f).toFloat())
                    } else {
                        0f
                    }
                    val barHeight = (normalizedAmp * centerY * 0.8f).coerceAtLeast(4.dp.toPx())

                    drawLine(
                        color = barColor,
                        start = Offset(
                            x = i * (barWidth + barSpacing) + barWidth / 2,
                            y = centerY - barHeight / 2
                        ),
                        end = Offset(
                            x = i * (barWidth + barSpacing) + barWidth / 2,
                            y = centerY + barHeight / 2
                        ),
                        strokeWidth = barWidth
                    )
                }
            } else {
                for (i in 0 until barCount) {
                    val barHeight = 4.dp.toPx()
                    drawLine(
                        color = barColor.copy(alpha = 0.3f),
                        start = Offset(
                            x = i * (barWidth + barSpacing) + barWidth / 2,
                            y = centerY - barHeight / 2
                        ),
                        end = Offset(
                            x = i * (barWidth + barSpacing) + barWidth / 2,
                            y = centerY + barHeight / 2
                        ),
                        strokeWidth = barWidth
                    )
                }
            }
        }

        if (isRecording) {
            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopEnd)
            ) {
                RecordingIndicator()
            }
        }
    }
}

@Composable
fun RecordingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color.Red.copy(alpha = alpha))
    )
}

@Composable
fun PlaybackWaveform(
    progress: Float,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    playedColor: Color = MaterialTheme.colorScheme.primary,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Canvas(
        modifier = modifier
            .height(60.dp)
            .fillMaxWidth()
    ) {
        val barWidth = 3.dp.toPx()
        val barSpacing = 2.dp.toPx()
        val barCount = (size.width / (barWidth + barSpacing)).toInt()
        val centerY = size.height / 2

        for (i in 0 until barCount) {
            val randomHeight = ((kotlin.math.sin(i * 0.5f) + 1f) / 2 * 0.6f + 0.2f).toFloat()
            val barHeight = randomHeight * centerY * 0.8f
            val x = i * (barWidth + barSpacing)
            val progressX = progress * size.width

            val color = if (x < progressX) playedColor else barColor.copy(alpha = 0.3f)

            drawLine(
                color = color,
                start = Offset(x = x + barWidth / 2, y = centerY - barHeight / 2),
                end = Offset(x = x + barWidth / 2, y = centerY + barHeight / 2),
                strokeWidth = barWidth
            )
        }
    }
}