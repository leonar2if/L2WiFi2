package com.example.l2wifi

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<TextView>(R.id.toolbarTitle).text = "Configuración"
        findViewById<TextView>(R.id.contentText).text = "Pantalla de configuración\n(Próximamente)"
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
    }
}

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        findViewById<TextView>(R.id.toolbarTitle).text = "Acerca de"
        findViewById<TextView>(R.id.contentText).text = "L2 WiFi v1.0\nDesarrollado para gestión de cuentas ETECSA"
        findViewById<ImageView>(R.id.backButton).setOnClickListener { finish() }
    }
}