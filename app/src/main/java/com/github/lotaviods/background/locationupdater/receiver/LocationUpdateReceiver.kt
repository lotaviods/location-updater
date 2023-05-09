package com.github.lotaviods.background.locationupdater.receiver

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.github.lotaviods.background.locationupdater.helper.ActivityServiceHelper.isAppInForeground
import com.github.lotaviods.background.locationupdater.notification.MyNotificationService
import com.google.android.gms.location.LocationResult
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.util.Calendar
import kotlin.random.Random


class LocationUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val calendar = Calendar.getInstance()
        val date = DateFormat.getInstance().format(calendar.time)

        val location = intent?.let { LocationResult.extractResult(it) }

        val notification = MyNotificationService.createLocationReceivedNotification(context, location, date)

        val notificationManager = context?.getSystemService(NotificationManager::class.java) as NotificationManager

        notificationManager.notify(Random.nextInt(), notification)

        Log.i(
            TAG,
            "onReceive: lastLocale: latitude - ${location?.lastLocation?.latitude} longitude - ${location?.lastLocation?.longitude}"
        )

        saveLocationPrefs(context, location, date)

    }

    private fun saveLocationPrefs(context: Context, location: LocationResult?, date: String, ) {
        val prefs = context.getSharedPreferences("location", Context.MODE_PRIVATE)
        val editor = prefs?.edit()
        val oldLocation = prefs.getString("last_location", "{}") ?: "{}"

        val jsonObject = JSONObject(oldLocation)

        if (!jsonObject.has("last_location")) {
            // Add the "last_location" key and value.
            jsonObject.put("last_location", JSONArray())
        }

        val locationArray = jsonObject.getJSONArray("last_location")


        locationArray.put(
            JSONObject()
                .put("latitude", location?.lastLocation?.latitude.toString())
                .put("longitude", location?.lastLocation?.longitude.toString())
                .put("time", date)
                .put("background", !isAppInForeground(context))
        )

        editor?.putString("last_location", jsonObject.toString())

        editor?.apply()
    }

    companion object {
        private const val TAG = "LocationUpdateReceiver"
    }
}