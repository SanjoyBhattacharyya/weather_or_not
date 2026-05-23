package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated Dynamic Weather Background component.
 *
 * Designed securely to run at high frames-per-second without causing memory leak
 * or main thread lockups/deadlocks. It strictly separates animation ticker cycles
 * from Compose state writes: state mutations are kept in pure standard objects (non-State),
 * and only the animation frame triggers draw pass evaluations.
 */

// Simple data classes with mutable coordinates for manual UI rendering loops
class RaindropData(
    var x: Float,
    var y: Float,
    val length: Float,
    val speed: Float,
    val alpha: Float,
    val width: Float
)

class SnowflakeData(
    var x: Float,
    var y: Float,
    val radius: Float,
    val speed: Float,
    var angle: Float,
    val angleSpeed: Float,
    val alpha: Float
)

class StarData(
    val x: Float,
    val y: Float,
    val size: Float,
    val alpha: Float,
    val twinkleSpeed: Float,
    val twinkleOffset: Float
)

class FloatingCloudData(
    var x: Float,
    val y: Float,
    val scale: Float,
    val speed: Float,
    val alpha: Float
)

class ShootingStarData(
    var x: Float = 0f,
    var y: Float = 0f,
    var targetX: Float = 0f,
    var targetY: Float = 0f,
    var progress: Float = -1f,
    var speed: Float = 0f,
    var active: Boolean = false
)

// Non-reactive memory state class to hold rendering assets securely
class ParticleStateHolder(val random: Random = Random(System.currentTimeMillis())) {
    val raindrops = ArrayList<RaindropData>()
    val snowflakes = ArrayList<SnowflakeData>()
    val stars = ArrayList<StarData>()
    val clouds = ArrayList<FloatingCloudData>()
    var shootingStar = ShootingStarData()

    var initializedWidth = -1f
    var initializedHeight = -1f

    fun initialize(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        initializedWidth = width
        initializedHeight = height

        // 1. Raindrops Setup
        raindrops.clear()
        for (i in 0 until 60) {
            raindrops.add(
                RaindropData(
                    x = random.nextFloat() * width,
                    y = random.nextFloat() * height,
                    length = 25f + random.nextFloat() * 20f,
                    speed = 22f + random.nextFloat() * 15f,
                    alpha = 0.3f + random.nextFloat() * 0.5f,
                    width = 1.8f + random.nextFloat() * 1.5f
                )
            )
        }

        // 2. Snowflakes Setup
        snowflakes.clear()
        for (i in 0 until 50) {
            snowflakes.add(
                SnowflakeData(
                    x = random.nextFloat() * width,
                    y = random.nextFloat() * height,
                    radius = 4f + random.nextFloat() * 7f,
                    speed = 2f + random.nextFloat() * 3f,
                    angle = random.nextFloat() * 2 * Math.PI.toFloat(),
                    angleSpeed = 0.02f + random.nextFloat() * 0.04f,
                    alpha = 0.4f + random.nextFloat() * 0.5f
                )
            )
        }

        // 3. Stars Setup
        stars.clear()
        for (i in 0 until 45) {
            stars.add(
                StarData(
                    x = random.nextFloat() * width,
                    y = random.nextFloat() * height * 0.65f,
                    size = 1.5f + random.nextFloat() * 2.5f,
                    alpha = 0.1f + random.nextFloat() * 0.8f,
                    twinkleSpeed = 0.03f + random.nextFloat() * 0.05f,
                    twinkleOffset = random.nextFloat() * 100f
                )
            )
        }

        // 4. Clouds Setup
        clouds.clear()
        for (i in 0 until 4) {
            clouds.add(
                FloatingCloudData(
                    x = random.nextFloat() * width - 150f,
                    y = 100f + random.nextFloat() * height * 0.35f,
                    scale = 0.7f + random.nextFloat() * 0.8f,
                    speed = 0.3f + random.nextFloat() * 0.5f,
                    alpha = 0.15f + random.nextFloat() * 0.2f
                )
            )
        }
    }
}

@Composable
fun DynamicWeatherBackground(
    currentTheme: String,
    weatherCode: Int,
    isNight: Boolean,
    modifier: Modifier = Modifier
) {
    // Render only when dynamic theme mode is selected to conserve processor battery resources
    if (currentTheme != "Animated") {
        return
    }

    // Classify weather states from official WMO code list
    val isRainy = weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)
    val isSnowy = weatherCode in listOf(71, 73, 75, 77, 85, 86)
    val isCloudy = weatherCode in listOf(2, 3, 45, 48)
    val isThunderstorm = weatherCode in listOf(95, 96, 99)

    // Infinite cyclic frame counter used purely to force regular surface redraw ticks
    val infiniteTransition = rememberInfiniteTransition(label = "background_frame_ticks")
    val frameCount by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 10000f,
        animationSpec = infiniteRepeatable(
            animation = tween(120000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuous_clock"
    )

    // Secure persistent game model storage container (recreated only on theme switches or cleanup)
    val stateHolder = remember { ParticleStateHolder() }

    // Coroutine managing thunderstorm overlay flash transitions safely
    var thunderFlashAlpha by remember { mutableStateOf(0f) }
    if (isThunderstorm) {
        val rand = stateHolder.random
        LaunchedEffect(isNight) {
            while (true) {
                // Occurs every 4 to 12 seconds
                val nextDelay = 4000L + rand.nextInt(8000).toLong()
                kotlinx.coroutines.delay(nextDelay)

                // Render Strike Flash 1
                thunderFlashAlpha = if (isNight) 0.45f else 0.65f
                kotlinx.coroutines.delay(70)
                thunderFlashAlpha = 0.0f
                kotlinx.coroutines.delay(80)

                // Instant secondary strike
                if (rand.nextFloat() < 0.65f) {
                    thunderFlashAlpha = if (isNight) 0.3f else 0.45f
                    kotlinx.coroutines.delay(50)
                    thunderFlashAlpha = 0.0f
                }
            }
        }
    } else {
        thunderFlashAlpha = 0f
    }

    // Sun rays rotation degrees directly derived from frameCount to prevent state loops
    val sunRotationAngle = (frameCount * 0.15f) % 360f

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val width = size.width
            val height = size.height

            // Screen resizing initialization check (perfectly safe because state is NOT a compose State)
            if (width > 0 && height > 0 &&
                (stateHolder.initializedWidth != width || stateHolder.initializedHeight != height)) {
                stateHolder.initialize(width, height)
            }

            // --- 1. RENDER STARS & CRESCENT MOON (Night Theme Canopy) ---
            if (isNight) {
                // Ensure list elements are filled
                if (stateHolder.stars.isEmpty()) {
                    stateHolder.initialize(width, height)
                }

                // Render twinkling star fields
                for (star in stateHolder.stars) {
                    // Unique sine shift for beautiful individual twinkling
                    val oscillation = sin(frameCount * star.twinkleSpeed + star.twinkleOffset)
                    val starAlpha = (star.alpha + (oscillation * 0.42f)).coerceIn(0.12f, 1.0f)

                    drawCircle(
                        color = Color.White.copy(alpha = starAlpha),
                        radius = star.size,
                        center = Offset(star.x, star.y)
                    )
                }

                // Manage shooting star trajectories on-demand
                val sStar = stateHolder.shootingStar
                val rand = stateHolder.random
                if (!sStar.active && rand.nextFloat() < 0.002f) {
                    val sX = rand.nextFloat() * width * 0.8f
                    val sY = rand.nextFloat() * height * 0.35f
                    sStar.x = sX
                    sStar.y = sY
                    sStar.targetX = sX + 220f + rand.nextFloat() * 180f
                    sStar.targetY = sY + 130f + rand.nextFloat() * 100f
                    sStar.progress = 0f
                    sStar.speed = 0.06f + rand.nextFloat() * 0.06f
                    sStar.active = true
                }

                if (sStar.active) {
                    sStar.progress += sStar.speed
                    if (sStar.progress >= 1f) {
                        sStar.active = false
                    } else {
                        val currX = sStar.x + (sStar.targetX - sStar.x) * sStar.progress
                        val currY = sStar.y + (sStar.targetY - sStar.y) * sStar.progress

                        val tailProgress = (sStar.progress - 0.22f).coerceAtLeast(0f)
                        val startX = sStar.x + (sStar.targetX - sStar.x) * tailProgress
                        val startY = sStar.y + (sStar.targetY - sStar.y) * tailProgress

                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.75f)),
                                start = Offset(startX, startY),
                                end = Offset(currX, currY)
                            ),
                            start = Offset(startX, startY),
                            end = Offset(currX, currY),
                            strokeWidth = 2.4f
                        )
                    }
                }

                // Crescent Moon Glowing elements
                val moonX = width * 0.82f
                val moonY = height * 0.12f
                val moonRadius = 45f

                // Outer Moon Glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFEF08A).copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(moonX, moonY),
                        radius = moonRadius * 2.8f
                    ),
                    radius = moonRadius * 2.8f,
                    center = Offset(moonX, moonY)
                )

                // Clear moon body
                drawCircle(
                    color = Color(0xFFFEF3C7),
                    radius = moonRadius,
                    center = Offset(moonX, moonY)
                )

                // Subtractive offset shadow circle to carve a sharp yellow crescent shape
                drawCircle(
                    color = if (isRainy) Color(0xFF1E1E2C) else Color(0xFF0B0E14),
                    radius = moonRadius * 0.94f,
                    center = Offset(moonX - moonRadius * 0.44f, moonY - moonRadius * 0.16f)
                )
            }

            // --- 2. RENDER SHINING SOLAR DISC (Day / Radiant Atmosphere) ---
            if (!isNight && !isRainy && !isCloudy && !isSnowy) {
                val sunX = width * 0.82f
                val sunY = height * 0.12f
                val sunRadius = 55f

                // Pulsate glowing shell over time
                val glowScaler = 1.0f + 0.12f * sin(frameCount * 0.05f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFDBA74).copy(alpha = 0.24f), Color.Transparent),
                        center = Offset(sunX, sunY),
                        radius = sunRadius * 3.5f * glowScaler
                    ),
                    radius = sunRadius * 3.5f * glowScaler,
                    center = Offset(sunX, sunY)
                )

                // Inner core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFEF08A), Color(0xFFF97316)),
                        center = Offset(sunX, sunY),
                        radius = sunRadius
                    ),
                    radius = sunRadius,
                    center = Offset(sunX, sunY)
                )

                // Sunshine rays rotating softly
                rotate(degrees = sunRotationAngle, pivot = Offset(sunX, sunY)) {
                    val rayCount = 8
                    val rayLen = sunRadius * 0.55f
                    for (i in 0 until rayCount) {
                        val angleDeg = (i * 360f / rayCount)
                        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                        val startRadDist = sunRadius * 1.25f
                        val endRadDist = startRadDist + rayLen

                        drawLine(
                            color = Color(0xFFFDBA74).copy(alpha = 0.65f),
                            start = Offset(sunX + cos(angleRad) * startRadDist, sunY + sin(angleRad) * startRadDist),
                            end = Offset(sunX + cos(angleRad) * endRadDist, sunY + sin(angleRad) * endRadDist),
                            strokeWidth = 6f
                        )
                    }
                }
            }

            // --- 3. RENDER STRATUS OVERCAST & DRIFTS (Cloudy Layouts) ---
            if (isCloudy || isRainy || isSnowy) {
                if (stateHolder.clouds.isEmpty()) {
                    stateHolder.initialize(width, height)
                }

                for (cloud in stateHolder.clouds) {
                    // Update movement coordinate (perfectly safe inside draw loop because state is not a compose State)
                    cloud.x += cloud.speed
                    if (cloud.x > width + 220f) {
                        cloud.x = -250f
                    }

                    val cloudBrush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFCBD5E1).copy(alpha = cloud.alpha),
                            Color(0xFFCBD5E1).copy(alpha = 0f)
                        ),
                        center = Offset(cloud.x, cloud.y),
                        radius = 160f * cloud.scale
                    )

                    drawCircle(
                        brush = cloudBrush,
                        radius = 160f * cloud.scale,
                        center = Offset(cloud.x, cloud.y)
                    )
                    drawCircle(
                        brush = cloudBrush,
                        radius = 110f * cloud.scale,
                        center = Offset(cloud.x + 80f * cloud.scale, cloud.y - 30f * cloud.scale)
                    )
                    drawCircle(
                        brush = cloudBrush,
                        radius = 110f * cloud.scale,
                        center = Offset(cloud.x - 80f * cloud.scale, cloud.y + 10f * cloud.scale)
                    )
                }
            }

            // --- 4. RENDER PRECIPITATION STREAKS (Rain & Windy Showers) ---
            if (isRainy) {
                if (stateHolder.raindrops.isEmpty()) {
                    stateHolder.initialize(width, height)
                }

                val breezeTilt = 4f
                for (drop in stateHolder.raindrops) {
                    // Update raindrop coordinates (not using compose States, perfectly safe!)
                    drop.y += drop.speed
                    drop.x += breezeTilt * (drop.speed / 25f)

                    if (drop.y > height) {
                        drop.y = -drop.length
                        drop.x = stateHolder.random.nextFloat() * width
                    }
                    if (drop.x > width + 50f) {
                        drop.x = -30f
                    }

                    drawLine(
                        color = Color(0xFF60A5FA).copy(alpha = drop.alpha),
                        start = Offset(drop.x, drop.y),
                        end = Offset(drop.x + breezeTilt * (drop.length / 30f), drop.y + drop.length),
                        strokeWidth = drop.width
                    )
                }
            }

            // --- 5. RENDER FROST CRYSTALS (Winter Snowfall) ---
            if (isSnowy) {
                if (stateHolder.snowflakes.isEmpty()) {
                    stateHolder.initialize(width, height)
                }

                for (flake in stateHolder.snowflakes) {
                    // Fluttering angle fluctuations
                    flake.angle += flake.angleSpeed
                    flake.y += flake.speed
                    flake.x += sin(flake.angle) * 1.1f

                    if (flake.y > height) {
                        flake.y = -15f
                        flake.x = stateHolder.random.nextFloat() * width
                    }

                    drawCircle(
                        color = Color.White.copy(alpha = flake.alpha),
                        radius = flake.radius,
                        center = Offset(flake.x, flake.y)
                    )
                }
            }

            // --- 6. RENDER FLASH STRIP (Transient Storm Lightning overlay) ---
            if (thunderFlashAlpha > 0f) {
                drawRect(
                    color = Color.White.copy(alpha = thunderFlashAlpha),
                    size = size
                )
            }
        }
    }
}
