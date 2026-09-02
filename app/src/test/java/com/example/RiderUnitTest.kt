package com.example

import com.example.util.DistanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiderUnitTest {

    @Test
    fun testFareCalculation() {
        val fare = DistanceCalculator.calculateFare("Mall Road", "Airport", baseFare = 100.0, perKmRate = 30.0)
        assertTrue(fare >= 100.0)
    }

    @Test
    fun testDistanceEstimation() {
        val distance = DistanceCalculator.estimateDistance("Gulberg", "DHA")
        assertTrue(distance > 0.0)
    }

    @Test
    fun testEmptyLocationDefaults() {
        val distance = DistanceCalculator.estimateDistance("", "")
        assertEquals(3.0, distance, 0.01)
    }
}
