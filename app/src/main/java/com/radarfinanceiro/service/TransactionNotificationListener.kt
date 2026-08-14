package com.radarfinanceiro.service

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.radarfinanceiro.data.database.AppDatabase
import com.radarfinanceiro.data.entity.Transaction
import com.radarfinanceiro.data.repository.TransactionRepository
import com.radarfinanceiro.parser.NotificationParser
import com.radarfinanceiro.parser.ParsedTransaction
import com.radarfinanceiro.ui.overlay.TransactionOverlayActivity
import com.radarfinanceiro.util.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class TransactionNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: TransactionRepository
    private lateinit var locationHelper: LocationHelper

    companion object {
        private const val TAG = "RadarNotifListener"

        // Package names dos apps que nos interessam
        private const val ITAU_PACKAGE = "com.itau"
        private const val ITAU_PERSONNALITE_PACKAGE = "com.itau.personnalite"
        private const val GOOGLE_WALLET_PACKAGE = "com.google.android.apps.walletnfcrel"

        // Tambem captura de outros packages do Itau
        private val ITAU_PACKAGES = setOf(
            "com.itau",
            "com.itau.personnalite",
            "com.itau.personnalite.app",
            "br.com.itau",
            "br.com.itau.personnalite"
        )
    }

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.getInstance(applicationContext)
        repository = TransactionRepository(db.transactionDao(), db.merchantMappingDao())
        locationHelper = LocationHelper(applicationContext)
        Log.d(TAG, "NotificationListener criado")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text

        Log.d(TAG, "Notificacao: pkg=$packageName title=$title text=$bigText")

        val parsed: ParsedTransaction? = when {
            // Itau Personnalite
            ITAU_PACKAGES.any { packageName.startsWith(it) } -> {
                NotificationParser.parseItau(title, bigText)
            }

            // Carteira do Google
            packageName == GOOGLE_WALLET_PACKAGE -> {
                NotificationParser.parseGoogleWallet(title, bigText)
            }

            else -> null
        }

        if (parsed != null) {
            processTransaction(parsed)
        }
    }

    private fun processTransaction(parsed: ParsedTransaction) {
        scope.launch {
            try {
                // Pega localizacao atual
                val location = locationHelper.getLastLocation()

                // Sugere categoria baseado no aprendizado
                val suggestedCategory = repository.suggestCategory(parsed.merchantName)

                // Cria a transacao
                val transaction = Transaction(
                    amount = parsed.amount,
                    merchantName = parsed.merchantName,
                    merchantClean = cleanMerchantName(parsed.merchantName),
                    date = parsed.date,
                    time = parsed.time,
                    timestamp = System.currentTimeMillis(),
                    category = if (parsed.isLuciana) "Luciana" else suggestedCategory,
                    isLuciana = parsed.isLuciana,
                    source = parsed.source,
                    cardLast4 = parsed.cardLast4,
                    isInstallment = parsed.isInstallment,
                    installmentInfo = parsed.installmentInfo,
                    merchantCity = parsed.merchantCity,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    notificationId = parsed.notificationId,
                    confirmed = false
                )

                val id = repository.insert(transaction)

                if (id > 0) {
                    Log.d(TAG, "Transacao salva: id=$id ${parsed.merchantName} R$${parsed.amount}")
                    // Abre o pop-up de anotacao
                    launchOverlay(id)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar transacao", e)
            }
        }
    }

    private fun launchOverlay(transactionId: Long) {
        val intent = Intent(applicationContext, TransactionOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("transaction_id", transactionId)
        }
        startActivity(intent)
    }

    private fun cleanMerchantName(name: String): String {
        return name
            .replace(Regex("""(FORMIGA|DIVINOPOLIS|SAO PAULO|BRASILIA|BARUERI|BRA?|BR)\s*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .uppercase()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        // Nao precisamos fazer nada quando a notificacao e removida
    }
}
