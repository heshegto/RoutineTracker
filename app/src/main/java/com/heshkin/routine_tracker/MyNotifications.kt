package com.heshkin.routine_tracker

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast

import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Class with function to operate with notifications.
 *
 * Check permission, create notification channels, create notifications and show them.
 */
object MyNotifications {
    /** This importance allows notification to make noise and visually distract user */
    const val IMPORTANCE_HIGH = NotificationManager.IMPORTANCE_HIGH

    /** This importance does NOT allow your notification to make noise or visually distract user */
    const val IMPORTANCE_LOW = NotificationManager.IMPORTANCE_LOW

    /** General function for android versions check */
    private fun isVersion(version: Int): Boolean = Build.VERSION.SDK_INT >= version

    /** Function to check if version is Tiramisu */
    private fun isVersionTiramisu(): Boolean = isVersion(Build.VERSION_CODES.TIRAMISU)

    /** Function to check if version is Oreo */
    private fun isVersionOreo(): Boolean = isVersion(Build.VERSION_CODES.O)

    /**
     * Creates a Notification Channel
     *
     * Can be used before notification permission is granted
     *
     * If android version lower than Oreo, this function does nothing
     *
     * @param context The context of the app.
     * @param channelId The id of the notification channel.
     * @param channelName The name of the notification channel. That will be shown in settings for user.
     * @param channelDescription The description of the notification channel. That will be shown in settings for user.
     * @param importance The importance of the notification channel.
     * Can be [IMPORTANCE_HIGH] or [IMPORTANCE_LOW].
     * Or any other importance level from [NotificationManager]
     */
    fun createNotificationChannel(
        context: Context,
        channelId: String,
        channelName: String,
        channelDescription: String,
        importance: Int
    ) {
        // Check android version here so we don't think about versions in other parts of app
        if (isVersionOreo()) {
            val channel = NotificationChannel(
                channelId, channelName, importance
            ).apply {
                description = channelDescription
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.v("Notification", "Notification channel $channelName created")
        }
    }

    /**
     * Checks if the app has the permission to send notifications.
     *
     * @param context The context of the app.
     *
     * @return true if the permission is granted, false otherwise.
     */
    fun checkNotificationPermission(context: Context): Boolean {
        // Check android version here so we don't think about versions in other parts of app
        if (isVersionTiramisu()) {
            Log.v("Notification", "Checking notification permission")
            return ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        Log.v("Notification", "No need to check notification permission. Version < 33")
        return true
    }

    /**
     * Asks for notification permission from users or sends them to the settings.
     * Android has restriction on how many times the permission request dialog can be shown.
     * It's 3 times. If in dialog user refused to give notification permission in the dialog 3 times,
     * then Notification Settings is shown.
     *
     * @param activity Required to show dialog on it and to serve as the context for intent
     * to show Notification Settings
     * */
    fun askNotificationPermission(activity: Activity) {
        // Check android version here so we don't think about versions in other parts of app
        if (isVersionTiramisu()) {
            Log.v("Notification", "Check if permission dialog can be shown")
            if (
            //Check if permission dialog can be shown
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity, Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                openNotificationSettings(activity) //if not
            } else {
                showNotificationPermissionDialog(activity) //if yes
            }
        }
    }

    /**
     * Opens the notification settings for the app.
     *
     * @param context The context of the app.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun openNotificationSettings(context: Context) {
        Log.v("Notification", "Opening notification settings")
        Toast.makeText(
            context, context.getString(R.string.ask_for_notification), Toast.LENGTH_SHORT
        ).show()
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        }
        context.startActivity(intent)
    }

    /**
     * Shows notification permission dialog on an activity.
     *
     * @param activity The activity of the app where the dialog must be shown.
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun showNotificationPermissionDialog(activity: Activity) {
        Log.v("Notification", "Showing notification permission dialog")
        ActivityCompat.requestPermissions(
            activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0
        )
    }

    /**
     * Shows notification on Status Bar and Notification Shade
     *
     * @param context The context of the app. Required to get the Notification Manager
     * @param notificationId The id of the notification. If the id is different,
     * new notification will be shown in a new place
     * @param notification The notification to be shown. Can be created with [createNotification]
     * or [createNotificationUpdate]
     */
    fun showNotification(context: Context, notificationId: Int, notification: Notification) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, notification)
    }

    /**
     * Creates a notification object with a title, text, and click action.
     *
     * @param context Context for the builder.
     * @param notificationChannelId The channel ID to post to.
     * @param contentTitle The title of the notification.
     * @param contentText The body text of the notification.
     * @param notificationIntent The intent to launch when the notification is clicked.
     * @return A built Notification object.
     */
    fun createNotification(
        context: Context,
        notificationChannelId: String,
        contentTitle: String,
        contentText: String,
        notificationIntent: Intent
    ): Notification {
        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle(contentTitle)
            .setContentText(contentText).setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(notificationSound)
            .setVibrate(longArrayOf(1000, 1000, 1000))
            .setAutoCancel(true)
            .build()
    }

    /**
     * Creates a silent notification object with a title, text, and click action.
     *
     * @param context Context for the builder.
     * @param notificationChannelId The channel ID to post to.
     * @param contentTitle The title of the notification.
     * @param contentText The body text of the notification.
     * @param notificationIntent The intent to launch when the notification is clicked.
     * @return A built Notification object.
     */
    fun createNotificationUpdate(
        context: Context,
        notificationChannelId: String,
        contentTitle: String,
        contentText: String,
        notificationIntent: Intent
    ): Notification {
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, notificationChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setAutoCancel(true)
            .setSound(null)
            .setVibrate(null)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_DEFERRED)
            .setOngoing(true)
            .build()
    }
}
