package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object ShareUtil {

    fun shareViaWhatsApp(context: Context, phone: String, message: String) {
        val cleanPhone = phone.replace(Regex("[^0-9+]"), "")
        val formattedPhone = if (cleanPhone.startsWith("0")) "92" + cleanPhone.substring(1) else cleanPhone
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    fun sharePdf(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "File does not exist", Toast.LENGTH_SHORT).show()
            return
        }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Share PDF Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to share PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPdf(context: Context, file: File) {
        if (!file.exists()) {
            Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
            return
        }
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_SHORT).show()
        }
    }
}
