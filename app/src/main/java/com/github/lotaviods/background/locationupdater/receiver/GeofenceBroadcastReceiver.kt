package com.github.lotaviods.background.locationupdater.receiver;

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.github.lotaviods.background.locationupdater.helper.ActivityServiceHelper
import com.github.lotaviods.background.locationupdater.helper.ActivityServiceHelper.isAppInForeground
import com.github.lotaviods.background.locationupdater.notification.MyNotificationService.createGeofenceNotification
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.random.Random


class GeofenceBroadcastReceiver : BroadcastReceiver() {
    // ...
    override fun onReceive(context: Context?, intent: Intent?) {
        val geofencingEvent = intent?.let { GeofencingEvent.fromIntent(it) }

        if (geofencingEvent?.hasError() == true) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Log.e(TAG, errorMessage)
            return
        }

        // Get the transition type.
        val geofenceTransition = geofencingEvent?.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val geofenceTransitionDetails = getGeofenceTransitionDetails(geofencingEvent)
            val notification = createGeofenceNotification(context, geofenceTransitionDetails)

            context?.let {
                saveLocationPrefs(it, geofenceTransitionDetails, geofenceTransition)


                (context.getSystemService(NotificationManager::class.java) as NotificationManager)
                    .notify(Random.nextInt(), notification)
            }
        } else {
            Log.e(
                TAG,
                "Tipo de transição invalida: ${geofenceTransition ?: -1}",
            )
        }
    }

    private fun getGeofenceTransitionDetails(event: GeofencingEvent): String {
        val transitionString: String
        val c: Calendar = Calendar.getInstance()

        val dataFormat = SimpleDateFormat("HH:mm:ss").format(c.time)

        val geofenceTransition = event.geofenceTransition
        transitionString = when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                "IN-${dataFormat}"
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                "OUT-${dataFormat}"
            }

            else -> {
                "OTHER-${dataFormat}"
            }
        }
        val triggeringIDs: MutableList<String?>
        triggeringIDs = ArrayList()
        for (geofence in event.triggeringGeofences!!) {
            triggeringIDs.add(geofence.requestId)
        }
        return String.format("%s: %s", transitionString, TextUtils.join(", ", triggeringIDs))
    }

    private fun saveLocationPrefs(
        context: Context,
        detail: String,
        geofencingEvent: Int?
    ) {
        val prefs = context.getSharedPreferences("geofence_updates", Context.MODE_PRIVATE)
        val editor = prefs?.edit()
        val previousEvent = prefs.getString("events", "{}") ?: "{}"

        val jsonObject = JSONObject(previousEvent)

        if (!jsonObject.has("geo_events")) {
            // Add the "last_location" key and value.
            jsonObject.put("geo_events", JSONArray())
        }

        val locationArray = jsonObject.getJSONArray("geo_events")

        val eventType = when (geofencingEvent) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                "IN"
            }

            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                "OUT"
            }

            else -> {
                "OTHER"
            }
        }

        locationArray.put(
            JSONObject()
                .put("detail", detail)
                .put("type", eventType)
                .put("background", !isAppInForeground(context))
        )

        editor?.putString("events", jsonObject.toString())

        editor?.apply()
    }

    companion object {
        private const val TAG = "GeofenceBroadcastReceiver"
    }
}