package com.example.l2wifi

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.RemoteViews
import org.json.JSONArray
import java.util.Timer
import java.util.TimerTask

class MyWiFiWidget : AppWidgetProvider() {

    companion object {
        const val ACTION_WIFI = "com.example.l2wifi.ACTION_WIFI"
        const val ACTION_SALDO = "com.example.l2wifi.ACTION_SALDO"

        private var isConnected = false
        private var activeUuid = ""
        private var tiempoSegundos = 6312 // 01:45:12 de muestra inicial
        private var timerCronometro: Timer? = null
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_wifi)
            val prefs = context.getSharedPreferences("L2WiFiPrefs", Context.MODE_PRIVATE)
            val accounts = JSONArray(prefs.getString("accounts_json", "[]"))

            if (accounts.length() > 0) {
                views.setTextViewText(R.id.txt_username, accounts.getJSONObject(0).getString("username"))
            }

            views.setOnClickPendingIntent(R.id.btn_left, getPendingIntent(context, ACTION_WIFI))
            views.setOnClickPendingIntent(R.id.btn_right, getPendingIntent(context, ACTION_SALDO))

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val thisWidget = ComponentName(context, MyWiFiWidget::class.java)
        val views = RemoteViews(context.packageName, R.layout.widget_wifi)

        val prefs = context.getSharedPreferences("L2WiFiPrefs", Context.MODE_PRIVATE)
        val accounts = JSONArray(prefs.getString("accounts_json", "[]"))
        if (accounts.length() == 0) return

        val usuarioActivo = accounts.getJSONObject(0).getString("username")
        val claveActiva = accounts.getJSONObject(0).getString("password")
        val handlerUI = Handler(Looper.getMainLooper())

        if (intent.action == ACTION_WIFI) {
            if (!isConnected) {
                views.setDisplayedChild(R.id.view_flipper_top, 1)
                views.setTextViewText(R.id.txt_status_msg, "Conectando...")
                appWidgetManager.updateAppWidget(thisWidget, views)

                Thread {
                    val resultUuid = EtecsaNetworkClient.iniciarSesionEtecsa(usuarioActivo, claveActiva)
                    handlerUI.post {
                        if (resultUuid != null && resultUuid != "SIN_SALDO") {
                            isConnected = true
                            activeUuid = resultUuid

                            // Reemplazo dinámico de iconos a estados activos (Sincronizar y Cancelar)
                            views.setImageViewResource(R.id.btn_left, android.R.drawable.ic_popup_sync)
                            views.setImageViewResource(R.id.btn_right, android.R.drawable.ic_menu_close_clear_cancel)

                            startWidgetTimer(context, appWidgetManager, thisWidget, views)
                        } else {
                            views.setDisplayedChild(R.id.view_flipper_top, 1)
                            views.setTextViewText(R.id.txt_status_msg, if (resultUuid == "SIN_SALDO") "Sin Saldo" else "¡Error!")
                            appWidgetManager.updateAppWidget(thisWidget, views)

                            handlerUI.postDelayed({
                                views.setDisplayedChild(R.id.view_flipper_top, 0)
                                views.setTextViewText(R.id.txt_username, usuarioActivo)
                                appWidgetManager.updateAppWidget(thisWidget, views)
                            }, 3000)
                        }
                    }
                }.start()
            } else {
                views.setDisplayedChild(R.id.view_flipper_top, 1)
                views.setTextViewText(R.id.txt_status_msg, "Actualizando...")
                appWidgetManager.updateAppWidget(thisWidget, views)

                Thread {
                    val nuevoSaldo = EtecsaNetworkClient.consultarSaldoEtecsa(activeUuid)
                    handlerUI.post {
                        tiempoSegundos += 3600
                        views.setDisplayedChild(R.id.view_flipper_top, 1)
                        views.setTextViewText(R.id.txt_status_msg, formatTime(tiempoSegundos))
                        appWidgetManager.updateAppWidget(thisWidget, views)
                    }
                }.start()
            }
        }

        if (intent.action == ACTION_SALDO) {
            if (isConnected) {
                views.setDisplayedChild(R.id.view_flipper_top, 1)
                views.setTextViewText(R.id.txt_status_msg, "Cerrando...")
                appWidgetManager.updateAppWidget(thisWidget, views)

                Thread {
                    EtecsaNetworkClient.cerrarSesionEtecsa(activeUuid)
                    handlerUI.post {
                        isConnected = false
                        timerCronometro?.cancel()
                        views.setDisplayedChild(R.id.view_flipper_top, 0)
                        views.setTextViewText(R.id.txt_username, usuarioActivo)
                        views.setImageViewResource(R.id.btn_left, android.R.drawable.ic_menu_compass)
                        views.setImageViewResource(R.id.btn_right, android.R.drawable.ic_menu_agenda)
                        appWidgetManager.updateAppWidget(thisWidget, views)
                    }
                }.start()
            } else {
                views.setDisplayedChild(R.id.view_flipper_top, 1)
                views.setTextViewText(R.id.txt_status_msg, "Consultando...")
                appWidgetManager.updateAppWidget(thisWidget, views)

                handlerUI.postDelayed({
                    views.setTextViewText(R.id.txt_status_msg, formatTime(tiempoSegundos))
                    appWidgetManager.updateAppWidget(thisWidget, views)

                    handlerUI.postDelayed({
                        views.setDisplayedChild(R.id.view_flipper_top, 0)
                        views.setTextViewText(R.id.txt_username, usuarioActivo)
                        appWidgetManager.updateAppWidget(thisWidget, views)
                    }, 3000)
                }, 1500)
            }
        }
    }

    private fun startWidgetTimer(context: Context, manager: AppWidgetManager, comp: ComponentName, views: RemoteViews) {
        timerCronometro?.cancel()
        timerCronometro = Timer()
        timerCronometro?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (tiempoSegundos > 0 && isConnected) {
                    tiempoSegundos--
                    Handler(Looper.getMainLooper()).post {
                        views.setDisplayedChild(R.id.view_flipper_top, 1)
                        views.setTextViewText(R.id.txt_status_msg, formatTime(tiempoSegundos))
                        manager.updateAppWidget(comp, views)
                    }
                } else {
                    cancel()
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

    private fun getPendingIntent(context: Context, action: String): PendingIntent {
        val intent = Intent(context, MyWiFiWidget::class.java).setAction(action)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}