package com.example.util

import kotlin.math.abs

object DistanceCalculator {

    /**
     * Calculates estimated distance in KM between origin and destination strings.
     * Uses location keyword rules and deterministic fallback calculation for any input.
     */
    fun estimateDistanceKm(from: String, to: String): Double {
        val src = from.trim().lowercase()
        val dest = to.trim().lowercase()

        if (src.isEmpty() || dest.isEmpty()) return 0.0
        if (src == dest) return 1.5

        // Known location pairs estimate
        val knownKm = findKnownPairKm(src, dest)
        if (knownKm != null) return knownKm

        // Deterministic fallback based on character hash code & length diff for consistent KM estimation
        val hashDiff = abs((src.hashCode() xor dest.hashCode()) % 18) + 3
        val lenBonus = (src.length + dest.length) % 5
        val estimated = (hashDiff + lenBonus * 0.8)
        return String.format("%.1f", estimated.coerceIn(2.0, 35.0)).toDouble()
    }

    private fun findKnownPairKm(src: String, dest: String): Double? {
        val pairs = mapOf(
            setOf("airport", "saddar") to 16.5,
            setOf("gulberg", "dha") to 8.2,
            setOf("johar", "tariq road") to 9.5,
            setOf("station", "bazar") to 4.2,
            setOf("cantt", "clifton") to 7.8,
            setOf("f-7", "blue area") to 3.5,
            setOf("i-8", "f-6") to 6.2,
            setOf("scheme 3", "sadar") to 5.0,
            setOf("model town", "barkat market") to 3.8
        )

        for ((key, km) in pairs) {
            if (key.any { src.contains(it) } && key.any { dest.contains(it) }) {
                return km
            }
        }
        return null
    }
}
