package com.github.lotaviods.background.locationupdater.ui.map

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import com.github.lotaviods.background.locationupdater.MainActivity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

@Composable
fun LocationMapScreen(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("location", Context.MODE_PRIVATE)
    val oldLocation = prefs.getString("last_location", "{}") ?: "{}"
    val jsonObject = JSONObject(oldLocation).apply {
        if (!this.has("last_location")) {

            // Add the "last_location" key and value.
            this.put("last_location", JSONArray())
        }
    }

    val locationArray = jsonObject.getJSONArray("last_location")

    val latLongList = mutableListOf<Pair<Double, Double>>()


    for (index in 0 until locationArray.length()) {
        val element = locationArray.get(index) as JSONObject

        val latitude = element.get("latitude").toString().toDoubleOrNull()
        val longitude = element.get("longitude").toString().toDoubleOrNull()

        if (latitude != null && longitude != null)
            latLongList.add(Pair(latitude, longitude))
    }

    if (latLongList.isEmpty()) {
        LaunchedEffect(Unit) {
            navController.navigate("periodically")
        }
        return
    }

    val markerList = latLongList.joinToString(",") { "[${it.first},${it.second}]" }
    val leafletHtml = """
    <html>
    <head>
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/leaflet.css">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/MarkerCluster.css">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/MarkerCluster.Default.css">
        <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.7.1/leaflet.js"></script>
        <script src="https://cdnjs.cloudflare.com/ajax/libs/leaflet.markercluster/1.4.1/leaflet.markercluster.js"></script>
        <style>
            #map {
                height: 100%;
            }
        </style>
    </head>
    <body>
        <div id="map"></div>
        <script>
            var map = L.map('map').setView([${latLongList[0].first}, ${latLongList[0].second}], 12);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors'
            }).addTo(map);
            var markers = L.markerClusterGroup();
            var locations = [${markerList}];
            locations.forEach(function (location) {
                var marker = L.marker([location[0], location[1]]);
                markers.addLayer(marker);
            });
            map.addLayer(markers);
        </script>
    </body>
    </html>
""".trimIndent()

    val fileName = "map.html"
    val dir = context.getExternalFilesDir(null)
    val file = File(dir, fileName)
    file.writeText(leafletHtml)

    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = uri
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
        (context as? MainActivity)?.finish()
    }
}
