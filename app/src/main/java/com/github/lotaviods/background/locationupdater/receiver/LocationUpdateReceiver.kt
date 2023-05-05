package com.github.lotaviods.background.locationupdater.receiver

import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.util.Log
import com.google.android.gms.location.LocationResult
import java.text.DateFormat
import java.util.Calendar
import kotlin.random.Random

class LocationUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val calendar = Calendar.getInstance()
        val date = DateFormat.getInstance().format(calendar.time)

        val location = intent?.let { LocationResult.extractResult(it) }

        val notification = createLastLocationNotification(context, location, date)

        val notificationManager = context?.getSystemService(NotificationManager::class.java) as NotificationManager

        notificationManager.notify(Random.nextInt(), notification)

        Log.i(
            TAG,
            "onReceive: lastLocale: latitude - ${location?.lastLocation?.latitude} longitude - ${location?.lastLocation?.longitude}"
        )


        val prefs = context.getSharedPreferences("location", Context.MODE_PRIVATE)
        val editor = prefs?.edit()

        editor?.putString("last_latitude", (location?.lastLocation?.latitude).toString())
        editor?.putString("last_longitude", (location?.lastLocation?.longitude).toString())
        editor?.putString("last_date", date)
        editor?.apply()
    }


    private fun createLastLocationNotification(context: Context?, location: LocationResult?, date: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification.Builder(context, "default")
                .setContentTitle("onReceive location: $date")
                .setSmallIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_mylocation))
                .setContentText("latitude: ${location?.lastLocation?.latitude} longitude: ${location?.lastLocation?.longitude}")
                .setSubText("lastLocation")
                .build()
        } else {
            Notification.Builder(context)
                .setContentTitle("onReceive location: $date")
                .setSmallIcon(Icon.createWithResource(context, android.R.drawable.ic_menu_mylocation))
                .setContentText("latitude: ${location?.lastLocation?.latitude} longitude: ${location?.lastLocation?.longitude}")
                .setSubText("lastLocation")
                .build()
        }
    }

    companion object {
        private const val TAG = "LocationUpdateReceiver"
    }
}