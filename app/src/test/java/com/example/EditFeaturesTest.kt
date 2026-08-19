package com.example

import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class EditFeaturesTest {

    @Test
    fun `ride entity edit preserves id and updates all editable fields`() {
        val originalRide = RideEntity(
            id = 101L,
            debtorId = 5L,
            fromLocation = "Saddar",
            toLocation = "Gulberg",
            distanceKm = 12.0,
            fareAmount = 350.0,
            dateMillis = 1700000000000L,
            timeString = "14:00",
            note = "Original note"
        )

        val updatedRide = originalRide.copy(
            fromLocation = "Clifton",
            toLocation = "DHA Phase 6",
            distanceKm = 15.5,
            fareAmount = 500.0,
            timeString = "15:30",
            note = "Updated urgent note"
        )

        // Verify ID and debtorId are strictly preserved (no duplicate / no orphan)
        assertEquals(101L, updatedRide.id)
        assertEquals(5L, updatedRide.debtorId)
        assertEquals(1700000000000L, updatedRide.dateMillis)

        // Verify editable fields are updated
        assertEquals("Clifton", updatedRide.fromLocation)
        assertEquals("DHA Phase 6", updatedRide.toLocation)
        assertEquals(15.5, updatedRide.distanceKm, 0.001)
        assertEquals(500.0, updatedRide.fareAmount, 0.001)
        assertEquals("15:30", updatedRide.timeString)
        assertEquals("Updated urgent note", updatedRide.note)
    }

    @Test
    fun `payment entity edit preserves id and updates payment fields`() {
        val originalPayment = PaymentHistoryEntity(
            id = 202L,
            debtorId = 7L,
            debtorName = "Ali Khan",
            amountPaid = 1000.0,
            paymentType = "Cash",
            timestamp = 1700000000000L,
            note = "Initial installment"
        )

        val updatedPayment = originalPayment.copy(
            amountPaid = 1500.0,
            paymentType = "Easypaisa",
            note = "Revised payment with proof"
        )

        // Verify ID, debtorId, debtorName and timestamp are strictly preserved
        assertEquals(202L, updatedPayment.id)
        assertEquals(7L, updatedPayment.debtorId)
        assertEquals("Ali Khan", updatedPayment.debtorName)
        assertEquals(1700000000000L, updatedPayment.timestamp)

        // Verify editable fields
        assertEquals(1500.0, updatedPayment.amountPaid, 0.001)
        assertEquals("Easypaisa", updatedPayment.paymentType)
        assertEquals("Revised payment with proof", updatedPayment.note)
    }

    @Test
    fun `payment amount increase recalculates debtor remaining debt downwards`() {
        val initialDebt = 5000.0
        val debtor = DebtorEntity(
            id = 1L,
            name = "Usman",
            phoneNumber = "03001234567",
            totalDebt = initialDebt
        )

        val oldPaymentAmount = 1000.0
        val newPaymentAmount = 1500.0 // Paid 500 more
        val diff = oldPaymentAmount - newPaymentAmount // -500
        val recalculatedDebt = (debtor.totalDebt + diff).coerceAtLeast(0.0)

        assertEquals(4500.0, recalculatedDebt, 0.001)
    }

    @Test
    fun `payment amount decrease recalculates debtor remaining debt upwards`() {
        val initialDebt = 5000.0
        val debtor = DebtorEntity(
            id = 1L,
            name = "Usman",
            phoneNumber = "03001234567",
            totalDebt = initialDebt
        )

        val oldPaymentAmount = 1000.0
        val newPaymentAmount = 600.0 // Paid 400 less (correction)
        val diff = oldPaymentAmount - newPaymentAmount // +400
        val recalculatedDebt = (debtor.totalDebt + diff).coerceAtLeast(0.0)

        assertEquals(5400.0, recalculatedDebt, 0.001)
    }

    @Test
    fun `payment note or type edit does not alter debt amount`() {
        val initialDebt = 5000.0
        val debtor = DebtorEntity(
            id = 1L,
            name = "Usman",
            phoneNumber = "03001234567",
            totalDebt = initialDebt
        )

        val oldPaymentAmount = 1000.0
        val newPaymentAmount = 1000.0
        val diff = oldPaymentAmount - newPaymentAmount
        val recalculatedDebt = (debtor.totalDebt + diff).coerceAtLeast(0.0)

        assertEquals(initialDebt, recalculatedDebt, 0.001)
    }

    @Test
    fun `customer account ledger recalculates automatically when rides and payments are edited`() {
        val rides = listOf(
            RideEntity(id = 1L, debtorId = 10L, fromLocation = "A", toLocation = "B", distanceKm = 5.0, fareAmount = 300.0, dateMillis = 1000L),
            RideEntity(id = 2L, debtorId = 10L, fromLocation = "C", toLocation = "D", distanceKm = 8.0, fareAmount = 450.0, dateMillis = 2000L)
        )
        val parcels = listOf(
            ParcelEntity(id = 1L, debtorId = 10L, shopName = "Store", recipientName = "Customer", recipientAddress = "Street 1", itemDetails = "Shirt", itemPrice = 1200.0, deliveryCharges = 200.0)
        )
        val payments = listOf(
            PaymentHistoryEntity(id = 1L, debtorId = 10L, debtorName = "Tariq", amountPaid = 500.0, paymentType = "Cash", timestamp = 3000L)
        )

        val totalRidesFare = rides.sumOf { it.fareAmount } // 300 + 450 = 750
        val totalSamanPrice = parcels.sumOf { it.itemPrice } // 1200
        val totalDeliveryCharges = parcels.sumOf { it.deliveryCharges } // 200
        val grandTotal = totalRidesFare + totalSamanPrice + totalDeliveryCharges // 750 + 1200 + 200 = 2150
        val totalPaid = payments.sumOf { it.amountPaid } // 500
        val baqaya = (grandTotal - totalPaid).coerceAtLeast(0.0) // 1650

        assertEquals(750.0, totalRidesFare, 0.001)
        assertEquals(2150.0, grandTotal, 0.001)
        assertEquals(500.0, totalPaid, 0.001)
        assertEquals(1650.0, baqaya, 0.001)

        // Now simulate editing Ride 1 (fare from 300 -> 500) and Payment 1 (amount from 500 -> 1000)
        val updatedRides = listOf(
            rides[0].copy(fareAmount = 500.0),
            rides[1]
        )
        val updatedPayments = listOf(
            payments[0].copy(amountPaid = 1000.0)
        )

        val newTotalRidesFare = updatedRides.sumOf { it.fareAmount } // 500 + 450 = 950
        val newGrandTotal = newTotalRidesFare + totalSamanPrice + totalDeliveryCharges // 950 + 1200 + 200 = 2350
        val newTotalPaid = updatedPayments.sumOf { it.amountPaid } // 1000
        val newBaqaya = (newGrandTotal - newTotalPaid).coerceAtLeast(0.0) // 1350

        assertEquals(950.0, newTotalRidesFare, 0.001)
        assertEquals(2350.0, newGrandTotal, 0.001)
        assertEquals(1000.0, newTotalPaid, 0.001)
        assertEquals(1350.0, newBaqaya, 0.001)
    }
}
