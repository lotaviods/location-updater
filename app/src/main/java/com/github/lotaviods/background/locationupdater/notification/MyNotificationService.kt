package com.github.lotaviods.background.locationupdater.notification

import android.app.Notification
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import com.github.lotaviods.background.locationupdater.R
import com.google.android.gms.location.LocationResult


object MyNotificationService {
    fun createLocationReceivedNotification(
        context: Context?,
        location: LocationResult?,
        date: String
    ): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification.Builder(context, "default")
                .setContentTitle("onReceive periodically location: $date")
                .setSmallIcon(
                    Icon.createWithResource(
                        context,
                        android.R.drawable.ic_menu_mylocation
                    )
                )
                .setContentText("latitude: ${location?.lastLocation?.latitude} longitude: ${location?.lastLocation?.longitude}")
                .setSubText("lastLocation")
                .build()
        } else {
            Notification.Builder(context)
                .setContentTitle("onReceive periodically location: $date")
                .setSmallIcon(
                    Icon.createWithResource(
                        context,
                        android.R.drawable.ic_menu_mylocation
                    )
                )
                .setContentText("latitude: ${location?.lastLocation?.latitude} longitude: ${location?.lastLocation?.longitude}")
                .setSubText("lastLocation")
                .build()
        }
    }

    fun createWorkerNotification(context: Context?, date: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification.Builder(context, "default")
                .setContentTitle("Worker executed: $date")
                .setSmallIcon(
                    Icon.createWithResource(
                        context,
                        R.mipmap.ic_launcher
                    )
                )
                .setSubText("lastLocation")
                .build()
        } else {
            Notification.Builder(context)
                .setContentTitle("Worker executed: $date")
                .setSmallIcon(
                    Icon.createWithResource(
                        context,
                        android.R.drawable.ic_menu_mylocation
                    )
                )
                .setSubText("lastLocation")
                .build()
        }
    }
    fun createGeofenceNotification(context: Context?, notificationDetails: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, "default")
                .setContentTitle(notificationDetails)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .build()
        } else {
            Notification.Builder(context)
                .setContentTitle(notificationDetails)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .build()
        }
    }
}