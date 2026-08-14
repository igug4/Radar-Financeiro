package com.radarfinanceiro.parser

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationParser {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    private val timeFormat = SimpleDateFormat("HH:mm", Locale("pt", "BR"))

    /**
     * Parsa notificacao do Itau Personnalite.
     *
     * Formatos conhecidos:
     * - "Ola! Compra aprovada de R$ 32,12 em PADARIA CRUZ no dia 08/08 as 07:12 no seu cartao Itau."
     * - "Ola! Compra parcelada aprovada de R$ 299,90 em 4x em AREZZO FORMIGA feita por LUCIANA ALVES no dia 06/08 as 09:42 no cartao Itau."
     * - PIX: "Ola! Pix de R$ 50,00 enviado para FULANO no dia 08/08 as 10:00."
     * - PIX: "Ola! Pix de R$ 100,00 recebido de FULANO no dia 08/08 as 10:00."
     */
    fun parseItau(title: String, text: String): ParsedTransaction? {
        val fullText = "$title $text"

        // Detecta se e compra do adicional (Luciana)
        val isLuciana = fullText.contains("adicional", ignoreCase = true) ||
                fullText.contains("LUCIANA", ignoreCase = true)

        // PIX enviado
        val pixEnviadoRegex = Regex(
            """Pix de R\$\s?([\d.,]+)\s+enviado para\s+(.+?)\s+no dia\s+(\d{2}/\d{2})\s+[aà]s\s+(\d{2}:\d{2})""",
            RegexOption.IGNORE_CASE
        )
        pixEnviadoRegex.find(fullText)?.let { match ->
            val (valor, dest, data, hora) = match.destructured
            return ParsedTransaction(
                amount = parseAmount(valor),
                merchantName = dest.trim(),
                date = completarData(data),
                time = hora,
                source = "pix",
                isLuciana = isLuciana,
                notificationId = "pix_${data}_${hora}_${parseAmount(valor)}"
            )
        }

        // PIX recebido
        val pixRecebidoRegex = Regex(
            """Pix de R\$\s?([\d.,]+)\s+recebido de\s+(.+?)\s+no dia\s+(\d{2}/\d{2})\s+[aà]s\s+(\d{2}:\d{2})""",
            RegexOption.IGNORE_CASE
        )
        pixRecebidoRegex.find(fullText)?.let { match ->
            val (valor, remetente, data, hora) = match.destructured
            return ParsedTransaction(
                amount = -parseAmount(valor), // Negativo = entrada
                merchantName = "PIX de $remetente",
                date = completarData(data),
                time = hora,
                source = "pix",
                isLuciana = isLuciana,
                notificationId = "pix_rec_${data}_${hora}_${parseAmount(valor)}"
            )
        }

        // Compra parcelada
        val parceladaRegex = Regex(
            """Compra\s+parcelada\s+aprovada\s+de\s+R\$\s?([\d.,]+)\s+em\s+(\d+)x\s+em\s+(.+?)\s+(?:feita por\s+(.+?)\s+)?no dia\s+(\d{2}/\d{2})\s+[aà]s\s+(\d{2}:\d{2})""",
            RegexOption.IGNORE_CASE
        )
        parceladaRegex.find(fullText)?.let { match ->
            val valor = match.groupValues[1]
            val parcelas = match.groupValues[2]
            val estabelecimento = match.groupValues[3]
            val responsavel = match.groupValues[4]
            val data = match.groupValues[5]
            val hora = match.groupValues[6]
            return ParsedTransaction(
                amount = parseAmount(valor),
                merchantName = estabelecimento.trim(),
                date = completarData(data),
                time = hora,
                source = "itau",
                cardLast4 = "7233",
                isLuciana = isLuciana || responsavel.contains("LUCIANA", ignoreCase = true),
                isInstallment = true,
                installmentInfo = "${parcelas}x",
                notificationId = "itau_${data}_${hora}_${parseAmount(valor)}"
            )
        }

        // Compra normal
        val compraRegex = Regex(
            """Compra\s+aprovada\s+de\s+R\$\s?([\d.,]+)\s+em\s+(.+?)\s+(?:feita por\s+(.+?)\s+)?no dia\s+(\d{2}/\d{2})\s+[aà]s\s+(\d{2}:\d{2})""",
            RegexOption.IGNORE_CASE
        )
        compraRegex.find(fullText)?.let { match ->
            val valor = match.groupValues[1]
            val estabelecimento = match.groupValues[2]
            val responsavel = match.groupValues[3]
            val data = match.groupValues[4]
            val hora = match.groupValues[5]
            return ParsedTransaction(
                amount = parseAmount(valor),
                merchantName = estabelecimento.trim(),
                date = completarData(data),
                time = hora,
                source = "itau",
                cardLast4 = "7233",
                isLuciana = isLuciana || responsavel.contains("LUCIANA", ignoreCase = true),
                notificationId = "itau_${data}_${hora}_${parseAmount(valor)}"
            )
        }

        return null
    }

    /**
     * Parsa notificacao da Carteira do Google.
     *
     * Formato: Title = "PADARIA SANTA CRUZ", Text = "R$ 32,12 com Azul Infinite ••7233"
     */
    fun parseGoogleWallet(title: String, text: String): ParsedTransaction? {
        val valorRegex = Regex("""R\$\s?([\d.,]+)""")
        val cartaoRegex = Regex("""[•·]+\s?(\d{4})""")

        val valor = valorRegex.find(text)?.groupValues?.get(1) ?: return null
        val cartao = cartaoRegex.find(text)?.groupValues?.get(1) ?: ""
        val now = Date()

        return ParsedTransaction(
            amount = parseAmount(valor),
            merchantName = title.trim(),
            date = dateFormat.format(now),
            time = timeFormat.format(now),
            source = "google_wallet",
            cardLast4 = cartao,
            notificationId = "gw_${dateFormat.format(now)}_${timeFormat.format(now)}_${parseAmount(valor)}"
        )
    }

    /**
     * Parsa SMS do Bradesco (Cartao Amazon).
     *
     * Formato: "CARTAO AMAZON: COMPRA APROVADA NO CARTAO FINAL 8014 06/08/2026 09:36.
     *           VALOR DE R$100,00, AUTO-POSTOTERMINAL. LIMITE DISPONIVEL DE R$8598,19"
     */
    fun parseBradescoSms(message: String): ParsedTransaction? {
        if (!message.contains("CARTAO AMAZON", ignoreCase = true)) return null

        val regex = Regex(
            """CARTAO AMAZON:\s*COMPRA APROVADA.*?FINAL\s+(\d{4})\s+(\d{2}/\d{2}/\d{4})\s+(\d{2}:\d{2}).*?VALOR DE R\$\s?([\d.,]+),\s*(.+?)\.\s*LIMITE""",
            RegexOption.IGNORE_CASE
        )

        regex.find(message)?.let { match ->
            val cartao = match.groupValues[1]
            val data = match.groupValues[2]
            val hora = match.groupValues[3]
            val valor = match.groupValues[4]
            val estabelecimento = match.groupValues[5]

            // Formata data de dd/MM/yyyy
            val dataFormatada = if (data.length == 10) data else completarData(data)

            return ParsedTransaction(
                amount = parseAmount(valor),
                merchantName = estabelecimento.trim().replace("-", " ").trim(),
                date = dataFormatada,
                time = hora,
                source = "bradesco_sms",
                cardLast4 = cartao,
                notificationId = "bradesco_${data}_${hora}_${parseAmount(valor)}"
            )
        }

        return null
    }

    // --- Utilitarios ---

    private fun parseAmount(value: String): Double {
        return value
            .replace(".", "")
            .replace(",", ".")
            .toDoubleOrNull() ?: 0.0
    }

    private fun completarData(shortDate: String): String {
        // Converte "08/08" para "08/08/2026"
        val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
        return if (shortDate.length <= 5) "$shortDate/$year" else shortDate
    }
}
