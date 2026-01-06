package com.heshkin.routine_tracker.pomadoro

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout

import androidx.core.content.ContextCompat.getColor
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle

import com.heshkin.routine_tracker.MainActivity
import com.heshkin.routine_tracker.MyNotifications
import com.heshkin.routine_tracker.R

import kotlinx.coroutines.launch

/**
 * @property timeButtonsArray Contains [TimeButton] objects on fragment view
 * @property buttonArray Contains action buttons on fragment view (Start, Pause, Resume, Change, Stop)
 * @property activeTimeButtonId Index of active [TimeButton]  in [timeButtonsArray].
 * @property isRunningOnThisFragment Required to make less UI changes
*/
class PomadoroFragment : Fragment(R.layout.fragment_pomadoro) {
    companion object {
        fun newInstance() = PomadoroFragment()
    }

    private lateinit var timeButtonsArray: Array<TimeButton>
    private lateinit var buttonArray: Array<View>
    private  var activeTimeButtonId: Int = 0

    private var isRunningOnThisFragment = false // Required to make less UI changes
    private val viewModel: PomadoroViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pomadoro, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("PomadoroFragment", "View created")

        super.onViewCreated(view, savedInstanceState)
        isRunningOnThisFragment = false

        val workTimeButton: TimeButton = requireView().findViewById(R.id.workTime)
        val restTimeButton: TimeButton = requireView().findViewById(R.id.restTime)

        val buttonStart: Button = requireView().findViewById(R.id.PomadoroStart)
        val buttonPause: Button = requireView().findViewById(R.id.PomadoroPause)
        val buttonResume: Button = requireView().findViewById(R.id.PomadoroResume)
        val buttonChange: ImageButton = requireView().findViewById(R.id.PomadoroChange)
        val buttonStop: ImageButton = requireView().findViewById(R.id.PomadoroStop)

        timeButtonsArray = arrayOf(workTimeButton, restTimeButton)
        buttonArray = arrayOf(buttonStart, buttonPause, buttonResume, buttonChange, buttonStop)

        buttonStart.setOnClickListener { clickStart() }
        buttonPause.setOnClickListener { clickPause() }
        buttonResume.setOnClickListener { clickResume() }
        buttonChange.setOnClickListener { clickChangeActiveTime() }
        buttonStop.setOnClickListener { clickStop() }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.timerState.collect { state ->
                    if (state.isRunning) {
                        if (!isRunningOnThisFragment) {
                            activeTimeButtonId = state.activeTimeButtonId
                            setUpColors(activeTimeButtonId)
                            makeButtonsVisible( arrayOf(1, 4))
                            isRunningOnThisFragment = true
                        }
                        val timeButton = timeButtonsArray[activeTimeButtonId]
                        timeButton.setTimeOnView((state.secondsLeft / 1000).toInt())
                    } else {
                        setUpColors(activeTimeButtonId)
                        if (isRunningOnThisFragment) clickStop()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        /** This thing repairs bug that changes background color of [R.id.PomadoroTable] */
        val pomadoroTable = requireView().findViewById<View>(R.id.PomadoroTable)
        val newColor = getColor(requireContext(), R.color.white)
        (pomadoroTable.background.mutate() as GradientDrawable).setColor(newColor)
    }

    private fun clickStart() {
        if (MyNotifications.checkNotificationPermission(this.requireContext())) {
            timeButtonsArray[activeTimeButtonId].isClickable = false
            makeButtonsVisible(arrayOf(1, 4))
            viewModel.startTimer(timeButtonsArray[activeTimeButtonId].getInitTime() * 1000L, activeTimeButtonId)
        } else {
            MyNotifications.askNotificationPermission(activity as MainActivity)
        }
    }
    private fun clickPause() {
        makeButtonsVisible(arrayOf(2, 4))
        viewModel.pauseTimer()
    }
    private fun clickResume() {
        makeButtonsVisible(arrayOf(1, 4))
        viewModel.startTimer(viewModel.timeOnPaused, activeTimeButtonId)
    }
    private fun clickChangeActiveTime() {
        val size = timeButtonsArray.size
        activeTimeButtonId = if (activeTimeButtonId == size - 1) 0 else activeTimeButtonId + 1
        setUpColors(activeTimeButtonId)
    }
    private fun clickStop() {
        isRunningOnThisFragment = false
        timeButtonsArray[activeTimeButtonId].restoreInitTime()
        timeButtonsArray[activeTimeButtonId].isClickable = true
        clickChangeActiveTime()
        makeButtonsVisible( arrayOf(0, 3))
        viewModel.stopTimer()
    }

    /** Activates required [TimeButton] and deactivates others in [timeButtonsArray].
     * Changes background color of fragment view ([R.id.Pomadoro]) to the active color of chosen [TimeButton].
     *
     * @param activeTimeButtonId - [TimeButton] that should be activated */
    private fun setUpColors(activeTimeButtonId: Int) {
        val size = timeButtonsArray.size
        for (i in 0 until size) {
            if (i != activeTimeButtonId) {
                timeButtonsArray[i].deactivate()
            }
            else {
                val color = timeButtonsArray[i].activate()
                val background = requireView().findViewById<LinearLayout>(R.id.Pomadoro)
                background.setBackgroundColor(color)
            }
        }
    }

    /** Changes visibility of action buttons in [buttonArray] (Start, Pause, Resume, Change, Stop)
     *
     * @param indexesToVisible - indexes of buttons in [buttonArray] that should be visible, others will be invisible */
    private fun makeButtonsVisible(indexesToVisible: Array<Int>) {
        for (i in buttonArray.indices) {
            if (i in indexesToVisible) {
                buttonArray[i].visibility = Button.VISIBLE
            } else {
                buttonArray[i].visibility = Button.GONE
            }
        }
    }
}
