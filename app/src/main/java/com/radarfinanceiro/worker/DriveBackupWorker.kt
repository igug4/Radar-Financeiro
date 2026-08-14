package com.radarfinanceiro.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.radarfinanceiro.data.database.AppDatabase
import com.radarfinanceiro.data.repository.TransactionRepository
import com.radarfinanceiro.util.CsvExporter
import java.io.File
import java.util.concurrent.TimeUnit

class DriveBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DriveBackupWorker"
        private const val WORK_NAME = "radar_backup"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(
                1, TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )

            Log.d(TAG, "Backup diario agendado")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val repo = TransactionRepository(db.transactionDao(), db.merchantMappingDao())
            val transactions = repo.getAllForExport()

            if (transactions.isEmpty()) {
                Log.d(TAG, "Nenhuma transacao para backup")
                return Result.success()
            }

            // Exporta CSV para o armazenamento interno
            val csvFile = CsvExporter.exportToCsv(applicationContext, transactions)

            // Copia para a pasta de backups do app
            val backupDir = File(applicationContext.filesDir, "backups")
            backupDir.mkdirs()
            val backupFile = File(backupDir, "backup_latest.csv")
            csvFile.copyTo(backupFile, overwrite = true)

            // TODO: Upload para Google Drive (requer autenticacao OAuth)
            // Por enquanto, salva localmente. A integracao com Drive
            // sera ativada quando o usuario fizer login com Google.

            Log.d(TAG, "Backup local realizado: ${transactions.size} transacoes")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Erro no backup", e)
            Result.retry()
        }
    }
}
