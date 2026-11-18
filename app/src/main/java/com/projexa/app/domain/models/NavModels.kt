package com.projexa.app.domain.models

data class GeoPoint(val lat: Double, val lon: Double)

data class Place(val name: String, val point: GeoPoint)

enum class ManeuverKind { DEPART, STRAIGHT, LEFT, RIGHT, UTURN, ARRIVE }

/**
 * @property at where this step's maneuver happens
 * @property distanceM length of this step, i.e. distance from [at] to the next step's maneuver
 */
data class RouteStep(
    val instruction: String,
    val kind: ManeuverKind,
    val distanceM: Double,
    val at: GeoPoint,
)

data class Route(
    val distanceM: Double,
    val steps: List<RouteStep>,
    val geometry: List<GeoPoint>,
)

data class SavedPlaces(
    val home: Place? = null,
    val work: Place? = null,
    val history: List<Place> = emptyList(),
)
