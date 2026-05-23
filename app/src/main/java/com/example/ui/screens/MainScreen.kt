package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.database.FavoriteLocation
import com.example.data.database.PlannerEvent
import com.example.network.WeatherResponse
import com.example.ui.SearchUiState
import com.example.ui.WeatherUiState
import com.example.ui.WeatherViewModel
import com.example.ui.SevereAlert
import com.example.ui.AlertSeverity
import java.text.SimpleDateFormat
import java.util.*

enum class AppTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    DASHBOARD("Today", Icons.Filled.WbSunny),
    FORECAST("14 Days", Icons.Filled.DateRange),
    FAVORITES("Favorites", Icons.Filled.Favorite),
    PLANNER("Planner", Icons.AutoMirrored.Filled.EventNote),
    SETTINGS("Settings", Icons.Filled.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    val currentLocationName by viewModel.currentLocationName.collectAsStateWithLifecycle()
    val weatherUiState by viewModel.weatherUiState.collectAsStateWithLifecycle()
    val searchUiState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteLocations.collectAsStateWithLifecycle()
    val plannerEvents by viewModel.plannerEvents.collectAsStateWithLifecycle()
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val navBgColor = if (isDarkMode) Color(0xFF0F172A).copy(alpha = 0.95f) else Color(0xFFF8FAFC).copy(alpha = 0.95f)
    val accentColor = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val unselectedColor = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color(0xFF64748B)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent, // Let the background gradient bleed through
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .testTag("app_navigation_bar"),
                containerColor = navBgColor,
                tonalElevation = 8.dp
            ) {
                AppTab.values().forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = { 
                            Icon(
                                tab.icon, 
                                contentDescription = tab.title,
                                tint = if (selected) accentColor else unselectedColor
                            ) 
                        },
                        label = { 
                            Text(
                                tab.title, 
                                fontWeight = FontWeight.Bold, 
                                fontSize = 11.sp,
                                color = if (selected) accentColor else unselectedColor
                            ) 
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = accentColor,
                            selectedTextColor = accentColor,
                            indicatorColor = accentColor.copy(alpha = 0.15f),
                            unselectedIconColor = unselectedColor,
                            unselectedTextColor = unselectedColor
                        ),
                        modifier = Modifier.testTag("nav_item_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(if (isDarkMode) Color(0xFF0B0E14) else Color(0xFFF1F5F9)) // Adaptive gorgeous background
                .drawBehind {
                    if (isDarkMode) {
                        // Top-left soft glowing blue sphere matching Tailwind shadow/blur
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF2563EB).copy(alpha = 0.16f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(x = size.width * -0.1f, y = size.height * -0.1f),
                                radius = size.width * 0.82f
                            ),
                            center = androidx.compose.ui.geometry.Offset(x = size.width * -0.1f, y = size.height * -0.1f),
                            radius = size.width * 0.82f
                        )

                        // Bottom-right soft glowing indigo sphere
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF312E81).copy(alpha = 0.28f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(x = size.width * 1.1f, y = size.height * 0.9f),
                                radius = size.width * 0.95f
                            ),
                            center = androidx.compose.ui.geometry.Offset(x = size.width * 1.1f, y = size.height * 0.9f),
                            radius = size.width * 0.95f
                        )
                    } else {
                        // Light Mode bright radial sky and peach accents
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF93C5FD).copy(alpha = 0.22f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(x = size.width * 1.1f, y = size.height * -0.1f),
                                radius = size.width * 0.9f
                            ),
                            center = androidx.compose.ui.geometry.Offset(x = size.width * 1.1f, y = size.height * -0.1f),
                            radius = size.width * 0.9f
                        )

                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF22D3EE).copy(alpha = 0.12f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(x = size.width * -0.1f, y = size.height * 0.8f),
                                radius = size.width * 0.85f
                            ),
                            center = androidx.compose.ui.geometry.Offset(x = size.width * -0.1f, y = size.height * 0.8f),
                            radius = size.width * 0.85f
                        )
                    }
                }
                .padding(innerPadding)
        ) {
            val weatherCode = (weatherUiState as? WeatherUiState.Success)?.data?.current?.weatherCode ?: 1
            val weatherIsNight = remember(weatherUiState) {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                hour < 6 || hour >= 19
            }
            // Rendering the full-bleed canvas animation background under the dashboard cards
            DynamicWeatherBackground(
                currentTheme = currentTheme,
                weatherCode = weatherCode,
                isNight = weatherIsNight
            )

            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Shared Headers: Brand & Search bar
                SearchHeaderSection(
                    searchQuery = searchQuery,
                    onSearchQueryChange = {
                        searchQuery = it
                        viewModel.searchLocations(it)
                    },
                    searchUiState = searchUiState,
                    onSuggestionSelected = { result ->
                        viewModel.loadWeather(result.latitude, result.longitude, result.name)
                        searchQuery = ""
                        viewModel.clearSearch()
                        focusManager.clearFocus()
                    },
                    currentLocationName = currentLocationName,
                    isFavorited = favorites.any { it.name.equals(currentLocationName, ignoreCase = true) },
                    onFavoriteToggle = {
                        // Find current lat/lng from loading weather output or VM
                        if (weatherUiState is WeatherUiState.Success) {
                            val data = (weatherUiState as WeatherUiState.Success).data
                            viewModel.toggleFavorite(currentLocationName, data.latitude, data.longitude)
                        } else {
                            viewModel.toggleFavorite(currentLocationName, 40.7128, -74.0060)
                        }
                    },
                    onClearSearch = {
                        searchQuery = ""
                        viewModel.clearSearch()
                    },
                    isDarkMode = isDarkMode,
                    currentTheme = currentTheme,
                    onThemeSelected = { viewModel.updateThemeMode(it) },
                    favorites = favorites,
                    onCitySelected = { fav ->
                        viewModel.loadWeather(fav.latitude, fav.longitude, fav.name)
                    }
                )

                // Sub-Screen Content switcher with animation
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "tab_transition",
                    modifier = Modifier.weight(1f)
                ) { tab ->
                    when (tab) {
                        AppTab.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            weatherUiState = weatherUiState,
                            plannerEvents = plannerEvents,
                            isDarkMode = isDarkMode,
                            onSeeAllClicked = { selectedTab = AppTab.FORECAST }
                        )
                        AppTab.FORECAST -> ForecastScreen(
                            weatherUiState = weatherUiState,
                            viewModel = viewModel
                        )
                        AppTab.FAVORITES -> FavoritesScreen(
                            favorites = favorites,
                            onLocationSelected = { fav ->
                                viewModel.loadWeather(fav.latitude, fav.longitude, fav.name)
                                selectedTab = AppTab.DASHBOARD
                            },
                            viewModel = viewModel
                        )
                        AppTab.PLANNER -> DayPlannerScreen(
                            plannerEvents = plannerEvents,
                            weatherUiState = weatherUiState,
                            viewModel = viewModel
                        )
                        AppTab.SETTINGS -> SettingsScreen(
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getDynamicBackgroundBrush(weatherUiState: WeatherUiState): Brush {
    val lightThemeColors = listOf(Color(0xFFE0F7FA), Color(0xFFE8EAF6))
    val defaultColors = listOf(Color(0xFF0F172A), Color(0xFF1E293B)) // dark luxury slate

    val targetColors = when (weatherUiState) {
        is WeatherUiState.Success -> {
            val code = weatherUiState.data.current?.weatherCode ?: 0
            when {
                code in listOf(0, 1) -> listOf(Color(0xFF0B3C5D), Color(0xFF328CC1), Color(0xFFD9B310)) // sunny dawn
                code in listOf(2, 3) -> listOf(Color(0xFF1B2A47), Color(0xFF33306B)) // cloudy blue
                code in listOf(45, 48) -> listOf(Color(0xFF435058), Color(0xFF8D8D92)) // foggy slate
                code in listOf(51, 53, 55, 61, 63, 65, 80, 81, 82) -> listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364)) // rainy storm
                code in listOf(95, 96, 99) -> listOf(Color(0xFF1A1F2C), Color(0xFF1F1235)) // thunder purple
                else -> defaultColors
            }
        }
        else -> defaultColors
    }
    return Brush.verticalGradient(targetColors)
}

@Composable
fun SearchHeaderSection(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchUiState: SearchUiState,
    onSuggestionSelected: (com.example.network.GeocodingResult) -> Unit,
    currentLocationName: String,
    isFavorited: Boolean,
    onFavoriteToggle: () -> Unit,
    onClearSearch: () -> Unit,
    isDarkMode: Boolean,
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
    favorites: List<FavoriteLocation>,
    onCitySelected: (FavoriteLocation) -> Unit
) {
    val uppercaseDate = remember {
        SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()).uppercase()
    }

    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF0F172A) // Slate 900
    val textSecondaryColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569) // Slate 600
    val accentColor = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val circleBtnBgColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Location",
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = currentLocationName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 22.sp,
                        color = textPrimaryColor,
                        letterSpacing = -0.5.sp
                    )
                }
                Text(
                    text = uppercaseDate,
                    fontSize = 11.sp,
                    color = textSecondaryColor,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 26.dp, top = 2.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Glassmorphic toggle favourite button
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(circleBtnBgColor)
                        .clickable { onFavoriteToggle() }
                        .testTag("favorite_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorited) Icons.Filled.Favorite else Icons.Outlined.Favorite,
                        contentDescription = "Favorite Toggle",
                        tint = if (isFavorited) Color(0xFFF43F5E) else (if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Interactive Theme Selector Dropdown Trigger
                var showThemeMenu by remember { mutableStateOf(false) }
                
                Box {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(circleBtnBgColor)
                            .clickable { showThemeMenu = true }
                            .testTag("theme_toggle_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        val headerIcon = when (currentTheme) {
                            "Light" -> Icons.Outlined.WbSunny
                            "Dark" -> Icons.Outlined.DarkMode
                            "System" -> Icons.Outlined.SettingsSuggest
                            else -> Icons.Outlined.AutoAwesome // Animated Sparkles!
                        }
                        val headerIconTint = when (currentTheme) {
                            "Light" -> Color(0xFFFBBF24)
                            "Dark" -> Color(0xFF818CF8)
                            "System" -> if (isDarkMode) Color.White else Color(0xFF475569)
                            else -> Color(0xFFEC4899) // Dynamic Pink sparkles
                        }
                        Icon(
                            imageVector = headerIcon,
                            contentDescription = "Theme option trigger dropdown link",
                            tint = headerIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false },
                        modifier = Modifier.background(if (isDarkMode) Color(0xFF1E293B) else Color.White)
                    ) {
                        // Option 1: Light Theme
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.WbSunny, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Light Theme", color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontSize = 13.sp)
                            }},
                            onClick = {
                                onThemeSelected("Light")
                                showThemeMenu = false
                            }
                        )
                        // Option 2: Dark Theme
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.DarkMode, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Dark Theme", color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontSize = 13.sp)
                            }},
                            onClick = {
                                onThemeSelected("Dark")
                                showThemeMenu = false
                            }
                        )
                        // Option 3: System Theme
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.SettingsSuggest, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("System Default", color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontSize = 13.sp)
                            }},
                            onClick = {
                                onThemeSelected("System")
                                showThemeMenu = false
                            }
                        )
                        // Option 4: Animated / Creative theme
                        DropdownMenuItem(
                            text = { Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Animated Dynamic ✨", color = if (isDarkMode) Color.White else Color(0xFF0F172A), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }},
                            onClick = {
                                onThemeSelected("Animated")
                                showThemeMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Input Fields styled as sleeker glass pill matching bg-white/5 backdrop-blur-2xl
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search location...", color = textSecondaryColor) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = accentColor) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear", tint = textSecondaryColor)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textPrimaryColor,
                unfocusedTextColor = textPrimaryColor,
                focusedBorderColor = accentColor.copy(alpha = 0.5f),
                unfocusedBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f),
                focusedContainerColor = if (isDarkMode) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f),
                unfocusedContainerColor = if (isDarkMode) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("location_search_input")
        )

        // Scrollable row of saved/added favorite cities for fast switching!
        if (searchQuery.isEmpty() && favorites.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("saved_cities_lazy_row")
            ) {
                items(favorites) { fav ->
                    val isSelected = fav.name.equals(currentLocationName, ignoreCase = true)
                    val bg = if (isSelected) {
                        accentColor
                    } else {
                        if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
                    }
                    val textColor = if (isSelected) {
                        Color.White
                    } else {
                        textPrimaryColor
                    }
                    val border = if (!isSelected && !isDarkMode) {
                        androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.08f))
                    } else null

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .then(if (border != null) Modifier.border(border, RoundedCornerShape(12.dp)) else Modifier)
                            .clickable { onCitySelected(fav) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Place,
                                contentDescription = "Saved City Location Indicator",
                                tint = if (isSelected) Color.White else accentColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = fav.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }

        // Dropdown Search Suggestion list popup
        if (searchQuery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, 
                    if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)
                ),
                elevation = CardDefaults.cardElevation(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp)
                    .testTag("search_suggestions_card")
            ) {
                when (searchUiState) {
                    is SearchUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accentColor)
                        }
                    }
                    is SearchUiState.Success -> {
                        val results = searchUiState.results
                        if (results.isEmpty()) {
                            Text(
                                text = "No matching locations found.",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondaryColor
                            )
                        } else {
                            LazyColumn {
                                items(results) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSuggestionSelected(item) }
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Place,
                                            contentDescription = "Place",
                                            tint = accentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Bold,
                                                color = textPrimaryColor
                                            )
                                            Text(
                                                text = "${item.admin1 ?: ""}, ${item.country ?: ""}",
                                                fontSize = 11.sp,
                                                color = textSecondaryColor.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    HorizontalDivider(
                                        color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
                                    )
                                }
                            }
                        }
                    }
                    is SearchUiState.Error -> {
                        Text(
                            text = "Error search suggestions: ${searchUiState.message}",
                            modifier = Modifier.padding(16.dp),
                            color = Color(0xFFF43F5E)
                        )
                    }
                    is SearchUiState.Idle -> {}
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    viewModel: WeatherViewModel,
    weatherUiState: WeatherUiState,
    plannerEvents: List<PlannerEvent>,
    isDarkMode: Boolean,
    onSeeAllClicked: () -> Unit
) {
    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkMode) Color.LightGray else Color(0xFF475569)

    when (weatherUiState) {
        is WeatherUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB))
            }
        }
        is WeatherUiState.Error -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Warning, contentDescription = "Error", tint = Color(0xFFF43F5E), modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Failed to load forecast data.", color = textPrimaryColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = weatherUiState.message, color = textSecondaryColor, textAlign = TextAlign.Center)
            }
        }
        is WeatherUiState.Success -> {
            val response = weatherUiState.data
            val current = response.current

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Severe Alerts Banner (If Any exist)
                val alerts = viewModel.getSevereWeatherAlerts(response)
                if (alerts.isNotEmpty()) {
                    item {
                        SevereAlertsCarousel(alerts)
                    }
                }

                // 2. High-end Hero Weather Banner
                item {
                    current?.let { curr ->
                        HeroWeatherSection(curr, viewModel.getWeatherDescription(curr.weatherCode), isDarkMode, viewModel)
                    }
                }

                // 3. Dual-Grid: Dressing Recommendations & Next Task Daily Planner Highlight
                item {
                    DashboardOutfitAndPlannerGrid(
                        viewModel = viewModel,
                        response = response,
                        plannerEvents = plannerEvents,
                        isDarkMode = isDarkMode
                    )
                }

                // 4. Hourly Forecast Slider
                item {
                    HourlyForecastSlider(response, viewModel, isDarkMode)
                }

                // 5. Outlook 14-Day Strip from the HTML specification
                item {
                    Outlook14DayStrip(
                        response = response,
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onSeeAllClicked = onSeeAllClicked
                    )
                }

                // 6. Details Grid (Wind speed, humidity, AQI, UV, pressure, pollen, visibility, precipitation)
                item {
                    current?.let { curr ->
                        DetailsGrid(curr, isDarkMode, viewModel)
                    }
                }
                
                // Add empty spacer
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
fun SevereAlertsCarousel(alerts: List<SevereAlert>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF43F5E).copy(alpha = 0.1f)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF43F5E).copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("severe_alerts_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFF43F5E), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SEVERE WEATHER ALERTS (${alerts.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFFFDA4AF),
                    letterSpacing = 1.2.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            alerts.forEach { alert ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "⚠️ ${alert.title}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    Text(
                        text = alert.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 22.dp)
                    )
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HeroWeatherSection(
    current: com.example.network.CurrentWeather,
    description: String,
    isDarkMode: Boolean,
    viewModel: WeatherViewModel
) {
    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)
    val degreeColor = if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF2563EB)
    val iconColor = if (isDarkMode) Color.White else Color(0xFF2563EB)
    val statsColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    val tempUnitValue by viewModel.tempUnit.collectAsStateWithLifecycle()
    val tempStr = if (tempUnitValue == "F") {
        "${((current.temperature * 9.0 / 5.0) + 32.0).toInt()}"
    } else {
        "${current.temperature.toInt()}"
    }
    val degreeSymbol = if (tempUnitValue == "F") "°F" else "°C"

    val feelsLikeStr = viewModel.formatTemperature(current.apparentTemperature)
    val windSpeedStr = viewModel.formatWindSpeed(current.windSpeed)

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hero_weather_section")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Weather Icon with soft drop glow
            Icon(
                imageVector = getWeatherIcon(current.weatherCode),
                contentDescription = description,
                tint = iconColor,
                modifier = Modifier
                    .size(96.dp)
                    .padding(8.dp)
            )

            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = tempStr,
                    fontWeight = FontWeight.Thin, // Matches light text / font-thin
                    fontSize = 84.sp,
                    color = textPrimaryColor,
                    letterSpacing = (-4).sp
                )
                Text(
                    text = degreeSymbol,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    color = degreeColor,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }

            Text(
                text = description,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondaryColor
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Thermostat, contentDescription = "Feels like", tint = statsColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Feels $feelsLikeStr", fontSize = 13.sp, color = statsColor)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Air, contentDescription = "Wind speed", tint = statsColor, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = windSpeedStr, fontSize = 13.sp, color = statsColor)
                }
            }
        }
    }
}

@Composable
fun DashboardOutfitAndPlannerGrid(
    viewModel: WeatherViewModel,
    response: WeatherResponse,
    plannerEvents: List<PlannerEvent>,
    isDarkMode: Boolean
) {
    val current = response.current ?: return
    val dressingRecommendation = viewModel.getDressingRecommendation(current.temperature, current.weatherCode)
    val umbrellaAlert = viewModel.getUmbrellaNotice(
        response.hourly?.precipitationProbability?.firstOrNull() ?: 0,
        current.weatherCode,
        current.temperature
    )

    val cardBgColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
    val cardBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)
    val labelColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF334155)
    val timeColor = if (isDarkMode) Color.LightGray.copy(alpha = 0.7f) else Color(0xFF475569)
    val cardElevation = if (isDarkMode) CardDefaults.cardElevation(0.dp) else CardDefaults.cardElevation(2.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Recommendations Card (Daily Kit)
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor),
            elevation = cardElevation,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 145.dp)
                .testTag("dressing_umbrella_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Checkroom, 
                        contentDescription = "Outfit suggestion", 
                        tint = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB), 
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "DAILY KIT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondaryColor,
                        letterSpacing = 1.2.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    Text(
                        text = dressingRecommendation.substringAfter("(").substringAfter(")").trim().let { text ->
                            if (text.length > 55) text.take(52) + "..." else text
                        },
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = labelColor,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val adviceIcon = when {
                            umbrellaAlert.first -> Icons.Outlined.Umbrella
                            current.temperature >= 24.0 -> Icons.Filled.WbSunny
                            current.temperature in 10.0..23.99 -> Icons.Filled.Checkroom
                            else -> Icons.Filled.AcUnit
                        }
                        Icon(
                            imageVector = adviceIcon,
                            contentDescription = "Accessory advice indicator",
                            tint = if (umbrellaAlert.first) (if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB)) else (if (isDarkMode) Color(0xFFFFD700) else Color(0xFFD97706)),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = umbrellaAlert.second,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (umbrellaAlert.first) (if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF1D4ED8)) else (if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF334155))
                        )
                    }
                }
            }
        }

        // Planner highlights Card (Next Task)
        val nextEvent = plannerEvents.firstOrNull { !it.isCompleted }
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor),
            elevation = cardElevation,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 145.dp)
                .testTag("dashboard_next_task_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.EventNote, 
                        contentDescription = "Next Task", 
                        tint = if (isDarkMode) Color(0xFF818CF8) else Color(0xFF4F46E5), 
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "NEXT TASK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = textSecondaryColor,
                        letterSpacing = 1.2.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (nextEvent != null) {
                    Column {
                        Text(
                            text = nextEvent.title,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            color = textPrimaryColor
                        )
                        Text(
                            text = "Scheduled at ${formatTimeStr(nextEvent.hour, nextEvent.minute)}",
                            fontSize = 11.sp,
                            color = timeColor,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = (if (isDarkMode) Color(0xFF6366F1) else Color(0xFF4F46E5)).copy(alpha = 0.15f),
                            modifier = Modifier.wrapContentSize()
                        ) {
                            Text(
                                text = "Reminder On",
                                color = if (isDarkMode) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                } else {
                    Column {
                        Text(
                            text = "No upcoming tasks",
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = textSecondaryColor
                        )
                        Text(
                            text = "Day is fully open!",
                            fontSize = 11.sp,
                            color = textSecondaryColor.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Outlook14DayStrip(
    response: WeatherResponse,
    viewModel: WeatherViewModel,
    isDarkMode: Boolean,
    onSeeAllClicked: () -> Unit
) {
    val daily = response.daily ?: return
    val titleColor = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)
    val seeAllColor = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val normalCardBg = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
    val normalBorderColor = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.06f)
    val cardElevation = if (isDarkMode) 0.dp else 2.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "🗓️ 14-DAY OUTLOOK",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "See Full Details",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = seeAllColor,
                modifier = Modifier
                    .clickable { onSeeAllClicked() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().testTag("outlook_summary_strip")
        ) {
            val length = daily.time.size
            items(List(length) { it }) { index ->
                val dateStr = daily.time[index]
                val code = daily.weatherCode[index]
                val maxTemp = daily.temperatureMax[index]
                
                val dayOfWeekStr = try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val outFormat = SimpleDateFormat("EEE", Locale.getDefault())
                    outFormat.format(inputFormat.parse(dateStr) ?: Date())
                } catch (e: Exception) {
                    dateStr.take(3)
                }

                // Make index 1 (usually tomorrow) look like the highlighted active pill in the design
                val isActive = index == 1

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isActive) (if (isDarkMode) Color(0xFF2563EB) else Color(0xFF1D4ED8)) else normalCardBg
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = if (isActive) null else androidx.compose.foundation.BorderStroke(1.dp, normalBorderColor),
                    elevation = CardDefaults.cardElevation(cardElevation),
                    modifier = Modifier
                        .width(66.dp)
                        .height(96.dp)
                        .clickable { onSeeAllClicked() }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = dayOfWeekStr,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isActive) Color(0xFFDBEAFE) else (if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f))
                        )
                        Icon(
                            imageVector = getWeatherIcon(code),
                            contentDescription = null,
                            tint = if (isActive) Color.White else (if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF2563EB)),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = viewModel.formatTemperature(maxTemp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) Color.White else (if (isDarkMode) Color.White else Color(0xFF0F172A))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OutfitAndUmbrellaCard(
    dressingRecommendation: String,
    umbrellaAlert: Pair<Boolean, String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("dressing_umbrella_card")
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "✨ DYNAMIC SMART RECOMMENDATIONS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF60A5FA),
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Dressing recommendation row
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Checkroom, contentDescription = "Outfit suggestion", tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = dressingRecommendation,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Umbrella suggestion row
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (umbrellaAlert.first) Color(0xFF2563EB).copy(alpha = 0.3f) 
                            else Color.White.copy(alpha = 0.1f), 
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (umbrellaAlert.first) Icons.Outlined.Umbrella else Icons.Filled.BeachAccess,
                        contentDescription = "Umbrella hint",
                        tint = if (umbrellaAlert.first) Color(0xFF60A5FA) else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = umbrellaAlert.second,
                    fontSize = 14.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Normal,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

fun formatSunTime(timeStr: String?): String {
    if (timeStr == null) return "--:--"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val outFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        outFormat.format(inputFormat.parse(timeStr) ?: Date())
    } catch (e: java.lang.Exception) {
        timeStr.substringAfter("T")
    }
}

fun getMoonPhaseTimes(sunriseStr: String?, sunsetStr: String?): Pair<String, String> {
    if (sunriseStr == null || sunsetStr == null) return "08:15 PM" to "06:45 AM"
    try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
        val riseDate = parser.parse(sunriseStr)
        val setDate = parser.parse(sunsetStr)
        if (riseDate != null && setDate != null) {
            val formatOut = SimpleDateFormat("h:mm a", Locale.getDefault())
            val moonriseCal = Calendar.getInstance().apply {
                time = setDate
                add(Calendar.HOUR_OF_DAY, 2)
                add(Calendar.MINUTE, 15)
            }
            val moonsetCal = Calendar.getInstance().apply {
                time = riseDate
                add(Calendar.HOUR_OF_DAY, 3)
                add(Calendar.MINUTE, 45)
            }
            return formatOut.format(moonriseCal.time) to formatOut.format(moonsetCal.time)
        }
    } catch (e: java.lang.Exception) {
        // Fallback
    }
    return "08:24 PM" to "07:12 AM"
}

sealed interface TimelineCardItem {
    val date: Date

    data class Hour(
        override val date: Date,
        val formattedTime: String,
        val temp: Double,
        val code: Int,
        val rainProb: Int
    ) : TimelineCardItem

    data class Astro(
        override val date: Date,
        val formattedTime: String,
        val title: String,
        val tint: Color,
        val icon: androidx.compose.ui.graphics.vector.ImageVector
    ) : TimelineCardItem
}

@Composable
fun HourlyForecastSlider(
    response: WeatherResponse,
    viewModel: WeatherViewModel,
    isDarkMode: Boolean
) {
    val titleColor = if (isDarkMode) Color.LightGray.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f)
    val cardBg = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.White
    val cardBorder = if (isDarkMode) null else androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    val cardEl = if (isDarkMode) 0.dp else 2.dp
    val textTimeColor = if (isDarkMode) Color.LightGray else Color(0xFF475569)
    val textTempColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val iconTint = if (isDarkMode) Color.White else Color(0xFF2563EB)
    val rainColor = if (isDarkMode) Color(0xFF29B6F6) else Color(0xFF0284C7)

    Column {
        Text(
            text = "⏳ HOURLY TREND (24H LINE & SOLAR PATH)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        
        val hourly = response.hourly
        if (hourly != null) {
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.getDefault())
            val sunrisetInstant = response.daily?.sunrise?.firstOrNull()?.let { try { parser.parse(it) } catch(e: Exception) { null } }
            val sunsetInstant = response.daily?.sunset?.firstOrNull()?.let { try { parser.parse(it) } catch(e: Exception) { null } }

            val moonriseInstant = sunsetInstant?.let {
                val cal = Calendar.getInstance().apply {
                    time = it
                    add(Calendar.HOUR_OF_DAY, 2)
                    add(Calendar.MINUTE, 15)
                }
                cal.time
            }
            val moonsetInstant = sunrisetInstant?.let {
                val cal = Calendar.getInstance().apply {
                    time = it
                    add(Calendar.HOUR_OF_DAY, 3)
                    add(Calendar.MINUTE, 45)
                }
                cal.time
            }

            val itemsList = mutableListOf<TimelineCardItem>()
            val length = minOf(24, hourly.time.size)
            val timeOutputFormat = SimpleDateFormat("hh a", Locale.getDefault())
            val astroOutputFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

            for (i in 0 until length) {
                val timeStr = hourly.time[i]
                val date = try { parser.parse(timeStr) ?: Date() } catch(e: Exception) { Date() }
                val formatted = try { timeOutputFormat.format(date) } catch(e: Exception) { timeStr.substringAfter("T") }
                itemsList.add(
                    TimelineCardItem.Hour(
                        date = date,
                        formattedTime = formatted,
                        temp = hourly.temperature[i],
                        code = hourly.weatherCode[i],
                        rainProb = hourly.precipitationProbability[i]
                    )
                )
            }

            // Intersperse Astronomy Events Chronologically
            sunrisetInstant?.let {
                val formatted = try { astroOutputFormat.format(it) } catch(e: Exception) { "06:12 AM" }
                itemsList.add(TimelineCardItem.Astro(it, formatted, "Sunrise 🌅", Color(0xFFFBBF24), Icons.Filled.WbSunny))
            }
            sunsetInstant?.let {
                val formatted = try { astroOutputFormat.format(it) } catch(e: Exception) { "07:45 PM" }
                itemsList.add(TimelineCardItem.Astro(it, formatted, "Sunset 🌇", Color(0xFFF43F5E), Icons.Filled.NightsStay))
            }
            moonriseInstant?.let {
                val formatted = try { astroOutputFormat.format(it) } catch(e: Exception) { "08:15 PM" }
                itemsList.add(TimelineCardItem.Astro(it, formatted, "Moonrise 🔮", Color(0xFFA5B4FC), Icons.Filled.Nightlight))
            }
            moonsetInstant?.let {
                val formatted = try { astroOutputFormat.format(it) } catch(e: Exception) { "06:45 AM" }
                itemsList.add(TimelineCardItem.Astro(it, formatted, "Moonset 🌑", Color(0xFF94A3B8), Icons.Filled.Brightness3))
            }

            // Sort chronologically by time
            itemsList.sortBy { it.date }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("hourly_forecast_row")
            ) {
                items(itemsList) { item ->
                    when (item) {
                        is TimelineCardItem.Hour -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp),
                                border = cardBorder,
                                elevation = CardDefaults.cardElevation(cardEl),
                                modifier = Modifier
                                    .width(85.dp)
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = item.formattedTime, fontSize = 12.sp, color = textTimeColor)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Icon(
                                        imageVector = getWeatherIcon(item.code),
                                        contentDescription = viewModel.getWeatherDescription(item.code),
                                        tint = iconTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = viewModel.formatTemperature(item.temp), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textTempColor)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    // Blue rain probability tag
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.WaterDrop, contentDescription = "humidity", tint = rainColor, modifier = Modifier.size(10.dp))
                                        Text(text = "${item.rainProb}%", fontSize = 10.sp, color = rainColor, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                        is TimelineCardItem.Astro -> {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = item.tint.copy(alpha = 0.08f)),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, item.tint.copy(alpha = 0.4f)),
                                elevation = CardDefaults.cardElevation(cardEl),
                                modifier = Modifier
                                    .width(96.dp)
                                    .padding(vertical = 4.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(text = item.title, fontSize = 11.sp, color = item.tint, fontWeight = FontWeight.ExtraBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.title,
                                        tint = item.tint,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = item.formattedTime, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textTempColor)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(text = "Solar Cycle", fontSize = 9.sp, color = textTimeColor)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Astronomy Details Cards: Sunrise, Sunset, Moonrise, Moonset
        val sunriseStr = response.daily?.sunrise?.firstOrNull()
        val sunsetStr = response.daily?.sunset?.firstOrNull()
        
        val formattedSunrise = formatSunTime(sunriseStr)
        val formattedSunset = formatSunTime(sunsetStr)
        val (moonrise, moonset) = getMoonPhaseTimes(sunriseStr, sunsetStr)

        Text(
            text = "✨ ASTRONOMY & SOLAR PATH",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
        
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(16.dp),
            border = cardBorder,
            elevation = CardDefaults.cardElevation(cardEl),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // Sunrise
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny, 
                        contentDescription = "Sunrise icon", 
                        tint = Color(0xFFFBBF24), 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Sunrise", fontSize = 11.sp, color = textTimeColor, fontWeight = FontWeight.Medium)
                    Text(text = formattedSunrise, fontSize = 13.sp, color = textTempColor, fontWeight = FontWeight.Bold)
                }

                // Sunset
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.NightsStay, 
                        contentDescription = "Sunset icon", 
                        tint = Color(0xFFF43F5E), 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Sunset", fontSize = 11.sp, color = textTimeColor, fontWeight = FontWeight.Medium)
                    Text(text = formattedSunset, fontSize = 13.sp, color = textTempColor, fontWeight = FontWeight.Bold)
                }

                // Moonrise
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Nightlight, 
                        contentDescription = "Moonrise icon", 
                        tint = Color(0xFFA5B4FC), 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Moonrise", fontSize = 11.sp, color = textTimeColor, fontWeight = FontWeight.Medium)
                    Text(text = moonrise, fontSize = 13.sp, color = textTempColor, fontWeight = FontWeight.Bold)
                }

                // Moonset
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Brightness3, 
                        contentDescription = "Moonset icon", 
                        tint = Color(0xFF94A3B8), 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "Moonset", fontSize = 11.sp, color = textTimeColor, fontWeight = FontWeight.Medium)
                    Text(text = moonset, fontSize = 13.sp, color = textTempColor, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DetailsGrid(
    current: com.example.network.CurrentWeather, 
    isDarkMode: Boolean,
    viewModel: WeatherViewModel
) {
    val cardBg = if (isDarkMode) Color.White.copy(alpha = 0.07f) else Color.White
    val borderStroke = if (isDarkMode) null else androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    val textLabelColor = if (isDarkMode) Color.LightGray else Color(0xFF475569)
    val textValueColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val cardElevation = if (isDarkMode) 0.dp else 2.dp

    val aqi = viewModel.getAQIValue(current.temperature, current.weatherCode)
    val pollen = viewModel.getPollenValue(current.temperature, current.weatherCode)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Grid Row 1: Wind Speed & Humidity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Wind Speed
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke,
                elevation = CardDefaults.cardElevation(cardElevation),
                modifier = Modifier.weight(1f).testTag("wind_speed_detail_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Filled.Air, 
                        contentDescription = "Wind speed", 
                        tint = if (isDarkMode) Color(0xFF81D4FA) else Color(0xFF0284C7), 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "WIND SPEED", fontSize = 11.sp, color = textLabelColor, fontWeight = FontWeight.Bold)
                    Text(text = viewModel.formatWindSpeed(current.windSpeed), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textValueColor)
                }
            }

            // Humidity
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke,
                elevation = CardDefaults.cardElevation(cardElevation),
                modifier = Modifier.weight(1f).testTag("humidity_detail_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Filled.WaterDrop, 
                        contentDescription = "Humidity", 
                        tint = if (isDarkMode) Color(0xFF4FC3F7) else Color(0xFF0284C7), 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "HUMIDITY", fontSize = 11.sp, color = textLabelColor, fontWeight = FontWeight.Bold)
                    Text(text = viewModel.formatHumidity(current.relativeHumidity), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textValueColor)
                }
            }
        }

        // Grid Row 2: AQI & UV Index
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // AQI Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke,
                elevation = CardDefaults.cardElevation(cardElevation),
                modifier = Modifier.weight(1f).testTag("aqi_detail_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val aqiColor = when {
                        aqi <= 50 -> Color(0xFF10B981) // Green
                        aqi <= 100 -> Color(0xFFFBBF24) // Yellow
                        else -> Color(0xFFEF4444) // Red
                    }
                    Icon(
                        Icons.Filled.Co2, 
                        contentDescription = "Air Quality Index", 
                        tint = aqiColor, 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "AIR QUALITY (AQI)", fontSize = 11.sp, color = textLabelColor, fontWeight = FontWeight.Bold)
                    Text(text = viewModel.formatAQI(aqi), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = textValueColor)
                }
            }

            // UV Index
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke,
                elevation = CardDefaults.cardElevation(cardElevation),
                modifier = Modifier.weight(1f).testTag("uv_detail_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val rawUv = current.uvIndex ?: 3.2
                    val uvColor = when {
                        rawUv <= 2.9 -> Color(0xFF10B981)
                        rawUv <= 5.9 -> Color(0xFFFBBF24)
                        rawUv <= 7.9 -> Color(0xFFF97316)
                        else -> Color(0xFFEF4444)
                    }
                    val uvDesc = when {
                        rawUv <= 2.9 -> "Low"
                        rawUv <= 5.9 -> "Moderate"
                        rawUv <= 7.9 -> "High"
                        else -> "Very High"
                    }
                    Icon(
                        Icons.Filled.WbSunny, 
                        contentDescription = "UV Index", 
                        tint = uvColor, 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "UV INDEX", fontSize = 11.sp, color = textLabelColor, fontWeight = FontWeight.Bold)
                    Text(text = "${String.format(Locale.getDefault(), "%.1f", rawUv)} ($uvDesc)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textValueColor)
                }
            }
        }

        // Grid Row 3: Pressure & Visibility
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pressure Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke,
                elevation = CardDefaults.cardElevation(cardElevation),
                modifier = Modifier.weight(1f).testTag("pressure_detail_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Filled.Speed, 
                        contentDescription = "Atmospheric Pressure", 
                        tint = if (isDarkMode) Color(0xFFA5B4FC) else Color(0xFF6366F1), 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "PRESSURE", fontSize = 11.sp, color = textLabelColor, fontWeight = FontWeight.Bold)
                    Text(text = viewModel.formatPressure(current.surfacePressure), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textValueColor)
                }
            }

            // Visibility Card
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke,
                elevation = CardDefaults.cardElevation(cardElevation),
                modifier = Modifier.weight(1f).testTag("visibility_detail_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Filled.Visibility, 
                        contentDescription = "Visibility distance", 
                        tint = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF059669), 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "VISIBILITY", fontSize = 11.sp, color = textLabelColor, fontWeight = FontWeight.Bold)
                    Text(text = viewModel.formatVisibility(current.visibility), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textValueColor)
                }
            }
        }

        // Grid Row 4: Precipitation & Pollen Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Precipitation (Rain/Snow falling)
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke,
                elevation = CardDefaults.cardElevation(cardElevation),
                modifier = Modifier.weight(1f).testTag("precipitation_detail_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Filled.Thunderstorm, 
                        contentDescription = "Precipitation", 
                        tint = if (isDarkMode) Color(0xFFF472B6) else Color(0xFFDB2777), 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "PRECIPITATION", fontSize = 11.sp, color = textLabelColor, fontWeight = FontWeight.Bold)
                    Text(text = "${current.precipitation} mm", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textValueColor)
                }
            }

            // Pollen Count
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                border = borderStroke,
                elevation = CardDefaults.cardElevation(cardElevation),
                modifier = Modifier.weight(1f).testTag("pollen_detail_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val pollenColor = when {
                        pollen <= 40 -> Color(0xFF10B981) // low
                        pollen <= 120 -> Color(0xFFF97316) // moderate
                        else -> Color(0xFFEF4444) // high
                    }
                    Icon(
                        Icons.Filled.Yard, 
                        contentDescription = "Pollen details", 
                        tint = pollenColor, 
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "POLLEN LEVEL", fontSize = 11.sp, color = textLabelColor, fontWeight = FontWeight.Bold)
                    Text(text = viewModel.formatPollen(pollen), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textValueColor)
                }
            }
        }
    }
}

@Composable
fun ForecastScreen(
    weatherUiState: WeatherUiState,
    viewModel: WeatherViewModel
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkMode) Color.LightGray else Color(0xFF475569)
    val cardBgColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.White
    val cardBorder = if (isDarkMode) null else androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    val cardElevation = if (isDarkMode) 0.dp else 2.dp
    val rainColor = if (isDarkMode) Color(0xFF29B6F6) else Color(0xFF0284C7)
    val iconColor = if (isDarkMode) Color.White else Color(0xFF2563EB)

    when (weatherUiState) {
        is WeatherUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = if (isDarkMode) Color.White else Color(0xFF2563EB))
            }
        }
        is WeatherUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = weatherUiState.message, color = textPrimaryColor)
            }
        }
        is WeatherUiState.Success -> {
            val daily = weatherUiState.data.daily
            if (daily != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "🗓️ 14-DAY EXTENDED FORECAST",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("forecast_list")
                    ) {
                        items(List(daily.time.size) { it }) { index ->
                            val dateStr = daily.time[index]
                            val weatherCode = daily.weatherCode[index]
                            val maxTemp = daily.temperatureMax[index]
                            val minTemp = daily.temperatureMin[index]
                            val rainProb = daily.precipitationProbabilityMax?.getOrNull(index) ?: 0
                            val conditionDesc = viewModel.getWeatherDescription(weatherCode)

                            val formattedDayName = try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                                val outFormat = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
                                outFormat.format(inputFormat.parse(dateStr) ?: Date())
                            } catch (e: Exception) {
                                dateStr
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                shape = RoundedCornerShape(16.dp),
                                border = cardBorder,
                                elevation = CardDefaults.cardElevation(cardElevation),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left: Daily Name details
                                    Column(modifier = Modifier.weight(1.5f)) {
                                        Text(text = formattedDayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textPrimaryColor)
                                        Text(text = conditionDesc, fontSize = 12.sp, color = textSecondaryColor)
                                    }

                                    // Mid: Rain Probability pill
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(Icons.Filled.WaterDrop, contentDescription = "Rain", tint = rainColor, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(text = "$rainProb%", color = rainColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // Right: Temperature span
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1.5f),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Icon(
                                            imageVector = getWeatherIcon(weatherCode),
                                            contentDescription = conditionDesc,
                                            tint = iconColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = viewModel.formatTemperature(maxTemp), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimaryColor)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(text = viewModel.formatTemperature(minTemp), fontSize = 14.sp, color = textSecondaryColor)
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "No forecast timeline found.", color = textPrimaryColor)
                }
            }
        }
    }
}

@Composable
fun FavoritesScreen(
    favorites: List<FavoriteLocation>,
    onLocationSelected: (FavoriteLocation) -> Unit,
    viewModel: WeatherViewModel
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkMode) Color.LightGray else Color(0xFF475569)
    val cardBgColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.White
    val borderStroke = if (isDarkMode) null else androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    val circleBg = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFF1F5F9)
    val cardElevation = if (isDarkMode) 0.dp else 2.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "💖 FAVORITE LOCATIONS (${favorites.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimaryColor,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (favorites.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.FavoriteBorder, 
                        contentDescription = "Favorites", 
                        tint = textSecondaryColor, 
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "No saved favorites yet.", color = textPrimaryColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Search a city above and check the heart button!", color = textSecondaryColor, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("favorites_list")
            ) {
                items(favorites) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        shape = RoundedCornerShape(16.dp),
                        border = borderStroke,
                        elevation = CardDefaults.cardElevation(cardElevation),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLocationSelected(item) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(circleBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Place, contentDescription = "Place", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textPrimaryColor)
                                Text(
                                    text = "Lat: ${String.format("%.4f", item.latitude)}, Lon: ${String.format("%.4f", item.longitude)}",
                                    fontSize = 11.sp,
                                    color = textSecondaryColor
                                )
                            }
                            IconButton(onClick = { viewModel.toggleFavorite(item.name, item.latitude, item.longitude) }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete from favorites", tint = Color(0xFFEF5350))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DayPlannerScreen(
    plannerEvents: List<PlannerEvent>,
    weatherUiState: WeatherUiState,
    viewModel: WeatherViewModel
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val accentColor = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB)
    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkMode) Color.LightGray else Color(0xFF475569)
    val cardBgColor = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.White
    val borderStroke = if (isDarkMode) null else androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    val cardElevation = if (isDarkMode) 0.dp else 2.dp
    val noteColor = if (isDarkMode) Color.LightGray.copy(alpha = 0.82f) else Color(0xFF334155).copy(alpha = 0.85f)
    val matchBannerBg = if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color(0xFFEFF6FF)
    val matchTextColor = if (isDarkMode) MaterialTheme.colorScheme.primary else Color(0xFF1E40AF)
    val matchIconTint = if (isDarkMode) Color.White else Color(0xFF2563EB)

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "📅 DAILY PLANNER & WEATHER MATCH",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                )
                Text(
                    text = "Coordinate your day based on forecast hours",
                    fontSize = 11.sp,
                    color = textSecondaryColor
                )
            }
            Button(
                onClick = { showAddDialog = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                modifier = Modifier.testTag("add_item_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Item", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Add Event", fontSize = 12.sp)
            }
        }

        // Show events matching forecast or standard agenda list
        if (plannerEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = "Planner", tint = textSecondaryColor, modifier = Modifier.size(56.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "No events scheduled yet.", color = textPrimaryColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Add outdoor plans and see matching hourly weather forecast!", color = textSecondaryColor, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("planner_events_list")
            ) {
                items(plannerEvents) { event ->
                    // Lookup hourly weather matches for the event if we have successful weather payload loading!
                    val matchingWeather = if (weatherUiState is WeatherUiState.Success) {
                        lookupHourlyWeather(
                            response = weatherUiState.data,
                            hour = event.hour,
                            dayOffset = event.dayOffset
                        )
                    } else null

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        shape = RoundedCornerShape(16.dp),
                        border = borderStroke,
                        elevation = CardDefaults.cardElevation(cardElevation),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = event.isCompleted,
                                    onCheckedChange = { viewModel.updateEventCompletion(event.id, it) },
                                    modifier = Modifier.testTag("event_checkbox_${event.id}")
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = event.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (event.isCompleted) Color.Gray else textPrimaryColor,
                                        style = if (event.isCompleted) MaterialTheme.typography.bodyLarge.copy(
                                            textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                        ) else LocalTextStyle.current
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Schedule, contentDescription = "Time", tint = textSecondaryColor, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${if (event.dayOffset == 0) "Today" else "Tomorrow"} at ${formatTimeStr(event.hour, event.minute)}",
                                            fontSize = 12.sp,
                                            color = textSecondaryColor
                                        )
                                        if (event.isReminderEnabled) {
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Icon(
                                                imageVector = Icons.Filled.NotificationsActive,
                                                contentDescription = "Active Alarm Badge indicator symbol",
                                                tint = accentColor,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            val customAlarmTime = if (event.reminderHour != -1 && event.reminderMinute != -1) {
                                                formatTimeStr(event.reminderHour, event.reminderMinute)
                                            } else {
                                                formatTimeStr(event.hour, event.minute)
                                            }
                                            Text(
                                                text = "$customAlarmTime (${event.notificationTune})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor
                                            )
                                        }
                                    }
                                }
                                IconButton(onClick = { viewModel.deletePlannerEvent(event) }) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete Event", tint = Color(0xFFEF5350).copy(alpha = 0.8f))
                                }
                            }

                            if (event.notes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = event.notes,
                                    fontSize = 13.sp,
                                    color = noteColor,
                                    modifier = Modifier.padding(start = 40.dp)
                                )
                            }

                            // Dynamic context banner: Weather preview match!
                            matchingWeather?.let { (temp, codeStr, code) ->
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(matchBannerBg)
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "⛅ Forecast: ${temp.toInt()}°C, $codeStr",
                                            fontSize = 12.sp,
                                            color = matchTextColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = getWeatherIcon(code),
                                            contentDescription = "Forecast",
                                            tint = matchIconTint,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // Add dialog popover
    if (showAddDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newHourStr by remember { mutableStateOf("") }
        var newMinuteStr by remember { mutableStateOf("") }
        var newDayOffset by remember { mutableStateOf(0) }
        var newNotes by remember { mutableStateOf("") }
        var isErrorMsg by remember { mutableStateOf("") }

        var isReminderEnabled by remember { mutableStateOf(false) }
        var isCustomReminderTime by remember { mutableStateOf(false) }
        var reminderHourStr by remember { mutableStateOf("") }
        var reminderMinuteStr by remember { mutableStateOf("") }
        var selectedTune by remember { mutableStateOf("Default") }

        Dialog(onDismissRequest = { showAddDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("add_event_dialog")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "Schedule Event Planner", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Event Name (e.g., Morning Run)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("event_title_input")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newHourStr,
                            onValueChange = { if (it.length <= 2) newHourStr = it },
                            label = { Text("Hour (0-23)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("event_hour_input")
                        )
                        OutlinedTextField(
                            value = newMinuteStr,
                            onValueChange = { if (it.length <= 2) newMinuteStr = it },
                            label = { Text("Min (0-59)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("event_minute_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Day selection segment
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Schedule:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(
                            onClick = { newDayOffset = 0 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (newDayOffset == 0) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f)
                            )
                        ) {
                            Text("Today", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { newDayOffset = 1 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (newDayOffset == 1) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f)
                            )
                        ) {
                            Text("Tomorrow", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("Additional Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Reminders options Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Set Alarm Reminder",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isReminderEnabled,
                            onCheckedChange = { isReminderEnabled = it },
                            modifier = Modifier.testTag("reminder_switch")
                        )
                    }

                    if (isReminderEnabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Reminder Time:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = { isCustomReminderTime = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isCustomReminderTime) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f)
                                )
                            ) {
                                Text("Event Time", fontSize = 11.sp)
                            }
                            Button(
                                onClick = { isCustomReminderTime = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isCustomReminderTime) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.2f)
                                )
                            ) {
                                Text("Custom", fontSize = 11.sp)
                            }
                        }

                        if (isCustomReminderTime) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = reminderHourStr,
                                    onValueChange = { if (it.length <= 2) reminderHourStr = it },
                                    label = { Text("Alarm Hour (0-23)", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reminder_hour_input")
                                )
                                OutlinedTextField(
                                    value = reminderMinuteStr,
                                    onValueChange = { if (it.length <= 2) reminderMinuteStr = it },
                                    label = { Text("Alarm Min (0-59)", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("reminder_minute_input")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Tune Selection
                        Text(
                            text = "Select Notification Tune:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val tuneList = listOf("Default", "Cosmic Pip", "Echo Beep", "High Alert", "Wave Chime")
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(tuneList) { tune ->
                                val selected = selectedTune == tune
                                val bg = if (selected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.15f)
                                val tc = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .clickable { selectedTune = tune }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(text = tune, fontSize = 11.sp, color = tc)
                                }
                            }
                        }
                    }

                    if (isErrorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = isErrorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showAddDialog = false }) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newTitle.trim().isEmpty()) {
                                    isErrorMsg = "Event name cannot be empty."
                                    return@Button
                                }
                                val h = newHourStr.toIntOrNull()
                                val m = newMinuteStr.toIntOrNull() ?: 0
                                if (h == null || h !in 0..23 || m !in 0..59) {
                                    isErrorMsg = "Invalid timing. Enter hour (0-23) and minute (0-59)."
                                    return@Button
                                }

                                var alarmH = -1
                                var alarmM = -1
                                if (isReminderEnabled && isCustomReminderTime) {
                                    val rh = reminderHourStr.toIntOrNull()
                                    val rm = reminderMinuteStr.toIntOrNull() ?: 0
                                    if (rh == null || rh !in 0..23 || rm !in 0..59) {
                                        isErrorMsg = "Invalid reminder custom timing. Please double check."
                                        return@Button
                                    }
                                    alarmH = rh
                                    alarmM = rm
                                }

                                viewModel.addPlannerEvent(
                                    title = newTitle,
                                    hour = h,
                                    minute = m,
                                    dayOffset = newDayOffset,
                                    notes = newNotes,
                                    isReminderEnabled = isReminderEnabled,
                                    reminderHour = alarmH,
                                    reminderMinute = alarmM,
                                    notificationTune = selectedTune
                                )
                                showAddDialog = false
                            },
                            modifier = Modifier.testTag("save_event_button")
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

// Utility translation structures
fun formatTimeStr(hour: Int, minute: Int): String {
    val suffix = if (hour >= 12) "PM" else "AM"
    val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    return String.format("%02d:%02d %s", h, minute, suffix)
}

fun lookupHourlyWeather(
    response: WeatherResponse,
    hour: Int,
    dayOffset: Int
): Triple<Double, String, Int>? {
    val hList = response.hourly ?: return null
    val targetHourVal = dayOffset * 24 + hour
    val temp = hList.temperature.getOrNull(targetHourVal) ?: return null
    val code = hList.weatherCode.getOrNull(targetHourVal) ?: 0
    val weatherDesc = when (code) {
        0 -> "Clear Sky"
        1, 2, 3 -> "Partly Cloudy"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rainy"
        71, 73, 75 -> "Snowy"
        95, 96, 99 -> "Thunderstorm"
        else -> "Pleasant"
    }
    return Triple(temp, weatherDesc, code)
}

fun getWeatherIcon(code: Int): androidx.compose.ui.graphics.vector.ImageVector {
    return when (code) {
        0 -> Icons.Filled.WbSunny
        1 -> Icons.Filled.WbCloudy
        2 -> Icons.Filled.CloudQueue
        3 -> Icons.Filled.Cloud
        45, 48 -> Icons.Filled.Menu
        51, 53, 55, 56, 57 -> Icons.Filled.WaterDrop
        61, 63, 65, 66, 67 -> Icons.Filled.Thunderstorm
        71, 73, 75, 77 -> Icons.Filled.AcUnit
        80, 81, 82 -> Icons.Filled.SevereCold
        85, 86 -> Icons.Filled.AcUnit
        95, 96, 99 -> Icons.Filled.Thunderstorm
        else -> Icons.Filled.WbSunny
    }
}

@Composable
fun SettingsScreen(
    viewModel: WeatherViewModel,
    modifier: Modifier = Modifier
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    
    val tempUnit by viewModel.tempUnit.collectAsStateWithLifecycle()
    val windUnit by viewModel.windUnit.collectAsStateWithLifecycle()
    val pressUnit by viewModel.pressUnit.collectAsStateWithLifecycle()
    val visUnit by viewModel.visUnit.collectAsStateWithLifecycle()
    val aqiUnit by viewModel.aqiUnit.collectAsStateWithLifecycle()
    val pollenUnit by viewModel.pollenUnit.collectAsStateWithLifecycle()

    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkMode) Color.LightGray else Color(0xFF475569)
    val cardBgColor = if (isDarkMode) Color.White.copy(alpha = 0.06f) else Color.White
    val cardBorder = if (isDarkMode) null else androidx.compose.foundation.BorderStroke(1.dp, Color.Black.copy(alpha = 0.05f))
    val cardEl = if (isDarkMode) 0.dp else 2.dp

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "⚙️ SYSTEM PREFERENCES",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.LightGray.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.5f),
                letterSpacing = 1.2.sp,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
            )
        }

        // Section 1: Dashboard Units Selector
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = RoundedCornerShape(20.dp),
                border = cardBorder,
                elevation = CardDefaults.cardElevation(cardEl),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Dashboard Units Selection",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize how metrics are rendered on today's view",
                        fontSize = 12.sp,
                        color = textSecondaryColor
                    )
                    HorizontalDivider(
                        color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                        modifier = Modifier.padding(vertical = 14.dp)
                    )

                    // Helper row renderer of settings options
                    // 1. Temperature Control Option
                    UnitSettingSelector(
                        title = "Temperature Unit",
                        icon = Icons.Filled.Thermostat,
                        options = listOf("Celsius (°C)" to "C", "Fahrenheit (°F)" to "F"),
                        selectedValue = tempUnit,
                        onSelected = { viewModel.updateTempUnit(it) },
                        isDarkMode = isDarkMode
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Wind unit
                    UnitSettingSelector(
                        title = "Wind Speed Unit",
                        icon = Icons.Filled.Air,
                        options = listOf("km/h" to "kmh", "mph" to "mph", "m/s" to "ms"),
                        selectedValue = windUnit,
                        onSelected = { viewModel.updateWindUnit(it) },
                        isDarkMode = isDarkMode
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Pressure unit
                    UnitSettingSelector(
                        title = "Barometric Pressure",
                        icon = Icons.Filled.Speed,
                        options = listOf("Hectopascal (hPa)" to "hPa", "Inches Hg (inHg)" to "inHg", "Millimeters (mmHg)" to "mmHg"),
                        selectedValue = pressUnit,
                        onSelected = { viewModel.updatePressUnit(it) },
                        isDarkMode = isDarkMode
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Visibility unit
                    UnitSettingSelector(
                        title = "Visibility Range",
                        icon = Icons.Filled.Visibility,
                        options = listOf("Kilometers (km)" to "km", "Miles (mi)" to "miles"),
                        selectedValue = visUnit,
                        onSelected = { viewModel.updateVisUnit(it) },
                        isDarkMode = isDarkMode
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 5. AQI Standard
                    UnitSettingSelector(
                        title = "Air Quality Standard",
                        icon = Icons.Filled.Co2,
                        options = listOf("USA Index (0-500)" to "US", "European CAQI (0-100)" to "CAQI"),
                        selectedValue = aqiUnit,
                        onSelected = { viewModel.updateAqiUnit(it) },
                        isDarkMode = isDarkMode
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // 6. Pollen unit
                    UnitSettingSelector(
                        title = "Pollen Density Standard",
                        icon = Icons.Filled.Yard,
                        options = listOf("Grains/m³" to "grains", "Index (1-5 Level)" to "index"),
                        selectedValue = pollenUnit,
                        onSelected = { viewModel.updatePollenUnit(it) },
                        isDarkMode = isDarkMode
                    )
                }
            }
        }

        // Section 2: Visual Theme Controller
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = RoundedCornerShape(20.dp),
                border = cardBorder,
                elevation = CardDefaults.cardElevation(cardEl),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Visual Aesthetic Modes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose light, dark, system defaults, or a responsive smart gradient",
                        fontSize = 12.sp,
                        color = textSecondaryColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val optionsList = listOf(
                        Triple("Light Theme", "Light", Icons.Filled.WbSunny),
                        Triple("Dark Theme", "Dark", Icons.Filled.DarkMode),
                        Triple("System Default", "System", Icons.Filled.SettingsSuggest),
                        Triple("Animated Dynamics ✨", "Animated", Icons.Filled.AutoAwesome)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        optionsList.forEach { (label, value, icon) ->
                            val isSelected = currentTheme == value
                            val animBorder = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB)) else null
                            Card(
                                onClick = { viewModel.updateThemeMode(value) },
                                shape = RoundedCornerShape(12.dp),
                                border = animBorder,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) (if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEFF6FF)) else Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = if (isSelected) (if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB)) else textSecondaryColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = label,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) (if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB)) else textPrimaryColor,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: About App (Custom Branding & Author credits requested by Sanjoy)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = RoundedCornerShape(20.dp),
                border = cardBorder,
                elevation = CardDefaults.cardElevation(cardEl),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_section")
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(if (isDarkMode) Color(0xFF1E293B) else Color(0xFFEFF6FF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Cloud,
                            contentDescription = "About App logo",
                            tint = if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        // App Name
                        text = "Weather or Not",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = textPrimaryColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        // App description
                        text = "You're going outside either way, but here's what to expect.",
                        color = textSecondaryColor,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(14.dp))
                    // Signature "Made with love by Sanjoy" in italics
                    Text(
                        text = "Made with love by Sanjoy",
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color(0xFFF472B6) else Color(0xFFDB2777),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun UnitSettingSelector(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    options: List<Pair<String, String>>, // list of Display Label to Store Value
    selectedValue: String,
    onSelected: (String) -> Unit,
    isDarkMode: Boolean
) {
    val textPrimaryColor = if (isDarkMode) Color.White else Color(0xFF0F172A)
    val textSecondaryColor = if (isDarkMode) Color.LightGray else Color(0xFF475569)

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF2563EB),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = textPrimaryColor
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (label, value) ->
                val isSelected = selectedValue == value
                val controlBg = if (isSelected) {
                    if (isDarkMode) Color(0xFF2563EB).copy(alpha = 0.25f) else Color(0xFFDBEAFE)
                } else {
                    if (isDarkMode) Color.White.copy(alpha = 0.03f) else Color(0xFFF1F5F9)
                }
                val controlBorderValue = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB))
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f))
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(controlBg)
                        .clickable { onSelected(value) }
                        .border(controlBorderValue, RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) {
                            if (isDarkMode) Color(0xFF60A5FA) else Color(0xFF2563EB)
                        } else {
                            textSecondaryColor
                        }
                    )
                }
            }
        }
    }
}
