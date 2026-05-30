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

        // --- ENHANCED COLOR PALETTE PAINTS ---
        val primaryColor = Color.parseColor("#1E3A8A") // Deep Royal Navy
        val secondaryColor = Color.parseColor("#2563EB") // Accent Blue
        val dividerColor = Color.parseColor("#E2E8F0") // Subtle Gray/Slate
        val textDarkColor = Color.parseColor("#1F2937") // Charcoal Slate (on-surface)
        val textMutedColor = Color.parseColor("#64748B") // Cool Muted Slate
        
        val titlePaint = Paint().apply {
            color = primaryColor
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerPaint = Paint().apply {
            color = textDarkColor
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val whiteHeaderPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = textDarkColor
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val labelPaint = Paint().apply {
            color = textMutedColor
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = dividerColor
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val bgPaint = Paint().apply {
            color = primaryColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val rowEvenPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC") // Very clean subtle row fill
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val topBarPaint = Paint().apply {
            color = primaryColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val accentIndicatorPaint = Paint().apply {
            color = secondaryColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val totalsCardBgPaint = Paint().apply {
            color = Color.parseColor("#F0F5FF") // Clean soft-blue totals tint
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // --- PAGE LAYOUT COORDINATES ---
        var yPos = 40f
        val leftMargin = 40f
        val rightMargin = 555f
        val colWidths = floatArrayOf(25f, 180f, 60f, 60f, 60f, 60f, 70f) // S.No, Name, Stock/Unit, Qty, Price, Tax Rate, Total

        // Date format
        val sfd = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        val invoiceDate = sfd.format(Date(invoice.dateTimestamp))

        // Decorative top bar stripe 
        canvas.drawRect(leftMargin, 15f, rightMargin, 21f, topBarPaint)

        yPos = 55f

        // 1. Header Title
        canvas.drawText("TAX INVOICE", leftMargin, yPos, titlePaint)
        
        // Beautiful Rounded Status Badge Card 
        val statusText = invoice.status.uppercase(Locale.ROOT)
        val (badgeBg, badgeText) = when (invoice.status) {
            "Paid" -> Pair("#DCFCE7", "#166534") // Emerald soft tint
            "Sent" -> Pair("#DBEAFE", "#1E40AF") // Cobalt soft tint
            else -> Pair("#FEF3C7", "#92400E")   // Amber soft tint
        }

        val badgeBgPaint = Paint().apply {
            color = Color.parseColor(badgeBg)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val badgeTextPaint = Paint().apply {
            color = Color.parseColor(badgeText)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Draw status badge
        val badgeRight = rightMargin
        val badgeLeft = rightMargin - 95f
        val badgeTop = yPos - 22f
        val badgeBottom = yPos + 2f
        
        // Draw matching rounded badge
        canvas.drawRoundRect(badgeLeft, badgeTop, badgeRight, badgeBottom, 6f, 6f, badgeBgPaint)
        
        // Center text in badge
        val textWidth = badgeTextPaint.measureText(statusText)
        val textX = badgeLeft + ((badgeRight - badgeLeft) - textWidth) / 2f
        val textY = badgeTop + 15f
        canvas.drawText(statusText, textX, textY, badgeTextPaint)
        
        yPos += 20f
        canvas.drawLine(leftMargin, yPos, rightMargin, yPos, linePaint)
        yPos += 25f

        // 2. Business Profile Details (Left Column)
        val businessX = leftMargin
        
        // Side block decorative line
        canvas.drawRect(businessX, yPos - 11f, businessX + 3f, yPos + 2f, accentIndicatorPaint)
        canvas.drawText("FROM:", businessX + 8f, yPos, labelPaint)
        
        yPos += 16f
        val bIcon = if (profile == null || profile.shortIcon.isBlank()) "💼" else profile.shortIcon
        val bName = if (profile?.businessName.isNullOrBlank()) "My Business" else profile!!.businessName
        val headerWithIcon = "$bIcon  $bName"
        canvas.drawText(headerWithIcon, businessX, yPos, headerPaint.apply { color = primaryColor; textSize = 11f })
        headerPaint.apply { color = textDarkColor; textSize = 10f } // reset
        
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
        
        canvas.drawRect(metaX - 8f, metaY - 11f, metaX - 5f, metaY + 2f, accentIndicatorPaint)
        canvas.drawText("INVOICE DETAILS:", metaX, metaY, labelPaint)
        
        metaY += 16f
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
        yPos += 25f

        // 4. Customer Details (To Address)
        canvas.drawRect(leftMargin, yPos - 11f, leftMargin + 3f, yPos + 2f, accentIndicatorPaint)
        canvas.drawText("BILL TO:", leftMargin + 8f, yPos, labelPaint)
        
        yPos += 16f
        if (customer != null) {
            canvas.drawText(customer.name, leftMargin, yPos, headerPaint.apply { color = primaryColor; textSize = 11f })
            headerPaint.apply { color = textDarkColor; textSize = 10f } // reset
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

        yPos += 28f

        // 5. Products/Services Table Headers (Premium Rich navy Blue filled header block)
        canvas.drawRect(leftMargin, yPos - 12f, rightMargin, yPos + 18f, bgPaint)
        
        var colX = leftMargin
        val headerColLabels = arrayOf("S.N.", "Description / Product", "Unit", "Qty", "Rate", "Tax(%)", "Total")
        for (i in headerColLabels.indices) {
            canvas.drawText(headerColLabels[i], colX + 4f, yPos + 5f, whiteHeaderPaint)
            colX += colWidths[i]
        }
        
        yPos += 24f

        // 6. Table Rows
        var itemIndex = 1
        for (item in items) {
            // Guard height overflow
            if (yPos > 650f) {
                canvas.drawText("... More items omitted (Multi-page not fully rendered) ...", leftMargin, yPos, textPaint)
                break
            }
            
            // Draw alternating shaded rows for even indices
            if (itemIndex % 2 == 0) {
                canvas.drawRect(leftMargin, yPos - 11f, rightMargin, yPos + 11f, rowEvenPaint)
            }
            
            canvas.drawLine(leftMargin, yPos + 11f, rightMargin, yPos + 11f, linePaint)
            yPos += 4f

            var colRowX = leftMargin
            // index
            canvas.drawText("$itemIndex", colRowX + 4f, yPos, textPaint)
            colRowX += colWidths[0]

            // productName (truncate if too long)
            val hsnSuffix = if (item.hsnSac.isNotBlank()) " (HSN: ${item.hsnSac})" else ""
            val displayName = if (item.productName.length > 22) item.productName.take(19) + "..." else item.productName
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

            yPos += 18f
            itemIndex++
        }

        yPos += 10f
        canvas.drawLine(leftMargin, yPos, rightMargin, yPos, linePaint)
        yPos += 15f

        // 7. Premium Calculations Box Card (Beautiful soft-blue border frame container aligned on the right)
        val totalsX = 350f
        val totalsStartY = yPos - 10f
        
        // Let's determine totals box height based on tax lines
        val infoLines = if (invoice.taxTotal > 0) 5 else 3
        val totalsHeight = infoLines * 18f + 14f
        
        // Draw elegant card panel highlight background
        canvas.drawRoundRect(totalsX - 10f, totalsStartY, rightMargin, totalsStartY + totalsHeight, 8f, 8f, totalsCardBgPaint)
        // Accent line on left side of Card
        canvas.drawRect(totalsX - 10f, totalsStartY, totalsX - 7f, totalsStartY + totalsHeight, accentIndicatorPaint)
        
        yPos += 10f
        canvas.drawText("Subtotal:", totalsX, yPos, textPaint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", invoice.subtotal), rightMargin - 70f, yPos, textPaint)
        
        yPos += 18f
        canvas.drawText("GST Tax total:", totalsX, yPos, textPaint)
        canvas.drawText(String.format(Locale.US, "₹%.2f", invoice.taxTotal), rightMargin - 70f, yPos, textPaint)

        if (invoice.taxTotal > 0) {
            val baseGstPercent = if (invoice.subtotal > 0) (invoice.taxTotal / invoice.subtotal) * 100.0 else 0.0
            val halfGstPercent = baseGstPercent / 2.0
            val percentStr = if (halfGstPercent % 1.0 == 0.0) {
                String.format(Locale.US, "%.0f%%", halfGstPercent)
            } else {
                String.format(Locale.US, "%.2f%%", halfGstPercent)
            }
            
            yPos += 18f
            canvas.drawText("  CGST (Central - $percentStr):", totalsX, yPos, labelPaint.apply { textSize = 8.5f })
            canvas.drawText(String.format(Locale.US, "₹%.2f", invoice.taxTotal / 2.0), rightMargin - 70f, yPos, labelPaint)

            yPos += 18f
            canvas.drawText("  SGST (State - $percentStr):", totalsX, yPos, labelPaint)
            canvas.drawText(String.format(Locale.US, "₹%.2f", invoice.taxTotal / 2.0), rightMargin - 70f, yPos, labelPaint)
            
            labelPaint.apply { textSize = 9f } // reset
        }

        yPos += 20f
        // Separator inside Totals Card
        canvas.drawLine(totalsX - 4f, yPos - 10f, rightMargin - 10f, yPos - 10f, linePaint)
        
        val grandPaint = Paint().apply {
            color = primaryColor
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
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
