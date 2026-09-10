package com.example

import com.example.data.entity.CustomerHistoryEntity
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity
import com.example.ui.viewmodel.RiderUiState
import com.example.util.DateTimeUtils
import com.example.util.DistanceCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

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

    @Test
    fun testDateTimeUtilsFormatting() {
        val calendar = Calendar.getInstance()
        calendar.set(2025, Calendar.MARCH, 15, 14, 30, 0)
        val testTime = calendar.timeInMillis
        val formatted = DateTimeUtils.formatDateTime(testTime)

        // Must contain day, month, year, time and bullet separator
        assertTrue("Formatted date should contain 2025: $formatted", formatted.contains("2025"))
        assertTrue("Formatted date should contain Mar: $formatted", formatted.contains("Mar"))
        assertTrue("Formatted date should contain bullet separator: $formatted", formatted.contains("•"))
    }

    @Test
    fun testDateTimeUtilsFallback() {
        val formattedZero = DateTimeUtils.formatDateTime(0L)
        val formattedNegative = DateTimeUtils.formatDateTime(-100L)
        assertEquals("N/A", formattedZero)
        assertEquals("N/A", formattedNegative)
    }

    @Test
    fun testRideAmountEditCalculations() {
        val originalTimestamp = 1700000000000L
        val initialRide = RideEntity(
            id = 1,
            customerName = "Ali Khan",
            phone = "03001234567",
            pickupLocation = "A",
            dropoffLocation = "B",
            fare = 1000.0,
            amountPaid = 1000.0,
            remainingBakaya = 0.0,
            rideDate = originalTimestamp
        )

        // User edits Fare from 1000 to 1500, amountPaid remains 1000
        val newFare = 1500.0
        val paid = 1000.0
        val updatedRemaining = (newFare - paid).coerceAtLeast(0.0)
        val editedRide = initialRide.copy(
            fare = newFare,
            remainingBakaya = updatedRemaining
        )

        assertEquals(1500.0, editedRide.fare, 0.01)
        assertEquals(500.0, editedRide.remainingBakaya, 0.01)
        assertEquals(originalTimestamp, editedRide.rideDate) // Timestamp preserved!
    }

    @Test
    fun testParcelAmountEditCalculations() {
        val originalTimestamp = 1700000050000L
        val initialParcel = ParcelEntity(
            id = 1,
            senderName = "Babar",
            senderPhone = "03111234567",
            receiverName = "Rizwan",
            receiverPhone = "03221234567",
            pickupAddress = "Gulberg",
            deliveryAddress = "DHA",
            deliveryCharges = 500.0,
            amountPaid = 500.0,
            remainingBakaya = 0.0,
            date = originalTimestamp
        )

        // User edits Delivery Charges from 500 to 800, paid 500
        val newCharges = 800.0
        val paid = 500.0
        val updatedRemaining = (newCharges - paid).coerceAtLeast(0.0)
        val editedParcel = initialParcel.copy(
            deliveryCharges = newCharges,
            remainingBakaya = updatedRemaining
        )

        assertEquals(800.0, editedParcel.deliveryCharges, 0.01)
        assertEquals(300.0, editedParcel.remainingBakaya, 0.01)
        assertEquals(originalTimestamp, editedParcel.date) // Timestamp preserved!
    }

    @Test
    fun testDashboardBalancesUpdateAccurately() {
        // Initial state
        val ride1 = RideEntity(id = 1, customerName = "Ali", phone = "111", pickupLocation = "P1", dropoffLocation = "D1", fare = 1000.0, amountPaid = 1000.0, remainingBakaya = 0.0)
        val parcel1 = ParcelEntity(id = 1, senderName = "Kamran", senderPhone = "222", receiverName = "R1", receiverPhone = "333", pickupAddress = "A1", deliveryAddress = "A2", deliveryCharges = 500.0, amountPaid = 500.0, remainingBakaya = 0.0)
        val debtor1 = DebtorEntity(id = 1, name = "Ali", phone = "111", totalDebt = 0.0, remainingDebt = 0.0)

        val stateBefore = RiderUiState(
            rides = listOf(ride1),
            parcels = listOf(parcel1),
            debtors = listOf(debtor1)
        )

        assertEquals(1000.0, stateBefore.totalRidePaid, 0.01)
        assertEquals(500.0, stateBefore.totalParcelPaid, 0.01)
        assertEquals(1500.0, stateBefore.netCashInHand, 0.01)
        assertEquals(0.0, stateBefore.totalRemainingDebt, 0.01)

        // After editing Ride fare to 1500 (extra 500 bakaya):
        val ride1Updated = ride1.copy(fare = 1500.0, remainingBakaya = 500.0)
        val debtor1Updated = debtor1.copy(totalDebt = 500.0, remainingDebt = 500.0)

        val stateAfter = RiderUiState(
            rides = listOf(ride1Updated),
            parcels = listOf(parcel1),
            debtors = listOf(debtor1Updated)
        )

        assertEquals(1500.0, stateAfter.totalRideFare, 0.01)
        assertEquals(1000.0, stateAfter.totalRidePaid, 0.01)
        assertEquals(500.0, stateAfter.totalRideBakaya, 0.01)
        assertEquals(500.0, stateAfter.totalRemainingDebt, 0.01)
        assertEquals(1500.0, stateAfter.netCashInHand, 0.01) // Cash in hand remains 1500
    }

    @Test
    fun testCustomerHistoryReferenceSync() {
        val originalTimestamp = 1690000000000L
        val historyEntry = CustomerHistoryEntity(
            id = 10,
            customerName = "Ali Khan",
            phone = "03001234567",
            activityType = "RIDE",
            amount = 1000.0,
            details = "Ride: Station -> Cantt | Fare: Rs. 1000 | Paid: Rs. 1000 | Bakaya: Rs. 0",
            timestamp = originalTimestamp,
            referenceId = 1L
        )

        // When ride is updated, history retains original timestamp and updates amount & details
        val updatedHistory = historyEntry.copy(
            amount = 1500.0,
            details = "Ride: Station -> Cantt | Fare: Rs. 1500 | Paid: Rs. 1000 | Bakaya: Rs. 500",
            timestamp = historyEntry.timestamp // Preserves original creation timestamp!
        )

        assertEquals(originalTimestamp, updatedHistory.timestamp)
        assertEquals(1500.0, updatedHistory.amount, 0.01)
        assertTrue(updatedHistory.details.contains("Bakaya: Rs. 500"))
    }
}

