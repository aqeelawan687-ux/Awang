package com.example.util

object DistanceCalculator {

    fun calculateFare(
        pickup: String,
        dropoff: String,
        baseFare: Double = 100.0,
        perKmRate: Double = 30.0
    ): Double {
        val estimatedKm = estimateDistance(pickup, dropoff)
        return (baseFare + (estimatedKm * perKmRate)).coerceAtLeast(baseFare)
    }

    fun estimateDistance(pickup: String, dropoff: String): Double {
        val p = pickup.trim().lowercase()
        val d = dropoff.trim().lowercase()
        if (p.isEmpty() || d.isEmpty()) return 3.0
        val hash = (p.hashCode() + d.hashCode()).let { if (it < 0) -it else it }
        val distance = (hash % 15) + 2.5
        return (distance * 10).toInt() / 10.0
    }
}
