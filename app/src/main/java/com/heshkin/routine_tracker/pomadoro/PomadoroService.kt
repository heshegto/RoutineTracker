package com.heshkin.routine_tracker.pomadoro

import android.annotation.SuppressLint
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import android.util.Log

import com.heshkin.routine_tracker.MainActivity
import com.heshkin.routine_tracker.R
import com.heshkin.routine_tracker.pomadoro.TimeButton.TimeValue
import com.heshkin.routine_tracker.MyNotifications

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** @property timerState Contains state of timer that is started in [PomadoroFragment].*/
class PomadoroService : Service() {
    val timerState: MutableStateFlow<TimerState> = MutableStateFlow(TimerState())
    private var timer: CountDownTimer? = null

    companion object {
        const val SILENT_NOTIFICATION_CHANNEL_ID = "Pomadoro_silent_channel"
        const val LOUD_NOTIFICATION_CHANNEL_ID = "Pomadoro_loud_channel"

        private const val NOTIFICATION_ID = 1
    }

    override fun startService(service: Intent?): ComponentName? {
        Log.d("PomadoroService", "startService")
        return super.startService(service)
    }

    fun startTimer(timeToCount: Long, activeTimeButtonId: Int) {
        timerState.update { it.copy(isRunning = true, activeTimeButtonId = activeTimeButtonId) }

        timer = object : CountDownTimer(timeToCount, 1000) {
            val intent = Intent(this@PomadoroService, MainActivity::class.java)
                .putExtra("fragment", "pomadoro")

            override fun onTick(millisUntilFinished: Long) {
                timerState.update { it.copy(secondsLeft = millisUntilFinished) }
                val time: TimeValue = TimeValue.breakTime(timerState.value.secondsLeft / 1000)
                MyNotifications.showNotification(
                    this@PomadoroService,
                    NOTIFICATION_ID,

                    MyNotifications.createNotificationUpdate(
                        this@PomadoroService,
                        SILENT_NOTIFICATION_CHANNEL_ID,
                        getString(R.string.pomadoro_label),
                        getString(R.string.pomadoro_time_left) + (
                                "%02d:%02d:%02d".format(
                                    time.hours,
                                    time.minutes,
                                    time.seconds
                                )
                                ),
                        intent
                    )
                )
            }

            override fun onFinish() {
                MyNotifications.showNotification(
                    this@PomadoroService,
                    NOTIFICATION_ID,

                    MyNotifications.createNotification(
                        this@PomadoroService,
                        LOUD_NOTIFICATION_CHANNEL_ID,
                        getString(R.string.pomadoro_notification_title),
                        getString(R.string.pomadoro_notification_text),
                        intent
                    )
                )
                timerState.update { it.copy(secondsLeft = 0, isRunning = false) }
            }
        }
        timer!!.start()
        Log.d("PomadoroService", "startTimer")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MyNotifications.createNotificationChannel(
            this@PomadoroService,
            SILENT_NOTIFICATION_CHANNEL_ID,
            getString(R.string.pomadoro_label),
            getString(R.string.pomadoro_notification_description_silent),
            MyNotifications.IMPORTANCE_LOW
        )

        MyNotifications.createNotificationChannel(
            this@PomadoroService,
            LOUD_NOTIFICATION_CHANNEL_ID,
            getString(R.string.pomadoro_label),
            getString(R.string.pomadoro_notification_description_loud),
            MyNotifications.IMPORTANCE_HIGH
        )
        return START_STICKY
    }

    fun pauseTimer(): Long {
        timer?.cancel()
        return timerState.value.secondsLeft
    }

    @SuppressLint("MissingPermission")
    fun stopTimer() {
        timer?.cancel()
        timerState.update { it.copy(isRunning = false, secondsLeft = 0) }
        val intent = Intent(this@PomadoroService, MainActivity::class.java)
            .putExtra("fragment", "pomadoro")
        MyNotifications.showNotification(
            this@PomadoroService,
            NOTIFICATION_ID,

            MyNotifications.createNotification(
                this@PomadoroService,
                SILENT_NOTIFICATION_CHANNEL_ID,
                getString(R.string.pomadoro_notification_title),
                getString(R.string.pomadoro_notification_text),
                intent
            )
        )
    }

    override fun onDestroy() {
        timer?.cancel()
        super.onDestroy()
    }

    /** Support class to store timer state */
    data class TimerState(
        val secondsLeft: Long = 0,
        val isRunning: Boolean = false,
        val activeTimeButtonId: Int = 0
    )

    /** Binding stuff */
    override fun onBind(intent: Intent?): IBinder = binder
    private val binder = PomadoroServiceBinder()
    inner class PomadoroServiceBinder: Binder() { fun getService(): PomadoroService = this@PomadoroService }
}
