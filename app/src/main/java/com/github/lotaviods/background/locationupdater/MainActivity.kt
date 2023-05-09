package com.github.lotaviods.background.locationupdater

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.github.lotaviods.background.locationupdater.ui.geofence.GeofenceScreen
import com.github.lotaviods.background.locationupdater.ui.home.HomeScreen
import com.github.lotaviods.background.locationupdater.ui.map.LocationMapScreen
import com.github.lotaviods.background.locationupdater.ui.periodically.PeriodicallyScreen
import com.github.lotaviods.background.locationupdater.ui.theme.MyApplicationTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                NavHost(navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(navController)
                    }
                    composable("map") {
                        LocationMapScreen(navController)
                    }
                    composable("periodically") {
                        PeriodicallyScreen(navController)
                    }
                    composable("geofence") {
                        GeofenceScreen(navController)
                    }
                }
            }
        }
    }
}
