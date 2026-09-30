package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.entity.Transaction
import java.io.File
import java.io.FileWriter

object ExportHelper {

    fun exportTransactionsToCsv(context: Context, transactions: List<Transaction>): File? {
        return try {
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val file = File(exportDir, "finflow_transactions_${System.currentTimeMillis()}.csv")

            FileWriter(file).use { writer ->
                // Header
                writer.append("ID,Date,Time,Account,Type,Category,Amount,Merchant,Notes,Source,Status\n")
                for (tx in transactions) {
                    val safeMerchant = tx.merchant.replace("\"", "\"\"")
                    val safeNotes = tx.notes.replace("\"", "\"\"")
                    val safeAccount = tx.accountName.replace("\"", "\"\"")
                    val safeCategory = tx.categoryName.replace("\"", "\"\"")

                    writer.append("${tx.id},")
                    writer.append("\"${DateUtils.formatDate(tx.dateMillis)}\",")
                    writer.append("\"${tx.timeFormatted}\",")
                    writer.append("\"$safeAccount\",")
                    writer.append("${tx.type.name},")
                    writer.append("\"$safeCategory\",")
                    writer.append("${tx.amount},")
                    writer.append("\"$safeMerchant\",")
                    writer.append("\"$safeNotes\",")
                    writer.append("${tx.source.name},")
                    writer.append("${tx.confirmationStatus.name}\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareCsvFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "FinFlow Transactions Export")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Transactions CSV"))
        } catch (_: Exception) {
            // Fallback generic send
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_TEXT, "Exported file saved to: ${file.absolutePath}")
            }
            context.startActivity(Intent.createChooser(intent, "Share Transactions CSV"))
        }
    }
}
