package com.radarfinanceiro.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Dados basicos da compra
    val amount: Double,
    val merchantName: String,        // Nome como veio na notificacao
    val merchantClean: String,       // Nome limpo/normalizado
    val date: String,                // dd/MM/yyyy
    val time: String,                // HH:mm
    val timestamp: Long,             // epoch millis (para ordenacao)

    // Classificacao
    val category: String,            // Alimentacao, Combustivel, etc.
    val note: String = "",           // Nota do usuario ("pao e leite")
    val isLuciana: Boolean = false,  // Compra da Luciana (adicional)

    // Fonte
    val source: String,              // "itau", "google_wallet", "bradesco_sms", "pix", "manual"
    val cardLast4: String = "",      // Ultimos 4 digitos do cartao
    val isInstallment: Boolean = false,  // Compra parcelada
    val installmentInfo: String = "",     // "4x" etc.

    // Localizacao
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAddress: String = "", // Endereco reverso do GPS
    val merchantCity: String = "",    // Cidade do estabelecimento (da notificacao)

    // Controle
    val confirmed: Boolean = false,  // Usuario confirmou no pop-up
    val createdAt: Long = System.currentTimeMillis(),
    val notificationId: String = ""  // Para evitar duplicatas
)
