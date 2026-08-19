package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.entity.DebtorEntity
import com.example.data.entity.ParcelEntity
import com.example.data.entity.RideEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareUtil {

    fun sharePdfReport(context: Context, pdfFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Aqeel Rider Report / عقیل رائڈر رپورٹ")
            putExtra(Intent.EXTRA_TEXT, "Assalam-o-Alaikum! Here is the Aqeel Rider Report PDF document: ${pdfFile.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        // Target WhatsApp if available, or show chooser
        val chooser = Intent.createChooser(intent, "Share Aqeel Rider PDF Report / واٹس ایپ شیئر کریں")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareCustomerPdfReport(context: Context, pdfFile: File, customerName: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Aqeel Rider - $customerName Hisab Report")
            putExtra(Intent.EXTRA_TEXT, "Assalam-o-Alaikum! $customerName bhai, yahan aap ka Aqeel Rider account hisab report PDF document hai.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(intent, "Share $customerName PDF Report / واٹس ایپ پر بھیجیں")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareSummaryText(
        context: Context,
        selectedDateMillis: Long,
        rides: List<RideEntity>,
        parcels: List<ParcelEntity>,
        debtors: List<DebtorEntity>
    ) {
        val dateStr = SimpleDateFormat("dd-MMM-yyyy", Locale.US).format(Date(selectedDateMillis))
        val totalRides = rides.size
        val totalKm = rides.sumOf { it.distanceKm }
        val totalFare = rides.sumOf { it.fareAmount }
        val parcelMoney = parcels.filter { it.isPaid }.sumOf { it.deliveryCharges }
        val totalEarning = totalFare + parcelMoney
        val totalDebt = debtors.filter { it.totalDebt > 0 }.sumOf { it.totalDebt }

        val textBuilder = StringBuilder()
        textBuilder.append("🟢 *AQEEL RIDER REPORT / عقیل رائڈر رپورٹ*\n")
        textBuilder.append("📅 Date: $dateStr\n\n")
        textBuilder.append("🚗 Total Rides: $totalRides\n")
        textBuilder.append("📍 Total KM: ${String.format("%.1f", totalKm)} KM\n")
        textBuilder.append("💵 Total Earning: Rs. ${totalEarning.toInt()}\n")
        textBuilder.append("📦 Parcel Earnings: Rs. ${parcelMoney.toInt()}\n")
        textBuilder.append("⚠️ Total Pending Debt: Rs. ${totalDebt.toInt()}\n\n")
        textBuilder.append("Shared via Aqeel Rider App 📱")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
        }

        val chooser = Intent.createChooser(intent, "Share via WhatsApp / واٹس ایپ پر بھیجیں")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun sendWhatsAppReminderToDebtor(context: Context, debtor: DebtorEntity) {
        val message = "Assalam-o-Alaikum ${debtor.name} bhai! Aap ke taraf Aqeel Rider ke total Rs. ${debtor.totalDebt.toInt()} baqi hain. Baraye meherbani jald se jald ada kar dein. Shukriya!"
        val cleanPhone = debtor.phoneNumber.replace("[^0-9]".toRegex(), "")

        try {
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // General share fallback
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            val chooser = Intent.createChooser(intent, "Send Reminder / ریمائنڈر بھیجیں")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        }
    }
}
