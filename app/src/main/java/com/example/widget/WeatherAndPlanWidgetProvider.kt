package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.database.WeatherDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WeatherAndPlanWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        try {
            val goAppIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, goAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Run coroutine to read DB and build view updates
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = WeatherDatabase.getDatabase(context)
                    // Query plans for today (dayOffset = 0) that are not completed
                    val todayEvents = try {
                        db.plannerEventDao().getTodayEventsSync()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    // Read theme preferences and cached weather
                    val sharedPrefs = context.getSharedPreferences("skyline_weather_prefs", Context.MODE_PRIVATE)
                    val cityName = sharedPrefs.getString("default_city_name", "New York") ?: "New York"
                    val tempVal = sharedPrefs.getFloat("cached_temp", 22.5f)
                    val weatherCode = sharedPrefs.getInt("cached_weather_code", 1)

                    val tempStr = if (tempVal == -99f) "--" else "${tempVal.toInt()}°C"
                    val conditionStr = getWeatherDescription(weatherCode)

                    // Creative Generic Activity hint when there are no events scheduled
                    val creativeHint = when {
                        tempVal >= 24.0 -> "No plans today!\nWear sunglasses & sunscreen! 🕶️"
                        tempVal in 18.0..23.99 -> "No plans!\nPerfect day for a light cap! 🧢"
                        tempVal in 10.0..17.99 -> "No plans today!\nCozy outerwear day! 🧥"
                        tempVal in 0.0..9.99 -> "Snug as a bug!\nGet a cozy scarf! 🧣"
                        else -> "Stay warm limit outdoor!\nPut on warm mittens! 🧤"
                    }

                    withContext(Dispatchers.Main) {
                        try {
                            for (appWidgetId in appWidgetIds) {
                                val views = RemoteViews(context.packageName, R.layout.widget_layout)

                                // Apply weather UI details
                                views.setTextViewText(R.id.widget_temp, tempStr)
                                views.setTextViewText(R.id.widget_location, cityName)
                                views.setTextViewText(R.id.widget_condition, conditionStr)

                                // Set pending click to launch main app
                                views.setOnClickPendingIntent(R.id.widget_title, pendingIntent)
                                views.setOnClickPendingIntent(R.id.widget_temp, pendingIntent)

                                // Bind dynamic schedules to RemoteViews lines safely
                                if (todayEvents.isEmpty()) {
                                    views.setViewVisibility(R.id.widget_no_plans, View.VISIBLE)
                                    views.setTextViewText(R.id.widget_no_plans, creativeHint)
                                    views.setViewVisibility(R.id.widget_plans_container, View.GONE)
                                } else {
                                    views.setViewVisibility(R.id.widget_no_plans, View.GONE)
                                    views.setViewVisibility(R.id.widget_plans_container, View.VISIBLE)

                                    // Clear and show up to 3 elements dynamically
                                    val size = todayEvents.size
                                    // Item 1
                                    if (size >= 1) {
                                        val event = todayEvents[0]
                                        views.setViewVisibility(R.id.widget_plan_item1, View.VISIBLE)
                                        views.setTextViewText(R.id.widget_plan_time1, formatTime(event.hour, event.minute))
                                        views.setTextViewText(R.id.widget_plan_title1, event.title)
                                    } else {
                                        views.setViewVisibility(R.id.widget_plan_item1, View.GONE)
                                    }

                                    // Item 2
                                    if (size >= 2) {
                                        val event = todayEvents[1]
                                        views.setViewVisibility(R.id.widget_plan_item2, View.VISIBLE)
                                        views.setTextViewText(R.id.widget_plan_time2, formatTime(event.hour, event.minute))
                                        views.setTextViewText(R.id.widget_plan_title2, event.title)
                                    } else {
                                        views.setViewVisibility(R.id.widget_plan_item2, View.GONE)
                                    }

                                    // Item 3
                                    if (size >= 3) {
                                        val event = todayEvents[2]
                                        views.setViewVisibility(R.id.widget_plan_item3, View.VISIBLE)
                                        views.setTextViewText(R.id.widget_plan_time3, formatTime(event.hour, event.minute))
                                        views.setTextViewText(R.id.widget_plan_title3, event.title)
                                    } else {
                                        views.setViewVisibility(R.id.widget_plan_item3, View.GONE)
                                    }
                                }

                                // Perform the update
                                appWidgetManager.updateAppWidget(appWidgetId, views)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun formatTime(hour: Int, minute: Int): String {
        return String.format("%02d:%02d", hour, minute)
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky ☀️"
            1 -> "Mainly Clear 🌤️"
            2 -> "Partly Cloudy ⛅"
            3 -> "Overcast ☁️"
            45, 48 -> "Foggy 🌫️"
            51, 53, 55 -> "Drizzle 🌧️"
            56, 57 -> "Freezing Drizzle ❄️"
            61, 63 -> "Slight Rain 🌦️"
            65 -> "Heavy Rain 🌧️"
            66, 67 -> "Freezing Rain 🌨️"
            71, 73, 75 -> "Snow Fall ❄️"
            77 -> "Snow Grains ❄️"
            80, 81 -> "Rain Showers 🌦️"
            82, 85, 86 -> "Storm Showers ⛈️"
            95, 96, 99 -> "Thunderstorm ⛈️"
            else -> "Dry weather expected 🌤️"
        }
    }

    companion object {
        fun triggerUpdate(context: Context) {
            try {
                val intent = Intent(context, WeatherAndPlanWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                }
                val ids = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, WeatherAndPlanWidgetProvider::class.java)
                )
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                context.sendBroadcast(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
