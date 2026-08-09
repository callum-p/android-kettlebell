package com.kettlebell.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class Confetti(
    val emoji: String,
    val angleRad: Float,
    val distance: Float,     // 0..1 fraction of half-screen reached at full progress
    val sizeSp: Float,
    val spin: Float,         // total degrees of rotation over the animation
    val startFraction: Float, // small stagger so they don't all fire on the exact same frame
)

/**
 * A non-interactive full-screen burst of celebratory emoji that explode outward from the centre,
 * spin, and fade over ~3 seconds. Purely decorative — it does not intercept touches.
 */
@Composable
fun CelebrationOverlay(modifier: Modifier = Modifier) {
    val confetti = remember {
        val emojis = listOf("👏", "🎉")
        List(40) { index ->
            Confetti(
                emoji = emojis[index % emojis.size],
                angleRad = Random.nextFloat() * (2f * PI.toFloat()),
                distance = 0.35f + Random.nextFloat() * 0.75f,
                sizeSp = 26f + Random.nextFloat() * 22f,
                spin = (Random.nextFloat() - 0.5f) * 540f,
                startFraction = Random.nextFloat() * 0.2f,
            )
        }
    }

    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(durationMillis = 3000, easing = LinearEasing))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val halfWidth = maxWidth.value / 2f
        val halfHeight = maxHeight.value / 2f

        confetti.forEach { piece ->
            val life = ((progress.value - piece.startFraction) / (1f - piece.startFraction))
                .coerceIn(0f, 1f)

            // Ease-out for the outward flight: fast burst, then settle.
            val flight = 1f - (1f - life) * (1f - life)
            val spreadX = halfWidth * piece.distance
            val spreadY = halfHeight * piece.distance
            val dx = cos(piece.angleRad) * spreadX * flight
            // Add a little gravity so pieces arc downward as they fade.
            val dy = sin(piece.angleRad) * spreadY * flight + (life * life) * halfHeight * 0.25f

            val popIn = (life * 4f).coerceIn(0f, 1f)
            val fade = if (life < 0.45f) 1f else (1f - (life - 0.45f) / 0.55f).coerceIn(0f, 1f)

            Text(
                text = piece.emoji,
                fontSize = piece.sizeSp.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(x = dx.dp, y = dy.dp)
                    .graphicsLayer {
                        rotationZ = piece.spin * life
                        scaleX = popIn
                        scaleY = popIn
                    }
                    .alpha(fade),
            )
        }
    }
}
