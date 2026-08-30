package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.PaymentHistoryEntity
import com.example.data.entity.RideEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    fun generatePdfReport(
        context: Context,
        selectedDateMillis: Long,
        rides: List<RideEntity>,
        parcels: List<ParcelEntity>,
        debtors: List<DebtorEntity>,
        hideSamanTotal: Boolean = false,
        hideRideCharges: Boolean = false,
        customTitle: String = "AQEEL RIDER / عقیل رائڈر",
        customSubtitle: String = "",
        customNote: String = "",
        includeRides: Boolean = true,
        includeParcels: Boolean = true,
        includeDebtors: Boolean = true
    ): File {
        val pdfDocument = PdfDocument()

        // Standard A4 dimensions in points: 595 x 842
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint()
        val headerPaint = Paint()
        val linePaint = Paint()

        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
        val dateStr = dateFormat.format(Date(selectedDateMillis))

        // Colors
        val primaryGreen = Color.rgb(13, 99, 56)
        val darkGray = Color.rgb(40, 40, 40)
        val lightGreenBg = Color.rgb(220, 252, 231)

        var y = 40f

        // Header Banner
        paint.color = primaryGreen
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        titlePaint.color = Color.WHITE
        titlePaint.textSize = 22f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val reportHeading = if (customTitle.isNotBlank()) customTitle else "AQEEL RIDER / عقیل رائڈر"
        canvas.drawText(reportHeading, 30f, 45f, titlePaint)

        titlePaint.textSize = 13f
        titlePaint.typeface = Typeface.DEFAULT
        val reportSubHeading = if (customSubtitle.isNotBlank()) customSubtitle else "Daily Ledger & Rider Summary Report | Date: $dateStr"
        canvas.drawText(reportSubHeading, 30f, 70f, titlePaint)

        y = 120f

        // Effective collections
        val effectiveRides = if (includeRides) rides else emptyList()
        val effectiveParcels = if (includeParcels) parcels else emptyList()
        val effectiveDebtors = if (includeDebtors) debtors else emptyList()

        // Summary Statistics Box
        val totalRidesCount = effectiveRides.size
        val totalKm = effectiveRides.sumOf { it.distanceKm }
        val ridesEarning = effectiveRides.sumOf { it.fareAmount }
        val parcelEarnings = effectiveParcels.filter { it.isPaid }.sumOf { it.deliveryCharges }
        val totalDebtOutstanding = effectiveDebtors.filter { it.totalDebt > 0 }.sumOf { it.totalDebt }

        paint.color = lightGreenBg
        canvas.drawRoundRect(25f, y, 570f, y + 85f, 10f, 10f, paint)

        headerPaint.color = primaryGreen
        headerPaint.textSize = 14f
        headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SUMMARY STATISTICS / خلاصہ", 40f, y + 25f, headerPaint)

        paint.color = darkGray
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Total Rides: $totalRidesCount", 40f, y + 50f, paint)
        canvas.drawText("Total Distance: ${String.format("%.1f", totalKm)} KM", 170f, y + 50f, paint)

        val totalEarningsText = formatDailySummaryEarnings(ridesEarning, parcelEarnings, hideSamanTotal, hideRideCharges)
        canvas.drawText(totalEarningsText, 330f, y + 50f, paint)

        val parcelText = if (hideSamanTotal) "Parcel Deliveries: ${effectiveParcels.size}" else "Parcel Earnings: Rs. ${parcelEarnings.toInt()}"
        canvas.drawText(parcelText, 40f, y + 70f, paint)
        canvas.drawText("Pending Debts: Rs. ${totalDebtOutstanding.toInt()}", 330f, y + 70f, paint)

        y += 110f

        linePaint.color = Color.LTGRAY
        linePaint.strokeWidth = 1f

        var sectionNum = 1

        // 1. RIDES SECTION
        if (includeRides) {
            headerPaint.textSize = 13f
            canvas.drawText("$sectionNum. RIDES LOG / رائڈز کی تفصیل", 30f, y, headerPaint)
            sectionNum++
            y += 15f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 20f

            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("From -> To", 30f, y, paint)
            canvas.drawText("KM", 320f, y, paint)
            canvas.drawText(if (hideRideCharges) "Kiraya (Rs) [Hidden]" else "Kiraya (Rs)", 420f, y, paint)
            canvas.drawText("Time", 500f, y, paint)
            y += 10f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 15f

            paint.typeface = Typeface.DEFAULT
            if (effectiveRides.isEmpty()) {
                canvas.drawText("No rides recorded for this period.", 30f, y, paint)
                y += 20f
            } else {
                for (ride in effectiveRides.take(8)) {
                    val routeText = "${ride.fromLocation} -> ${ride.toLocation}"
                    val truncatedRoute = if (routeText.length > 45) routeText.substring(0, 42) + "..." else routeText
                    canvas.drawText(truncatedRoute, 30f, y, paint)
                    canvas.drawText("${ride.distanceKm} KM", 320f, y, paint)
                    val fareStr = formatRideFare(ride.fareAmount, hideRideCharges)
                    canvas.drawText(fareStr, 420f, y, paint)
                    canvas.drawText(ride.timeString.ifEmpty { "--" }, 500f, y, paint)
                    y += 18f
                }
            }

            y += 15f
        }

        // 2. PARCELS SECTION
        if (includeParcels) {
            headerPaint.textSize = 13f
            canvas.drawText("$sectionNum. SAMAN / PARCEL DELIVERIES / سامان ڈیلیوری", 30f, y, headerPaint)
            sectionNum++
            y += 15f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 20f

            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Shop / Recipient", 30f, y, paint)
            canvas.drawText(if (hideSamanTotal) "Price [Hidden]" else "Price (Rs)", 280f, y, paint)
            canvas.drawText(if (hideRideCharges) "Delivery [Hidden]" else "Delivery (Rs)", 360f, y, paint)
            canvas.drawText("Status", 470f, y, paint)
            y += 10f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 15f

            paint.typeface = Typeface.DEFAULT
            if (effectiveParcels.isEmpty()) {
                canvas.drawText("No parcel deliveries recorded.", 30f, y, paint)
                y += 20f
            } else {
                for (parcel in effectiveParcels.take(6)) {
                    val photoBitmap = loadThumbnailBitmap(context, parcel.imageUri, 14)
                    val photoIndicator = if (photoBitmap != null || !parcel.imageUri.isNullOrBlank()) "📷 " else ""
                    val samanPart = if (parcel.samanName.isNotBlank()) " [${parcel.samanName}]" else if (parcel.itemDetails.isNotBlank()) " [${parcel.itemDetails}]" else ""
                    val info = "$photoIndicator${parcel.shopName} -> ${parcel.recipientName}$samanPart"
                    val truncatedInfo = if (info.length > 38) info.substring(0, 35) + "..." else info
                    val statusText = if (parcel.isDelivered && parcel.isPaid) "Delivered & Paid"
                    else if (parcel.isDelivered) "Delivered"
                    else "Pending"

                    if (photoBitmap != null) {
                        try {
                            canvas.drawBitmap(photoBitmap, 30f, y - 10f, null)
                            canvas.drawText(truncatedInfo, 48f, y, paint)
                        } catch (e: Exception) {
                            canvas.drawText(truncatedInfo, 30f, y, paint)
                        }
                    } else {
                        canvas.drawText(truncatedInfo, 30f, y, paint)
                    }

                    val priceStr = formatItemPrice(parcel.itemPrice, hideSamanTotal)
                    val deliveryStr = formatDeliveryCharges(parcel.deliveryCharges, hideRideCharges)
                    canvas.drawText(priceStr, 280f, y, paint)
                    canvas.drawText(deliveryStr, 360f, y, paint)
                    canvas.drawText(statusText, 470f, y, paint)
                    y += 18f
                }
            }

            y += 15f
        }

        // 3. LOGON KA QARZA SECTION
        if (includeDebtors) {
            headerPaint.textSize = 13f
            canvas.drawText("$sectionNum. LOGON KA QARZA / لوگوں کا قرضہ", 30f, y, headerPaint)
            y += 15f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 20f

            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Name", 30f, y, paint)
            canvas.drawText("Phone", 200f, y, paint)
            canvas.drawText("Total Debt", 340f, y, paint)
            canvas.drawText("Note", 450f, y, paint)
            y += 10f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 15f

            paint.typeface = Typeface.DEFAULT
            val activeDebtors = effectiveDebtors.filter { it.totalDebt > 0 }
            if (activeDebtors.isEmpty()) {
                canvas.drawText("No active debts.", 30f, y, paint)
                y += 20f
            } else {
                for (debtor in activeDebtors.take(6)) {
                    canvas.drawText(debtor.name, 30f, y, paint)
                    canvas.drawText(debtor.phoneNumber.ifEmpty { "--" }, 200f, y, paint)
                    canvas.drawText("Rs. ${debtor.totalDebt.toInt()}", 340f, y, paint)
                    val truncatedNote = if (debtor.note.length > 20) debtor.note.substring(0, 17) + "..." else debtor.note
                    canvas.drawText(truncatedNote.ifEmpty { "-" }, 450f, y, paint)
                    y += 18f
                }
            }
        }

        // Optional Custom Footer Note
        if (customNote.isNotBlank()) {
            paint.color = darkGray
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Note: $customNote", 30f, 640f, paint)
        }

        // Draw Payment Information Box
        val boxTop = maxOf(y + 10f, 655f)
        drawPaymentInformationBox(canvas, boxTop)

        // Footer
        paint.color = Color.GRAY
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Generated by Aqeel Rider App | www.aqeelrider.com", 30f, 818f, paint)

        pdfDocument.finishPage(page)

        // Save PDF to Cache Dir
        val fileName = "AqeelRider_Report_$dateStr.pdf"
        val pdfFile = File(context.cacheDir, fileName)
        val fileOutputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(fileOutputStream)
        pdfDocument.close()
        fileOutputStream.close()

        return pdfFile
    }

    fun generateCustomerPdfReport(
        context: Context,
        debtor: DebtorEntity,
        rides: List<RideEntity>,
        parcels: List<ParcelEntity>,
        payments: List<PaymentHistoryEntity>,
        dateFilterLabel: String = "All Record",
        customTitle: String = "AQEEL RIDER - CUSTOMER PAYMENT REPORT",
        customNote: String = "Shukriya! Meherbani karke baqaya payment time par adaa karein.",
        includeRide: Boolean = true,
        includeSaman: Boolean = true,
        includePaymentHistory: Boolean = true,
        hideSamanTotal: Boolean = false,
        hideRideCharges: Boolean = false,
        customCustomerName: String = debtor.name,
        customPhoneNumber: String = debtor.phoneNumber,
        isPdfTotalOnly: Boolean = false
    ): File {
        val pdfDocument = PdfDocument()

        // Standard A4 dimensions in points: 595 x 842
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val titlePaint = Paint()
        val headerPaint = Paint()
        val linePaint = Paint()

        val dateFormat = SimpleDateFormat("dd-MMM-yyyy", Locale.US)

        // Colors
        val primaryGreen = Color.rgb(13, 99, 56)
        val darkGray = Color.rgb(40, 40, 40)
        val lightGreenBg = Color.rgb(220, 252, 231)
        val accentRed = Color.rgb(220, 38, 38)

        var y = 40f

        // Header Banner
        paint.color = primaryGreen
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        titlePaint.color = Color.WHITE
        titlePaint.textSize = 18f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val displayTitle = if (customTitle.isNotBlank()) customTitle else "AQEEL RIDER - CUSTOMER PAYMENT REPORT"
        canvas.drawText(displayTitle, 30f, 42f, titlePaint)

        titlePaint.textSize = 12f
        titlePaint.typeface = Typeface.DEFAULT
        val displayName = if (customCustomerName.isNotBlank()) customCustomerName else debtor.name
        val displayPhone = if (customPhoneNumber.isNotBlank()) customPhoneNumber else debtor.phoneNumber.ifEmpty { "N/A" }
        canvas.drawText("Customer: $displayName | Phone: $displayPhone | Filter: $dateFilterLabel", 30f, 68f, titlePaint)

        y = 115f

        // Effective Lists based on mark options
        val effectiveRides = if (includeRide) rides else emptyList()
        val effectiveParcels = if (includeSaman) parcels else emptyList()
        val effectivePayments = if (includePaymentHistory) payments else emptyList()

        // Calculation Totals - Uses the authoritative formula (Remaining Balance = Total Amount - Total Paid)
        val rideTotal = effectiveRides.sumOf { it.fareAmount }
        val samanTotal = effectiveParcels.sumOf { it.itemPrice + it.deliveryCharges }
        val totalPaid = effectivePayments.sumOf { it.amountPaid }
        val grandTotal = (debtor.totalDebt + totalPaid).coerceAtLeast(rideTotal + samanTotal)
        val previousBakaya = (grandTotal - (rideTotal + samanTotal)).coerceAtLeast(0.0)
        val baqaya = (grandTotal - totalPaid).coerceAtLeast(0.0)

        val effectiveHideSaman = hideSamanTotal || isPdfTotalOnly
        val effectiveHideRide = hideRideCharges || isPdfTotalOnly

        // Summary Box
        val summaryBoxHeight = if (isPdfTotalOnly) 95f else 115f
        paint.color = lightGreenBg
        canvas.drawRoundRect(25f, y, 570f, y + summaryBoxHeight, 10f, 10f, paint)

        headerPaint.color = primaryGreen
        headerPaint.textSize = 12f
        headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CUSTOMER PAYMENT BREAKDOWN / کسٹمر پیمنٹ و بقایا خلاصہ", 40f, y + 22f, headerPaint)

        paint.color = darkGray
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT

        var currentSummaryY = y + 42f
        if (!isPdfTotalOnly) {
            val line1 = if (previousBakaya > 0) {
                "Ride: ${if (effectiveHideRide) "--" else "Rs. ${rideTotal.toInt()}"}  |  Saman: ${if (effectiveHideSaman) "--" else "Rs. ${samanTotal.toInt()}"}  |  Bakaya: Rs. ${previousBakaya.toInt()}"
            } else {
                "Ride Total: ${if (effectiveHideRide) "--" else "Rs. ${rideTotal.toInt()}"}    |    Saman/Parcel Total: ${if (effectiveHideSaman) "--" else "Rs. ${samanTotal.toInt()}"}"
            }
            canvas.drawText(line1, 40f, currentSummaryY, paint)
            currentSummaryY += 20f
        }

        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 11f
        val line2 = "TOTAL AMOUNT: Rs. ${grandTotal.toInt()}    |    PAID: Rs. ${totalPaid.toInt()}"
        canvas.drawText(line2, 40f, currentSummaryY, paint)
        currentSummaryY += 24f

        paint.color = if (baqaya <= 0) primaryGreen else accentRed
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textSize = 11.5f
        val line3 = if (baqaya <= 0) {
            "REMAINING BALANCE: Rs. 0  (✨ FULL PAID / تمام ادا)"
        } else {
            "REMAINING BALANCE: Rs. ${baqaya.toInt()}  (⚠️ PENDING / بقایا واجب الادا)"
        }
        canvas.drawText(line3, 40f, currentSummaryY, paint)

        y += summaryBoxHeight + 20f

        linePaint.color = Color.LTGRAY
        linePaint.strokeWidth = 1f

        var sectionIndex = 1

        // 1. RIDES SECTION (If marked)
        if (includeRide) {
            paint.color = darkGray
            headerPaint.textSize = 12f
            canvas.drawText("$sectionIndex. RIDE PAYMENT HISTORY (${effectiveRides.size} Rides)", 30f, y, headerPaint)
            sectionIndex++
            y += 12f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 18f

            paint.textSize = 10f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Date & Time", 30f, y, paint)
            canvas.drawText("Route (From -> To)", 150f, y, paint)
            canvas.drawText("KM", 420f, y, paint)
            canvas.drawText(if (effectiveHideRide) "Fare (Rs) [Hidden]" else "Fare (Rs)", 480f, y, paint)
            y += 8f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 15f

            paint.typeface = Typeface.DEFAULT
            if (effectiveRides.isEmpty()) {
                canvas.drawText("Is duration me koi ride nahi hui.", 30f, y, paint)
                y += 18f
            } else {
                for (ride in effectiveRides.take(6)) {
                    val rideDate = dateFormat.format(Date(ride.dateMillis))
                    val route = "${ride.fromLocation} -> ${ride.toLocation}"
                    val truncatedRoute = if (route.length > 38) route.substring(0, 35) + "..." else route
                    canvas.drawText("$rideDate ${ride.timeString}", 30f, y, paint)
                    canvas.drawText(truncatedRoute, 150f, y, paint)
                    canvas.drawText("${ride.distanceKm} KM", 420f, y, paint)
                    val fareStr = if (effectiveHideRide) "--" else "Rs. ${ride.fareAmount.toInt()}"
                    canvas.drawText(fareStr, 480f, y, paint)
                    y += 16f
                }
            }

            y += 12f
        }

        // 2. SAMAN / PARCEL SECTION (If marked)
        if (includeSaman) {
            headerPaint.textSize = 12f
            canvas.drawText("$sectionIndex. SAMAN / PARCEL PAYMENT (${effectiveParcels.size} Items)", 30f, y, headerPaint)
            sectionIndex++
            y += 12f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 18f

            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Date", 30f, y, paint)
            canvas.drawText("Dukan / Details", 130f, y, paint)
            canvas.drawText(if (effectiveHideSaman) "Item Price [Hidden]" else "Item Price", 360f, y, paint)
            canvas.drawText(if (effectiveHideRide) "Delivery [Hidden]" else "Delivery", 440f, y, paint)
            canvas.drawText(if (effectiveHideSaman) "Total [Hidden]" else "Total", 510f, y, paint)
            y += 8f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 15f

            paint.typeface = Typeface.DEFAULT
            if (effectiveParcels.isEmpty()) {
                canvas.drawText("Is duration me koi saman add nahi hua.", 30f, y, paint)
                y += 18f
            } else {
                for (p in effectiveParcels.take(5)) {
                    val pDate = dateFormat.format(Date(p.createdTimestamp))
                    val sName = if (p.samanName.isNotBlank()) p.samanName else p.itemDetails
                    val photoBitmap = loadThumbnailBitmap(context, p.imageUri, 12)
                    val photoIndicator = if (photoBitmap != null || !p.imageUri.isNullOrBlank()) "📷 " else ""
                    val details = "$photoIndicator${p.shopName}: $sName"
                    val truncated = if (details.length > 32) details.substring(0, 29) + "..." else details
                    val totalP = p.itemPrice + p.deliveryCharges
                    canvas.drawText(pDate, 30f, y, paint)
                    if (photoBitmap != null) {
                        try {
                            canvas.drawBitmap(photoBitmap, 115f, y - 9f, null)
                            canvas.drawText(truncated, 132f, y, paint)
                        } catch (e: Exception) {
                            canvas.drawText(truncated, 130f, y, paint)
                        }
                    } else {
                        canvas.drawText(truncated, 130f, y, paint)
                    }
                    val priceStr = if (effectiveHideSaman) "--" else "Rs. ${p.itemPrice.toInt()}"
                    val delStr = if (effectiveHideRide) "--" else "Rs. ${p.deliveryCharges.toInt()}"
                    val totalStr = if (effectiveHideSaman) "--" else "Rs. ${totalP.toInt()}"
                    canvas.drawText(priceStr, 360f, y, paint)
                    canvas.drawText(delStr, 440f, y, paint)
                    canvas.drawText(totalStr, 510f, y, paint)
                    y += 16f
                }
            }

            y += 12f
        }

        // 3. PAYMENT HISTORY / WASOOLI (If marked)
        if (includePaymentHistory) {
            headerPaint.textSize = 12f
            canvas.drawText("$sectionIndex. PAYMENT HISTORY / WASOOLI (${effectivePayments.size} Transactions)", 30f, y, headerPaint)
            sectionIndex++
            y += 12f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 18f

            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("Date & Time", 30f, y, paint)
            canvas.drawText(if (isPdfTotalOnly) "Amount [Hidden]" else "Amount Paid", 180f, y, paint)
            canvas.drawText("Type", 300f, y, paint)
            canvas.drawText("Note", 400f, y, paint)
            y += 8f
            canvas.drawLine(30f, y, 565f, y, linePaint)
            y += 15f

            paint.typeface = Typeface.DEFAULT
            if (effectivePayments.isEmpty()) {
                canvas.drawText("Koi payment vasool nahi hui.", 30f, y, paint)
                y += 18f
            } else {
                for (pay in effectivePayments.take(5)) {
                    val payDate = dateFormat.format(Date(pay.timestamp))
                    canvas.drawText(payDate, 30f, y, paint)
                    val paidAmountStr = if (isPdfTotalOnly) "--" else "Rs. ${pay.amountPaid.toInt()}"
                    canvas.drawText(paidAmountStr, 180f, y, paint)
                    canvas.drawText(pay.paymentType, 300f, y, paint)
                    val truncatedNote = if (pay.note.length > 22) pay.note.substring(0, 19) + "..." else pay.note
                    canvas.drawText(truncatedNote, 400f, y, paint)
                    y += 16f
                }
            }
        }

        // Draw Payment Information Box
        val boxTop = maxOf(y + 10f, 645f)
        drawPaymentInformationBox(canvas, boxTop)

        // Footer Note & Info
        if (customNote.isNotBlank()) {
            paint.color = darkGray
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Note: $customNote", 30f, 792f, paint)
        }

        paint.color = Color.GRAY
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Generated by Aqeel Rider App | Customer Payment Report for $displayName", 30f, 818f, paint)

        pdfDocument.finishPage(page)

        // File Save
        val safeCustomerName = displayName.replace("\\s+".toRegex(), "_").replace("[^a-zA-Z0-9_]".toRegex(), "")
        val fileName = "${safeCustomerName}_Account_Report.pdf"
        val pdfFile = File(context.cacheDir, fileName)
        val fileOutputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(fileOutputStream)
        pdfDocument.close()
        fileOutputStream.close()

        return pdfFile
    }

    private fun drawPaymentInformationBox(canvas: Canvas, startY: Float) {
        val boxLeft = 25f
        val boxRight = 570f
        val boxTop = startY
        val boxHeight = 110f
        val boxBottom = boxTop + boxHeight

        val bgPaint = Paint().apply {
            color = Color.rgb(240, 253, 244)
            style = Paint.Style.FILL
        }
        val borderPaint = Paint().apply {
            color = Color.rgb(13, 99, 56)
            style = Paint.Style.STROKE
            strokeWidth = 1.2f
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(13, 99, 56)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.rgb(187, 247, 208)
            strokeWidth = 1f
        }
        val labelPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val valuePaint = Paint().apply {
            color = Color.rgb(13, 99, 56)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val notePaint = Paint().apply {
            color = Color.rgb(71, 85, 105)
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 8f, 8f, bgPaint)
        canvas.drawRoundRect(boxLeft, boxTop, boxRight, boxBottom, 8f, 8f, borderPaint)

        val titleY = boxTop + 20f
        canvas.drawText("PAYMENT INFORMATION / ادائیگی کی معلومات", boxLeft + 15f, titleY, headerPaint)
        canvas.drawLine(boxLeft + 15f, titleY + 5f, boxRight - 15f, titleY + 5f, linePaint)

        val row1Y = boxTop + 44f
        canvas.drawText("Calling Number:", boxLeft + 15f, row1Y, labelPaint)
        canvas.drawText("0312-3722248", boxLeft + 115f, row1Y, valuePaint)

        canvas.drawText("Easypaisa Number:", boxLeft + 275f, row1Y, labelPaint)
        canvas.drawText("0341-2055455", boxLeft + 385f, row1Y, valuePaint)

        val row2Y = boxTop + 66f
        canvas.drawText("WhatsApp Number:", boxLeft + 15f, row2Y, labelPaint)
        canvas.drawText("0311-2018002", boxLeft + 128f, row2Y, valuePaint)

        canvas.drawText("Askari Bank A/C:", boxLeft + 275f, row2Y, labelPaint)
        canvas.drawText("01980200001569", boxLeft + 375f, row2Y, valuePaint)

        canvas.drawText("* Baraye meherbani payment karne ke baad screenshot WhatsApp par zaroor share karein.", boxLeft + 15f, boxTop + 93f, notePaint)
    }

    fun formatItemPrice(itemPrice: Double, hideSamanTotal: Boolean): String {
        return if (hideSamanTotal) "--" else "Rs. ${itemPrice.toInt()}"
    }

    fun formatDeliveryCharges(deliveryCharges: Double, hideRideCharges: Boolean): String {
        return if (hideRideCharges) "--" else "Rs. ${deliveryCharges.toInt()}"
    }

    fun formatItemTotal(total: Double, hideSamanTotal: Boolean): String {
        return if (hideSamanTotal) "--" else "Rs. ${total.toInt()}"
    }

    fun formatRideFare(fare: Double, hideRideCharges: Boolean): String {
        return if (hideRideCharges) "--" else "Rs. ${fare.toInt()}"
    }

    fun formatDailySummaryEarnings(ridesEarning: Double, parcelEarnings: Double, hideSamanTotal: Boolean, hideRideCharges: Boolean): String {
        return if (hideRideCharges && hideSamanTotal) {
            "Total Earnings: [Hidden]"
        } else if (hideRideCharges) {
            "Parcel Earnings: Rs. ${parcelEarnings.toInt()}"
        } else if (hideSamanTotal) {
            "Rides Earning: Rs. ${ridesEarning.toInt()}"
        } else {
            val totalEarning = ridesEarning + parcelEarnings
            "Total Earnings: Rs. ${totalEarning.toInt()}"
        }
    }

    fun formatCustomerSamanSummary(parcelCount: Int, samanTotal: Double, hideSamanTotal: Boolean): String {
        return if (hideSamanTotal) {
            "Saman Items: $parcelCount"
        } else {
            "Saman Total: Rs. ${samanTotal.toInt()}"
        }
    }

    fun formatCustomerGrandTotal(grandTotal: Double, hideSamanTotal: Boolean, hideRideCharges: Boolean): String? {
        return if (hideRideCharges || hideSamanTotal) {
            null
        } else {
            "Grand Total: Rs. ${grandTotal.toInt()}"
        }
    }

    fun formatCustomerBaqayaStatus(baqaya: Double, hideSamanTotal: Boolean, hideRideCharges: Boolean): String {
        return if (hideRideCharges && hideSamanTotal) {
            "Total Amounts: [Hidden]"
        } else if (baqaya <= 0) {
            "Status: FULL PAID ✨"
        } else {
            "Baqaya: Rs. ${baqaya.toInt()} ⚠️"
        }
    }

    private fun loadThumbnailBitmap(context: Context, uriString: String?, maxDim: Int = 14): Bitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val inputStream = if (uriString.startsWith("file://") || uriString.startsWith("/")) {
                val path = if (uriString.startsWith("file://")) uriString.substring(7) else uriString
                File(path).inputStream()
            } else {
                context.contentResolver.openInputStream(Uri.parse(uriString))
            }
            val original = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (original != null) {
                Bitmap.createScaledBitmap(original, maxDim, maxDim, true)
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
