package com.example.l2wifi

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.util.Timer
import java.util.TimerTask

class ContadorFragment : Fragment() {

    private lateinit var counterText: TextView
    private lateinit var btnCerrar: Button
    private lateinit var btnActualizar: Button
    private var timer: Timer? = null
    private var segundos = 6312 // 01:45:12
    private var uuid: String = ""
    private var username: String = ""

    companion object {
        fun newInstance(username: String, uuid: String): ContadorFragment {
            val f = ContadorFragment()
            val args = Bundle()
            args.putString("username", username)
            args.putString("uuid", uuid)
            f.arguments = args
            return f
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_contador, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        counterText = view.findViewById(R.id.counterText)
        btnCerrar = view.findViewById(R.id.btnCerrar)
        btnActualizar = view.findViewById(R.id.btnActualizar)

        username = arguments?.getString("username") ?: ""
        uuid = arguments?.getString("uuid") ?: ""

        (activity as? MainActivity)?.supportActionBar?.title = username.substringBefore("@")

        startTimer()

        btnCerrar.setOnClickListener {
            cerrarSesion()
        }

        btnActualizar.setOnClickListener {
            actualizarSaldo()
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = Timer()
        timer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (segundos > 0) segundos--
                Handler(Looper.getMainLooper()).post {
                    counterText.text = formatTime(segundos)
                }
            }
        }, 0, 1000)
    }

    private fun formatTime(segundosTotales: Int): String {
        val h = segundosTotales / 3600
        val m = (segundosTotales % 3600) / 60
        val s = segundosTotales % 60
        return String.format("%02d:%02d:%02d", h, m, s)
    }

    private fun actualizarSaldo() {
        Thread {
            val saldo = EtecsaNetworkClient.consultarSaldoEtecsa(uuid)
            val partes = saldo.split(":")
            if (partes.size == 3) {
                val horas = partes[0].toInt()
                val minutos = partes[1].toInt()
                val segs = partes[2].toInt()
                segundos = horas * 3600 + minutos * 60 + segs
                Handler(Looper.getMainLooper()).post {
                    counterText.text = saldo
                }
            }
        }.start()
    }

    private fun cerrarSesion() {
        Thread {
            val success = EtecsaNetworkClient.cerrarSesionEtecsa(uuid)
            requireActivity().runOnUiThread {
                if (success) {
                    // Limpiar uuid de todas las cuentas
                    val prefs = requireActivity().getSharedPreferences("L2WiFiPrefs", android.content.Context.MODE_PRIVATE)
                    val cuentasJson = prefs.getString("accounts_json", "[]") ?: "[]"
                    val cuentas = org.json.JSONArray(cuentasJson)
                    for (i in 0 until cuentas.length()) {
                        cuentas.getJSONObject(i).put("uuid", "")
                    }
                    prefs.edit().putString("accounts_json", cuentas.toString()).apply()
                    // Volver atrás
                    requireActivity().onBackPressed()
                } else {
                    android.widget.Toast.makeText(requireContext(), "Error al cerrar sesión", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }
}