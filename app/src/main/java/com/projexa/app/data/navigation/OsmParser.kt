package com.projexa.app.data.navigation

import com.projexa.app.domain.models.GeoPoint
import com.projexa.app.domain.models.ManeuverKind
import com.projexa.app.domain.models.Place
import com.projexa.app.domain.models.Route
import com.projexa.app.domain.models.RouteStep
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

/** Pure JSON parsing for Nominatim (search) and OSRM (route) responses. */
object OsmParser {
    fun parseSearch(body: String): List<Place> {
        val arr = JSONArray(body)
        return (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val lat = o.optString("lat").toDoubleOrNull()
            val lon = o.optString("lon").toDoubleOrNull()
            if (lat == null || lon == null) null
            else Place(o.optString("display_name", "Unnamed place"), GeoPoint(lat, lon))
        }
    }

    fun parseRoute(body: String): Route {
        val root = JSONObject(body)
        if (root.optString("code") != "Ok") throw IOException("No walking route found")
        val r = root.getJSONArray("routes").getJSONObject(0)
        val coords = r.getJSONObject("geometry").getJSONArray("coordinates")
        val geometry = (0 until coords.length()).map {
            val c = coords.getJSONArray(it)
            GeoPoint(c.getDouble(1), c.getDouble(0))
        }
        val steps = mutableListOf<RouteStep>()
        val legs = r.getJSONArray("legs")
        for (l in 0 until legs.length()) {
            val st = legs.getJSONObject(l).getJSONArray("steps")
            for (i in 0 until st.length()) {
                val s = st.getJSONObject(i)
                val m = s.getJSONObject("maneuver")
                val loc = m.getJSONArray("location")
                val modifier = m.optString("modifier")
                val kind = kindOf(m.optString("type"), modifier)
                steps += RouteStep(
                    instruction = instructionText(kind, modifier, s.optString("name")),
                    kind = kind,
                    distanceM = s.optDouble("distance", 0.0),
                    at = GeoPoint(loc.getDouble(1), loc.getDouble(0)),
                )
            }
        }
        if (steps.isEmpty()) throw IOException("Route has no steps")
        return Route(r.optDouble("distance", 0.0), steps, geometry)
    }

    fun kindOf(type: String, modifier: String): ManeuverKind = when {
        type == "arrive" -> ManeuverKind.ARRIVE
        type == "depart" -> ManeuverKind.DEPART
        modifier == "uturn" -> ManeuverKind.UTURN
        modifier.contains("left") -> ManeuverKind.LEFT
        modifier.contains("right") -> ManeuverKind.RIGHT
        else -> ManeuverKind.STRAIGHT
    }

    fun instructionText(kind: ManeuverKind, modifier: String, street: String): String {
        val onto = if (street.isNotBlank()) " onto $street" else ""
        return when (kind) {
            ManeuverKind.ARRIVE -> "You have arrived"
            ManeuverKind.DEPART -> "Start walking" + if (street.isNotBlank()) " on $street" else ""
            ManeuverKind.UTURN -> "Make a U-turn$onto"
            ManeuverKind.LEFT -> when (modifier) {
                "sharp left" -> "Turn sharp left$onto"
                "slight left" -> "Bear left$onto"
                else -> "Turn left$onto"
            }
            ManeuverKind.RIGHT -> when (modifier) {
                "sharp right" -> "Turn sharp right$onto"
                "slight right" -> "Bear right$onto"
                else -> "Turn right$onto"
            }
            ManeuverKind.STRAIGHT -> "Continue straight$onto"
        }
    }
}
