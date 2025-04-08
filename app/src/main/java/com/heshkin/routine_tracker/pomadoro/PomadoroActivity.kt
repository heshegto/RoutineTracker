package com.heshkin.routine_tracker.pomadoro

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
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class PomadoroActivity : ComponentActivity() {
    //bases to remember in Shared Preferences
    private val WORK_TIME = "wt"
    private val REST_TIME = "rt"

    //fields of every base
    private val FIELD_HOURS = "hours"
    private val FIELD_MINUTES = "minutes"
    private val FIELD_SECONDS = "seconds"
    private val FIELD_IS_ACTIVE = "isActive"

    private val CHANNEL_ID = "timer_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pomadoro)

        createNotificationChannel()

        setUpInitialSharedPreferences(WORK_TIME, 25, true)
        setUpInitialSharedPreferences(REST_TIME, 5, false)

        val buttonEditWorkTime: Button = findViewById(R.id.editWorkTime)
        setTime(buttonEditWorkTime, WORK_TIME)
        buttonEditWorkTime.setOnClickListener { showPicker(buttonEditWorkTime, WORK_TIME) }

        val buttonEditChillTime: Button = findViewById(R.id.editChillTime)
        setTime(buttonEditChillTime, REST_TIME)
        buttonEditChillTime.setOnClickListener { showPicker(buttonEditChillTime, REST_TIME) }

        val buttonStartDaSHIT: Button = findViewById(R.id.PomadoroStart)
        buttonStartDaSHIT.setOnClickListener { startCountDown() }
    }

    private fun setUpInitialSharedPreferences(
        SPName: String,
        defaultMinutes: Int,
        isActive: Boolean
    ) {
        val sharedPreferences = getSharedPreferences(SPName, MODE_PRIVATE)
        if (sharedPreferences.all.isEmpty()
            or !sharedPreferences.contains(FIELD_HOURS)
            or !sharedPreferences.contains(FIELD_MINUTES)
            or !sharedPreferences.contains(FIELD_SECONDS)
            or !sharedPreferences.contains(FIELD_IS_ACTIVE)
        ) {
            val editor = sharedPreferences.edit()
            editor.putInt(FIELD_HOURS, 0)
            editor.putInt(FIELD_MINUTES, defaultMinutes)
            editor.putInt(FIELD_SECONDS, 0)
            editor.putBoolean(FIELD_IS_ACTIVE, isActive)
            editor.apply()
        }
    }

    private fun setTime(timeReceiver: Button, timeName: String) {
        val sharedPreferences = getSharedPreferences(timeName, MODE_PRIVATE)
        timeReceiver.setText(
            "%02d:%02d:%02d".format(
                sharedPreferences.getInt(FIELD_HOURS, 0),
                sharedPreferences.getInt(FIELD_MINUTES, 0),
                sharedPreferences.getInt(FIELD_SECONDS, 0),
            )
        )
        if (sharedPreferences.getBoolean(FIELD_IS_ACTIVE, false)) {
            timeReceiver.setBackgroundColor(getColor(R.color.green))
        } else {
            timeReceiver.setBackgroundColor(getColor(R.color.red))
        }
    }

    private fun showPicker(changeReceiver: Button, timeName: String) {
        val sharedPreferences = getSharedPreferences(timeName, MODE_PRIVATE)

        val mTimePicker = MyTimePickerDialog(
            this,
            { _, hours, minutes, seconds ->

                val editor = sharedPreferences.edit()
                editor.putInt(FIELD_HOURS, hours)
                editor.putInt(FIELD_MINUTES, minutes)
                editor.putInt(FIELD_SECONDS, seconds)
                editor.apply()

                changeReceiver.setText(
                    "%02d:%02d:%02d".format(
                        hours,
                        minutes,
                        seconds
                    )
                )
            },
            sharedPreferences.getInt(FIELD_HOURS, 0),
            sharedPreferences.getInt(FIELD_MINUTES, 0),
            sharedPreferences.getInt(FIELD_SECONDS, 0),
            true
        )
        mTimePicker.show()
    }

    private fun startCountDown() {
        val sharedPreferencesWork = getSharedPreferences(WORK_TIME, MODE_PRIVATE)
        val sharedPreferencesRest = getSharedPreferences(REST_TIME, MODE_PRIVATE)

        val sharedPreferences: SharedPreferences
        val timeButton: Button

        if (sharedPreferencesWork.getBoolean(FIELD_IS_ACTIVE, false)) {
            sharedPreferences = sharedPreferencesWork
            timeButton = findViewById(R.id.editWorkTime)
        } else {
            sharedPreferences = sharedPreferencesRest
            timeButton = findViewById(R.id.editChillTime)
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
                timeButton.setText(
                    "%02d:%02d:%02d".format(
                        hours,
                        minutes,
                        seconds
                    )
                )
            }

            override fun onFinish() {
                showNotification()
            }
        }.start()

    }

    private fun showNotification() {
        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Иконка уведомления
            .setContentTitle("Тайме завершён")
            .setContentText("Время истекло!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(notificationSound) // Добавляем звук
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        if (notificationManager ){//TODO}
        notificationManager.notify(1, builder.build()) // Отправляем уведомление
    }

    // Создаём канал уведомлений (для Android 8+)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Timer Notifications"
            val descriptionText = "Уведомления для таймера"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}