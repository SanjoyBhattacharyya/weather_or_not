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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Animated Dynamic Weather Background component.
 *
 * This system renders an immersive, high-performance particle-based background
 * corresponding to the current time of day and WMO weather conditions.
 *
 * Supported Scenes:
 *  - Rain & Thunderstorms (with dynamic wind drift and optional thunder light-flashes)
 *  - Snowy / Winter fall (with horizontal drift sway/swimming patterns)
 *  - Overcast & Cloud drifts (large smooth semi-transparent floating cloud shapes)
 *  - Sunny / Radiant Sky (glowing warm core with slow-rotating geometric solar rays)
 *  - Moonlit Twinkling Night (star field with random individual twinkle rates, crescent moon & occasional shooting stars)
 *
 * Extension & Extension Points:
 *  - To add a new weather effect (e.g. Tornado, Sandstorm):
 *    1. Create a data representation class (e.g. DustParticle).
 *    2. Add status variables to the game loop.
 *    3. In the Canvas update list, draw the points and update positions.
 */

// Data classes for particle systems
data class Raindrop(
    var x: Float,
    var y: Float,
    var length: Float,
    var speed: Float,
    var alpha: Float,
    var width: Float
)

data class Snowflake(
    var x: Float,
    var y: Float,
    var radius: Float,
    var speed: Float,
    var angle: Float,
    var angleSpeed: Float,
    var alpha: Float
)

data class Star(
    var x: Float,
    var y: Float,
    var size: Float,
    var alpha: Float,
    var twinkleSpeed: Float,
    var twinkleOffset: Float
)

data class FloatingCloud(
    var x: Float,
    var y: Float,
    var scale: Float,
    var speed: Float,
    var alpha: Float
)

data class ShootingStar(
    var x: Float,
    var y: Float,
    var targetX: Float,
    var targetY: Float,
    var progress: Float, // 0.0 to 1.0
    var speed: Float,
    var active: Boolean
)

@Composable
fun DynamicWeatherBackground(
    currentTheme: String,
    weatherCode: Int,
    isNight: Boolean,
    modifier: Modifier = Modifier
) {
    // If we're not using the Animated theme, render a clean fallback or don't render animations to save battery.
    if (currentTheme != "Animated") {
        return
    }

    // Determine the active weather type
    val isRainy = weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)
    val isSnowy = weatherCode in listOf(71, 73, 75, 77, 85, 86)
    val isCloudy = weatherCode in listOf(2, 3, 45, 48)
    val isThunderstorm = weatherCode in listOf(95, 96, 99)

    // Game loop tick count using animation state
    val infiniteTransition = rememberInfiniteTransition(label = "weather_ticks")
    val frameCount by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "frame_generator"
    )

    // Keep state lists for particles initialized on demand
    val raindrops = remember { mutableStateListOf<Raindrop>() }
    val snowflakes = remember { mutableStateListOf<Snowflake>() }
    val stars = remember { mutableStateListOf<Star>() }
    val clouds = remember { mutableStateListOf<FloatingCloud>() }
    var shootingStar by remember { mutableStateOf(ShootingStar(0f, 0f, 0f, 0f, -1f, 0f, false)) }

    // Thunder flash trigger count & state
    var thunderFlashAlpha by remember { mutableStateOf(0f) }

    // Soft rotation states for solar rays
    var sunRotationAngle by remember { mutableStateOf(0f) }

    // Re-initialize particles when viewport size changes or when scene type switches
    var canvasSizeWidth by remember { mutableStateOf(1080f) }
    var canvasSizeHeight by remember { mutableStateOf(1920f) }

    val random = remember { Random(System.currentTimeMillis()) }

    fun reinitializeParticles(width: Float, height: Float) {
        canvasSizeWidth = width
        canvasSizeHeight = height

        // 1. Raindrops Setup
        raindrops.clear()
        for (i in 0 until 60) {
            raindrops.add(
                Raindrop(
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
                Snowflake(
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
                Star(
                    x = random.nextFloat() * width,
                    y = random.nextFloat() * height * 0.65f, // Mostly top part
                    size = 1.5f + random.nextFloat() * 2.5f,
                    alpha = 0.1f + random.nextFloat() * 0.8f,
                    twinkleSpeed = 0.05f + random.nextFloat() * 0.08f,
                    twinkleOffset = random.nextFloat() * 100f
                )
            )
        }

        // 4. Clouds Setup
        clouds.clear()
        for (i in 0 until 4) {
            clouds.add(
                FloatingCloud(
                    x = random.nextFloat() * width - 150f,
                    y = 100f + random.nextFloat() * height * 0.35f,
                    scale = 0.7f + random.nextFloat() * 0.8f,
                    speed = 0.3f + random.nextFloat() * 0.5f,
                    alpha = 0.15f + random.nextFloat() * 0.2f
                )
            )
        }
    }

    // Initialize/sync on first layout sizing
    LaunchedEffect(Unit) {
        reinitializeParticles(1080f, 1920f)
    }

    // Periodic lightning flash generator inside a Thunderstorm
    if (isThunderstorm && !isNight) {
        LaunchedEffect(frameCount) {
            if (random.nextFloat() < 0.015f) {
                // Flash 1
                thunderFlashAlpha = 0.7f
                kotlinx.coroutines.delay(80)
                thunderFlashAlpha = 0.0f
                kotlinx.coroutines.delay(100)
                // Double Flash
                thunderFlashAlpha = 0.5f
                kotlinx.coroutines.delay(60)
                thunderFlashAlpha = 0f
            }
        }
    } else if (isThunderstorm && isNight) {
        LaunchedEffect(frameCount) {
            if (random.nextFloat() < 0.012f) {
                thunderFlashAlpha = 0.5f
                kotlinx.coroutines.delay(120)
                thunderFlashAlpha = 0.0f
            }
        }
    }

    // Slow sun turning rotation
    LaunchedEffect(frameCount) {
        sunRotationAngle = (sunRotationAngle + 0.15f) % 360f
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val width = size.width
            val height = size.height

            // Detect if size has drastically changed (screen rot, fold, dynamic load)
            if (width > 0 && height > 0 && (Math.abs(canvasSizeWidth - width) > 10f || Math.abs(canvasSizeHeight - height) > 10f)) {
                reinitializeParticles(width, height)
            }

            // --- 1. RENDER STARS / MOON (Night Scene) ---
            if (isNight) {
                // Ensure array lists are non-empty
                if (stars.isEmpty()) {
                    reinitializeParticles(width, height)
                }

                // Draw stars
                for (star in stars) {
                    // Unique oscillating twinkle formula
                    val oscillation = sin(frameCount * star.twinkleSpeed + star.twinkleOffset)
                    val currentAlpha = (star.alpha + (oscillation * 0.4f)).coerceIn(0.1f, 1.0f)

                    drawCircle(
                        color = Color.White.copy(alpha = currentAlpha),
                        radius = star.size,
                        center = Offset(star.x, star.y)
                    )
                }

                // Random periodic Shooting Star
                if (!shootingStar.active && random.nextFloat() < 0.003f) {
                    val sX = random.nextFloat() * width * 0.8f
                    val sY = random.nextFloat() * height * 0.3f
                    shootingStar = ShootingStar(
                        x = sX,
                        y = sY,
                        targetX = sX + 250f + random.nextFloat() * 150f,
                        targetY = sY + 150f + random.nextFloat() * 100f,
                        progress = 0f,
                        speed = 0.08f + random.nextFloat() * 0.06f,
                        active = true
                    )
                }

                if (shootingStar.active) {
                    val nextProgress = shootingStar.progress + shootingStar.speed
                    if (nextProgress >= 1f) {
                        shootingStar = shootingStar.copy(active = false)
                    } else {
                        shootingStar = shootingStar.copy(progress = nextProgress)
                        val currX = shootingStar.x + (shootingStar.targetX - shootingStar.x) * nextProgress
                        val currY = shootingStar.y + (shootingStar.targetY - shootingStar.y) * nextProgress
                        val startX = shootingStar.x + (shootingStar.targetX - shootingStar.x) * (nextProgress - 0.25f).coerceAtLeast(0f)
                        val startY = shootingStar.y + (shootingStar.targetY - shootingStar.y) * (nextProgress - 0.25f).coerceAtLeast(0f)

                        drawLine(
                            brush = Brush.linearGradient(
                                colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.8f)),
                                start = Offset(startX, startY),
                                end = Offset(currX, currY)
                            ),
                            start = Offset(startX, startY),
                            end = Offset(currX, currY),
                            strokeWidth = 2.5f
                        )
                    }
                }

                // Render beautiful Glowing Crescent Moon at the top right
                val moonCenterX = width * 0.82f
                val moonCenterY = height * 0.12f
                val moonRadius = 45f

                // Large soft moon halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFEF08A).copy(alpha = 0.18f), Color.Transparent),
                        center = Offset(moonCenterX, moonCenterY),
                        radius = moonRadius * 2.8f
                    ),
                    radius = moonRadius * 2.8f,
                    center = Offset(moonCenterX, moonCenterY)
                )

                // Beautiful crisp yellow/white crescent moon using offset subtraction illusion
                drawCircle(
                    color = Color(0xFFFEF3C7),
                    radius = moonRadius,
                    center = Offset(moonCenterX, moonCenterY)
                )

                // Subtract shadow to form crescent
                drawCircle(
                    color = if (isRainy) Color(0xFF1E1E2C) else Color(0xFF0B0E14), // blending to dark background
                    radius = moonRadius * 0.95f,
                    center = Offset(moonCenterX - moonRadius * 0.42f, moonCenterY - moonRadius * 0.15f)
                )
            }

            // --- 2. RENDER WARM GLOWING SUN (Day & Clear Scene) ---
            if (!isNight && !isRainy && !isCloudy && !isSnowy) {
                val sunCenterX = width * 0.82f
                val sunCenterY = height * 0.12f
                val sunRadius = 55f

                // Sun Halo Ambient glow (pulsing over time)
                val pulseRatio = 1.0f + 0.12f * sin(frameCount * 0.04f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFDBA74).copy(alpha = 0.25f), Color.Transparent),
                        center = Offset(sunCenterX, sunCenterY),
                        radius = sunRadius * 3.4f * pulseRatio
                    ),
                    radius = sunRadius * 3.4f * pulseRatio,
                    center = Offset(sunCenterX, sunCenterY)
                )

                // Inner yellow core
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFEF08A), Color(0xFFF97316)),
                        center = Offset(sunCenterX, sunCenterY),
                        radius = sunRadius
                    ),
                    radius = sunRadius,
                    center = Offset(sunCenterX, sunCenterY)
                )

                // Dynamic rotating crown rays
                rotate(degrees = sunRotationAngle, pivot = Offset(sunCenterX, sunCenterY)) {
                    val rayCount = 8
                    val rayLength = sunRadius * 0.55f
                    for (i in 0 until rayCount) {
                        val angleDeg = (i * 360f / rayCount)
                        val angleRad = Math.toRadians(angleDeg.toDouble()).toFloat()
                        val startDistance = sunRadius * 1.2f
                        val endDistance = startDistance + rayLength

                        val start = Offset(
                            x = sunCenterX + cos(angleRad) * startDistance,
                            y = sunCenterY + sin(angleRad) * startDistance
                        )
                        val end = Offset(
                            x = sunCenterX + cos(angleRad) * endDistance,
                            y = sunCenterY + sin(angleRad) * endDistance
                        )

                        drawLine(
                            color = Color(0xFFFDBA74).copy(alpha = 0.70f),
                            start = start,
                            end = end,
                            strokeWidth = 6f
                        )
                    }
                }
            }

            // --- 3. RENDER SLOWLY FLOATING OVERCAST CLOUDS (Cloudy Scene) ---
            if (isCloudy || isRainy || isSnowy) {
                if (clouds.isEmpty()) {
                    reinitializeParticles(width, height)
                }

                for (cloud in clouds) {
                    // Update cloud horizontal drifting
                    cloud.x += cloud.speed
                    if (cloud.x > width + 200f) {
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

                    // Draw organic compound fluffly circles
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

            // --- 4. RENDER RAINDROPS PARTICLES (Rainy/Storm Scene) ---
            if (isRainy) {
                if (raindrops.isEmpty()) {
                    reinitializeParticles(width, height)
                }

                val tiltAngleX = 4f // Slanted rain effect mimicking dynamic breeze

                for (drop in raindrops) {
                    // Descend raindrop in space
                    drop.y += drop.speed
                    drop.x += tiltAngleX * (drop.speed / 25f)

                    // Wrap-around boundary conditions
                    if (drop.y > height) {
                        drop.y = -drop.length
                        drop.x = random.nextFloat() * width
                    }
                    if (drop.x > width + 50f) {
                        drop.x = -30f
                    }

                    // Render droplet streak
                    drawLine(
                        color = Color(0xFF60A5FA).copy(alpha = drop.alpha),
                        start = Offset(drop.x, drop.y),
                        end = Offset(drop.x + tiltAngleX * (drop.length / 30f), drop.y + drop.length),
                        strokeWidth = drop.width
                    )
                }
            }

            // --- 5. RENDER SNOWFLAKES PARTICLES (Snowy Winter Scene) ---
            if (isSnowy) {
                if (snowflakes.isEmpty()) {
                    reinitializeParticles(width, height)
                }

                for (flake in snowflakes) {
                    // Update horizontal sway values
                    flake.angle += flake.angleSpeed
                    flake.y += flake.speed
                    flake.x += sin(flake.angle) * 1.1f // Horizontal wobble flight path

                    if (flake.y > height) {
                        flake.y = -15f
                        flake.x = random.nextFloat() * width
                    }

                    // Beautiful snowy crystals
                    drawCircle(
                        color = Color.White.copy(alpha = flake.alpha),
                        radius = flake.radius,
                        center = Offset(flake.x, flake.y)
                    )
                }
            }

            // --- 6. RENDER THUNDERSTORM FLASH EFFECT ---
            if (thunderFlashAlpha > 0f) {
                drawRect(
                    color = Color.White.copy(alpha = thunderFlashAlpha),
                    size = size
                )
            }
        }
    }
}
