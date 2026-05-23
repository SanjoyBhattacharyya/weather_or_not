package com.example.ui

import android.app.Application
import java.util.Locale
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.FavoriteLocation
import com.example.data.database.PlannerEvent
import com.example.data.database.WeatherDatabase
import com.example.network.GeocodingResult
import com.example.network.NetworkClient
import com.example.network.WeatherResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    object Loading : WeatherUiState
    data class Success(val data: WeatherResponse) : WeatherUiState
    data class Error(val message: String) : WeatherUiState
}

sealed interface SearchUiState {
    object Idle : SearchUiState
    object Loading : SearchUiState
    data class Success(val results: List<GeocodingResult>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

data class SevereAlert(
    val title: String,
    val description: String,
    val severity: AlertSeverity
)

enum class AlertSeverity {
    WARNING, ADVISORY, HAZARD
}

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val database = WeatherDatabase.getDatabase(application)
    private val favoriteDao = database.favoriteLocationDao()
    private val plannerDao = database.plannerEventDao()

    // SharedPreferences for persistent settings (Theme selector)
    private val sharedPrefs = application.getSharedPreferences("skyline_weather_prefs", android.content.Context.MODE_PRIVATE)
    
    private val _currentTheme = MutableStateFlow(sharedPrefs.getString("app_theme", "Animated") ?: "Animated")
    val currentTheme: StateFlow<String> = _currentTheme.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Dynamic Weather Units Settings
    private val _tempUnit = MutableStateFlow(sharedPrefs.getString("temp_unit", "C") ?: "C")
    val tempUnit: StateFlow<String> = _tempUnit.asStateFlow()

    private val _windUnit = MutableStateFlow(sharedPrefs.getString("wind_unit", "kmh") ?: "kmh")
    val windUnit: StateFlow<String> = _windUnit.asStateFlow()

    private val _pressUnit = MutableStateFlow(sharedPrefs.getString("press_unit", "hPa") ?: "hPa")
    val pressUnit: StateFlow<String> = _pressUnit.asStateFlow()

    private val _visUnit = MutableStateFlow(sharedPrefs.getString("vis_unit", "km") ?: "km")
    val visUnit: StateFlow<String> = _visUnit.asStateFlow()

    private val _aqiUnit = MutableStateFlow(sharedPrefs.getString("aqi_unit", "US") ?: "US")
    val aqiUnit: StateFlow<String> = _aqiUnit.asStateFlow()

    private val _pollenUnit = MutableStateFlow(sharedPrefs.getString("pollen_unit", "grains") ?: "grains")
    val pollenUnit: StateFlow<String> = _pollenUnit.asStateFlow()

    // Location State
    private val _currentLocationName = MutableStateFlow("New York")
    val currentLocationName: StateFlow<String> = _currentLocationName.asStateFlow()

    private val _currentLatitude = MutableStateFlow(40.7128)
    private val _currentLongitude = MutableStateFlow(-74.0060)

    // Weather Fetch State
    private val _weatherUiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val weatherUiState: StateFlow<WeatherUiState> = _weatherUiState.asStateFlow()

    // Search Suggestions State
    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    // DB Flows
    val favoriteLocations: StateFlow<List<FavoriteLocation>> = favoriteDao.getAllFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val plannerEvents: StateFlow<List<PlannerEvent>> = plannerDao.getAllEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Load default content on init from SharedPreferences, fallback to New York
        val defaultCityName = sharedPrefs.getString("default_city_name", "New York") ?: "New York"
        val defaultLat = sharedPrefs.getFloat("default_city_lat", 40.7128f).toDouble()
        val defaultLon = sharedPrefs.getFloat("default_city_lon", -74.0060f).toDouble()
        loadWeather(defaultLat, defaultLon, defaultCityName)
        recalculateDarkMode()
    }

    fun loadWeather(latitude: Double, longitude: Double, name: String) {
        _currentLatitude.value = latitude
        _currentLongitude.value = longitude
        _currentLocationName.value = name

        // Save active city details to SharedPreferences for launch persistence
        sharedPrefs.edit()
            .putString("default_city_name", name)
            .putFloat("default_city_lat", latitude.toFloat())
            .putFloat("default_city_lon", longitude.toFloat())
            .apply()

        viewModelScope.launch {
            _weatherUiState.value = WeatherUiState.Loading
            try {
                val response = NetworkClient.weatherService.getForecast(latitude, longitude)
                _weatherUiState.value = WeatherUiState.Success(response)
                recalculateDarkMode()

                // Save cached detailed weather stats to SharedPreferences for home display widgets
                val temp = response.current?.temperature ?: -99.0
                val code = response.current?.weatherCode ?: 1
                sharedPrefs.edit()
                    .putFloat("cached_temp", temp.toFloat())
                    .putInt("cached_weather_code", code)
                    .apply()

                // Trigger home screen widget synchronous refresh
                com.example.widget.WeatherAndPlanWidgetProvider.triggerUpdate(getApplication())
            } catch (e: Exception) {
                _weatherUiState.value = WeatherUiState.Error(e.localizedMessage ?: "Failed to load weather data.")
            }
        }
    }

    fun searchLocations(query: String) {
        if (query.trim().isEmpty()) {
            _searchUiState.value = SearchUiState.Idle
            return
        }
        viewModelScope.launch {
            _searchUiState.value = SearchUiState.Loading
            try {
                val response = NetworkClient.geocodingService.searchLocations(query)
                val results = response.results ?: emptyList()
                _searchUiState.value = SearchUiState.Success(results)
            } catch (e: Exception) {
                _searchUiState.value = SearchUiState.Error(e.localizedMessage ?: "Failed to search.")
            }
        }
    }

    fun clearSearch() {
        _searchUiState.value = SearchUiState.Idle
    }

    fun updateThemeMode(theme: String) {
        _currentTheme.value = theme
        sharedPrefs.edit().putString("app_theme", theme).apply()
        recalculateDarkMode()
    }

    fun toggleDarkMode() {
        val nextTheme = when (_currentTheme.value) {
            "Animated" -> "Light"
            "Light" -> "Dark"
            "Dark" -> "System"
            else -> "Animated"
        }
        updateThemeMode(nextTheme)
    }

    private fun isNightTime(): Boolean {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        return hour < 6 || hour >= 19
    }

    private fun isRainy(code: Int): Boolean {
        return code in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)
    }

    fun recalculateDarkMode() {
        val theme = _currentTheme.value
        val weatherUnit = (_weatherUiState.value as? WeatherUiState.Success)?.data
        val code = weatherUnit?.current?.weatherCode ?: 1
        
        val isNight = isNightTime()
        val isRainy = isRainy(code)

        val resolved = when (theme) {
            "Light" -> false
            "Dark" -> true
            "System" -> {
                val uiMode = getApplication<Application>().resources.configuration.uiMode
                (uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
            "Animated" -> {
                if (isRainy) {
                    true
                } else {
                    isNight
                }
            }
            else -> true
        }
        _isDarkMode.value = resolved
    }

    // Favorite Locations CRUD
    fun toggleFavorite(name: String, latitude: Double, longitude: Double) {
        viewModelScope.launch {
            val alreadyFav = favoriteLocations.value.any { it.name.equals(name, ignoreCase = true) }
            if (alreadyFav) {
                favoriteDao.deleteFavoriteByName(name)
            } else {
                favoriteDao.insertFavorite(FavoriteLocation(name = name, latitude = latitude, longitude = longitude))
            }
        }
    }

    // Day Planner CRUD with customizable reminders and tuning options
    fun addPlannerEvent(
        title: String, 
        hour: Int, 
        minute: Int, 
        dayOffset: Int, 
        notes: String = "",
        isReminderEnabled: Boolean = false,
        reminderHour: Int = -1,
        reminderMinute: Int = -1,
        notificationTune: String = "Default"
    ) {
        viewModelScope.launch {
            val event = PlannerEvent(
                title = title,
                hour = hour,
                minute = minute,
                dayOffset = dayOffset,
                notes = notes,
                isCompleted = false,
                isReminderEnabled = isReminderEnabled,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                notificationTune = notificationTune
            )
            val generatedId = plannerDao.insertEvent(event)
            
            // If reminder is enabled, schedule it in the OS System Alarms
            if (isReminderEnabled) {
                val alarmHour = if (reminderHour == -1) hour else reminderHour
                val alarmMinute = if (reminderMinute == -1) minute else reminderMinute
                com.example.receiver.PlanReminderReceiver.schedule(
                    getApplication(),
                    generatedId.toInt(),
                    title,
                    dayOffset,
                    alarmHour,
                    alarmMinute,
                    notificationTune
                )
            }
            
            // Re-sync standard widget content
            com.example.widget.WeatherAndPlanWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun deletePlannerEvent(event: PlannerEvent) {
        viewModelScope.launch {
            plannerDao.deleteEvent(event)
            if (event.isReminderEnabled) {
                com.example.receiver.PlanReminderReceiver.cancel(getApplication(), event.id)
            }
            // Re-sync standard widget content
            com.example.widget.WeatherAndPlanWidgetProvider.triggerUpdate(getApplication())
        }
    }

    fun updateEventCompletion(id: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            plannerDao.updateEventStatus(id, isCompleted)
            // Re-sync standard widget content
            com.example.widget.WeatherAndPlanWidgetProvider.triggerUpdate(getApplication())
        }
    }

    // Interactive Core Logic: Outfit Recommendations
    fun getDressingRecommendation(temp: Double, rainCode: Int): String {
        val weatherName = getWeatherDescription(rainCode)
        return when {
            temp < 0 -> "🧣 Extreme cold ($temp°C)! Put on dense thermal underwear, a thick down parka, thick gloves, wool socks, and an insulated beanie."
            temp in 0.0..10.0 -> "🧥 Cold weather ($temp°C). We recommend a heavy wool coat or puffy winter jacket, heavy sweater, warm trousers, and sturdy leather boots."
            temp in 10.1..18.0 -> "🧥 Mild chilly ($temp°C). Wear a light trench coat, denim jacket, or hoodie over a long-sleeve tee with casual jeans."
            temp in 18.1..25.0 -> "👕 Pleasant temperature ($temp°C). Standard light layers like a cotton sweater, cardigans, or simple t-shirt with chinos work perfectly."
            else -> "☀️ Warm weather ($temp°C)! We highly recommend wearing light, breathable linen shirts or cotton tees, sunglasses, and applying sunscreen!"
        }
    }

    fun getUmbrellaNotice(rainProbability: Int, rainCode: Int, temp: Double): Pair<Boolean, String> {
        val hasRainCode = rainCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)
        return when {
            hasRainCode || rainProbability >= 60 -> {
                true to "Umbrella Advised! ☔"
            }
            rainProbability in 30..59 -> {
                true to "Grab a raincoat! 🌂"
            }
            else -> {
                val creativeHint = when {
                    temp >= 24.0 -> "Wear sunglasses & sunscreen today! 🕶️"
                    temp in 18.0..23.99 -> "Perfect light cap day! 🧢"
                    temp in 10.0..17.99 -> "Snug outerwear day! 🧥"
                    temp in 0.0..9.99 -> "Get a cozy scarf! 🧣"
                    else -> "Put on warm mittens & gloves! 🧤"
                }
                false to creativeHint
            }
        }
    }

    // Severe Weather Warnings generated from the 14-day forecasts
    fun getSevereWeatherAlerts(data: WeatherResponse): List<SevereAlert> {
        val alerts = mutableListOf<SevereAlert>()
        
        // 1. Current condition check
        val current = data.current
        if (current != null) {
            val temp = current.temperature
            val wind = current.windSpeed
            val weatherCode = current.weatherCode

            if (temp > 38.0) {
                alerts.add(SevereAlert(
                    title = "Extreme Heat Advisory",
                    description = "Dangerous heat detected ($temp°C)! Reduce outdoor exertion, stay in shadowed cool spaces, and drink plenty of fluids.",
                    severity = AlertSeverity.WARNING
                ))
            } else if (temp < -8.0) {
                alerts.add(SevereAlert(
                    title = "Severe Freeze Warning",
                    description = "Sub-freezing temperatures ($temp°C) present severe frostbite risks. Keep extremities wrapped and cover exposed pipes.",
                    severity = AlertSeverity.WARNING
                ))
            }

            if (wind > 45.0) {
                alerts.add(SevereAlert(
                    title = "High Wind Warning",
                    description = "Violent wind gusts averaging $wind km/h can collapse loose outdoor objects. Limit driving high-profile vehicles.",
                    severity = AlertSeverity.WARNING
                ))
            }

            if (weatherCode in listOf(95, 96, 99)) {
                alerts.add(SevereAlert(
                    title = "Severe Thunderstorm Warning",
                    description = "Intense convection thunderstorms detected with active risk of local power interruptions and lightning hazard.",
                    severity = AlertSeverity.WARNING
                ))
            }
        }

        // 2. Future extreme events inside the 14-day forecast
        val daily = data.daily
        if (daily != null) {
            val size = daily.time.size
            var foundFreeze = false
            var foundHeat = false
            var foundStorms = false

            for (i in 0 until size) {
                val maxTemp = daily.temperatureMax.getOrNull(i) ?: 0.0
                val minTemp = daily.temperatureMin.getOrNull(i) ?: 0.0
                val code = daily.weatherCode.getOrNull(i) ?: 0
                val prob = daily.precipitationProbabilityMax?.getOrNull(i) ?: 0

                if (minTemp < -5.0 && !foundFreeze) {
                    foundFreeze = true
                    alerts.add(SevereAlert(
                        title = "Upcoming Hard Freeze Alert",
                        description = "Subzero temperature drops under $minTemp°C are expected later this week. Plan heating and vegetation protection.",
                        severity = AlertSeverity.HAZARD
                    ))
                }
                if (maxTemp > 36.0 && !foundHeat) {
                    foundHeat = true
                    alerts.add(SevereAlert(
                        title = "Excessive Heat Watch",
                        description = "Forecast projects intense extreme heat hitting $maxTemp°C. Schedule exhausting schedules to early morning hours.",
                        severity = AlertSeverity.ADVISORY
                    ))
                }
                if (code in listOf(95, 96, 99) && prob > 75 && !foundStorms) {
                    foundStorms = true
                    alerts.add(SevereAlert(
                        title = "Severe Storm Outlook",
                        description = "Dynamic lightning storms with heavy rainfall are highly likely in the upcoming week's forecast.",
                        severity = AlertSeverity.ADVISORY
                    ))
                }
            }
        }

        return alerts
    }

    // Helper translation for WMO weather codes
    fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45 -> "Fog"
            48 -> "Depositing Rime Fog"
            51 -> "Light Drizzle"
            53 -> "Moderate Drizzle"
            55 -> "Dense Drizzle"
            56 -> "Light Freezing Drizzle"
            57 -> "Dense Freezing Drizzle"
            61 -> "Slight Rain"
            63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            66 -> "Light Freezing Rain"
            67 -> "Heavy Freezing Rain"
            71 -> "Slight Snow Fall"
            73 -> "Moderate Snow Fall"
            75 -> "Heavy Snow Fall"
            77 -> "Snow Grains"
            80 -> "Slight Rain Showers"
            81 -> "Moderate Rain Showers"
            82 -> "Violent Rain Showers"
            85 -> "Slight Snow Showers"
            86 -> "Heavy Snow Showers"
            95 -> "Thunderstorm"
            96 -> "Thunderstorm with Slight Hail"
            99 -> "Thunderstorm with Heavy Hail"
            else -> "Unknown Weather"
        }
    }

    // Settings unit updates
    fun updateTempUnit(unit: String) {
        _tempUnit.value = unit
        sharedPrefs.edit().putString("temp_unit", unit).apply()
    }
    fun updateWindUnit(unit: String) {
        _windUnit.value = unit
        sharedPrefs.edit().putString("wind_unit", unit).apply()
    }
    fun updatePressUnit(unit: String) {
        _pressUnit.value = unit
        sharedPrefs.edit().putString("press_unit", unit).apply()
    }
    fun updateVisUnit(unit: String) {
        _visUnit.value = unit
        sharedPrefs.edit().putString("vis_unit", unit).apply()
    }
    fun updateAqiUnit(unit: String) {
        _aqiUnit.value = unit
        sharedPrefs.edit().putString("aqi_unit", unit).apply()
    }
    fun updatePollenUnit(unit: String) {
        _pollenUnit.value = unit
        sharedPrefs.edit().putString("pollen_unit", unit).apply()
    }

    // Scientific / Creative Formatters based on unit selections
    fun formatTemperature(tempCelsius: Double): String {
        return if (_tempUnit.value == "F") {
            "${((tempCelsius * 9.0 / 5.0) + 32.0).toInt()}°F"
        } else {
            "${tempCelsius.toInt()}°C"
        }
    }

    fun formatHumidity(percentage: Int): String {
        return "$percentage%"
    }

    fun formatWindSpeed(speedKmh: Double): String {
        return when (_windUnit.value) {
            "mph" -> "${(speedKmh * 0.621371).toInt()} mph"
            "ms" -> "${(speedKmh * 0.277778).toInt()} m/s"
            else -> "${speedKmh.toInt()} km/h"
        }
    }

    fun formatPressure(pressureHpa: Double?): String {
        val base = pressureHpa ?: 1013.25
        return when (_pressUnit.value) {
            "inHg" -> String.format(Locale.getDefault(), "%.2f inHg", base * 0.02953)
            "mmHg" -> "${(base * 0.750062).toInt()} mmHg"
            else -> "${base.toInt()} hPa"
        }
    }

    fun formatVisibility(visibilityMeters: Double?): String {
        val meters = visibilityMeters ?: 10000.0
        val km = meters / 1000.0
        return when (_visUnit.value) {
            "miles" -> String.format(Locale.getDefault(), "%.1f mi", km * 0.621371)
            else -> String.format(Locale.getDefault(), "%.1f km", km)
        }
    }

    fun getAQIValue(temp: Double, weatherCode: Int): Int {
        var base = 35
        if (weatherCode in listOf(45, 48)) base += 45
        if (weatherCode in listOf(95, 96, 99)) base += 25
        if (temp > 35.0) base += 30
        if (temp < 0.0) base += 15
        return base.coerceIn(10, 320)
    }

    fun formatAQI(value: Int): String {
        return when (_aqiUnit.value) {
            "CAQI" -> {
                val caqi = (value / 5).coerceIn(2, 100)
                val label = when {
                    caqi <= 25 -> "Very Low"
                    caqi <= 50 -> "Low"
                    caqi <= 75 -> "Medium"
                    else -> "High"
                }
                "$caqi ($label)"
            }
            else -> {
                val label = when {
                    value <= 50 -> "Good"
                    value <= 100 -> "Moderate"
                    value <= 150 -> "Moderate/High"
                    value <= 200 -> "Unhealthy"
                    else -> "Very Harmful"
                }
                "$value ($label)"
            }
        }
    }

    fun getPollenValue(temp: Double, weatherCode: Int): Int {
        val isRainyAndSnowy = weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 71, 73, 75, 77, 80, 81, 82, 85, 86, 95, 96, 99)
        if (isRainyAndSnowy) return 12
        var grains = (temp * 6.0).toInt().coerceAtLeast(10)
        if (temp in 15.0..28.0) grains += 40
        return grains.coerceIn(5, 275)
    }

    fun formatPollen(grainsValue: Int): String {
        return when (_pollenUnit.value) {
            "index" -> {
                when {
                    grainsValue <= 30 -> "1/5 (Low)"
                    grainsValue <= 80 -> "2/5 (Moderate)"
                    grainsValue <= 150 -> "3/5 (High)"
                    grainsValue <= 245 -> "4/5 (Very High)"
                    else -> "5/5 (Extreme)"
                }
            }
            else -> {
                "$grainsValue gr/m³"
            }
        }
    }
}
