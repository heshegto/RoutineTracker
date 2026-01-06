package com.heshkin.routine_tracker.pomadoro

import android.annotation.SuppressLint
import android.app.Activity.MODE_PRIVATE
import android.content.Context
import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.Button

import androidx.core.content.edit

import com.heshkin.routine_tracker.R
import com.heshkin.routine_tracker.my_time_picker.MyTimePickerDialog

/** Extends [Button] class.
 *
 * Developed specifically to work with Shared preferences and button mechanics.
 * Current time stored and shown as [Button]'s Text and can be changed throw [setTimeOnView].
 * Initial time stored in SharedPreferences and can be changed by CLICK on [Button] by [showTimePicker].
 * Initial time can be restored to current time by [restoreInitTime].
 *
 * Have same XML attributes as [Button] class, plus its own:
 * * [R.styleable.TimeButton_name] - is a string that is used to get access
 * to [TimeButton]'s SharedPreferences. Stored in property [timeName].
 * This attribute (and parameter [timeName] as well) should be unique for every [TimeButton]
 * to be SharedPreferences unique too.
 * * [R.styleable.TimeButton_default_hours], [R.styleable.TimeButton_default_minute],
 * [R.styleable.TimeButton_default_seconds] - attributes that contain default values for initial time
 *
 * @property [timeName] contains [TimeButton]'s name that used to get access to SharedPreferences.
 * Should be unique to be SharedPreferences unique.
 *
 * @property [colorActive] contains color of [TimeButton] when it is active.
 * @property [colorInactive] contains color of [TimeButton] when it is inactive.
 *
 * @property [sp] contains [TimeButton]'s SharedPreferences.
 *
 * @constructor Same as in [Button].
 */

@SuppressLint("AppCompatCustomView")
class TimeButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : Button(context, attrs, defStyleAttr, defStyleRes) {

    companion object {
        /** Field of SharedPreferences that contains saved init time in seconds*/
        private const val FIELD_TIME = "time"
    }

    private val timeName: String

    val colorActive: Int
    val colorInactive: Int

    private val sp: SharedPreferences

    init {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TimeButton,
            defStyleAttr,
            defStyleRes
        )
        /* this String is used to get access to Button's SharedPreferences */
        timeName = typedArray.getString(R.styleable.TimeButton_name) ?: "default"

        val hours: Int = typedArray.getInt(R.styleable.TimeButton_default_hours, 0)
        val minutes: Int = typedArray.getInt(R.styleable.TimeButton_default_minute, 0)
        val seconds: Int = typedArray.getInt(R.styleable.TimeButton_default_seconds, 0)
        colorActive = typedArray.getColor(R.styleable.TimeButton_colorActive, 0)
        colorInactive = typedArray.getColor(R.styleable.TimeButton_colorInactive, 0)

        typedArray.recycle()

        setOnClickListener { showTimePicker() }
        sp = context.getSharedPreferences(timeName, MODE_PRIVATE)
        setInitialTime( TimeValue.putTimeTogether(hours, minutes, seconds) )
        restoreInitTime()
        deactivate()
    }

    /** Changes [TimeButton]'s background color to active
     * @return Color active */
    fun activate(): Int {
        this.changeBackground(colorActive)
        return colorActive
    }

    /** Changes [TimeButton]'s background color to inactive
     * @return Color inactive */
    fun deactivate(): Int {
        this.changeBackground(colorInactive)
        return colorInactive
    }

    /** Changes [TimeButton]'s background color
     * @param color Color that needs to be set */
    private fun changeBackground(color: Int) {
        (this.background.mutate() as GradientDrawable).setColor(color)
    }

    /** Sets time to [TimeButton]'s text
     * @param hours hours that needs to be shown
     * @param minutes minutes that needs to be shown
     * @param seconds seconds that needs to be shown */
    fun setTimeOnView(hours: Int, minutes: Int, seconds: Int) {
        @SuppressLint("SetTextI18n")
        text = "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    /** Sets time to [TimeButton]'s text
     * @param timeInSeconds time in seconds that needs to be shown */
    fun setTimeOnView(timeInSeconds: Int) {
        val time: TimeValue= TimeValue.breakTime(timeInSeconds)
        setTimeOnView(time.hours, time.minutes, time.seconds)
    }

    /** Saves time in seconds to SharedPreferences
     * @param time time in seconds that needs to be saved*/
    private fun setTimeSP(time: Int) {
        sp.edit { putInt(FIELD_TIME, time)}
    }

    /** Shows time picker dialog. Saves its output to [TimeButton]'s SharedPreferences
     * and puts it as a text of [TimeButton] */
    private fun showTimePicker() {
        val time : TimeValue = TimeValue.breakTime(sp.getInt(FIELD_TIME, 0))
        MyTimePickerDialog(
            context,
            { _, hours, minutes, seconds ->
                setTimeSP(TimeValue.putTimeTogether(hours, minutes, seconds))
                setTimeOnView(hours, minutes, seconds)
            },
            time.hours,
            time.minutes,
            time.seconds,
            true
        ).show()
    }

    /**
     * Gets initial time in seconds from SharedPreferences and sets it to [TimeButton]'s text
     */
    fun restoreInitTime() { setTimeOnView( getInitTime() ) }

    /**
     * Sets initial time in seconds to SharedPreferences
     *
     * This function exists only to make code more readable
     *
     * @param defaultSeconds initial time in seconds
     */
    private fun setInitialTime(defaultSeconds: Int = 0) {
        /* if something wrong with SharedPreferences, set default values to SharedPreferences
         * if everything is ok, values from SharedPreferences goes to TimeButton */
        if (sp.all.isEmpty() or !sp.contains(FIELD_TIME)) {
            setTimeSP( defaultSeconds )
        }
    }

    /**
     * @return Initial time in seconds from SharedPreferences */
    fun getInitTime() : Int = sp.getInt(FIELD_TIME, 0)

    /** Contains time in hours, minutes and seconds */
    data class TimeValue(val hours: Int, val minutes: Int, val seconds: Int) {
        companion object {
            /** Creates [TimeValue] from time in seconds */
            fun breakTime(timeInSeconds: Int): TimeValue {
                val seconds: Int = timeInSeconds % 60
                val minutes: Int = timeInSeconds % 3600 / 60
                val hours: Int = timeInSeconds / 3600
                return TimeValue(hours, minutes, seconds)
            }

            /** Creates [TimeValue] from time in seconds */
            fun breakTime(timeInSeconds: Long): TimeValue {
                return breakTime(timeInSeconds.toInt())
            }

            /** Sums up time into seconds*/
            fun putTimeTogether(hours: Int, minutes: Int, seconds: Int) : Int =
                hours * 3600 + minutes * 60 + seconds
        }
    }
}