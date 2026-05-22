package com.example.l2wifi

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fab: FloatingActionButton
    private lateinit var themeSwitch: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNavigation)
        fab = findViewById(R.id.fab)
        themeSwitch = findViewById(R.id.themeSwitch)

        // Configurar tema según preferencia guardada
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        themeSwitch.isChecked = currentNightMode == Configuration.UI_MODE_NIGHT_YES
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                themeSwitch.thumbDrawable = getDrawable(R.drawable.ic_sun)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                themeSwitch.thumb = getDrawable(R.drawable.ic_moon)
            }
        }

        // Cargar fragmento inicial
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, InicioFragment())
                .commit()
            bottomNav.selectedItemId = R.id.nav_inicio
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_inicio -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, InicioFragment())
                        .commit()
                    fab.visibility = FloatingActionButton.VISIBLE
                    true
                }
                R.id.nav_perfil -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, PerfilFragment())
                        .commit()
                    fab.visibility = FloatingActionButton.GONE
                    true
                }
                else -> false
            }
        }

        // El FAB se controla desde InicioFragment (se comunica mediante callback)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_edit -> {
                (supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? InicioFragment)?.toggleEditMode()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Para que InicioFragment pueda cambiar el FAB
    fun setFabIcon(iconRes: Int) {
        fab.setImageResource(iconRes)
    }

    fun setFabVisibility(visible: Boolean) {
        fab.visibility = if (visible) FloatingActionButton.VISIBLE else FloatingActionButton.GONE
    }

    fun getFab(): FloatingActionButton = fab
}