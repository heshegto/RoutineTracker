package com.heshkin.routine_tracker.pomadoro

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * @property timeOnPaused Saves time when timer is paused. This instance isn't in [PomadoroFragment]
 * because it's lifecycle is too short; this instance isn't in [PomadoroService] because it's lifecycle
 * is too long
 * @property timerState Contains state of timer that is started in [PomadoroFragment]. This has only
 * one role in the program - to be transitional point between state flow in [PomadoroService] and
 * its usage in [PomadoroFragment]
*/
class PomadoroViewModel(application: Application) : AndroidViewModel(application) {

    private var serviceBinder: PomadoroService.PomadoroServiceBinder? = null
    private var isBound = false
    var timeOnPaused: Long= 0
    private var serviceTimerState: MutableStateFlow<PomadoroService.TimerState> =
        MutableStateFlow(PomadoroService.TimerState())
    val timerState: StateFlow<PomadoroService.TimerState> = serviceTimerState.asStateFlow()


    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as PomadoroService.PomadoroServiceBinder
            serviceBinder = binder
            isBound = true

            Log.d("PomadoroViewModel", "Service Connected")
            viewModelScope.launch {
                binder.getService().timerState.collect { state ->
                    serviceTimerState.value = state
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            serviceBinder = null
            Log.d("PomadoroViewModel", "Service Disconnected")
        }
    }

    init {
        val intent = Intent(application, PomadoroService::class.java)
        application.startService(intent)
        application.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }
    fun startTimer(timeToCount: Long, activeTimeButtonId: Int) {
        Log.d("PomadoroViewModel", "startTimer")
        serviceBinder?.getService()?.startTimer(timeToCount, activeTimeButtonId) ?: Log.e("PomadoroViewModel", "ViewModel/startTimer error. No service")
    }

    fun pauseTimer() {
        serviceBinder ?: Log.e("PomadoroViewModel", "ViewModel/pauseTimer error. No service")
        timeOnPaused = serviceBinder?.getService()!!.pauseTimer()
    }

    fun stopTimer() {
        timeOnPaused = 0
        serviceBinder?.getService()?.stopTimer() ?: Log.e("PomadoroViewModel", "ViewModel/stopTimer error. No service")
    }

    override fun onCleared() {
        super.onCleared()
        if (isBound) {
            getApplication<Application>().unbindService(connection)
            isBound = false
        }
        if (!timerState.value.isRunning) {
            getApplication<Application>().stopService(Intent(getApplication(), PomadoroService::class.java))
        }
    }
}
