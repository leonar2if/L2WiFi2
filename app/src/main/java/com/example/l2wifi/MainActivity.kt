package com.example.l2wifi

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etUser = findViewById<EditText>(R.id.et_user)
        val etPass = findViewById<EditText>(R.id.et_pass)
        val btnSave = findViewById<Button>(R.id.btn_save)

        btnSave.setOnClickListener {
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString().trim()

            if (user.isNotEmpty() && pass.isNotEmpty()) {
                val prefs = getSharedPreferences("L2WiFiPrefs", Context.MODE_PRIVATE)
                val jsonArray = JSONArray(prefs.getString("accounts_json", "[]"))

                val nuevaCuenta = JSONObject().apply {
                    put("username", user)
                    put("password", pass)
                    put("uuid", "")
                }

                jsonArray.put(nuevaCuenta)
                prefs.edit().putString("accounts_json", jsonArray.toString()).apply()

                etUser.text.clear()
                etPass.text.clear()
                Toast.makeText(this, "¡Cuenta guardada con éxito en L2 WiFi!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Por favor rellena ambos campos", Toast.LENGTH_SHORT).show()
            }
        }
    }
}