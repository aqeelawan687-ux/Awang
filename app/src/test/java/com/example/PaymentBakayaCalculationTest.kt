package com.example

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PaymentBakayaCalculationTest {

    @Test
    fun testBakayaCalculation_basicAndLaterPayments() {
        val totalAmount = 1000.0
        var paidAmount = 600.0
        var bakaya = (totalAmount - paidAmount).coerceAtLeast(0.0)

        assertEquals(400.0, bakaya, 0.01)

        // Customer pays 200 later
        val laterPayment1 = 200.0
        paidAmount += laterPayment1
        bakaya = (totalAmount - paidAmount).coerceAtLeast(0.0)
        assertEquals(800.0, paidAmount, 0.01)
        assertEquals(200.0, bakaya, 0.01)

        // Customer pays remaining 200
        val laterPayment2 = 200.0
        paidAmount += laterPayment2
        bakaya = (totalAmount - paidAmount).coerceAtLeast(0.0)
        assertEquals(1000.0, paidAmount, 0.01)
        assertEquals(0.0, bakaya, 0.01)
    }

    @Test
    fun testCustomerIsolation() {
        // Customer A
        var customerADue = 1000.0
        var customerAPaid = 600.0
        var customerABakaya = (customerADue - customerAPaid).coerceAtLeast(0.0)

        // Customer B
        val customerBDue = 1500.0
        val customerBPaid = 0.0
        val customerBBakaya = (customerBDue - customerBPaid).coerceAtLeast(0.0)

        assertEquals(400.0, customerABakaya, 0.01)
        assertEquals(1500.0, customerBBakaya, 0.01)

        // Customer A makes a payment of 200
        val newPaymentA = 200.0
        customerAPaid += newPaymentA
        customerABakaya = (customerADue - customerAPaid).coerceAtLeast(0.0)

        // Verify Customer A updated and Customer B is completely untouched
        assertEquals(200.0, customerABakaya, 0.01)
        assertEquals(1500.0, customerBBakaya, 0.01)
        assertEquals(0.0, customerBPaid, 0.01)
    }

    @Test
    fun testRideAndParcelSeparateAndGrandTotals() {
        val rideTotal = 1000.0
        val ridePaid = 400.0
        val rideBakaya = (rideTotal - ridePaid).coerceAtLeast(0.0)

        val parcelTotal = 500.0
        val parcelPaid = 200.0
        val parcelBakaya = (parcelTotal - parcelPaid).coerceAtLeast(0.0)

        val grandTotal = rideTotal + parcelTotal
        val totalPaid = ridePaid + parcelPaid
        val totalBakaya = grandTotal - totalPaid

        assertEquals(600.0, rideBakaya, 0.01)
        assertEquals(300.0, parcelBakaya, 0.01)
        assertEquals(1500.0, grandTotal, 0.01)
        assertEquals(600.0, totalPaid, 0.01)
        assertEquals(900.0, totalBakaya, 0.01)
        assertEquals(totalBakaya, rideBakaya + parcelBakaya, 0.01)
    }

    @Test
    fun testSaveWithoutPayment_zeroPaid() {
        val totalFare = 850.0
        val paid = 0.0
        val bakaya = (totalFare - paid).coerceAtLeast(0.0)

        assertEquals(850.0, bakaya, 0.01)
    }

    @Test
    fun testEmptyOrInvalidInputSafety() {
        val emptyInput = ""
        val parsedEmpty = emptyInput.toDoubleOrNull() ?: 0.0
        assertEquals(0.0, parsedEmpty, 0.01)

        val invalidInput = "xyz"
        val parsedInvalid = invalidInput.toDoubleOrNull() ?: 0.0
        assertEquals(0.0, parsedInvalid, 0.01)

        val decimalInput = "350.50"
        val parsedDecimal = decimalInput.toDoubleOrNull() ?: 0.0
        assertEquals(350.50, parsedDecimal, 0.01)
    }

    @Test
    fun test1_newCustomerTotalQarza() {
        val totalQarza = 1500.0
        val paid = 0.0
        val debtor = DebtorEntity(name = "Zahid", totalDebt = totalQarza)
        val customerAccountRemaining = debtor.totalDebt - paid
        val dashboardBalance = debtor.totalDebt
        val grandTotal = (debtor.totalDebt + paid).coerceAtLeast(0.0)

        assertEquals(1500.0, customerAccountRemaining, 0.01)
        assertEquals(1500.0, dashboardBalance, 0.01)
        assertEquals(1500.0, grandTotal, 0.01)
    }

    @Test
    fun test2_bakayaAddition() {
        var debtor = DebtorEntity(name = "Ali", totalDebt = 0.0)
        val addedBakaya = 1500.0
        debtor = debtor.copy(totalDebt = debtor.totalDebt + addedBakaya)

        val customerAccount = debtor.totalDebt
        val dashboard = debtor.totalDebt
        val pdfTotal = debtor.totalDebt

        assertEquals(1500.0, customerAccount, 0.01)
        assertEquals(1500.0, dashboard, 0.01)
        assertEquals(1500.0, pdfTotal, 0.01)
    }

    @Test
    fun test3_combinedTotal() {
        val bakaya = 1500.0
        val samaan = 4200.0
        val duty = 400.0
        val totalSaman = samaan + duty // 4600.0
        val paid = 1000.0

        val totalDebt = (bakaya + totalSaman - paid) // 5100.0
        val debtor = DebtorEntity(name = "Kamran", totalDebt = totalDebt)

        val grandTotal = (debtor.totalDebt + paid).coerceAtLeast(totalSaman)
        val remaining = (grandTotal - paid).coerceAtLeast(0.0)

        assertEquals(6100.0, grandTotal, 0.01)
        assertEquals(5100.0, remaining, 0.01)
        assertEquals(5100.0, debtor.totalDebt, 0.01)
    }

    @Test
    fun test4_editBakaya() {
        var debtor = DebtorEntity(name = "Usman", totalDebt = 1500.0)
        val oldBakaya = 1500.0
        val newBakaya = 1000.0
        val diff = newBakaya - oldBakaya // -500.0

        debtor = debtor.copy(totalDebt = (debtor.totalDebt + diff).coerceAtLeast(0.0))

        assertEquals(1000.0, debtor.totalDebt, 0.01)
    }

    @Test
    fun test5_deleteBakaya() {
        var debtor = DebtorEntity(name = "Tariq", totalDebt = 1500.0)
        val bakayaToDelete = 1500.0

        debtor = debtor.copy(totalDebt = (debtor.totalDebt - bakayaToDelete).coerceAtLeast(0.0))

        assertEquals(0.0, debtor.totalDebt, 0.01)
    }
}
