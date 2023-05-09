@file:OptIn(ExperimentalMaterial3Api::class)

package com.github.lotaviods.background.locationupdater.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.github.lotaviods.background.locationupdater.R

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar({ Text(text = context.getString(R.string.app_name)) }, Modifier)
        },
        content = { paddingValues ->
            HomeContent(Modifier.padding(paddingValues), navController)
        },
    )
}

@Composable
private fun HomeContent(modifier: Modifier = Modifier, navController: NavHostController) {
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { navController.navigate("periodically") }
            ) {
                Text("Open Periodically location")
            }
            Button(
                onClick = {
                    navController.navigate("geofence")
                }
            ) {
                Text("Open geofence location")
            }
        }
    }


}