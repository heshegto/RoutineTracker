package com.heshkin.routine_tracker.pomadoro

import android.annotation.SuppressLint
import android.app.Activity.MODE_PRIVATE
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.icu.util.Calendar
import android.util.AttributeSet
import android.widget.Button

import androidx.core.content.edit

import com.heshkin.routine_tracker.R
import com.heshkin.routine_tracker.my_time_picker.MyTimePickerDialog

/** Extends [Button] class.
 *
 * Developed specifically to contain two times: current and initial.
 * Current time stored inside class, shown as [Button]'s Text and can be changed throw [setTime].
 * Initial time stored in SharedPreferences and can be changed by CLICK on [Button] by [showTimePicker].
 * Initial time can be restored to current time by [restoreTime].
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
 * @property [mHours] contains hours for current time.
 * @property [mMinutes] contains minutes for current time.
 * @property [mSeconds] contains seconds for current time.
 * @property [mMilliseconds] contains milliseconds for current time. Isn't shown for user. Just used for calculations.
 *
 * @property [mIsActive] contains state of [TimeButton].
 * @property [mColorActive] contains color of [TimeButton] when it is active.
 * @property [mColorInactive] contains color of [TimeButton] when it is inactive.
 *
 * @constructor Same as in [Button].
 */

class TimeButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : Button(context, attrs, defStyleAttr, defStyleRes) {
    /** Fields of SharedPreferences */
    companion object {
        private const val FIELD_HOURS = Calendar.HOUR.toString()
        private const val FIELD_MINUTES = Calendar.MINUTE.toString()
        private const val FIELD_SECONDS = Calendar.SECOND.toString()
    }

    private val timeName: String

    var mHours: Int
    var mMinutes: Int
    var mSeconds: Int
    var mMilliseconds: Int

    var mIsActive: Boolean = false
    var mColorActive: Int = 0
    private var mColorInactive: Int = 0

    init {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.TimeButton,
            defStyleAttr,
            defStyleRes
        )
        /* this String is used to get access to Button's SharedPreferences */
        timeName = typedArray.getString(R.styleable.TimeButton_name) ?: "default"

        mHours = typedArray.getInt(R.styleable.TimeButton_default_hours, 0)
        mMinutes = typedArray.getInt(R.styleable.TimeButton_default_minute, 0)
        mSeconds = typedArray.getInt(R.styleable.TimeButton_default_seconds, 0)
        mMilliseconds = 0

        mColorActive = typedArray.getColor(R.styleable.TimeButton_colorActive, 0)
        mColorInactive = typedArray.getColor(R.styleable.TimeButton_colorInactive, 0)

        typedArray.recycle()

        setInitialTime(mHours, mMinutes, mSeconds)

        setOnClickListener { showTimePicker() }
        deactivate()
    }

    fun activate() {
        mIsActive = true
        this.changeBackground(mColorActive)
    }

    fun deactivate() {
        mIsActive = false
        this.changeBackground(mColorInactive)
    }

    private fun changeBackground(color: Int) {
        val background: GradientDrawable = this.background as GradientDrawable
        background.setColor(color)
    }

    fun setTime(hours: Int, minutes: Int, seconds: Int, milliseconds: Int) {
        @SuppressLint("SetTextI18n")
        text = "%02d:%02d:%02d".format(hours, minutes, seconds)
        mHours = hours
        mMinutes = minutes
        mSeconds = seconds
        mMilliseconds = milliseconds
    }

    private fun showTimePicker() {
        MyTimePickerDialog(
            context,
            { _, hours, minutes, seconds ->
                setSharedPreferences(hours, minutes, seconds)
                setTime(hours, minutes, seconds, 0)
            },
            mHours,
            mMinutes,
            mSeconds,
            true
        ).show()
    }

    fun restoreTime() {
        val sp = context.getSharedPreferences(timeName, MODE_PRIVATE)
        setTime(
            sp.getInt(FIELD_HOURS, 0),
            sp.getInt(FIELD_MINUTES, 0),
            sp.getInt(FIELD_SECONDS, 0),
            0
        )
    }

    private fun setSharedPreferences(hours: Int = 0, minutes: Int = 0, seconds: Int = 0) {
        val sp = context.getSharedPreferences(timeName, MODE_PRIVATE)
        sp.edit {
            putInt(FIELD_HOURS, hours)
            putInt(FIELD_MINUTES, minutes)
            putInt(FIELD_SECONDS, seconds)
        }
    }

    private fun setInitialTime(
        defaultHours: Int = 0,
        defaultMinutes: Int = 0,
        defaultSeconds: Int = 0,
    ) {
        val sp = context.getSharedPreferences(timeName, MODE_PRIVATE)
        /* if something wrong with SharedPreferences, set default values to SharedPreferences
         * if everything is ok, values from SharedPreferences goes to TimeButton */
        if (
            sp.all.isEmpty()
            or !sp.contains(FIELD_HOURS)
            or !sp.contains(FIELD_MINUTES)
            or !sp.contains(FIELD_SECONDS)
        ) {
            sp.edit {
                putInt(FIELD_HOURS, defaultHours)
                putInt(FIELD_MINUTES, defaultMinutes)
                putInt(FIELD_SECONDS, defaultSeconds)
            }
        } else {
            setTime(
                sp.getInt(FIELD_HOURS, 0),
                sp.getInt(FIELD_MINUTES, 0),
                sp.getInt(FIELD_SECONDS, 0),
                0
            )
        }
    }


}