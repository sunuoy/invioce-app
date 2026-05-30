package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import android.widget.Toast
import com.example.data.BusinessProfile
import com.example.data.InvoiceWithDetails
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateInvoicePdf(
        context: Context,
        invoiceWithDetails: InvoiceWithDetails,
        profile: BusinessProfile?
    ): File {
        val invoice = invoiceWithDetails.invoice
        val customer = invoiceWithDetails.customer
        val items = invoiceWithDetails.lineItems

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Paints for drawing
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1E3A8A") // Deep blue primary
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val labelPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val bgPaint = Paint().apply {
            color = Color.parseColor("#F3F4F6") // Cool gray background
            style = Paint.Style.FILL
        }

        // --- PAGE LAYOUT COORDINATES ---
        var yPos = 40f
        val leftMargin = 40f
        val rightMargin = 555f
        val colWidths = floatArrayOf(25f, 180f, 60f, 60f, 60f, 60f, 70f) // S.No, Name, Stock/Unit, Qty, Price, Tax Rate, Total

        // Date format
        val sfd = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val invoiceDate = sfd.format(Date(invoice.dateTimestamp))

        // 1. Header Title
        canvas.drawText("TAX INVOICE", leftMargin, yPos, titlePaint)
        
        // Status stamp
        val statusPaint = Paint().apply {
            color = when (invoice.status) {
                "Paid" -> Color.parseColor("#10B981") // Green
                "Sent" -> Color.parseColor("#3B82F6") // Blue
                else -> Color.parseColor("#F59E0B")   // Amber for Draft
            }
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("[ ${invoice.status.uppercase(Locale.ROOT)} ]", rightMargin - 80f, yPos - 5f, statusPaint)
        
        yPos += 30f
        canvas.drawLine(leftMargin, yPos, rightMargin, yPos, linePaint)
        yPos += 20f

        // 2. Business Profile Details (Left Column)
        val businessX = leftMargin
        canvas.drawText("FROM:", businessX, yPos, labelPaint)
        yPos += 14f
        val bIcon = if (profile == null || profile.shortIcon.isBlank()) "💼" else profile.shortIcon
        val bName = if (profile?.businessName.isNullOrBlank()) "My Business" else profile!!.businessName
        val headerWithIcon = "$bIcon  $bName"
        canvas.drawText(headerWithIcon, businessX, yPos, headerPaint)
        
        if (profile != null) {
            if (profile.address.isNotBlank()) {
                yPos += 13f
                canvas.drawText(profile.address, businessX, yPos, textPaint)
            }
            if (profile.phone.isNotBlank() || profile.email.isNotBlank()) {
                yPos += 13f
                canvas.drawText("Phone: ${profile.phone}  Email: ${profile.email}", businessX, yPos, textPaint)
            }
            if (profile.gstin.isNotBlank()) {
                yPos += 13f
                canvas.drawText("GSTIN: ${profile.gstin}", businessX, yPos, textPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) // reset
            }
        } else {
            yPos += 13f
            canvas.drawText("Setup your profile in Settings Screen", businessX, yPos, textPaint)
        }

        // 3. Invoice Metadata (Right Column overlay)
        val metaX = 350f
        var metaY = yPos - (if (profile?.gstin.isNullOrBlank()) 40f else 54f)
        canvas.drawText("INVOICE DETAILS:", metaX, metaY, labelPaint)
        metaY += 14f
        canvas.drawText("Invoice No: ${invoice.invoiceNumber}", metaX, metaY, headerPaint)
        metaY += 13f
        canvas.drawText("Date: $invoiceDate", metaX, metaY, textPaint)
        
        if (invoice.placeOfSupply.isNotBlank()) {
            metaY += 13f
            canvas.drawText("Place of Supply: ${invoice.placeOfSupply}", metaX, metaY, textPaint)
        }
        if (invoice.vehicleNumber.isNotBlank()) {
            metaY += 13f
            canvas.drawText("Vehicle No: ${invoice.vehicleNumber}", metaX, metaY, textPaint)
        }
        if (invoice.brokerageBy.isNotBlank()) {
            metaY += 13f
            canvas.drawText("Brokerage By: ${invoice.brokerageBy}", metaX, metaY, textPaint)
        }
        
        yPos = maxOf(yPos, metaY) + 25f
        canvas.drawLine(leftMargin, yPos, rightMargin, yPos, linePaint)
        yPos += 20f

        // 4. Customer Details (To Address)
        canvas.drawText("BILL TO:", leftMargin, yPos, labelPaint)
        yPos += 14f
        if (customer != null) {
            canvas.drawText(customer.name, leftMargin, yPos, headerPaint)
            if (customer.address.isNotBlank()) {
                yPos += 13f
                canvas.drawText(customer.address, leftMargin, yPos, textPaint)
            }
            if (customer.phone.isNotBlank() || customer.email.isNotBlank()) {
                yPos += 13f
                val contactInfo = listOfNotNull(
                    customer.phone.takeIf { it.isNotBlank() }?.let { "Phone: $it" },
                    customer.email.takeIf { it.isNotBlank() }?.let { "Email: $it" }
                ).joinToString(" | ")
                canvas.drawText(contactInfo, leftMargin, yPos, textPaint)
            }
            if (customer.gstin.isNotBlank()) {
                yPos += 13f
                canvas.drawText("GSTIN: ${customer.gstin}", leftMargin, yPos, textPaint.apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) })
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
        } else {
            canvas.drawText("Walk-in Customer", leftMargin, yPos, headerPaint)
        }

        yPos += 25f

        // 5. Products/Services Table Headers
        canvas.drawRect(leftMargin, yPos - 12f, rightMargin, yPos + 15f, bgPaint)
        
        var colX = leftMargin
        val headerColLabels = arrayOf("S.N.", "Description / Product", "Unit", "Qty", "Rate", "Tax(%)", "Total")
        for (i in headerColLabels.indices) {
            canvas.drawText(headerColLabels[i], colX + 4f, yPos + 4f, headerPaint)
            colX += colWidths[i]
        }
        
        yPos += 20f

        // 6. Table Rows
        var itemIndex = 1
        for (item in items) {
            // Guard height overflow
            if (yPos > 650f) {
                // simple break or draw warning. For simple invoices, 1 page is plenty. Let's make it robust:
                canvas.drawText("... More items omitted (Multi-page not fully rendered) ...", leftMargin, yPos, textPaint)
                break
            }
            
            canvas.drawLine(leftMargin, yPos + 10f, rightMargin, yPos + 10f, linePaint)
            yPos += 8f

            var colRowX = leftMargin
            // index
            canvas.drawText("$itemIndex", colRowX + 4f, yPos, textPaint)
            colRowX += colWidths[0]

            // productName (truncate if too long)
            val hsnSuffix = if (item.hsnSac.isNotBlank()) " (HSN: ${item.hsnSac})" else ""
            val displayName = if (item.productName.length > 18) item.productName.take(15) + "..." else item.productName
            canvas.drawText("$displayName$hsnSuffix", colRowX + 4f, yPos, textPaint)
            colRowX += colWidths[1]

            // unit
            canvas.drawText(item.unit, colRowX + 4f, yPos, textPaint)
            colRowX += colWidths[2]

            // qty
            canvas.drawText(String.format(Locale.US, "%.1f", item.quantity), colRowX + 4f, yPos, textPaint)
            colRowX += colWidths[3]

            // rate
            canvas.drawText(String.format(Locale.US, "%.2f", item.price), colRowX + 4f, yPos, textPaint)
            colRowX += colWidths[4]

            // taxRate
            canvas.drawText("${item.taxRate}%", colRowX + 4f, yPos, textPaint)
            colRowX += colWidths[5]

            // total
            canvas.drawText(String.format(Locale.US, "%.2f", item.total), colRowX + 4f, yPos, textPaint)

            yPos += 14f
            itemIndex++
        }

        yPos += 15f
        canvas.drawLine(leftMargin, yPos, rightMargin, yPos, linePaint)
        yPos += 15f

        // 7. Calculations Box (Receipt totals aligned to the right)
        val totalsX = 350f
        canvas.drawText("Subtotal:", totalsX, yPos, textPaint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", invoice.subtotal), rightMargin - 70f, yPos, textPaint)
        
        yPos += 15f
        canvas.drawText("GST Tax total:", totalsX, yPos, textPaint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", invoice.taxTotal), rightMargin - 70f, yPos, textPaint)

        if (invoice.taxTotal > 0) {
            yPos += 13f
            canvas.drawText("  CGST (Central GST - 50%):", totalsX, yPos, labelPaint)
            canvas.drawText(String.format(Locale.US, "₹%.2f", invoice.taxTotal / 2.0), rightMargin - 70f, yPos, labelPaint)

            yPos += 13f
            canvas.drawText("  SGST (State GST - 50%):", totalsX, yPos, labelPaint)
            canvas.drawText(String.format(Locale.US, "₹%.2f", invoice.taxTotal / 2.0), rightMargin - 70f, yPos, labelPaint)
        }

        yPos += 16f
        // Grand Total highlights
        val grandPaint = Paint().apply {
            color = Color.parseColor("#111827")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Grand Total:", totalsX, yPos, grandPaint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", invoice.grandTotal), rightMargin - 70f, yPos, grandPaint)

        // 8. Invoice Notes & Terms (Left bottom)
        if (invoice.notes.isNotBlank()) {
            yPos += 30f
            canvas.drawText("NOTES / PAYMENT TERMS:", leftMargin, yPos, labelPaint)
            yPos += 14f
            canvas.drawText(invoice.notes, leftMargin, yPos, textPaint)
        }

        // UPI ID payment details in notes
        if (profile != null && profile.upiId.isNotBlank()) {
            yPos += 20f
            val upiPaint = Paint().apply {
                color = Color.parseColor("#065F46") // Deep forestry green
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("PAYMENT METHOD | UPI ID: ${profile.upiId}", leftMargin, yPos, upiPaint)
        }

        // Footer Sign-off
        val footerY = 800f
        canvas.drawLine(leftMargin, footerY, rightMargin, footerY, linePaint)
        canvas.drawText("Thank you for your business!", leftMargin, footerY + 15f, labelPaint)
        canvas.drawText("Generated via Invoice Generator App", rightMargin - 180f, footerY + 15f, labelPaint)

        pdfDocument.finishPage(page)

        // Save PDF to App Cache directory and return file
        val outputDir = context.cacheDir
        val outputFile = File(outputDir, "Invoice_${invoice.invoiceNumber.replace("/", "_")}.pdf")
        
        if (outputFile.exists()) {
            outputFile.delete()
        }

        pdfDocument.writeTo(FileOutputStream(outputFile))
        pdfDocument.close()

        return outputFile
    }

    fun shareInvoicePdf(context: Context, pdfFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.invoicegenerator.gqtwv.fileprovider",
            pdfFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Invoice: ${pdfFile.name}")
            putExtra(Intent.EXTRA_TEXT, "Hello, please find attached the invoice PDF.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Invoice PDF")
        // Resolve activity to make sure it doesn't crash on tablets
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    fun shareViaWhatsApp(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.invoicegenerator.gqtwv.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Hello, please find attached the invoice PDF.")
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share on WhatsApp")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            val uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.invoicegenerator.gqtwv.fileprovider",
                pdfFile
            )
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    setPackage("com.whatsapp.w4b") // WhatsApp Business
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share on WhatsApp Business")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (ex: Exception) {
                Toast.makeText(context, "WhatsApp not installed. Opening universal share.", Toast.LENGTH_SHORT).show()
                shareInvoicePdf(context, pdfFile)
            }
        }
    }

    fun shareViaEmail(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.invoicegenerator.gqtwv.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Invoice: ${pdfFile.name}")
                putExtra(Intent.EXTRA_TEXT, "Hello,\n\nPlease find attached the invoice PDF.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Send Email")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            shareInvoicePdf(context, pdfFile)
        }
    }

    fun previewPdf(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.invoicegenerator.gqtwv.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Open Invoice PDF")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer available. Try standard sharing.", Toast.LENGTH_SHORT).show()
        }
    }
}
