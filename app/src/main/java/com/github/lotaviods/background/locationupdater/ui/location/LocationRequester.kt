package com.github.lotaviods.background.locationupdater.ui.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.core.content.ContextCompat

fun executeUnderLocationPermission(
    context: Context,
    locationResultLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    happyPathCallback: () -> Unit
) {
    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {

        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        locationResultLauncher.launch(permissions)
        return
    }

    if (ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            locationResultLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
        }
        happyPathCallback()
        return
    }

    happyPathCallback()
}