package com.heshkin.routine_tracker.pomadoro

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import androidx.activity.ComponentActivity
import com.heshkin.routine_tracker.my_time_picker.MyTimePickerDialog
import com.heshkin.routine_tracker.R
import android.app.NotificationManager
import android.app.*
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit

class PomadoroActivity : ComponentActivity() {
    //bases to remember in Shared Preferences
    private val WORK_TIME = "work time"
    private val REST_TIME = "rest time"

    //fields of every base
    private val FIELD_HOURS = "hours"
    private val FIELD_MINUTES = "minutes"
    private val FIELD_SECONDS = "seconds"
    private val FIELD_IS_ACTIVE = "isActive"

    //notification chanel
    private val NOTIFICATION_CHANNEL_ID = "Pomadoro_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pomadoro)

        createNotificationChannel()

        setUpInitialSharedPreferences(WORK_TIME, defaultMinutes = 25, isActive = true)
        val buttonEditWorkTime: Button = findViewById(R.id.editWorkTime)
        setTime(buttonEditWorkTime, WORK_TIME)
        buttonEditWorkTime.setOnClickListener { showTimePicker(buttonEditWorkTime, WORK_TIME) }

        setUpInitialSharedPreferences(REST_TIME, defaultMinutes = 5, isActive = false)
        val buttonEditRestTime: Button = findViewById(R.id.editRestTime)
        setTime(buttonEditRestTime, REST_TIME)
        buttonEditRestTime.setOnClickListener { showTimePicker(buttonEditRestTime, REST_TIME) }

        val buttonStartPomadoro: Button = findViewById(R.id.PomadoroStart)
        buttonStartPomadoro.setOnClickListener { startCountDown() }
    }

    private fun setUpInitialSharedPreferences(
        sharedPreferencesName: String,
        defaultHours: Int = 0,
        defaultMinutes: Int = 0,
        defaultSeconds: Int = 0,
        isActive: Boolean = false
    ) {
        val SP = getSharedPreferences(sharedPreferencesName, MODE_PRIVATE)
        if (SP.all.isEmpty()
        ) {
            SP.edit {
                putInt(FIELD_HOURS, defaultHours)
                putInt(FIELD_MINUTES, defaultMinutes)
                putInt(FIELD_SECONDS, defaultSeconds)
                putBoolean(FIELD_IS_ACTIVE, isActive)
            }
        } else {
            // this part solving problems with missing data that can appear
            if (!SP.contains(FIELD_HOURS)) {
                SP.edit { putInt(FIELD_HOURS, defaultHours) }
            }
            if (!SP.contains(FIELD_MINUTES)) {
                SP.edit { putInt(FIELD_MINUTES, defaultMinutes) }
            }
            if (!SP.contains(FIELD_SECONDS)) {
                SP.edit { putInt(FIELD_SECONDS, defaultSeconds) }
            }
            if (!SP.contains(FIELD_IS_ACTIVE)) {
                SP.edit { putBoolean(FIELD_IS_ACTIVE, isActive) }
            }
        }
    }

    private fun setTime(timeReceiver: Button, timeName: String) {
        val SP = getSharedPreferences(timeName, MODE_PRIVATE)

        @SuppressLint("SetTextI18n")
        timeReceiver.text = "%02d:%02d:%02d".format(
            SP.getInt(FIELD_HOURS, 0),
            SP.getInt(FIELD_MINUTES, 0),
            SP.getInt(FIELD_SECONDS, 0),
        )
        if (SP.getBoolean(FIELD_IS_ACTIVE, false)) {
            timeReceiver.setBackgroundColor(getColor(R.color.green))
        } else {
            timeReceiver.setBackgroundColor(getColor(R.color.red))
        }
    }

    private fun showTimePicker(timeReceiver: Button, timeName: String) {
        val SP = getSharedPreferences(timeName, MODE_PRIVATE)

        MyTimePickerDialog(
            this,
            { _, hours, minutes, seconds ->

                SP.edit {
                    putInt(FIELD_HOURS, hours)
                    putInt(FIELD_MINUTES, minutes)
                    putInt(FIELD_SECONDS, seconds)
                }

                @SuppressLint("SetTextI18n")
                timeReceiver.text = "%02d:%02d:%02d".format(
                    hours,
                    minutes,
                    seconds
                )
            },
            SP.getInt(FIELD_HOURS, 0),
            SP.getInt(FIELD_MINUTES, 0),
            SP.getInt(FIELD_SECONDS, 0),
            true
        ).show()
    }

    private fun startCountDown() {
        if (
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            or
            (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            val sharedPreferencesWork = getSharedPreferences(WORK_TIME, MODE_PRIVATE)
            val sharedPreferencesRest = getSharedPreferences(REST_TIME, MODE_PRIVATE)

            val sharedPreferences: SharedPreferences
            val timeButton: Button

            if (sharedPreferencesWork.getBoolean(FIELD_IS_ACTIVE, false)) {
                sharedPreferences = sharedPreferencesWork
                timeButton = findViewById(R.id.editWorkTime)
            } else {
                sharedPreferences = sharedPreferencesRest
                timeButton = findViewById(R.id.editRestTime)
            }

            val timeToCount: Long = (((sharedPreferences.getInt(FIELD_HOURS, 0).toLong() * 60L +
                    sharedPreferences.getInt(FIELD_MINUTES, 0).toLong()) * 60L +
                    sharedPreferences.getInt(FIELD_SECONDS, 0).toLong()) * 1000L)

            object : CountDownTimer(timeToCount, 1000) {
                // 10 секунд, тикает каждую секунду
                override fun onTick(millisUntilFinished: Long) {
                    val hours = millisUntilFinished / 3_600_000L
                    val minutes = millisUntilFinished / 60_000L
                    val seconds = millisUntilFinished / 1000L

                    @SuppressLint("SetTextI18n")
                    timeButton.text = "%02d:%02d:%02d".format(
                        hours,
                        minutes,
                        seconds
                    )
                }

                @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
                override fun onFinish() {
                    showNotification()
                }
            }.start()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS),0)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification() {
        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Иконка уведомления
            .setContentTitle("Тайме завершён")
            .setContentText("Время истекло!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(notificationSound) // Добавляем звук
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(1, builder.build()) // Отправляем уведомление
    }

    // Создаём канал уведомлений (для Android 13+)
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
}