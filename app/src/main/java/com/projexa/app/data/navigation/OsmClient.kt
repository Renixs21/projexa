package com.projexa.app.data.navigation

import com.projexa.app.domain.models.GeoPoint
import com.projexa.app.domain.models.Place
import com.projexa.app.domain.models.Route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Free OpenStreetMap-based search (Nominatim) and walking routes (FOSSGIS foot routing).
 * These public servers are for light use; swap this class for Google Maps Platform for production.
 * The user's coordinates and search text are sent to these servers.
 */
class OsmClient {
    suspend fun search(query: String): List<Place> = withContext(Dispatchers.IO) {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        OsmParser.parseSearch(get("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=6&q=$q"))
    }

    suspend fun walkingRoute(from: GeoPoint, to: GeoPoint): Route = withContext(Dispatchers.IO) {
        val url = String.format(
            Locale.US,
            "https://routing.openstreetmap.de/routed-foot/route/v1/foot/%.6f,%.6f;%.6f,%.6f?steps=true&overview=full&geometries=geojson",
            from.lon, from.lat, to.lon, to.lat,
        )
        OsmParser.parseRoute(get(url))
    }

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.setRequestProperty("User-Agent", "PROJEXA-Android/0.1")
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            if (c.responseCode != 200) throw IOException("Server returned ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}
