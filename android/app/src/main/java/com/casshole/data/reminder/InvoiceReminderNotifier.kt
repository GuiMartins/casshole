package com.casshole.data.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.casshole.MainActivity
import com.casshole.R
import com.casshole.ui.theme.GreenPrimary

object InvoiceReminderNotifier {

    /** Tapping the notification opens the app with this action - MainActivity then fetches
     * e-mail, since the invoice the reminder is about has probably just arrived. */
    const val ACTION_OPEN_FROM_REMINDER = "com.casshole.action.OPEN_FROM_INVOICE_REMINDER"

    private const val CHANNEL_ID = "invoice_reminders"
    private const val NOTIFICATION_TAG_PREFIX = "invoice-reminder:"
    private const val NOTIFICATION_ID = 1

    /** Safe to call repeatedly: re-creating an existing channel only refreshes its name and
     * description (e.g. after a language change), never the user's own settings for it. */
    fun createChannel(context: Context) {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.reminder_channel_name))
            .setDescription(context.getString(R.string.reminder_channel_description))
            .build()
        NotificationManagerCompat.from(context).createNotificationChannel(channel)
    }

    fun needsPermissionRequest(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    /** Whether a reminder would actually show up: permission granted (Android 13+), app
     * notifications on, and this channel not switched off in the system settings. */
    fun canNotify(context: Context): Boolean {
        if (needsPermissionRequest(context)) return false
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        val channel = manager.getNotificationChannelCompat(CHANNEL_ID) ?: return true
        return channel.importance != NotificationManagerCompat.IMPORTANCE_NONE
    }

    fun notify(context: Context, card: ReminderCard, nickname: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_FROM_REMINDER)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val text = context.getString(R.string.reminder_text, nickname ?: cardLabel(context, card))
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_invoice)
            .setColor(GreenPrimary.toArgb())
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        // One notification per card (tag), replaced rather than stacked if it fires again.
        NotificationManagerCompat.from(context)
            .notify("$NOTIFICATION_TAG_PREFIX${card.bank}:${card.card}", NOTIFICATION_ID, notification)
    }

    /** Same fallback as the card tiles in "Faturas por cartão" when there's no nickname. */
    private fun cardLabel(context: Context, card: ReminderCard): String {
        val bank = card.bank.replaceFirstChar { it.uppercase() }
        return if (card.card.isBlank()) bank else "$bank ${context.getString(R.string.reminder_card_suffix, card.card)}"
    }
}
