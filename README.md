# 🌌 Weather or Not (You are going out any way, here is what to expect. All in one place.)

A modern, fluid, production-grade Android Weather & Planner application built with **Kotlin** and **Jetpack Compose**, styled to follow strictly **Material Design 3 (M3)** guidelines. 

This repository implements offline-first capabilities using **Room Database**, and features an immersive, adaptive visual aesthetic called **Animated Dynamics** that transforms the look of the application dynamically according to real-world weather states and daytime cycles.

---

## 🧭 Project Architecture & Key Navigation Points

To help you easily locate code, debug features, or extend functionality, here is the full directory map and structure:

```
├── app
│   └── src
│       └── main
│           ├── java
│           │   └── com
│           │       └── example
│           │           ├── MainActivity.kt             # Main Android entry point & initialization
│           │           ├── data
│           │           │   └── database
│           │           │       ├── FavoriteLocation.kt # Room Entity for favorite cities
│           │           │       ├── PlannerEvent.kt     # Room Entity for schedule items
│           │           │       └── WeatherDatabase.kt  # Local database migration and configuration (V2)
│           │           ├── network
│           │           │   └── WeatherModels.kt        # JSON mapping models for Geocoding & Open-Meteo
│           │           ├── receiver
│           │           │   └── PlanReminderReceiver.kt # Broadcaster handling task notification triggers
│           │           ├── ui
│           │           │   ├── WeatherViewModel.kt     # Core state machine, unit converters, CRUD operations
│           │           │   └── screens
│           │           │       ├── MainScreen.kt       # Outer shell (Scaffold), bottom nav slots, and modal sections
│           │           │       └── DynamicWeatherBackground.kt # Particle canvases and rotating celestial vectors
│           │           └── widget
│           │               ├── WeatherAndPlanWidgetProvider.kt # RemoteViews updates (background data fetcher)
│           │               └── WidgetModel.kt
│           └── res
│               ├── layout
│               │   └── widget_layout.xml               # Standard XML layout for the home screen widget
│               └── values
│                   └── strings.xml                     # Global localized strings
```

---

## 🎨 Creative Spotlight: Animated Dynamics

When the **Animated Dynamic ✨** theme is active in Settings, the screen background is powered by a high-performance, frame-rate synced visual rendering system found inside `DynamicWeatherBackground.kt`.

### Supported Atmospheric Environments:
1. **Clear Day Sky**: Draws a bright golden sun with slow-rotating geometric solar rays and dynamic pulsing halo waves.
2. **Clear Night Sky**: Renders solid crescent moon structures with depth masking (shadow subtraction), a starry canopy that twinkles individually, and a periodic shooting-star cascade with translucent motion-blur tails.
3. **Overcast & Fog**: Generates compound semi-transparent cloud masses floating smoothly from left to right.
4. **Rain & Storm Systems**: Implements a real-time falling particle system that mimics gravity, slanted wind drift speeds, and includes sudden lightning flashes (screen white-fades) during thunderstorm codes.
5. **Snow & Winter Crystals**: Renders smooth falling fluffy snowflakes that drift horizontally using low-frequency sine-wave functions to mimic real fluttering snow paths.

---

## 🛠️ Extension Guide: Adding Custom Background Animations

Developers can easily expand or tweak the animated background system. Follow these quick steps to add a new overlay state (e.g., adding a **Sandstorm / Wind Gusts** particle system):

### 1. Define your Particle Properties:
Create a data representation inside `DynamicWeatherBackground.kt`:
```kotlin
data class SandParticle(
    var x: Float,
    var y: Float,
    var speedX: Float,
    var speedY: Float,
    var alpha: Float,
    var size: Float
)
```

### 2. Instantiate and Update States:
Add a state list within `DynamicWeatherBackground`:
```kotlin
val sandParticles = remember { mutableStateListOf<SandParticle>() }

// Inside your initialization method:
fun reinitializeParticles(width: Float, height: Float) {
    // ...
    sandParticles.clear()
    for (i in 0 until 40) {
        sandParticles.add(
            SandParticle(
                x = random.nextFloat() * width,
                y = random.nextFloat() * height,
                speedX = 8f + random.nextFloat() * 12f, // Strong horizontal drift
                speedY = 1f + random.nextFloat() * 2f,
                alpha = 0.2f + random.nextFloat() * 0.4f,
                size = 2f + random.nextFloat() * 3f
            )
        )
    }
}
```

### 3. Draw on Canvas:
Inside the `Canvas` draw state machine, check for your specific weather conditions:
```kotlin
if (isSandstorm) {
    if (sandParticles.isEmpty()) { reinitializeParticles(width, height) }
    
    for (p in sandParticles) {
        p.x += p.speedX
        p.y += p.speedY
        
        // Boundaries reset
        if (p.x > width) { p.x = -10f; p.y = random.nextFloat() * height }
        
        drawCircle(
            color = Color(0xFFD2B48C).copy(alpha = p.alpha), // Sand color
            radius = p.size,
            center = Offset(p.x, p.y)
        )
    }
}
```

---

## 🐛 Local Debugging, Testing & Troubleshooting

### Integrity issues & Room Database version bump
If you run into database integrity or schema state conflicts while working with Room:
- The database is configured with `.fallbackToDestructiveMigration(true)`, meaning it can recover gracefully from version changes.
- To execute a clean wipe or expand schemas, increment the version value inside `@Database` in `WeatherDatabase.kt` (currently updated to `version = 2` to fix previous structural conflicts).

### Running Unit / Integration checks in Terminal
Execute unit and database unit tests seamlessly in your workspace:
```bash
gradle :app:testDebugUnitTest
```

---

## 📲 Publishing to GitHub & Downloading the APK

As a developer, the **Google AI Studio platform** offers integrated, seamless buttons to build your source tree, connect directly to GitHub, and sign APK files instantly to put on your phone.

Follow these direct steps in your browser window:

### Step 1: Connect & Push to GitHub
1. Look at the upper-right or top-left sidebar options in your AI Studio Build workspace.
2. Click the **Export** or **GitHub** integration button.
3. Authenticate with your GitHub account, select your repository name (`SanjoyBhattacharyya/weather_or_not`), and press **Push / Export**. All files (including your new dynamic background features and structural database configurations) will be committed immediately!

### Step 2: Generate and Install your signed APK File
1. In the top toolbar, click on the **Settings menu (⚙️)** or the **Build options drop-down** next to the active emulator stream.
2. Select **Generate signed APK / Build APK**.
3. The server-side Android SDK compiler will assemble the release-optimized bytecode and generate a downloadable `.apk` package.
4. Click on the download link, move it to your Android device (via USB, cloud, or your preferred file transfer), and tap to install! (Ensure you have authorized *Install from Unknown Sources* on your Android security preferences).
