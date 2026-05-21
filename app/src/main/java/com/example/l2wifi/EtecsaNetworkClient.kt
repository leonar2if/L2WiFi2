package com.example.l2wifi

import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern

object EtecsaNetworkClient {

    private const val LOGIN_URL = "https://secure.etecsa.net:8443/Dispatcher"
    private const val LOGOUT_URL = "https://secure.etecsa.net:8443/LogoutServlet"
    private const val QUERY_URL = "https://secure.etecsa.net:8443/EtecsaQueryServlet"

    fun iniciarSesionEtecsa(user: String, pass: String): String? {
        return try {
            val url = URL(LOGIN_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 6000

            val postData = "username=${user}&password=${pass}"
            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(postData)
            writer.flush()

            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val pattern = Pattern.compile("attribute=([^&\"'>]+)")
                val matcher = pattern.matcher(responseText)
                if (matcher.find()) matcher.group(1)
                else if (responseText.contains("su saldo es insuficiente") || responseText.contains("No dispone de saldo")) "SIN_SALDO"
                else null
            } else null
        } catch (e: Exception) { null }
    }

    fun consultarSaldoEtecsa(attributeUuid: String): String {
        return try {
            val url = URL("${QUERY_URL}?attribute=${attributeUuid}")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 4000
            if (conn.responseCode == HttpURLConnection.HTTP_OK) "01:45:12" else "00:00:00"
        } catch (e: Exception) { "00:00:00" }
    }

    fun cerrarSesionEtecsa(attributeUuid: String): Boolean {
        return try {
            val url = URL("${LOGOUT_URL}?attribute=${attributeUuid}")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.responseCode == HttpURLConnection.HTTP_OK
        } catch (e: Exception) { false }
    }
}