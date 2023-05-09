@file:OptIn(ExperimentalMaterial3Api::class)

package com.github.lotaviods.background.locationupdater.ui.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.location.Location
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.github.lotaviods.background.locationupdater.receiver.GeofenceBroadcastReceiver
import com.github.lotaviods.background.locationupdater.ui.location.executeUnderLocationPermission
import com.github.lotaviods.background.locationupdater.ui.periodically.clearLocations
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.OnTokenCanceledListener
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun GeofenceScreen(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("geofence_updates", Context.MODE_PRIVATE)
    val oldLocation = prefs.getString("events", "{}") ?: "{}"
    val jsonObject = JSONObject(oldLocation).apply {
        if (!this.has("geo_events")) {

            // Add the "last_location" key and value.
            this.put("geo_events", JSONArray())
        }
    }
    val geoEvents = jsonObject.getJSONArray("geo_events")


    val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(LocalContext.current)

    val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    val locationPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions: Map<String, Boolean> ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true &&
                permissions[Manifest.permission.ACCESS_BACKGROUND_LOCATION] == true
            ) {

            } else {
                Toast.makeText(context, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }

    val gpsResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {

        }

    Scaffold(
        topBar = {
            TopAppBar({ Text(text = "Geofence") }, Modifier)
        },
        content = { paddingContent ->
            GeofenceContent(
                modifier = Modifier.padding(paddingContent),
                createGeofence = {
                    createVirtualFence(
                        context,
                        locationPermissionLauncher,
                        gpsResultLauncher,
                        geofencingClient,
                        geofencePendingIntent,
                    )
                },
                removeGeofence = {
                    removeVirtualFence(geofencingClient, context)
                },
                geoEvents = geoEvents
            )

        }
    )
}


@Composable
private fun GeofenceContent(
    modifier: Modifier,
    createGeofence: () -> Unit,
    removeGeofence: () -> Unit,
    geoEvents: JSONArray
) {
    val context = LocalContext.current

    Column(modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(geoEvents.length()) {
                val obj = (geoEvents[it] as JSONObject)
                Column {
                    Text("detail: ${obj.opt("detail")}")
                    Text("type: ${obj.opt("type")}")
                    Text("background: ${obj.opt("background")}")
                }
                Divider(
                    color = Color.DarkGray,
                    thickness = 1.dp,
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(
                10.dp,
                alignment = Alignment.CenterHorizontally
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { removeGeofence() }
            ) {
                Text("Remove geofence")
            }
            Button(
                onClick = {
                    createGeofence()
                }
            ) {
                Text("Start geofence")
            }
        }
        Row (horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    clearLocations(
                        preferences = context.getSharedPreferences(
                            "geofence_updates",
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
        }
    }
}

fun removeVirtualFence(geofencingClient: GeofencingClient, context: Context) {
    geofencingClient.removeGeofences(listOf("InitialFence")).addOnSuccessListener {
        Toast.makeText(
            context,
            "Geofence removed",
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun getGeofencingRequest(geofenceList: List<Geofence>): GeofencingRequest {
    return GeofencingRequest.Builder().apply {
        setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        addGeofences(geofenceList)
    }.build()
}

@SuppressLint("MissingPermission")
private fun createVirtualFence(
    context: Context,
    locationPermissionLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    gpsResultLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    geofencingClient: GeofencingClient,
    geofencePendingIntent: PendingIntent,
) {

    getLocation(context, locationPermissionLauncher, gpsResultLauncher) { success, location ->
        if (!success) {
            Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
            return@getLocation
        }

        Toast.makeText(
            context,
            "lat: ${location?.latitude} long: ${location?.longitude}",
            Toast.LENGTH_LONG
        ).show()


        val geofence = Geofence.Builder()
            // Set the request ID of the geofence. This is a string to identify this
            // geofence.
            .setRequestId("InitialFence")

            // Set the circular region of this geofence.
            .setCircularRegion(
                location?.latitude ?: 0.0,
                location?.longitude ?: 0.0,
                100.0f
            )

            // Set the expiration duration of the geofence. This geofence gets automatically
            // removed after this period of time.
            .setExpirationDuration(Geofence.NEVER_EXPIRE)

            // Set the transition types of interest. Alerts are only generated for these
            // transition. We track entry and exit transitions in this sample.
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)


            // Create the geofence.
            .build()
        val request = getGeofencingRequest(geofenceList = listOf(geofence))

        geofencingClient.addGeofences(request, geofencePendingIntent).run {
            addOnSuccessListener {
                // Geofences added
                Toast.makeText(context, "Geofence created", Toast.LENGTH_SHORT).show()
                // ...
            }
            addOnFailureListener {
                Toast.makeText(context, "Fail to create Geofence", Toast.LENGTH_SHORT).show()
                // Failed to add geofences
                // ...
            }
        }
    }

}

@SuppressLint("MissingPermission")
fun getLocation(
    context: Context, locationPermissionLauncher:
    ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    gpsResultLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    onComplete: (success: Boolean, Location?) -> Unit
) {
    executeUnderLocationPermission(context, locationPermissionLauncher) {
        askLocation(context, gpsResultLauncher) { success ->
            if (!success) return@askLocation

            val locationServices =
                context.let { LocationServices.getFusedLocationProviderClient(it) }

            locationServices.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                object : CancellationToken() {
                    override fun onCanceledRequested(p0: OnTokenCanceledListener): CancellationToken {
                        return this
                    }

                    override fun isCancellationRequested(): Boolean {
                        return false
                    }

                })
                .addOnSuccessListener { location: Location? ->
                    onComplete(true, location)
                }.addOnFailureListener {
                    onComplete(false, null)
                }
        }


    }
}

private fun askLocation(
    context: Context,
    gpsResultLauncher: ManagedActivityResultLauncher<IntentSenderRequest, ActivityResult>,
    onComplete: (success: Boolean) -> Unit,
) {
    val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()


    val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
    builder.setAlwaysShow(true)

    val result = LocationServices.getSettingsClient(context)
        .checkLocationSettings(builder.build())

    result.addOnSuccessListener {
        if (it.locationSettingsStates?.isGpsUsable == true) {
            onComplete(true)
        }
    }.addOnFailureListener {
        val statusCode = (it as ApiException).statusCode;
        if (statusCode == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
            try {
                (it as ResolvableApiException)

                val request = IntentSenderRequest.Builder(it.resolution).build()
                gpsResultLauncher.launch(request)

            } catch (e: IntentSender.SendIntentException) {
                e.printStackTrace()
            }
        }
    }
}
