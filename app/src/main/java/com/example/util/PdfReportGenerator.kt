package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.entity.CustomerHistoryEntity
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

    fun generateFullReport(
        context: Context,
        rides: List<RideEntity>,
        parcels: List<ParcelEntity>,
        debtors: List<DebtorEntity>,
        payments: List<PaymentHistoryEntity>,
        title: String = "Aqeel Rider - Business Report"
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint()
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        // Header Background
        paint.color = Color.rgb(15, 23, 42) // Slate 900
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("AQEEL RIDER SERVICES", 30f, 40f, paint)

        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(title, 30f, 60f, paint)
        canvas.drawText("Generated on: ${dateFormat.format(Date())}", 30f, 78f, paint)

        var y = 120f
        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
        }
        val boldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionPaint = Paint().apply {
            color = Color.rgb(0, 200, 83)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        // Summary Stats
        val totalRideFare = rides.sumOf { it.fare }
        val totalRidePaid = rides.sumOf { it.amountPaid }
        val totalParcelCharges = parcels.sumOf { it.deliveryCharges }
        val totalParcelPaid = parcels.sumOf { it.amountPaid }
        val totalDebtRemaining = debtors.sumOf { it.remainingDebt }
        val totalRecovered = payments.sumOf { it.amountPaid }
        val netCash = totalRidePaid + totalParcelPaid + totalRecovered

        canvas.drawText("Financial Summary", 30f, y, sectionPaint)
        y += 20f

        canvas.drawText("Total Rides: ${rides.size} | Ride Revenue: Rs. ${totalRidePaid.toInt()}", 30f, y, textPaint)
        y += 16f
        canvas.drawText("Total Parcels: ${parcels.size} | Parcel Revenue: Rs. ${totalParcelPaid.toInt()}", 30f, y, textPaint)
        y += 16f
        canvas.drawText("Active Debtors: ${debtors.size} | Outstanding Debt (Bakaya): Rs. ${totalDebtRemaining.toInt()}", 30f, y, textPaint)
        y += 16f
        canvas.drawText("Debt Recoveries: Rs. ${totalRecovered.toInt()} | Net In-Hand Cash: Rs. ${netCash.toInt()}", 30f, y, boldPaint)
        y += 30f

        // Recent Rides
        canvas.drawText("Recent Rides (${rides.take(5).size} of ${rides.size})", 30f, y, sectionPaint)
        y += 18f
        for (r in rides.take(5)) {
            val rDate = DateTimeUtils.formatDateTime(r.rideDate)
            val line = "[$rDate] ${r.customerName} (${r.phone}) - ${r.pickupLocation} -> ${r.dropoffLocation} | Fare: Rs. ${r.fare.toInt()} (Paid: ${r.amountPaid.toInt()}, Bakaya: ${r.remainingBakaya.toInt()})"
            canvas.drawText(line, 30f, y, textPaint)
            y += 15f
        }
        y += 15f

        // Recent Parcels
        canvas.drawText("Recent Parcels (${parcels.take(5).size} of ${parcels.size})", 30f, y, sectionPaint)
        y += 18f
        for (p in parcels.take(5)) {
            val pDate = DateTimeUtils.formatDateTime(p.date)
            val status = if (p.isDelivered) "Delivered" else "Pending"
            val line = "[$pDate] From: ${p.senderName} -> To: ${p.receiverName} (${p.deliveryAddress}) | Rs. ${p.deliveryCharges.toInt()} [$status, Paid: Rs. ${p.amountPaid.toInt()}]"
            canvas.drawText(line, 30f, y, textPaint)
            y += 15f
        }
        y += 15f

        // Outstanding Debtors
        canvas.drawText("Top Outstanding Debtors (${debtors.take(5).size} of ${debtors.size})", 30f, y, sectionPaint)
        y += 18f
        for (d in debtors.filter { it.remainingDebt > 0 }.take(5)) {
            val line = "${d.name} (${d.phone}) - Remaining Debt: Rs. ${d.remainingDebt.toInt()} (Total: Rs. ${d.totalDebt.toInt()})"
            canvas.drawText(line, 30f, y, textPaint)
            y += 15f
        }

        // Footer
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawLine(30f, 800f, 565f, 800f, paint)
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 9f
        canvas.drawText("Aqeel Rider Dispatch Management System • Confidential", 30f, 815f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Aqeel_Rider_Report_${System.currentTimeMillis()}.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return file
    }

    fun generateCustomerLedgerPdf(
        context: Context,
        customerName: String,
        phone: String,
        remainingDebt: Double,
        history: List<CustomerHistoryEntity>,
        markedItemIds: Set<Long>? = null,
        rides: List<RideEntity> = emptyList(),
        parcels: List<ParcelEntity> = emptyList()
    ): File {
        val pdfDocument = PdfDocument()
        val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
        }
        val boldPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val redPaint = Paint().apply {
            color = Color.rgb(229, 57, 53)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val footerLinePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
        }
        val footerTextPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
        }

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Header
        val headerPaint = Paint().apply { color = Color.rgb(15, 23, 42) }
        canvas.drawRect(0f, 0f, 595f, 90f, headerPaint)

        val headerTitlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("CUSTOMER ACCOUNT STATEMENT", 30f, 40f, headerTitlePaint)

        val headerSubPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText("Customer: $customerName | Phone: $phone", 30f, 60f, headerSubPaint)
        canvas.drawText("Issued: ${dateFormat.format(Date())}", 30f, 78f, headerSubPaint)

        var y = 125f
        canvas.drawText("Outstanding Balance (Bakaya): Rs. ${remainingDebt.toInt()}", 30f, y, redPaint)
        y += 25f

        // Calculate Totals across ALL items (marked + unmarked)
        val totalRidesFare = rides.sumOf { it.fare }
        val totalRidesBakaya = rides.sumOf { it.remainingBakaya }
        val totalParcelsCharges = parcels.sumOf { it.totalCharges }
        val totalParcelsBakaya = parcels.sumOf { it.remainingBakaya }

        // Filter history rows for display based on markedItemIds
        val displayHistory = if (markedItemIds != null) {
            history.filter { markedItemIds.contains(it.id) }
        } else {
            history
        }

        val unmarkedCount = history.size - displayHistory.size
        val summaryNote = if (unmarkedCount > 0) {
            "Showing ${displayHistory.size} marked transactions (${unmarkedCount} details omitted; totals include all transactions):"
        } else {
            "Transaction & Ledger History (${displayHistory.size} records):"
        }

        canvas.drawText(summaryNote, 30f, y, boldPaint)
        y += 20f

        for (item in displayHistory) {
            if (y > 780f) {
                // Draw footer on current page
                canvas.drawLine(30f, 800f, 565f, 800f, footerLinePaint)
                canvas.drawText("Aqeel Rider Services • Page $pageNum", 30f, 815f, footerTextPaint)
                pdfDocument.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                // Small sub-header on continuation pages
                canvas.drawRect(0f, 0f, 595f, 40f, headerPaint)
                val contTitlePaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                canvas.drawText("CUSTOMER STATEMENT — $customerName (Contd.)", 30f, 25f, contTitlePaint)
                y = 65f
            }

            val dateStr = DateTimeUtils.formatDateTime(item.timestamp)
            val line = "[$dateStr] [${item.activityType}] ${item.details}"
            val printableLine = if (line.length > 95) line.substring(0, 92) + "..." else line
            canvas.drawText(printableLine, 30f, y, textPaint)
            y += 18f
        }

        // Summary block if space permits
        if (rides.isNotEmpty() || parcels.isNotEmpty()) {
            if (y > 730f) {
                canvas.drawLine(30f, 800f, 565f, 800f, footerLinePaint)
                canvas.drawText("Aqeel Rider Services • Page $pageNum", 30f, 815f, footerTextPaint)
                pdfDocument.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 65f
            } else {
                y += 15f
            }

            val summaryPaint = Paint().apply {
                color = Color.rgb(241, 245, 249)
            }
            canvas.drawRect(30f, y, 565f, y + 45f, summaryPaint)
            val summaryTextPaint = Paint().apply {
                color = Color.rgb(30, 41, 59)
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText(
                "TOTALS SUMMARY: Total Rides Fare: Rs. ${totalRidesFare.toInt()} (Bakaya: ${totalRidesBakaya.toInt()})",
                40f,
                y + 20f,
                summaryTextPaint
            )
            canvas.drawText(
                "Total Parcels/Delivery: Rs. ${totalParcelsCharges.toInt()} (Bakaya: ${totalParcelsBakaya.toInt()}) • Grand Outstanding: Rs. ${remainingDebt.toInt()}",
                40f,
                y + 36f,
                summaryTextPaint
            )
        }

        // Footer on last page
        canvas.drawLine(30f, 800f, 565f, 800f, footerLinePaint)
        canvas.drawText("Aqeel Rider Services • Page $pageNum • Thank you for your business!", 30f, 815f, footerTextPaint)
        pdfDocument.finishPage(page)

        val safeName = customerName.replace(Regex("[^a-zA-Z0-9]"), "_")
        val file = File(context.cacheDir, "Ledger_${safeName}_${System.currentTimeMillis()}.pdf")
        val outputStream = FileOutputStream(file)
        pdfDocument.writeTo(outputStream)
        outputStream.flush()
        outputStream.close()
        pdfDocument.close()

        return file
    }
}
