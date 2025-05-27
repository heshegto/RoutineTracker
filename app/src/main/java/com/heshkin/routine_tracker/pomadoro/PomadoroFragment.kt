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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast

import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment

import com.heshkin.routine_tracker.R


class PomadoroFragment : Fragment(R.layout.fragment_pomadoro) {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "Pomadoro_channel"
    }

    private var timer: CountDownTimer? = null
    private var timeButtonsArray: Array<TimeButton>? = null
    private var activeTimeButtonId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("PomadoroFragment", "Зашли в блок onCreateView")

        return inflater.inflate(R.layout.fragment_pomadoro, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("PomadoroFragment", "Зашли в блок onCreate")
        super.onCreate(savedInstanceState)

        createNotificationChannel()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val workTimeButton: TimeButton = requireView().findViewById(R.id.workTime)
        val restTimeButton: TimeButton = requireView().findViewById(R.id.restTime)

        timeButtonsArray = arrayOf(workTimeButton, restTimeButton)
        activeTimeButtonId = 0
        timeButtonsArray!![activeTimeButtonId!!].activate()
        val background = requireView().findViewById<LinearLayout>(R.id.Pomadoro)
        background.setBackgroundColor(timeButtonsArray!![activeTimeButtonId!!].mColorActive)

        val buttonStart: Button = requireView().findViewById(R.id.PomadoroStart)
        val buttonPause: Button = requireView().findViewById(R.id.PomadoroPause)
        val buttonResume: Button = requireView().findViewById(R.id.PomadoroResume)
        val buttonChange: ImageButton = requireView().findViewById(R.id.PomadoroChange)
        val buttonStop: ImageButton = requireView().findViewById(R.id.PomadoroStop)
        val buttonArray = arrayOf(buttonStart, buttonPause, buttonResume, buttonChange, buttonStop)

        buttonStart.setOnClickListener {
            if (haveNotificationPermission()) {
                timeButtonsArray
                    ?: throw NoTimeButtons("No time buttons while button Start clicked")
                activeTimeButtonId ?: throw NoActiveTimeButtonException("No active time button")
                timeButtonsArray!![activeTimeButtonId!!].isClickable = false
                makeButtonsVisible(buttonArray, arrayOf(1, 4))
                startCountDown()
            }
        }

        buttonPause.setOnClickListener {
            timer?.cancel() ?: throw NoTimerSetException("No timer set")
            makeButtonsVisible(buttonArray, arrayOf(2, 4))
        }

        buttonResume.setOnClickListener {
            timeButtonsArray ?: throw NoTimeButtons("No time buttons while Resume button clicked")
            makeButtonsVisible(buttonArray, arrayOf(1, 4))
            startCountDown()
        }

        buttonChange.setOnClickListener {
            changeActiveTimeButton()
        }

        buttonStop.setOnClickListener {
            timeButtonsArray ?: throw NoActiveTimeButtonException("No active time button")
            activeTimeButtonId ?: throw NoActiveTimeButtonException("No active time button")
            timeButtonsArray!![activeTimeButtonId!!].restoreTime()
            timeButtonsArray!![activeTimeButtonId!!].isClickable = true
            timer!!.cancel()
            changeActiveTimeButton()
            makeButtonsVisible(buttonArray, arrayOf(0, 3))
        }
    }

    private fun makeButtonsVisible(buttons: Array<View>, indexes: Array<Int>) {
        for (i in buttons.indices) {
            if (i in indexes) {
                buttons[i].visibility = Button.VISIBLE
            } else {
                buttons[i].visibility = Button.GONE
            }
        }
    }

    private fun changeActiveTimeButton() {
        val background = requireView().findViewById<LinearLayout>(R.id.Pomadoro)
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
                requireView().findViewById<ImageButton>(R.id.PomadoroStop).performClick()
            }
        }
        timer!!.start()
    }

    private fun haveNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true
        }
        if (
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (
                ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(),
                    Manifest.permission.POST_NOTIFICATIONS
                )
            ) {
                Toast.makeText(requireContext(), getString(R.string.ask_for_notification), Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, requireActivity().packageName)
                }
                startActivity(intent)
            } else {
                ActivityCompat.requestPermissions(
                    requireActivity(),
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

        val builder = NotificationCompat.Builder(requireContext(), NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(getString(R.string.pomadoro_notification_title))
            .setContentText(getString(R.string.pomadoro_notification_text))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(
                PendingIntent.getActivity(
                    requireContext(),
                    0,
                    Intent(requireContext(), PomadoroFragment::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setSound(notificationSound)
            .setAutoCancel(true)

        val notificationManager = NotificationManagerCompat.from(requireContext())
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
                requireActivity().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    class NoActiveTimeButtonException(message: String) : Exception(message)
    class NoTimerSetException(message: String) : Exception(message)
    class NoTimeButtons(message: String) : Exception(message)

}
