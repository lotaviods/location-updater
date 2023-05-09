@file:OptIn(ExperimentalMaterial3Api::class)

package com.github.lotaviods.background.locationupdater.ui.periodically

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.github.lotaviods.background.locationupdater.ui.location.executeUnderLocationPermission
import com.github.lotaviods.background.locationupdater.workers.LocationWorker
import com.github.lotaviods.background.locationupdater.workers.LocationWorker.Companion.MIN_DISTANCE
import com.github.lotaviods.background.locationupdater.workers.LocationWorker.Companion.UPDATE_INTERVAL
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@Composable
fun PeriodicallyScreen(navController: NavHostController) {
    val context = LocalContext.current
    val locationArray = remember {
        mutableStateOf(getLocations(context).getJSONArray("last_location"))
    }


    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions: Map<String, Boolean> ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] == true
            ) {
                createWorkManager(context)
            } else {
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }


    Scaffold(
        topBar = {
            TopAppBar({ Text(text = "Periodically updates") }, Modifier)
        },
        content = { paddingValues ->
            PeriodicallyContent(
                Modifier.padding(paddingValues),
                locationArray,
                locationPermissionLauncher,
                navController
            )
        },
    )
}

@Composable
private fun PeriodicallyContent(
    modifier: Modifier,
    locationArray: MutableState<JSONArray>,
    locationPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    navController: NavHostController
) {
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(locationArray.value.length()) {
                val obj = (locationArray.value[it] as JSONObject)
                Column {
                    Text("latitude: ${obj.opt("latitude")}")
                    Text("longitude: ${obj.opt("longitude")}")
                    Text("time: ${obj.opt("time")}")
                    Text("background: ${obj.opt("background")}")
                }
                Divider(
                    color = Color.DarkGray,
                    thickness = 1.dp,
                )
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 15.dp, end = 10.dp, start = 10.dp),
            horizontalArrangement = Arrangement.Absolute.SpaceEvenly
        ) {
            Button(
                onClick = {
                    executeUnderLocationPermission(
                        context,
                        locationPermissionLauncher
                    ) {
                        createWorkManager(context = context)
                    }
                }
            ) {
                Text(
                    text = "Schedule background location",
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = {
                    navController.navigate("map")
                }
            ) {
                Text(
                    text = "Open maps",
                    textAlign = TextAlign.Center
                )
            }

        }
        Row(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 15.dp, end = 10.dp, start = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    clearLocations(
                        preferences = context.getSharedPreferences(
                            "location",
                            Context.MODE_PRIVATE
                        )
                    )
                }
            ) {
                Text(
                    text = "Clear locations",
                    textAlign = TextAlign.Center
                )
            }
            Button(
                onClick = {
                    locationArray.value =
                        getLocations(context).getJSONArray("last_location")
                }
            ) {
                Text(
                    text = "Reload",
                    textAlign = TextAlign.Center
                )
            }
        }

    }
}

fun clearLocations(preferences: SharedPreferences) {
    preferences.edit().clear().apply()
}

private fun createWorkManager(context: Context) {
    val updateInterval = 15L

    val locationRequest: PeriodicWorkRequest = PeriodicWorkRequest.Builder(
        LocationWorker::class.java,
        updateInterval,
        TimeUnit.MINUTES
    ).setInitialDelay(0, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "LocationUpdates",
        ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
        locationRequest
    )

    Toast.makeText(
        context,
        "Min distance set: ${MIN_DISTANCE.toString().removeSuffix("f")} meters",
        Toast.LENGTH_LONG
    ).show()

    Toast.makeText(
        context,
        "Update interval: ${(UPDATE_INTERVAL / (1000 * 60) % 60)} minutes",
        Toast.LENGTH_LONG
    ).show()
}


fun getLocations(context: Context): JSONObject {
    val prefs = context.getSharedPreferences("location", Context.MODE_PRIVATE)
    val oldLocation = prefs.getString("last_location", "{}") ?: "{}"
    return JSONObject(oldLocation).apply {
        if (!this.has("last_location")) {

            // Add the "last_location" key and value.
            this.put("last_location", JSONArray())
        }
    }
}