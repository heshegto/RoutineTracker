package com.heshkin.routine_tracker.pomadoro

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast

import androidx.activity.ComponentActivity
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import com.heshkin.routine_tracker.R


class PomadoroActivity : ComponentActivity() {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "Pomadoro_channel"
    }

    private var timer: CountDownTimer? = null
    private var timeButtonsArray: Array<TimeButton>? = null
    private var activeTimeButtonId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.pomadoro)

        createNotificationChannel()

        val workTimeButton: TimeButton = findViewById(R.id.editWorkTime)
        val restTimeButton: TimeButton = findViewById(R.id.editRestTime)

        timeButtonsArray = arrayOf(workTimeButton, restTimeButton)
        activeTimeButtonId = 0
        timeButtonsArray!![activeTimeButtonId!!].activate()
        val background = findViewById<LinearLayout>(R.id.Pomadoro)
        background.setBackgroundColor(timeButtonsArray!![activeTimeButtonId!!].mColorActive)

        val buttonPomadoroStart: Button = findViewById(R.id.PomadoroStart)
        val buttonPomadoroPause: Button = findViewById(R.id.PomadoroPause)
        val buttonPomadoroResume: Button = findViewById(R.id.PomadoroResume)
        val buttonPomadoroChange: ImageButton = findViewById(R.id.PomadoroChange)
        val buttonPomadoroStop: ImageButton = findViewById(R.id.PomadoroStop)

        buttonPomadoroStart.setOnClickListener {
            if (haveNotificationPermission()) {
                timeButtonsArray
                    ?: throw NoTimeButtons("No time buttons while button Start clicked")
                activeTimeButtonId ?: throw NoActiveTimeButtonException("No active time button")
                timeButtonsArray!![activeTimeButtonId!!].isClickable = false
                buttonPomadoroStart.visibility = Button.GONE
                buttonPomadoroPause.visibility = Button.VISIBLE
                buttonPomadoroResume.visibility = Button.GONE
                buttonPomadoroChange.visibility = ImageButton.GONE
                buttonPomadoroStop.visibility = ImageButton.VISIBLE
                startCountDown()
            }

        }

        buttonPomadoroPause.setOnClickListener {
            timer?.cancel() ?: throw NoTimerSetException("No timer set")
            buttonPomadoroStart.visibility = Button.GONE
            buttonPomadoroPause.visibility = Button.GONE
            buttonPomadoroResume.visibility = Button.VISIBLE
            buttonPomadoroChange.visibility = ImageButton.GONE
            buttonPomadoroStop.visibility = ImageButton.VISIBLE
        }

        buttonPomadoroResume.setOnClickListener {
            timeButtonsArray ?: throw NoTimeButtons("No time buttons while Resume button clicked")
            buttonPomadoroStart.visibility = Button.GONE
            buttonPomadoroPause.visibility = Button.VISIBLE
            buttonPomadoroResume.visibility = Button.GONE
            buttonPomadoroChange.visibility = ImageButton.GONE
            buttonPomadoroStop.visibility = ImageButton.VISIBLE
            startCountDown()
        }

        buttonPomadoroChange.setOnClickListener {
            changeActiveTimeButton()
        }

        buttonPomadoroStop.setOnClickListener {
            timeButtonsArray ?: throw NoActiveTimeButtonException("No active time button")
            activeTimeButtonId ?: throw NoActiveTimeButtonException("No active time button")
            timeButtonsArray!![activeTimeButtonId!!].restoreTime()
            timeButtonsArray!![activeTimeButtonId!!].isClickable = true
            timer!!.cancel()
            changeActiveTimeButton()
            buttonPomadoroStart.visibility = Button.VISIBLE
            buttonPomadoroPause.visibility = Button.GONE
            buttonPomadoroResume.visibility = Button.GONE
            buttonPomadoroChange.visibility = ImageButton.VISIBLE
            buttonPomadoroStop.visibility = ImageButton.GONE
        }
    }

    private fun changeActiveTimeButton() {
        val background = findViewById<LinearLayout>(R.id.Pomadoro)
        var flag = 0
        timeButtonsArray ?: throw NoActiveTimeButtonException("No active time button")
        val size = timeButtonsArray!!.size
        for (i in 0 until size) {
            if (timeButtonsArray!![i].mIsActive) {
                timeButtonsArray!![i].deactivate()
                flag = i
                break
            }
        }
        activeTimeButtonId = if (flag == size - 1) {
            0
        } else {
            flag + 1
        }
        timeButtonsArray!![activeTimeButtonId!!].activate()
        background.setBackgroundColor(timeButtonsArray!![activeTimeButtonId!!].mColorActive)
    }

    private fun startCountDown() {
        val timeButton: TimeButton = timeButtonsArray!![activeTimeButtonId!!]

        val timeToCount: Long = ((timeButton.mHours.toLong() * 60L +
                timeButton.mMinutes.toLong()) * 60L +
                timeButton.mSeconds.toLong()) * 1000L +
                timeButton.mMilliseconds.toLong()

        timer = object : CountDownTimer(timeToCount, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val hours = millisUntilFinished / 3_600_000L
                val minutes = millisUntilFinished % 3_600_000L / 60_000L
                val seconds = millisUntilFinished % 60_000L / 1000L
                val milliseconds = millisUntilFinished % 1000L

                timeButton.setTime(
                    hours.toInt(),
                    minutes.toInt(),
                    seconds.toInt(),
                    milliseconds.toInt()
                )
            }

            @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
            override fun onFinish() {
                showNotification()
                findViewById<ImageButton>(R.id.PomadoroStop).performClick()
            }
        }
        timer!!.start()
    }

    private fun haveNotificationPermission(): Boolean {
        if (
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            and
            (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                Toast.makeText(this, getString(R.string.ask_for_notification), Toast.LENGTH_SHORT)
                    .show()
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                }
                //intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    0
                )
            }
            return false
        }
        return true
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification() {
        val notificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.pomadoro_notification_title))
            .setContentText(getString(R.string.pomadoro_notification_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, PomadoroActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setSound(notificationSound)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(1, builder.build())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.pomadoro_notification_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = getString(R.string.pomadoro_notification_description) }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    class NoActiveTimeButtonException(message: String) : Exception(message)
    class NoTimerSetException(message: String) : Exception(message)
    class NoTimeButtons(message: String) : Exception(message)

}
