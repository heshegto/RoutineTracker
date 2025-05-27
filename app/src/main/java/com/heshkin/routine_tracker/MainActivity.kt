package com.heshkin.routine_tracker

import android.os.Bundle
import android.widget.Button

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction

import com.heshkin.routine_tracker.pomadoro.PomadoroFragment
import com.heshkin.routine_tracker.routine_tracker.RoutineTracker

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val btnPomadoro = findViewById<Button>(R.id.pomadoro)
        val btnRoutineTracker = findViewById<Button>(R.id.routine_tracker)
        setNewFragment(RoutineTracker())

        btnPomadoro.setOnClickListener {
            setNewFragment(PomadoroFragment())
        }
        btnRoutineTracker.setOnClickListener {
            setNewFragment(RoutineTracker())
        }
    }

    private fun setNewFragment(fragment: Fragment){
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(R.id.fragment_container, fragment)
        ft.commit()
    }
}