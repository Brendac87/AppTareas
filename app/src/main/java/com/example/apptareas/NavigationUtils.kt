package com.example.apptareas

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView

object NavigationUtils {

    fun configurarNavegacion(actividadActual: Activity, bottomNav: BottomNavigationView) {

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                R.id.nav_home -> {
                    if (actividadActual !is HomeActivity) {
                        actividadActual.startActivity(Intent(actividadActual, HomeActivity::class.java))
                        actividadActual.overridePendingTransition(0, 0)
                        actividadActual.finish()
                    }
                    true
                }

                R.id.nav_tasks -> {
                    if (actividadActual !is TaskListActivity) {
                        actividadActual.startActivity(Intent(actividadActual, TaskListActivity::class.java))
                        actividadActual.overridePendingTransition(0, 0)
                        actividadActual.finish()
                    }
                    true
                }

                R.id.nav_profile -> {
                    if (actividadActual !is ProfileActivity) {
                        actividadActual.startActivity(Intent(actividadActual, ProfileActivity::class.java))
                        actividadActual.overridePendingTransition(0, 0)
                        actividadActual.finish()
                    }
                    true
                }

                R.id.nav_not -> {
                    if (actividadActual !is NotificationsActivity) {
                        actividadActual.startActivity(
                            Intent(
                                actividadActual,
                                NotificationsActivity::class.java
                            )
                        )
                        actividadActual.overridePendingTransition(0, 0)
                        actividadActual.finish()
                    }

                    true
                }

                else -> false
            }
        }
    }
}