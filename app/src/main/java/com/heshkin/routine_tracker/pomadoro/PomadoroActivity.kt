package com.heshkin.routine_tracker.pomadoro

import android.Manifest
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.app.NotificationManager
import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build

import androidx.activity.ComponentActivity
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.heshkin.routine_tracker.R


class PomadoroActivity : ComponentActivity() {
    private val NOTIFICATION_CHANNEL_ID = "Pomadoro_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pomadoro)

        createNotificationChannel()

        val buttonEditWorkTime: TimeButton = findViewById(R.id.editWorkTime)
        buttonEditWorkTime.activate()
        val buttonEditRestTime: TimeButton = findViewById(R.id.editRestTime)

        val buttonStartPomadoro: Button = findViewById(R.id.PomadoroStart)
        buttonStartPomadoro.setOnClickListener { startCountDown(buttonEditWorkTime, buttonEditRestTime) }
    }

    private fun startCountDown(vararg timeButtonList: TimeButton)  {
        if (
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            or
            (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            var timeButton: TimeButton? = null
            for (_timeButton in timeButtonList) {
                if (_timeButton.mIsActive) {
                    timeButton = _timeButton
                }
            }
            if (timeButton == null) {
                throw NoActiveTimeButtonException("No active time button")
            }

            val timeToCount: Long = ((timeButton.mHours.toLong() * 60L +
                    timeButton.mMinutes.toLong()) * 60L +
                    timeButton.mSeconds.toLong()) * 1000L

            object : CountDownTimer(timeToCount, 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    val hours = millisUntilFinished / 3_600_000L
                    val minutes = millisUntilFinished / 60_000L
                    val seconds = millisUntilFinished / 1000L

                    timeButton.setTime(hours.toInt(), minutes.toInt(), seconds.toInt())
                }
                @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
                override fun onFinish() {
                    showNotification()
                }
            }.start()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification() {
        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Тайме завершён")
            .setContentText("Время истекло!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(notificationSound)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(1, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Timer Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {description = "Уведомления для таймера"}
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    class NoActiveTimeButtonException(message: String) : Exception(message)
}
