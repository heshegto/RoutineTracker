package com.heshkin.routine_tracker

import android.os.Bundle
import android.util.Log

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.google.android.material.navigation.NavigationBarView

import com.heshkin.routine_tracker.pomadoro.PomadoroFragment
import com.heshkin.routine_tracker.routine_tracker.RoutineTracker

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavMenu = findViewById<NavigationBarView>(R.id.mainNavMenu)
        bottomNavMenu.setOnItemSelectedListener { it ->
            when(it.itemId) {
                R.id.BMPomadoro -> {
                    setNewFragment(PomadoroFragment.newInstance())
                    Log.v("MainActivity", "Pomadoro menu button clicked")
                }
                R.id.BMRoutine -> {
                    setNewFragment(RoutineTracker())
                    Log.v("MainActivity", "Routine menu button clicked")
                }
                R.id.smthElse -> {
                    Log.v("MainActivity", "Settings menu button clicked")
                }
                else -> false
            }
            true
        }

        if (savedInstanceState == null) {
            when(intent.getStringExtra("fragment")) {
                "pomadoro" -> bottomNavMenu.selectedItemId = R.id.BMPomadoro
                else -> bottomNavMenu.selectedItemId = R.id.BMRoutine
            }
        }
    }

    private fun setNewFragment(fragment: Fragment){
        val ft: FragmentTransaction = supportFragmentManager.beginTransaction()
        ft.replace(R.id.mainFragmentContainer, fragment)
        ft.commit()
    }
}