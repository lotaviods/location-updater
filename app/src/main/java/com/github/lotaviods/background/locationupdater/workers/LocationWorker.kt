package com.github.lotaviods.background.locationupdater.workers

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.lotaviods.background.locationupdater.receiver.LocationUpdateReceiver
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.DateFormat
import java.util.Calendar
import kotlin.coroutines.resume
import kotlin.random.Random


class LocationWorker(context: Context, workerParams: WorkerParameters) :
    CoroutineWorker(context, workerParams) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private fun buildLocationRequest(): LocationRequest {

        return LocationRequest
            .Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL)
            .setMinUpdateDistanceMeters(MIN_DISTANCE)
            .build()
    }

    override suspend fun doWork(): Result = suspendCancellableCoroutine { continuation ->
        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val calendar = Calendar.getInstance()
            val date = DateFormat.getInstance().format(calendar.time)

            val locationRequest = buildLocationRequest()

            val callbackIntent = Intent(applicationContext, LocationUpdateReceiver::class.java)

            val pendingIntent = PendingIntent.getBroadcast(
                applicationContext,
                0,
                callbackIntent,
                PendingIntent.FLAG_MUTABLE
            )

            val notification = createWorkerNotification(applicationContext, date)

            val notificationManager =
                applicationContext.getSystemService(NotificationManager::class.java) as NotificationManager

            notificationManager.notify(Random.nextInt(), notification)
            fusedLocationClient.requestLocationUpdates(locationRequest, pendingIntent)
            fusedLocationClient.flushLocations()

            continuation.resume(Result.success())
            return@suspendCancellableCoroutine
        }
        continuation.resume(Result.failure())
    }

    companion object {
        private const val UPDATE_INTERVAL = (10 * 60 * 30).toLong() // 3 minutes
        private const val MIN_DISTANCE = 05f // 5 meters
    }

    private fun createWorkerNotification(context: Context?, date: String): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            Notification.Builder(context, "default")
                .setContentTitle("Worker executed: $date")
                .setSmallIcon(
                    Icon.createWithResource(
                        context,
                        android.R.drawable.ic_menu_mylocation
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
}