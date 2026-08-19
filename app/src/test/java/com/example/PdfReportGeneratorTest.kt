package com.example

import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity
import com.example.ui.components.GeneralPdfOptions
import com.example.util.DistanceCalculator
import com.example.util.PdfReportGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfOptionsAndCalculationsTest {

    @Test
    fun hideSamanTotal_defaultsToOff() {
        val options = GeneralPdfOptions()
        assertFalse("Hide Saman Total should default to false (OFF)", options.hideSamanTotal)
        assertFalse("Hide Ride Charges should default to false (OFF)", options.hideRideCharges)
    }

    @Test
    fun hideSamanTotal_whenOff_samanPricesAndTotalsAppearNormally() {
        val itemPrice = 1500.0
        val deliveryCharges = 200.0
        val samanTotal = itemPrice + deliveryCharges

        // Item price formatting
        val formattedPrice = PdfReportGenerator.formatItemPrice(itemPrice, hideSamanTotal = false)
        assertEquals("Rs. 1500", formattedPrice)

        // Item total formatting
        val formattedTotal = PdfReportGenerator.formatItemTotal(samanTotal, hideSamanTotal = false)
        assertEquals("Rs. 1700", formattedTotal)

        // Customer saman summary
        val samanSummary = PdfReportGenerator.formatCustomerSamanSummary(
            parcelCount = 3,
            samanTotal = 4500.0,
            hideSamanTotal = false
        )
        assertEquals("Saman Total: Rs. 4500", samanSummary)

        // Daily summary earnings with hideSamanTotal = false
        val summaryEarnings = PdfReportGenerator.formatDailySummaryEarnings(
            ridesEarning = 1000.0,
            parcelEarnings = 500.0,
            hideSamanTotal = false,
            hideRideCharges = false
        )
        assertEquals("Total Earnings: Rs. 1500", summaryEarnings)
    }

    @Test
    fun hideSamanTotal_whenOn_samanAmountsAreGenuinelyOmitted() {
        val itemPrice = 1500.0
        val deliveryCharges = 200.0
        val samanTotal = itemPrice + deliveryCharges

        // Item price is omitted with placeholder '--'
        val formattedPrice = PdfReportGenerator.formatItemPrice(itemPrice, hideSamanTotal = true)
        assertEquals("--", formattedPrice)
        assertFalse("Formatted price must not contain currency or digits", formattedPrice.contains("1500"))

        // Item total is omitted with placeholder '--'
        val formattedTotal = PdfReportGenerator.formatItemTotal(samanTotal, hideSamanTotal = true)
        assertEquals("--", formattedTotal)
        assertFalse("Formatted total must not contain currency or digits", formattedTotal.contains("1700"))

        // Customer saman summary shows only items count and omits total amount
        val samanSummary = PdfReportGenerator.formatCustomerSamanSummary(
            parcelCount = 3,
            samanTotal = 4500.0,
            hideSamanTotal = true
        )
        assertEquals("Saman Items: 3", samanSummary)
        assertFalse("Saman summary must not contain total amount", samanSummary.contains("4500"))

        // Daily summary earnings with hideSamanTotal = true omits parcel totals from aggregate
        val summaryEarnings = PdfReportGenerator.formatDailySummaryEarnings(
            ridesEarning = 1000.0,
            parcelEarnings = 500.0,
            hideSamanTotal = true,
            hideRideCharges = false
        )
        assertEquals("Rides Earning: Rs. 1000", summaryEarnings)
        assertFalse("Daily summary must omit parcel earnings", summaryEarnings.contains("1500"))
    }

    @Test
    fun hideRideCharges_whenOff_and_whenOn() {
        val fare = 450.0

        // When OFF
        val fareOff = PdfReportGenerator.formatRideFare(fare, hideRideCharges = false)
        assertEquals("Rs. 450", fareOff)

        // When ON
        val fareOn = PdfReportGenerator.formatRideFare(fare, hideRideCharges = true)
        assertEquals("--", fareOn)
        assertFalse(fareOn.contains("450"))
    }

    @Test
    fun bothHideOptions_whenBothOn_and_whenBothOff() {
        // Both OFF -> Full earnings visible
        val bothOff = PdfReportGenerator.formatDailySummaryEarnings(
            ridesEarning = 800.0,
            parcelEarnings = 300.0,
            hideSamanTotal = false,
            hideRideCharges = false
        )
        assertEquals("Total Earnings: Rs. 1100", bothOff)

        // Both ON -> All earnings hidden
        val bothOn = PdfReportGenerator.formatDailySummaryEarnings(
            ridesEarning = 800.0,
            parcelEarnings = 300.0,
            hideSamanTotal = true,
            hideRideCharges = true
        )
        assertEquals("Total Earnings: [Hidden]", bothOn)

        val customerGrandTotalBothOff = PdfReportGenerator.formatCustomerGrandTotal(
            grandTotal = 1100.0,
            hideSamanTotal = false,
            hideRideCharges = false
        )
        assertEquals("Grand Total: Rs. 1100", customerGrandTotalBothOff)

        val customerGrandTotalBothOn = PdfReportGenerator.formatCustomerGrandTotal(
            grandTotal = 1100.0,
            hideSamanTotal = true,
            hideRideCharges = true
        )
        assertNull("Grand total must be null (omitted) when hide options are active", customerGrandTotalBothOn)
    }

    @Test
    fun parcelDetails_remainVisible_whenPriceIsHidden() {
        val parcel = ParcelEntity(
            id = 42,
            shopName = "Bismillah Garments",
            recipientName = "Zubair Khan",
            recipientAddress = "Flat #302, Phase 5",
            itemDetails = "3x Cotton Shirts",
            itemPrice = 3500.0,
            deliveryCharges = 250.0,
            isDelivered = true,
            isPaid = false
        )

        // Item non-price metadata remains available and unmodified
        assertEquals("Bismillah Garments", parcel.shopName)
        assertEquals("Zubair Khan", parcel.recipientName)
        assertEquals("Flat #302, Phase 5", parcel.recipientAddress)
        assertEquals("3x Cotton Shirts", parcel.itemDetails)

        // Price can be hidden independently without changing parcel details
        val priceStringHidden = PdfReportGenerator.formatItemPrice(parcel.itemPrice, hideSamanTotal = true)
        assertEquals("--", priceStringHidden)

        // Database entity values remain intact
        assertEquals(3500.0, parcel.itemPrice, 0.001)
        assertEquals(250.0, parcel.deliveryCharges, 0.001)
    }

    @Test
    fun distanceCalculator_estimatesKnownLocationsCorrectly() {
        val dist1 = DistanceCalculator.estimateDistanceKm("Gulshan-e-Iqbal", "Saddar")
        assertTrue("Distance should be greater than 0", dist1 > 0)

        val dist2 = DistanceCalculator.estimateDistanceKm("Johar", "Clifton")
        assertTrue("Distance should be greater than 0", dist2 > 0)
    }
}
