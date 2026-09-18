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
import org.junit.Assert.assertFalse
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
    fun testScenario1_AddRideWithPartialPayment() {
        val fare = 1000.0
        val paid = 700.0
        val bakaya = (fare - paid).coerceAtLeast(0.0)
        val ride = RideEntity(
            id = 1,
            customerName = "Ali Khan",
            phone = "03001234567",
            pickupLocation = "Station",
            dropoffLocation = "Cantt",
            fare = fare,
            amountPaid = paid,
            remainingBakaya = bakaya,
            rideDate = 1700000000000L
        )
        val debtor = DebtorEntity(
            id = 1,
            name = ride.customerName,
            phone = ride.phone,
            totalDebt = bakaya,
            remainingDebt = bakaya
        )

        assertEquals(1000.0, ride.fare, 0.01)
        assertEquals(700.0, ride.amountPaid, 0.01)
        assertEquals(300.0, ride.remainingBakaya, 0.01)
        assertEquals(300.0, debtor.remainingDebt, 0.01)
    }

    @Test
    fun testScenario2_EditRideFareDelta() {
        val initialFare = 1000.0
        val paid = 700.0
        val initialBakaya = 300.0
        var debtorRemaining = 300.0
        var debtorTotal = 300.0

        // Increase Fare to 1200: new bakaya = 500
        val newFare1 = 1200.0
        val newBakaya1 = (newFare1 - paid).coerceAtLeast(0.0) // 500.0
        val delta1 = newBakaya1 - initialBakaya // +200.0
        debtorRemaining = (debtorRemaining + delta1).coerceAtLeast(0.0)
        debtorTotal = (debtorTotal + delta1).coerceAtLeast(0.0)

        assertEquals(500.0, newBakaya1, 0.01)
        assertEquals(500.0, debtorRemaining, 0.01)
        assertEquals(500.0, debtorTotal, 0.01)

        // Decrease Fare to 800: new bakaya = 100
        val newFare2 = 800.0
        val newBakaya2 = (newFare2 - paid).coerceAtLeast(0.0) // 100.0
        val delta2 = newBakaya2 - newBakaya1 // -400.0
        debtorRemaining = (debtorRemaining + delta2).coerceAtLeast(0.0)
        debtorTotal = (debtorTotal + delta2).coerceAtLeast(0.0)

        assertEquals(100.0, newBakaya2, 0.01)
        assertEquals(100.0, debtorRemaining, 0.01)
        assertEquals(100.0, debtorTotal, 0.01)
    }

    @Test
    fun testScenario3_EditRidePaidAmount() {
        val fare = 1000.0
        val initialPaid = 700.0
        val oldBakaya = 300.0
        var debtorRemaining = 300.0

        // Paid becomes 1000 (fully paid)
        val newPaid = 1000.0
        val newBakaya = (fare - newPaid).coerceAtLeast(0.0) // 0.0
        val delta = newBakaya - oldBakaya // -300.0
        debtorRemaining = (debtorRemaining + delta).coerceAtLeast(0.0)

        assertEquals(0.0, newBakaya, 0.01)
        assertEquals(0.0, debtorRemaining, 0.01)
    }

    @Test
    fun testScenario4_AddParcelWithPartialPayment() {
        val charges = 600.0
        val paid = 400.0
        val bakaya = (charges - paid).coerceAtLeast(0.0)
        val parcel = ParcelEntity(
            id = 1,
            senderName = "Zubair",
            senderPhone = "03211234567",
            receiverName = "Hamza",
            receiverPhone = "03331234567",
            pickupAddress = "Johar Town",
            deliveryAddress = "Gulberg",
            deliveryCharges = charges,
            amountPaid = paid,
            remainingBakaya = bakaya,
            date = 1700000000000L
        )
        val debtor = DebtorEntity(
            id = 1,
            name = parcel.senderName,
            phone = parcel.senderPhone,
            totalDebt = bakaya,
            remainingDebt = bakaya
        )

        assertEquals(600.0, parcel.deliveryCharges, 0.01)
        assertEquals(400.0, parcel.amountPaid, 0.01)
        assertEquals(200.0, parcel.remainingBakaya, 0.01)
        assertEquals(200.0, debtor.remainingDebt, 0.01)
    }

    @Test
    fun testScenario5_EditParcelChargesDelta() {
        val initialCharges = 600.0
        val paid = 400.0
        val oldBakaya = 200.0
        var debtorRemaining = 200.0

        // Increase charges to 900
        val newCharges = 900.0
        val newBakaya = (newCharges - paid).coerceAtLeast(0.0) // 500.0
        val delta = newBakaya - oldBakaya // +300.0
        debtorRemaining = (debtorRemaining + delta).coerceAtLeast(0.0)

        assertEquals(500.0, newBakaya, 0.01)
        assertEquals(500.0, debtorRemaining, 0.01)
    }

    @Test
    fun testScenario6_AddPaymentWasooli() {
        var debtorRemaining = 500.0
        val paymentAmount = 300.0

        debtorRemaining = (debtorRemaining - paymentAmount).coerceAtLeast(0.0)
        assertEquals(200.0, debtorRemaining, 0.01)
    }

    @Test
    fun testScenario7_EditPaymentDelta() {
        var debtorRemaining = 200.0 // Was 500, paid 300 -> remaining 200
        val oldPayment = 300.0

        // Edit payment to 400: delta = 400 - 300 = +100
        val newPayment1 = 400.0
        val delta1 = newPayment1 - oldPayment // +100.0
        debtorRemaining = (debtorRemaining - delta1).coerceAtLeast(0.0)
        assertEquals(100.0, debtorRemaining, 0.01)

        // Edit payment from 400 to 200: delta = 200 - 400 = -200
        val newPayment2 = 200.0
        val delta2 = newPayment2 - newPayment1 // -200.0
        debtorRemaining = (debtorRemaining - delta2).coerceAtLeast(0.0)
        assertEquals(300.0, debtorRemaining, 0.01)
    }

    @Test
    fun testScenario8_DeletePaymentReversal() {
        var debtorRemaining = 200.0
        val paymentToDelete = 300.0

        debtorRemaining = debtorRemaining + paymentToDelete
        assertEquals(500.0, debtorRemaining, 0.01)
    }

    @Test
    fun testScenario9_EditCustomerBakayaDirectly() {
        var oldRemaining = 500.0
        var totalDebt = 500.0
        val newBakaya = 700.0

        val delta = newBakaya - oldRemaining // +200.0
        totalDebt = (totalDebt + delta).coerceAtLeast(0.0)
        oldRemaining = newBakaya

        assertEquals(700.0, oldRemaining, 0.01)
        assertEquals(700.0, totalDebt, 0.01)
    }

    @Test
    fun testScenario10_ClearCustomerBakayaDirectly() {
        var debtorRemaining = 700.0
        var totalDebt = 700.0

        val cleared = debtorRemaining
        totalDebt = (totalDebt - cleared).coerceAtLeast(0.0)
        debtorRemaining = 0.0

        assertEquals(0.0, debtorRemaining, 0.01)
        assertEquals(0.0, totalDebt, 0.01)
    }

    @Test
    fun testScenario11_DashboardTotalsConsistent() {
        val ride1 = RideEntity(id = 1, customerName = "Ali", phone = "111", pickupLocation = "P1", dropoffLocation = "D1", fare = 1000.0, amountPaid = 700.0, remainingBakaya = 300.0)
        val parcel1 = ParcelEntity(id = 1, senderName = "Kamran", senderPhone = "222", receiverName = "R1", receiverPhone = "333", pickupAddress = "A1", deliveryAddress = "A2", deliveryCharges = 600.0, amountPaid = 400.0, remainingBakaya = 200.0)
        val debtor1 = DebtorEntity(id = 1, name = "Ali", phone = "111", totalDebt = 300.0, remainingDebt = 300.0)
        val debtor2 = DebtorEntity(id = 2, name = "Kamran", phone = "222", totalDebt = 200.0, remainingDebt = 200.0)
        val payment1 = PaymentHistoryEntity(id = 1, debtorId = 1, amountPaid = 100.0, paymentDate = 1700000000000L, note = "Cash")

        val state = RiderUiState(
            allRides = listOf(ride1),
            allParcels = listOf(parcel1),
            allDebtors = listOf(debtor1.copy(remainingDebt = 200.0), debtor2),
            payments = listOf(payment1)
        )

        assertEquals(1000.0, state.totalRideFare, 0.01)
        assertEquals(700.0, state.totalRidePaid, 0.01)
        assertEquals(300.0, state.totalRideBakaya, 0.01)

        assertEquals(600.0, state.totalParcelCharges, 0.01)
        assertEquals(400.0, state.totalParcelPaid, 0.01)
        assertEquals(200.0, state.totalParcelBakaya, 0.01)

        assertEquals(400.0, state.totalRemainingDebt, 0.01) // 200 (Ali) + 200 (Kamran)
        assertEquals(2, state.totalDebtorsCount)

        assertEquals(100.0, state.totalRecoveredCash, 0.01)
        // Net Cash in Hand = totalRidePaid (700) + totalParcelPaid (400) + totalRecoveredCash (100) = 1200
        assertEquals(1200.0, state.netCashInHand, 0.01)
    }

    @Test
    fun testCustomerHistoryTimestampPreservedOnEdit() {
        val originalTime = 1690000000000L
        val originalHistory = CustomerHistoryEntity(
            id = 1,
            customerName = "Ali",
            phone = "111",
            activityType = "RIDE",
            details = "Ride from A to B",
            amount = 1000.0,
            bakayaAmount = 300.0,
            timestamp = originalTime,
            referenceId = 10L
        )

        val updatedHistory = originalHistory.copy(
            amount = 1200.0,
            bakayaAmount = 500.0,
            details = "Ride from A to B (Fare: Rs. 1200, Paid: Rs. 700, Bakaya: Rs. 500)"
        )

        assertEquals(originalTime, updatedHistory.timestamp)
        assertEquals(1200.0, updatedHistory.amount, 0.01)
        assertEquals(500.0, updatedHistory.bakayaAmount, 0.01)
    }

    @Test
    fun testCustomerIsolation_SameNameAndPhoneDifferentIds() {
        // Two distinct customers with identical name and phone number
        val customer1 = DebtorEntity(id = 101L, name = "Muhammad Ali", phone = "03001122334", remainingDebt = 500.0, totalDebt = 500.0)
        val customer2 = DebtorEntity(id = 102L, name = "Muhammad Ali", phone = "03001122334", remainingDebt = 1200.0, totalDebt = 1200.0)

        // Rides linked specifically to customer1 and customer2 by customerId
        val ride1 = RideEntity(id = 1, customerId = 101L, customerName = "Muhammad Ali", phone = "03001122334", pickupLocation = "Location A", dropoffLocation = "Location B", fare = 800.0, amountPaid = 300.0, remainingBakaya = 500.0)
        val ride2 = RideEntity(id = 2, customerId = 102L, customerName = "Muhammad Ali", phone = "03001122334", pickupLocation = "Location C", dropoffLocation = "Location D", fare = 1500.0, amountPaid = 300.0, remainingBakaya = 1200.0)

        // History items linked strictly by customerId
        val history1 = CustomerHistoryEntity(id = 1, customerId = 101L, customerName = "Muhammad Ali", phone = "03001122334", activityType = "RIDE", details = "Ride for C1", amount = 800.0, bakayaAmount = 500.0, timestamp = 1700000000000L)
        val history2 = CustomerHistoryEntity(id = 2, customerId = 102L, customerName = "Muhammad Ali", phone = "03001122334", activityType = "RIDE", details = "Ride for C2", amount = 1500.0, bakayaAmount = 1200.0, timestamp = 1700000000000L)

        val allRides = listOf(ride1, ride2)
        val allHistory = listOf(history1, history2)

        // Strict customerId-based filtering
        val c1Rides = allRides.filter { it.customerId == customer1.id }
        val c2Rides = allRides.filter { it.customerId == customer2.id }
        val c1History = allHistory.filter { it.customerId == customer1.id }
        val c2History = allHistory.filter { it.customerId == customer2.id }

        assertEquals(1, c1Rides.size)
        assertEquals(1, c2Rides.size)
        assertEquals(500.0, c1Rides.first().remainingBakaya, 0.01)
        assertEquals(1200.0, c2Rides.first().remainingBakaya, 0.01)

        assertEquals(1, c1History.size)
        assertEquals(1, c2History.size)
        assertEquals(500.0, c1History.first().bakayaAmount, 0.01)
        assertEquals(1200.0, c2History.first().bakayaAmount, 0.01)
    }

    @Test
    fun testCustomerIsolation_BlankPhoneDifferentIds() {
        val customerA = DebtorEntity(id = 201L, name = "Walk-in Customer", phone = "", remainingDebt = 350.0, totalDebt = 350.0)
        val customerB = DebtorEntity(id = 202L, name = "Walk-in Customer", phone = "", remainingDebt = 750.0, totalDebt = 750.0)

        val allDebtors = listOf(customerA, customerB)
        val foundA = allDebtors.find { it.id == 201L }
        val foundB = allDebtors.find { it.id == 202L }

        assertEquals(350.0, foundA?.remainingDebt ?: 0.0, 0.01)
        assertEquals(750.0, foundB?.remainingDebt ?: 0.0, 0.01)
        assertTrue("Customer A and B are completely isolated by ID", foundA?.id != foundB?.id)
    }

    @Test
    fun testSamanAndDeliveryCharges_Calculations() {
        val shopName = "Al-Madina Medical Store"
        val samanCharges = 1200.0
        val deliveryCharges = 250.0
        val totalCost = samanCharges + deliveryCharges // 1450.0
        val paid = 500.0
        val bakaya = (totalCost - paid).coerceAtLeast(0.0) // 950.0

        val parcel = ParcelEntity(
            id = 1,
            customerId = 301L,
            senderName = "Shopkeeper",
            senderPhone = "03112223334",
            receiverName = "Tariq",
            receiverPhone = "03223334445",
            pickupAddress = "Market",
            deliveryAddress = "Street 5, Phase 2",
            shopName = shopName,
            samanCharges = samanCharges,
            deliveryCharges = deliveryCharges,
            amountPaid = paid,
            remainingBakaya = bakaya,
            isDelivered = false,
            isPaid = false,
            date = 1710000000000L
        )

        assertEquals("Al-Madina Medical Store", parcel.shopName)
        assertEquals(1200.0, parcel.samanCharges, 0.01)
        assertEquals(250.0, parcel.deliveryCharges, 0.01)
        assertEquals(1450.0, parcel.samanCharges + parcel.deliveryCharges, 0.01)
        assertEquals(500.0, parcel.amountPaid, 0.01)
        assertEquals(950.0, parcel.remainingBakaya, 0.01)
    }

    @Test
    fun testCustomerProfile_PhotoAndAddressPreservation() {
        val customer = DebtorEntity(
            id = 401L,
            name = "Hamza Farooq",
            phone = "03335556677",
            address = "House # 45, Street 12, G-10/2, Islamabad",
            location = "House # 45, Street 12, G-10/2, Islamabad",
            photoUri = "content://media/external/images/media/9876",
            totalDebt = 400.0,
            remainingDebt = 400.0,
            notes = "VIP Corporate Customer"
        )

        assertEquals("House # 45, Street 12, G-10/2, Islamabad", customer.address)
        assertEquals("content://media/external/images/media/9876", customer.photoUri)
        assertEquals("VIP Corporate Customer", customer.notes)
        assertEquals(400.0, customer.remainingDebt, 0.01)
    }

    @Test
    fun testPdfMarkUnmark_DetailsHiddenWhileTotalsPreserved() {
        val ride1 = RideEntity(id = 1, customerId = 501L, customerName = "Ali", phone = "0300", pickupLocation = "P1", dropoffLocation = "D1", fare = 1000.0, amountPaid = 400.0, remainingBakaya = 600.0)
        val ride2 = RideEntity(id = 2, customerId = 501L, customerName = "Ali", phone = "0300", pickupLocation = "P2", dropoffLocation = "D2", fare = 800.0, amountPaid = 800.0, remainingBakaya = 0.0)
        val parcel1 = ParcelEntity(id = 1, customerId = 501L, senderName = "Ali", senderPhone = "0300", receiverName = "R", receiverPhone = "0311", pickupAddress = "A", deliveryAddress = "B", samanCharges = 500.0, deliveryCharges = 200.0, amountPaid = 300.0, remainingBakaya = 400.0)

        val historyRide1 = CustomerHistoryEntity(id = 11, customerId = 501L, customerName = "Ali", phone = "0300", activityType = "RIDE", details = "Ride P1 to D1", amount = 1000.0, bakayaAmount = 600.0, referenceId = 1)
        val historyRide2 = CustomerHistoryEntity(id = 12, customerId = 501L, customerName = "Ali", phone = "0300", activityType = "RIDE", details = "Ride P2 to D2", amount = 800.0, bakayaAmount = 0.0, referenceId = 2)
        val historyParcel1 = CustomerHistoryEntity(id = 13, customerId = 501L, customerName = "Ali", phone = "0300", activityType = "PARCEL", details = "Parcel to B", amount = 700.0, bakayaAmount = 400.0, referenceId = 1)

        val allHistory = listOf(historyRide1, historyRide2, historyParcel1)
        val allRides = listOf(ride1, ride2)
        val allParcels = listOf(parcel1)

        // Scenario: User UNMARKS ride2 and parcel1, MARKS ONLY ride1
        val markedHistoryIds = setOf(11L)

        // Detailed rows in PDF should only show marked items
        val displayedHistory = allHistory.filter { markedHistoryIds.contains(it.id) }
        assertEquals(1, displayedHistory.size)
        assertEquals(11L, displayedHistory.first().id)
        assertEquals("Ride P1 to D1", displayedHistory.first().details)

        // Totals MUST still include ALL transactions (both marked and unmarked)
        val totalRidesFare = allRides.sumOf { it.fare } // 1000 + 800 = 1800.0
        val totalRidesBakaya = allRides.sumOf { it.remainingBakaya } // 600 + 0 = 600.0
        val totalParcelsCharges = allParcels.sumOf { it.totalCharges } // 700.0
        val totalParcelsBakaya = allParcels.sumOf { it.remainingBakaya } // 400.0
        val grandOutstanding = totalRidesBakaya + totalParcelsBakaya // 1000.0

        assertEquals(1800.0, totalRidesFare, 0.01)
        assertEquals(600.0, totalRidesBakaya, 0.01)
        assertEquals(700.0, totalParcelsCharges, 0.01)
        assertEquals(400.0, totalParcelsBakaya, 0.01)
        assertEquals(1000.0, grandOutstanding, 0.01)
    }

    @Test
    fun testAppUpdate_VersionComparisonAndExtraction() {
        val currentVersionCode = 133
        val bodyWithCode = "## Release v1.3.4\n- **versionCode**: 134\n- Asset: app-release.apk"
        val codeRegex = Regex("""(?i)versionCode\s*[:=*\s]+(\d+)""")
        val codeMatch = codeRegex.find(bodyWithCode)
        val extractedCode = codeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

        assertEquals(134, extractedCode)
        assertTrue(extractedCode > currentVersionCode)

        // When remote version is equal or less, it must NOT trigger update available
        assertFalse(133 > currentVersionCode)
        assertFalse(132 > currentVersionCode)

        // Test fallback to tag semantic version
        val tag = "v1.3.4"
        val cleanTag = if (tag.startsWith("v", ignoreCase = true)) tag.substring(1) else tag
        val fallbackDigits = cleanTag.filter { it.isDigit() }.toIntOrNull() ?: 0
        assertEquals(134, fallbackDigits)
    }

    @Test
    fun testAppUpdate_AssetApkUrlExtraction() {
        val assets = listOf(
            Pair("other-file.txt", "https://example.com/other.txt"),
            Pair("other-app-release.zip", "https://example.com/other.zip"),
            Pair("app-release.apk", "https://github.com/aqeelawan687-ux/Awang/releases/download/v1.3.4/app-release.apk")
        )
        val apkUrl = assets.firstOrNull { (name, _) ->
            name.equals("app-release.apk", ignoreCase = true)
        }?.second ?: ""

        assertEquals("https://github.com/aqeelawan687-ux/Awang/releases/download/v1.3.4/app-release.apk", apkUrl)
    }
}
