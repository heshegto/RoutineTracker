package com.heshkin.routine_tracker.pomadoro

import android.Manifest
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.app.NotificationManager
import android.app.NotificationChannel
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.widget.ImageButton

import androidx.activity.ComponentActivity
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.heshkin.routine_tracker.R


class PomadoroActivity : ComponentActivity() {
    private val NOTIFICATION_CHANNEL_ID = "Pomadoro_channel"
    private var timer: MyCountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pomadoro)

        createNotificationChannel()

        val buttonEditWorkTime: TimeButton = findViewById(R.id.editWorkTime)
        buttonEditWorkTime.activate()
        val buttonEditRestTime: TimeButton = findViewById(R.id.editRestTime)

        val buttonStartPomadoro: Button = findViewById(R.id.PomadoroStart)
        val buttonPausePomadoro: Button = findViewById(R.id.PomadoroPause)
        val buttonResumePomadoro: Button = findViewById(R.id.PomadoroResume)
        val buttonChangePomadoro: ImageButton = findViewById(R.id.PomadoroChange)
        val buttonStopPomadoro: ImageButton = findViewById(R.id.PomadoroStop)

        buttonStartPomadoro.setOnClickListener {
            startCountDown(buttonEditWorkTime, buttonEditRestTime)
            timer?.timeButton?.isClickable = false
            buttonStartPomadoro.visibility = Button.GONE
            buttonChangePomadoro.visibility = ImageButton.GONE
            buttonPausePomadoro.visibility = Button.VISIBLE
            buttonStopPomadoro.visibility = ImageButton.VISIBLE

        }

        buttonPausePomadoro.setOnClickListener {
            timer?.cancel()
            buttonPausePomadoro.visibility = Button.GONE
            buttonResumePomadoro.visibility = Button.VISIBLE
        }

        buttonResumePomadoro.setOnClickListener {
            startCountDown(buttonEditWorkTime, buttonEditRestTime)
            buttonResumePomadoro.visibility = Button.GONE
            buttonPausePomadoro.visibility = Button.VISIBLE
        }

        buttonChangePomadoro.setOnClickListener {
            changeActiveTimeButton(buttonEditWorkTime, buttonEditRestTime)
        }

        buttonStopPomadoro.setOnClickListener {
            timer?.timeButton?.restoreTime()
            timer?.timeButton?.isClickable = true
            timer?.cancel()
            buttonStartPomadoro.visibility = Button.VISIBLE
            buttonChangePomadoro.visibility = ImageButton.VISIBLE
            buttonPausePomadoro.visibility = Button.GONE
            buttonResumePomadoro.visibility = Button.GONE
            buttonStopPomadoro.visibility = ImageButton.GONE
        }


    }

    private fun changeActiveTimeButton(vararg timeButtonList: TimeButton) {
        var flag = 0
        val size = timeButtonList.size
        for (i in 0 until size) {
            if (timeButtonList[i].mIsActive) {
                timeButtonList[i].deactivate()
                flag = i
                break
            }
        }
        if (flag == size - 1) {
            timeButtonList[0].activate()
        } else {
            timeButtonList[flag + 1].activate()
        }
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
                    timeButton.mSeconds.toLong()) * 1000L +
                    timeButton.mMiliseconds.toLong()

            timer = MyCountDownTimer(timeToCount, timeButton, this)
            timer?.start()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                0
            )
        }
    }

    private open class MyCountDownTimer(
        timeToCount: Long,
        val timeButton: TimeButton,
        private val parent: PomadoroActivity
    ) : CountDownTimer(timeToCount, 1000) {
        override fun onTick(millisUntilFinished: Long) {
            val hours = millisUntilFinished / 3_600_000L
            val minutes = millisUntilFinished % 3_600_000L / 60_000L
            val seconds = millisUntilFinished % 60_000L / 1000L
            val miliseconds = millisUntilFinished % 1000L

            timeButton.setTime(hours.toInt(), minutes.toInt(), seconds.toInt(), miliseconds.toInt())
        }
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        override fun onFinish() {
            parent.showNotification()
            parent.findViewById<ImageButton>(R.id.PomadoroStop).performClick()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification() {
        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
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
