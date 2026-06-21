package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.Customer
import com.example.data.InvoiceWithDetails
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    private fun String.escapeCsv(): String {
        val clean = this.replace("\r", " ").replace("\n", " ")
        if (clean.contains("\"") || clean.contains(",") || clean.contains(";")) {
            return "\"" + clean.replace("\"", "\"\"") + "\""
        }
        return clean
    }

    fun generateAccountingReportCsv(
        context: Context,
        invoices: List<InvoiceWithDetails>,
        customers: List<Customer>
    ): File? {
        try {
            val csvBuilder = StringBuilder()

            // 1. Title Section
            csvBuilder.append("INVOICE EASY ACCOUNTING REPORT\n")
            csvBuilder.append("Report Generation Date,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
            csvBuilder.append("Total Invoices Record,${invoices.size}\n")
            csvBuilder.append("Total Registered Clients,${customers.size}\n\n")

            // 2. Client Registry Table
            csvBuilder.append("CLIENT DIRECTORY DATABASE\n")
            csvBuilder.append("Client ID,Client Name,Contact Phone,Commercial Email,Billing Street Address,Tax ID/GSTIN,Place of Supply State\n")
            for (customer in customers) {
                csvBuilder.append("${customer.id},")
                csvBuilder.append("${customer.name.escapeCsv()},")
                csvBuilder.append("${customer.phone.escapeCsv()},")
                csvBuilder.append("${customer.email.escapeCsv()},")
                csvBuilder.append("${customer.address.escapeCsv()},")
                csvBuilder.append("${customer.gstin.escapeCsv()},")
                csvBuilder.append("${customer.placeOfSupply.escapeCsv()}\n")
            }
            csvBuilder.append("\n\n")

            // 3. Invoice Records Ledger Table
            csvBuilder.append("INVOICE TRANSACTION records LEDGER\n")
            csvBuilder.append("Invoice Database ID,Invoice Number,Date Stamp,Payment Status,Client Recipient Name,Client Tax ID/GSTIN,Subtotal (INR),Total Tax Amount (INR),Grand Total Amount (INR),Sourcing State / Place of Supply,Transport Vehicle,Brokerage Details,Administrative Notes,Line Items Count,Rendered Line Items Summary List\n")
            
            val sfd = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            for (invoiceWithDetail in invoices) {
                val inv = invoiceWithDetail.invoice
                val cust = invoiceWithDetail.customer
                
                // Construct a text string summarising all line items
                val itemSummaries = invoiceWithDetail.lineItems.joinToString("; ") { item ->
                    "${item.productName} (${item.quantity} ${item.unit} @ ₹${item.price})"
                }

                csvBuilder.append("${inv.id},")
                csvBuilder.append("${inv.invoiceNumber.escapeCsv()},")
                csvBuilder.append("${sfd.format(Date(inv.dateTimestamp))},")
                csvBuilder.append("${inv.status.escapeCsv()},")
                csvBuilder.append("${(cust?.name ?: "Walking Customer").escapeCsv()},")
                csvBuilder.append("${(cust?.gstin ?: "N.A.").escapeCsv()},")
                csvBuilder.append("${String.format(Locale.US, "%.2f", inv.subtotal)},")
                csvBuilder.append("${String.format(Locale.US, "%.2f", inv.taxTotal)},")
                csvBuilder.append("${String.format(Locale.US, "%.2f", inv.grandTotal)},")
                csvBuilder.append("${inv.placeOfSupply.escapeCsv()},")
                csvBuilder.append("${inv.vehicleNumber.escapeCsv()},")
                csvBuilder.append("${inv.brokerageBy.escapeCsv()},")
                csvBuilder.append("${inv.notes.escapeCsv()},")
                csvBuilder.append("${invoiceWithDetail.lineItems.size},")
                csvBuilder.append("${itemSummaries.escapeCsv()}\n")
            }

            // Write File to app cache
            val outputDir = context.cacheDir
            val fileName = "Accounting_Spreadsheet_Invoices_And_Clients.csv"
            val outputFile = File(outputDir, fileName)
            
            if (outputFile.exists()) {
                outputFile.delete()
            }

            FileOutputStream(outputFile).use { out ->
                out.write(csvBuilder.toString().toByteArray(Charsets.UTF_8))
            }

            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun exportCsvReportToDownloads(context: Context, csvFile: File): String? {
        try {
            val fileName = "Accounting_Report_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.csv"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        csvFile.inputStream().use { input ->
                            input.copyTo(out)
                        }
                    }
                    return "Saved to Downloads/$fileName"
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val destFile = File(downloadsDir, fileName)
                csvFile.inputStream().use { input ->
                    destFile.outputStream().use { out ->
                        input.copyTo(out)
                    }
                }
                return destFile.absolutePath
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun shareCsvFile(context: Context, csvFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Clients & Invoices Accounting Excel Report")
                putExtra(Intent.EXTRA_TEXT, "Hello, please find attached the invoices and client database spreadsheet report exported for accounting purposes.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Share Excel Accounting Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing report: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
