package com.radarfinanceiro.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.radarfinanceiro.data.database.AppDatabase
import com.radarfinanceiro.data.entity.Transaction
import com.radarfinanceiro.data.repository.TransactionRepository
import com.radarfinanceiro.parser.NotificationParser
import com.radarfinanceiro.ui.overlay.TransactionOverlayActivity
import com.radarfinanceiro.util.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RadarSmsReceiver"
        // Numeros conhecidos do Bradesco/Amazon
        private val BRADESCO_NUMBERS = setOf("27357", "2102914", "28033")
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        intent ?: return

        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val senderNumber = messages.firstOrNull()?.displayOriginatingAddress ?: return
        val fullMessage = messages.joinToString("") { it.displayMessageBody ?: "" }

        Log.d(TAG, "SMS recebido de: $senderNumber")

        // Verifica se e do Bradesco
        if (BRADESCO_NUMBERS.none { senderNumber.contains(it) }) return
        if (!fullMessage.contains("CARTAO AMAZON", ignoreCase = true)) return

        Log.d(TAG, "SMS do Bradesco/Amazon detectado: $fullMessage")

        val parsed = NotificationParser.parseBradescoSms(fullMessage) ?: return

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            try {
                val db = AppDatabase.getInstance(context)
                val repository = TransactionRepository(db.transactionDao(), db.merchantMappingDao())
                val locationHelper = LocationHelper(context)
                val location = locationHelper.getLastLocation()

                val suggestedCategory = repository.suggestCategory(parsed.merchantName)

                val transaction = Transaction(
                    amount = parsed.amount,
                    merchantName = parsed.merchantName,
                    merchantClean = parsed.merchantName.uppercase().trim(),
                    date = parsed.date,
                    time = parsed.time,
                    timestamp = System.currentTimeMillis(),
                    category = suggestedCategory,
                    source = "bradesco_sms",
                    cardLast4 = parsed.cardLast4,
                    latitude = location?.latitude,
                    longitude = location?.longitude,
                    notificationId = parsed.notificationId,
                    confirmed = false
                )

                val id = repository.insert(transaction)
                if (id > 0) {
                    Log.d(TAG, "Transacao Bradesco salva: id=$id R$${parsed.amount}")
                    val overlayIntent = Intent(context, TransactionOverlayActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra("transaction_id", id)
                    }
                    context.startActivity(overlayIntent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar SMS Bradesco", e)
            }
        }
    }
}
