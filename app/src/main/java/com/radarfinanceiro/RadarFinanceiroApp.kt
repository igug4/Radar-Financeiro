package com.radarfinanceiro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.radarfinanceiro.data.database.AppDatabase

class RadarFinanceiroApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Inicializa o banco de dados (cria categorias e mapeamentos padrao)
        AppDatabase.getInstance(this)

        // Cria canal de notificacao
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "radar_channel",
                "Radar Financeiro",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificacoes do Radar Financeiro"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
