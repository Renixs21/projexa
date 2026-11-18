package com.projexa.app

import com.projexa.app.data.navigation.OsmParser
import com.projexa.app.domain.models.GeoPoint
import com.projexa.app.domain.models.ManeuverKind
import com.projexa.app.domain.models.Route
import com.projexa.app.domain.models.RouteStep
import com.projexa.app.domain.usecases.Geo
import com.projexa.app.domain.usecases.RouteProgress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationLogicTest {
    // ~111 m per 0.001 degree of latitude.
    private val a = GeoPoint(12.9000, 77.5000)
    private val b = GeoPoint(12.9010, 77.5000) // ~111 m north
    private val c = GeoPoint(12.9010, 77.5010) // ~108 m east of b

    private val route = Route(
        distanceM = 220.0,
        steps = listOf(
            RouteStep("Start walking", ManeuverKind.DEPART, 111.0, a),
            RouteStep("Turn right", ManeuverKind.RIGHT, 108.0, b),
            RouteStep("You have arrived", ManeuverKind.ARRIVE, 0.0, c),
        ),
        geometry = listOf(a, b, c),
    )

    @Test
    fun haversineIsAboutRight() {
        assertEquals(111.2, Geo.distanceM(a, b), 1.0)
    }

    @Test
    fun distanceToRouteIsSmallOnRouteAndLargeOff() {
        assertTrue(RouteProgress.distanceToRouteM(GeoPoint(12.9005, 77.5000), route) < 2.0)
        assertTrue(RouteProgress.distanceToRouteM(GeoPoint(12.9005, 77.5005), route) > 40.0)
    }

    @Test
    fun approachingTurnReportsDistanceToIt() {
        val p = RouteProgress.advance(route, 0, GeoPoint(12.9008, 77.5000))
        assertEquals(0, p.stepIndex)
        assertEquals(22.0, p.distToNextM, 2.0)
        assertFalse(p.arrived)
    }

    @Test
    fun passingTurnAdvancesStepAndArrivalIsDetected() {
        val atTurn = RouteProgress.advance(route, 0, GeoPoint(12.90095, 77.5000))
        assertEquals(1, atTurn.stepIndex)
        val end = RouteProgress.advance(route, 1, GeoPoint(12.9010, 77.50095))
        assertTrue(end.arrived)
    }

    @Test
    fun parsesOsrmRouteAndMapsManeuvers() {
        val json = """{"code":"Ok","routes":[{"distance":220.0,"geometry":{"coordinates":[[77.5,12.9],[77.5,12.901],[77.501,12.901]]},
          "legs":[{"steps":[
            {"distance":111.0,"name":"Main Rd","maneuver":{"type":"depart","location":[77.5,12.9]}},
            {"distance":108.0,"name":"Side St","maneuver":{"type":"turn","modifier":"left","location":[77.5,12.901]}},
            {"distance":0.0,"name":"","maneuver":{"type":"arrive","location":[77.501,12.901]}}]}]}]}"""
        val r = OsmParser.parseRoute(json)
        assertEquals(3, r.steps.size)
        assertEquals(ManeuverKind.LEFT, r.steps[1].kind)
        assertEquals("Turn left onto Side St", r.steps[1].instruction)
        assertEquals(ManeuverKind.ARRIVE, r.steps[2].kind)
        assertEquals(12.901, r.steps[1].at.lat, 1e-9)
    }

    @Test
    fun parsesSearchResults() {
        val json = """[{"display_name":"Cubbon Park, Bengaluru","lat":"12.9763","lon":"77.5929"}]"""
        val places = OsmParser.parseSearch(json)
        assertEquals(1, places.size)
        assertEquals(77.5929, places[0].point.lon, 1e-9)
    }
}
