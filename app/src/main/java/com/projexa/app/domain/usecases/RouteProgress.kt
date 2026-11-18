package com.projexa.app.domain.usecases

import com.projexa.app.domain.models.GeoPoint
import com.projexa.app.domain.models.ManeuverKind
import com.projexa.app.domain.models.Route
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

object Geo {
    private const val R = 6_371_000.0

    fun distanceM(a: GeoPoint, b: GeoPoint): Double {
        val dLat = Math.toRadians(b.lat - a.lat)
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(a.lat)) * cos(Math.toRadians(b.lat)) * sin(dLon / 2) * sin(dLon / 2)
        return 2 * R * atan2(sqrt(h), sqrt(1 - h))
    }

    /** Distance from point [p] to segment a-b (local flat-earth approximation, fine for short segments). */
    fun distanceToSegmentM(p: GeoPoint, a: GeoPoint, b: GeoPoint): Double {
        val latScale = PI / 180 * R
        val lonScale = latScale * cos(Math.toRadians(p.lat))
        val ax = (a.lon - p.lon) * lonScale
        val ay = (a.lat - p.lat) * latScale
        val bx = (b.lon - p.lon) * lonScale
        val by = (b.lat - p.lat) * latScale
        val dx = bx - ax
        val dy = by - ay
        val len2 = dx * dx + dy * dy
        val t = if (len2 == 0.0) 0.0 else max(0.0, min(1.0, -(ax * dx + ay * dy) / len2))
        val cx = ax + t * dx
        val cy = ay + t * dy
        return sqrt(cx * cx + cy * cy)
    }
}

object RouteProgress {
    const val PASS_RADIUS_M = 12.0
    const val ARRIVE_RADIUS_M = 20.0

    data class Progress(
        val stepIndex: Int,
        /** Straight-line distance to the next maneuver. */
        val distToNextM: Double,
        val remainingM: Double,
        val arrived: Boolean,
    )

    fun distanceToRouteM(p: GeoPoint, route: Route): Double {
        val g = route.geometry
        if (g.size < 2) return g.firstOrNull()?.let { Geo.distanceM(p, it) } ?: Double.MAX_VALUE
        var best = Double.MAX_VALUE
        for (i in 0 until g.size - 1) best = min(best, Geo.distanceToSegmentM(p, g[i], g[i + 1]))
        return best
    }

    /**
     * Step i's maneuver starts step i; the NEXT maneuver to perform is steps[i+1].
     * Advances past maneuvers the walker has reached.
     */
    fun advance(route: Route, currentIndex: Int, p: GeoPoint): Progress {
        val steps = route.steps
        if (steps.size < 2) {
            val d = Geo.distanceM(p, route.geometry.lastOrNull() ?: p)
            return Progress(0, d, d, d <= ARRIVE_RADIUS_M)
        }
        var idx = currentIndex.coerceIn(0, steps.size - 2)
        while (true) {
            val next = steps[idx + 1]
            val d = Geo.distanceM(p, next.at)
            if (next.kind == ManeuverKind.ARRIVE) {
                return Progress(idx, d, d, d <= ARRIVE_RADIUS_M)
            }
            if (d <= PASS_RADIUS_M && idx + 1 < steps.size - 1) {
                idx++
                continue
            }
            var remaining = d
            for (j in idx + 1 until steps.size - 1) remaining += steps[j].distanceM
            return Progress(idx, d, remaining, false)
        }
    }
}
