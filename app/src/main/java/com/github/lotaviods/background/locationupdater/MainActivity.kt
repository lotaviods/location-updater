package com.github.lotaviods.background.locationupdater

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.github.lotaviods.background.locationupdater.ui.theme.MyApplicationTheme
import com.github.lotaviods.background.locationupdater.workers.LocationWorker
import java.util.concurrent.TimeUnit


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("location", Context.MODE_PRIVATE)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                Scaffold(topBar = {
                    TopAppBar({ Text(text = getString(R.string.app_name)) }, Modifier)
                }, content = { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        Column {
                            Text(
                                text = "Last location update: ${
                                    prefs.getString(
                                        "last_date",
                                        "null"
                                    )
                                } "
                            )

                            Text(text = "latitude: ${prefs.getString("last_latitude", "-1")}")
                            Text(text = "longitude: ${prefs.getString("last_longitude", "-1")}")

                        }

                        Button(onClick = {
                            createWorkManager(context = context)
                        }, Modifier.align(Alignment.Center)) {
                            Text(text = "Click me")
                        }
                    }
                })
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { isGranted ->

        }


    private fun createWorkManager(context: Context) {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val permissions = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }

            requestPermissionLauncher.launch(permissions.toTypedArray())
            Toast.makeText(context, "Insufficient permissions", Toast.LENGTH_SHORT).show()

            return
        }

        val constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val updateInterval = 15L

        val locationRequest: PeriodicWorkRequest = PeriodicWorkRequest.Builder(
            LocationWorker::class.java,
            updateInterval,
            TimeUnit.MINUTES
        ).setInitialDelay(0, TimeUnit.MILLISECONDS)
            .setConstraints(constraints)
            .build()

        OneTimeWorkRequestBuilder<LocationWorker>().setConstraints(constraints)

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "LocationUpdates",
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            locationRequest
        )
    }
}
