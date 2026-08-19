package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.entity.DebtorEntity

object NotificationHelper {

    private const val CHANNEL_ID = "overdue_debt_channel"
    private const val CHANNEL_NAME = "Overdue Debt Reminders / قرضہ ریمائنڈر"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for debts older than 7 days"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun sendOverdueNotification(context: Context, debtor: DebtorEntity) {
        createNotificationChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = debtor.id.toInt()
        val title = "Qarza Reminder / قرضہ یاددہانی ⚠️"
        val message = "${debtor.name} se Rs. ${debtor.totalDebt.toInt()} lene hain (7 din se zyada ho gaye)"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        manager.notify(notificationId, builder.build())
    }

    fun isOverdue(debtor: DebtorEntity): Boolean {
        if (debtor.totalDebt <= 0) return false
        val sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000
        val currentTime = System.currentTimeMillis()
        val debtAge = currentTime - debtor.createdTimestamp
        return debtAge >= sevenDaysInMillis
    }
}
