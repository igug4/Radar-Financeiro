package com.radarfinanceiro.parser

data class ParsedTransaction(
    val amount: Double,
    val merchantName: String,
    val date: String,           // dd/MM/yyyy
    val time: String,           // HH:mm
    val source: String,         // "itau", "google_wallet", "bradesco_sms", "pix"
    val cardLast4: String = "",
    val isLuciana: Boolean = false,
    val isInstallment: Boolean = false,
    val installmentInfo: String = "",
    val merchantCity: String = "",
    val notificationId: String = ""
)
