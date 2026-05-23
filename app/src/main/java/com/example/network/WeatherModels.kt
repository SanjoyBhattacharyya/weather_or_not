package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    @field:Json(name = "results") val results: List<GeocodingResult>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResult(
    @field:Json(name = "id") val id: Long,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "latitude") val latitude: Double,
    @field:Json(name = "longitude") val longitude: Double,
    @field:Json(name = "country") val country: String?,
    @field:Json(name = "admin1") val admin1: String?
)

@JsonClass(generateAdapter = true)
data class WeatherResponse(
    @field:Json(name = "latitude") val latitude: Double,
    @field:Json(name = "longitude") val longitude: Double,
    @field:Json(name = "timezone") val timezone: String,
    @field:Json(name = "current") val current: CurrentWeather?,
    @field:Json(name = "hourly") val hourly: HourlyForecast?,
    @field:Json(name = "daily") val daily: DailyForecast?
)

@JsonClass(generateAdapter = true)
data class CurrentWeather(
    @field:Json(name = "time") val time: String,
    @field:Json(name = "temperature_2m") val temperature: Double,
    @field:Json(name = "relative_humidity_2m") val relativeHumidity: Int,
    @field:Json(name = "apparent_temperature") val apparentTemperature: Double,
    @field:Json(name = "precipitation") val precipitation: Double,
    @field:Json(name = "weather_code") val weatherCode: Int,
    @field:Json(name = "wind_speed_10m") val windSpeed: Double,
    @field:Json(name = "surface_pressure") val surfacePressure: Double?,
    @field:Json(name = "visibility") val visibility: Double?,
    @field:Json(name = "uv_index") val uvIndex: Double?
)

@JsonClass(generateAdapter = true)
data class HourlyForecast(
    @field:Json(name = "time") val time: List<String>,
    @field:Json(name = "temperature_2m") val temperature: List<Double>,
    @field:Json(name = "precipitation_probability") val precipitationProbability: List<Int>,
    @field:Json(name = "weather_code") val weatherCode: List<Int>
)

@JsonClass(generateAdapter = true)
data class DailyForecast(
    @field:Json(name = "time") val time: List<String>,
    @field:Json(name = "weather_code") val weatherCode: List<Int>,
    @field:Json(name = "temperature_2m_max") val temperatureMax: List<Double>,
    @field:Json(name = "temperature_2m_min") val temperatureMin: List<Double>,
    @field:Json(name = "apparent_temperature_max") val apparentTemperatureMax: List<Double>,
    @field:Json(name = "apparent_temperature_min") val apparentTemperatureMin: List<Double>,
    @field:Json(name = "uv_index_max") val uvIndexMax: List<Double>?,
    @field:Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>?,
    @field:Json(name = "wind_speed_10m_max") val windSpeedMax: List<Double>?,
    @field:Json(name = "sunrise") val sunrise: List<String>?,
    @field:Json(name = "sunset") val sunset: List<String>?
)
