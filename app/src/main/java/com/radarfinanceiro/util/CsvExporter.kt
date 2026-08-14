package com.radarfinanceiro.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.radarfinanceiro.data.entity.Transaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun exportToCsv(context: Context, transactions: List<Transaction>): File {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val file = File(context.cacheDir, "radar_financeiro_$dateStr.csv")

        file.bufferedWriter().use { writer ->
            // Header
            writer.write("Data,Hora,Estabelecimento,Valor,Categoria,Nota,Titular,Cartao,Fonte,Localizacao,Endereco")
            writer.newLine()

            // Data
            transactions.forEach { t ->
                val titular = if (t.isLuciana) "Luciana" else "Gustavo"
                val loc = if (t.latitude != null) "${t.latitude},${t.longitude}" else ""
                val line = listOf(
                    t.date,
                    t.time,
                    escapeCsv(t.merchantName),
                    String.format("%.2f", t.amount),
                    t.category,
                    escapeCsv(t.note),
                    titular,
                    t.cardLast4,
                    t.source,
                    loc,
                    escapeCsv(t.locationAddress)
                ).joinToString(",")
                writer.write(line)
                writer.newLine()
            }
        }

        return file
    }

    fun shareFile(context: Context, file: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar extrato"))
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else value
    }
}
