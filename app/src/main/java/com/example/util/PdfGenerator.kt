package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.BitmapFactory
import android.graphics.RectF
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

    private fun getNormalTypeface(): Typeface {
        return Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private fun getBoldTypeface(): Typeface {
        return Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun getItalicTypeface(): Typeface {
        return Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private fun englishNumberToWords(number: Long): String {
        if (number == 0L) return "Zero"
        val units = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
            "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
        val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
        
        fun convertLessThanOneThousand(n: Int): String {
            var str = ""
            if (n % 100 < 20) {
                str = units[n % 100]
                val hundreds = n / 100
                if (hundreds > 0) {
                    str = units[hundreds] + " Hundred " + str
                }
            } else {
                str = units[n % 10]
                val ten = (n % 100) / 10
                str = tens[ten] + " " + str
                val hundreds = n / 100
                if (hundreds > 0) {
                    str = units[hundreds] + " Hundred " + str
                }
            }
            return str.trim()
        }
        
        var temp = number
        var result = ""
        
        val bill = temp / 1000000000L
        temp %= 1000000000L
        if (bill > 0) {
            result += convertLessThanOneThousand(bill.toInt()) + " Billion "
        }
        
        val mill = temp / 1000000L
        temp %= 1000000L
        if (mill > 0) {
            result += convertLessThanOneThousand(mill.toInt()) + " Million "
        }
        
        val thousand = temp / 1000L
        temp %= 1000L
        if (thousand > 0) {
            result += convertLessThanOneThousand(thousand.toInt()) + " Thousand "
        }
        
        if (temp > 0) {
            result += convertLessThanOneThousand(temp.toInt())
        }
        
        return result.trim()
    }
    
    private fun convertAmountToWords(amount: Double): String {
        val roundedInUnits = Math.round(amount * 100.0) / 100.0
        val rupees = roundedInUnits.toLong()
        val paise = Math.round((roundedInUnits - rupees) * 100.0)
        
        val rupeesStr = if (rupees > 0) {
            "${englishNumberToWords(rupees)} Rupees"
        } else {
            "Zero Rupees"
        }
        
        val paiseStr = if (paise > 0) {
            " and ${englishNumberToWords(paise)} Paise"
        } else {
            ""
        }
        
        return "$rupeesStr$paiseStr Only"
    }

    fun generateInvoicePdf(
        context: Context,
        invoiceWithDetails: InvoiceWithDetails,
        profile: BusinessProfile?,
        overrideModel: String? = null
    ): File {
        val invoice = invoiceWithDetails.invoice
        val customer = invoiceWithDetails.customer
        val items = invoiceWithDetails.lineItems

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(730, 842, 1).create() // Adjusted Page width: 730 x 842 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val prefs = context.getSharedPreferences("invoice_generator_prefs", Context.MODE_PRIVATE)
        val selectedModel = overrideModel ?: (prefs.getString("pdf_model", "Model 1") ?: "Model 1")
        if (selectedModel.equals("Model 2", ignoreCase = true)) {
            return generateModel2Pdf(context, invoiceWithDetails, profile)
        }

        val selectedTheme = prefs.getString("pdf_theme", "Classic Navy") ?: "Classic Navy"

        val primaryColorHex = when (selectedTheme) {
            "Forest Green" -> "#065F46"
            "Burgundy" -> "#881337"
            "Burnt sienna" -> "#E35336"
            "Modern Minimalist" -> "#0F172A"
            "Royal Violet" -> "#4C1D95"
            else -> "#1E3A8A" // "Classic Navy"
        }
        val secondaryColorHex = when (selectedTheme) {
            "Forest Green" -> "#10B981"
            "Burgundy" -> "#E11D48"
            "Burnt sienna" -> "#F4A460"
            "Modern Minimalist" -> "#64748B"
            "Royal Violet" -> "#A78BFA"
            else -> "#2563EB" // "Classic Navy"
        }
        val themeBgHex = when (selectedTheme) {
            "Forest Green" -> "#F0FDF4"
            "Burgundy" -> "#FFF1F2"
            "Burnt sienna" -> "#F5F5DC"
            "Modern Minimalist" -> "#F1F5F9"
            "Royal Violet" -> "#F5F3FF"
            else -> "#F1F5F9"
        }
        val dividerColorHex = when (selectedTheme) {
            "Burnt sienna" -> "#A0522D"
            "Modern Minimalist" -> "#CBD5E1"
            "Royal Violet" -> "#7C3AED"
            else -> "#94A3B8"
        }

        val primaryColor = Color.parseColor(primaryColorHex)
        val secondaryColor = Color.parseColor(secondaryColorHex)
        val dividerColor = Color.parseColor(dividerColorHex) // Dynamic frame color
        val textDarkColor = Color.parseColor("#0F172A") // Charcoal/Dark Slate (on-surface)
        val textMutedColor = Color.parseColor("#475569") // Cool Muted Slate Grey

        // Paints for drawing
        val borderPaint = Paint().apply {
            color = dividerColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }

        val topBarBgPaint = Paint().apply {
            color = Color.parseColor("#F1F5F9") // Light Slate bar background
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val primaryFillPaint = Paint().apply {
            color = primaryColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val headerFillPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC") // Soft Slate/White block background
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val greenUpiPaint = Paint().apply {
            color = Color.parseColor("#065F46") // Deep forestry green
            typeface = getBoldTypeface()
            textSize = 10.5f
            isAntiAlias = true
        }

        // Font Paints
        val titlePaint = Paint().apply {
            color = primaryColor
            textSize = 14f
            typeface = getBoldTypeface()
            isAntiAlias = true
        }

        val headerLabelPaint = Paint().apply {
            color = textMutedColor
            textSize = 8f
            typeface = getBoldTypeface()
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = textDarkColor
            textSize = 12.5f
            typeface = getNormalTypeface()
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = textDarkColor
            textSize = 12.5f
            typeface = getBoldTypeface()
            isAntiAlias = true
        }

        val whiteTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 12.5f
            typeface = getBoldTypeface()
            isAntiAlias = true
        }

        val footerSmallPaint = Paint().apply {
            color = textMutedColor
            textSize = 12.0f
            typeface = getItalicTypeface()
            isAntiAlias = true
        }

        // --- 1. OUTER DOCUMENT FRAME ---
        val leftBorder = 15f
        val rightBorder = 715f
        val topBorder = 30f
        val bottomBorder = 827f

        // Draw outer box
        canvas.drawRect(leftBorder, topBorder, rightBorder, bottomBorder, borderPaint)

        // Draw background watermark for 4th copy onwards (downloadCount >= 3)
        if (invoice.downloadCount >= 3) {
            canvas.save()
            val watermarkPaint = Paint().apply {
                color = Color.parseColor("#10000000") // ~6% opacity black, highly subtle and visible
                textSize = 60f
                typeface = getBoldTypeface()
                style = Paint.Style.FILL
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.rotate(-35f, 365f, 421f) // rotate 35 degrees under central point (365, 421)
            canvas.drawText("DUPLICATE COPY", 365f, 421f, watermarkPaint)
            canvas.restore()
        }

        // --- 2. HEADER TOP BAR (PREMIUM SOLID DESIGN) ---
        val headerBarPaint = Paint().apply {
            color = primaryColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(leftBorder + 0.5f, topBorder + 0.5f, rightBorder - 0.5f, topBorder + 23f, headerBarPaint)
        canvas.drawLine(leftBorder, topBorder + 23f, rightBorder, topBorder + 23f, borderPaint)

        // Document Info Title Type
        val docTitle = if (invoice.taxTotal > 0.0) "TAX INVOICE" else "BILL OF SUPPLY"
        
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10.5f
            typeface = getNormalTypeface()
            isAntiAlias = true
        }
        val headerBoldTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = getBoldTypeface()
            isAntiAlias = true
        }
        
        canvas.drawText("Page No. 1 of 1", 22f, topBorder + 13f, headerTextPaint)
        
        // Centered document title
        val titleWidth = headerBoldTextPaint.measureText(docTitle)
        val titleCenter = leftBorder + ((rightBorder - leftBorder) - titleWidth) / 2f
        canvas.drawText(docTitle, titleCenter, topBorder + 13f, headerBoldTextPaint)

        val rightHeaderLabel = if (invoice.downloadCount >= 3) "DUPLICATE COPY" else "ORIGINAL COPY"
        val rightHeaderWidth = headerTextPaint.measureText(rightHeaderLabel)
        val rightHeaderX = (rightBorder - 22f) - rightHeaderWidth
        canvas.drawText(rightHeaderLabel, rightHeaderX, topBorder + 13f, headerTextPaint)

        // --- 3. COMPANY / SELLER PROFILE BOX (y: 38 to 125) ---
        canvas.drawLine(leftBorder, topBorder + 110f, rightBorder, topBorder + 110f, borderPaint)

        // Logo image / short icon placeholder box
        val logoLeft = 22f
        val logoTop = topBorder + 30f
        val logoRight = 82f
        val logoBottom = topBorder + 100f

        // Draw stylized custom vector or monogram brand logo
        drawBrandLogo(canvas, logoLeft, logoTop, logoRight, logoBottom, profile, primaryColor, borderPaint)

        val bName = profile?.businessName ?: ""
        val bAddr = profile?.address ?: ""
        val bPhone = profile?.phone ?: ""
        val bEmail = profile?.email ?: ""
        val bGstin = profile?.gstin ?: ""

        var currentHeaderY = topBorder + 43f
        if (bName.isNotBlank()) {
            canvas.drawText(bName, 95f, currentHeaderY, titlePaint.apply { textSize = 18.0f })
            currentHeaderY += 18f
        }
        if (bAddr.isNotBlank()) {
            canvas.drawText(bAddr, 95f, currentHeaderY, textPaint.apply { textSize = 12.5f })
            currentHeaderY += 16f
        }
        
        val contactLine = listOfNotNull(
            bPhone.takeIf { it.isNotBlank() }?.let { "Mobile: $it" },
            bEmail.takeIf { it.isNotBlank() }?.let { "Email: $it" }
        ).joinToString(" | ")
        
        if (contactLine.isNotBlank()) {
            canvas.drawText(contactLine, 95f, currentHeaderY, textPaint)
            currentHeaderY += 16f
        }
        
        if (bGstin.isNotBlank()) {
            val panNumber = if (bGstin.trim().length == 15) bGstin.trim().substring(2, 12) else bGstin.take(10)
            val gstinAndPan = "GSTIN - $bGstin | PAN - $panNumber"
            canvas.drawText(gstinAndPan, 95f, currentHeaderY, boldTextPaint.apply { color = primaryColor; textSize = 12.5f })
            boldTextPaint.color = textDarkColor // reset
        }

        // --- 4. BILLING DETAILS & INVOICE META BLOCK (y: 125 to 245) ---
        val midX = 365f
        canvas.drawLine(leftBorder, topBorder + 230f, rightBorder, topBorder + 230f, borderPaint)
        canvas.drawLine(midX, topBorder + 110f, midX, topBorder + 230f, borderPaint)

        val subHeadingFillPaint = Paint().apply {
            color = Color.parseColor(themeBgHex)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val subHeadingTextPaint = Paint().apply {
            color = primaryColor
            textSize = 10.5f
            typeface = getBoldTypeface()
            isAntiAlias = true
        }

        // Shaded headers inside boxes
        canvas.drawRect(leftBorder + 0.5f, topBorder + 110.5f, midX - 0.5f, topBorder + 127f, subHeadingFillPaint)
        canvas.drawRect(midX + 0.5f, topBorder + 110.5f, rightBorder - 0.5f, topBorder + 127f, subHeadingFillPaint)
        canvas.drawLine(leftBorder, topBorder + 127f, rightBorder, topBorder + 127f, borderPaint)

        canvas.drawText("BILL TO (RECIPIENT)", leftBorder + 10f, topBorder + 122.5f, subHeadingTextPaint)
        canvas.drawText("INVOICE METADATA", midX + 10f, topBorder + 122.5f, subHeadingTextPaint)

        // Draw Recipient profile details
        val cName = customer?.let {
            if (it.companyName.isNotBlank()) "${it.name} (${it.companyName})" else it.name
        } ?: "Walking Customer"
        val cGstin = customer?.gstin?.takeIf { it.isNotBlank() } ?: "N.A."
        val cPhone = customer?.phone?.takeIf { it.isNotBlank() } ?: "N.A."
        val cEmail = customer?.email?.takeIf { it.isNotBlank() } ?: "N.A."
        val cAddr = customer?.address?.takeIf { it.isNotBlank() } ?: "Add Address"

        canvas.drawText("Name", leftBorder + 10f, topBorder + 143f, textPaint)
        canvas.drawText(": $cName", leftBorder + 65f, topBorder + 143f, boldTextPaint)
        
        canvas.drawText("GSTIN", leftBorder + 10f, topBorder + 160f, textPaint)
        canvas.drawText(": $cGstin", leftBorder + 65f, topBorder + 160f, textPaint)
        
        canvas.drawText("Mobile", leftBorder + 10f, topBorder + 177f, textPaint)
        canvas.drawText(": $cPhone", leftBorder + 65f, topBorder + 177f, textPaint)

        canvas.drawText("Email", leftBorder + 10f, topBorder + 194f, textPaint)
        canvas.drawText(": $cEmail", leftBorder + 65f, topBorder + 194f, textPaint)

        canvas.drawText("Address", leftBorder + 10f, topBorder + 211f, textPaint)
        canvas.drawText(": $cAddr", leftBorder + 65f, topBorder + 211f, textPaint)

        // Draw Invoice Details
        val sfd = SimpleDateFormat("dd-MMM-yy", Locale.US)
        val invoiceDate = sfd.format(Date(invoice.dateTimestamp))
        val dueDate = if (invoice.dueDateTimestamp != 0L) {
            sfd.format(Date(invoice.dueDateTimestamp))
        } else {
            sfd.format(Date(invoice.dateTimestamp + (5L * 24 * 60 * 60 * 1000))) // Simple 5 days fallback
        }

        canvas.drawText("Invoice No.", midX + 10f, topBorder + 143f, textPaint)
        canvas.drawText(" : ${invoice.invoiceNumber}", midX + 115f, topBorder + 143f, boldTextPaint)

        canvas.drawText("Invoice Date", midX + 10f, topBorder + 160f, textPaint)
        canvas.drawText(" : $invoiceDate", midX + 115f, topBorder + 160f, textPaint)

        canvas.drawText("Due Date", midX + 10f, topBorder + 177f, textPaint)
        canvas.drawText(" : $dueDate", midX + 115f, topBorder + 177f, textPaint)

        var optionalY = topBorder + 194f
        
        if (invoice.placeOfSupply.isNotBlank()) {
            canvas.drawText("Place of Supply", midX + 10f, optionalY, textPaint)
            canvas.drawText(" : ${invoice.placeOfSupply}", midX + 115f, optionalY, textPaint)
            optionalY += 17f
        }

        if (invoice.vehicleNumber.isNotBlank()) {
            canvas.drawText("Vehicle Number", midX + 10f, optionalY, textPaint)
            canvas.drawText(" : ${invoice.vehicleNumber}", midX + 115f, optionalY, textPaint)
            optionalY += 17f
        }

        if (invoice.brokerageBy.isNotBlank()) {
            canvas.drawText("Brokerage By", midX + 10f, optionalY, textPaint)
            canvas.drawText(" : ${invoice.brokerageBy}", midX + 115f, optionalY, textPaint)
            optionalY += 17f
        }

        // --- 5. PRODUCTS / SERVICES TABLE (y: 255 to 550) ---
        canvas.drawLine(leftBorder, topBorder + 240f, rightBorder, topBorder + 240f, borderPaint)

        // Columns definition with exact mathematical mappings to span right Border Box nicely
        val tableColsX = floatArrayOf(
            leftBorder,        // Sr (20)
            leftBorder + 20f,  // Item Description (185)
            leftBorder + 205f, // HSN/SAC (65)
            leftBorder + 270f, // Qty (45)
            leftBorder + 315f, // Unit (45)
            leftBorder + 360f, // List Price (65)
            leftBorder + 425f, // Disc (50)
            leftBorder + 475f, // Central GST % (55)
            leftBorder + 530f, // State GST % (55)
            leftBorder + 585f  // Amount (115) -> up to right border
        )
        val tableColsWidths = floatArrayOf(20f, 185f, 65f, 45f, 45f, 65f, 50f, 55f, 55f, 115f)

        // Table-specific dedicated text paints to ensure high precision alignment and zero overlap
        val tableTextPaint = Paint().apply {
            color = textDarkColor
            textSize = 11.5f
            typeface = getNormalTypeface()
            isAntiAlias = true
        }
        val tableBoldTextPaint = Paint().apply {
            color = textDarkColor
            textSize = 11.5f
            typeface = getBoldTypeface()
            isAntiAlias = true
        }
        val tableWhiteTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 11.5f
            typeface = getBoldTypeface()
            isAntiAlias = true
        }

        // Draw Table Header Fill block with Primary solid color
        canvas.drawRect(leftBorder + 0.5f, topBorder + 240.5f, rightBorder - 0.5f, topBorder + 260f, primaryFillPaint)
        canvas.drawLine(leftBorder, topBorder + 260f, rightBorder, topBorder + 260f, borderPaint)

        // Draw Header Titles inside Column Block
        canvas.drawText("Sr.", tableColsX[0] + 3f, topBorder + 254f, tableWhiteTextPaint)
        canvas.drawText("Item Description", tableColsX[1] + 4f, topBorder + 254f, tableWhiteTextPaint)
        canvas.drawText("HSN/SAC", tableColsX[2] + 4f, topBorder + 254f, tableWhiteTextPaint)
        canvas.drawText("Qty", tableColsX[3] + 4f, topBorder + 254f, tableWhiteTextPaint)
        canvas.drawText("Unit", tableColsX[4] + 4f, topBorder + 254f, tableWhiteTextPaint)
        canvas.drawText("List Price", tableColsX[5] + 4f, topBorder + 254f, tableWhiteTextPaint)
        canvas.drawText("Disc.", tableColsX[6] + 4f, topBorder + 254f, tableWhiteTextPaint)
        canvas.drawText("CGST %", tableColsX[7] + 4f, topBorder + 254f, tableWhiteTextPaint)
        canvas.drawText("SGST %", tableColsX[8] + 4f, topBorder + 254f, tableWhiteTextPaint)
        canvas.drawText("Amount (₹)", tableColsX[9] + 4f, topBorder + 254f, tableWhiteTextPaint)

        val rowShadingPaint = Paint().apply {
            color = Color.parseColor(themeBgHex) // Theme unified soft color tint
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        var tableRowY = topBorder + 272f
        var totalAmountBeforeDisc = 0.0

        for (idx in items.indices) {
            val item = items[idx]
            if (tableRowY > 495f) {
                canvas.drawText("... Extra items content omitted (fits 1 page) ...", tableColsX[1] + 10f, tableRowY + 12f, tableBoldTextPaint)
                break
            }

            // Alternating shaded background rows
            if ((idx + 1) % 2 == 0) {
                canvas.drawRect(leftBorder + 0.5f, tableRowY - 11f, rightBorder - 0.5f, tableRowY + 5f, rowShadingPaint)
            }

            canvas.drawLine(leftBorder, tableRowY + 5f, rightBorder, tableRowY + 5f, borderPaint)

            // Quantities details
            val displayName = if (item.productName.length > 32) item.productName.take(29) + "..." else item.productName
            val hsnSacVal = item.hsnSac.takeIf { it.isNotBlank() } ?: "9983"

            canvas.drawText("${idx + 1}", tableColsX[0] + 5f, tableRowY, tableTextPaint)
            canvas.drawText(displayName, tableColsX[1] + 5f, tableRowY, tableTextPaint)
            canvas.drawText(hsnSacVal, tableColsX[2] + 4f, tableRowY, tableTextPaint)
            canvas.drawText(String.format(Locale.US, "%,.2f", item.quantity), tableColsX[3] + 4f, tableRowY, tableTextPaint)
            canvas.drawText(item.unit.takeIf { it.isNotBlank() } ?: "N.A.", tableColsX[4] + 4f, tableRowY, tableTextPaint)
            canvas.drawText(String.format(Locale.US, "%,.2f", item.price), tableColsX[5] + 4f, tableRowY, tableTextPaint)
            
            // Computed discount representation
            val lineTotalOriginal = item.price * item.quantity
            totalAmountBeforeDisc += lineTotalOriginal
            val calculatedDiscount = maxOf(0.0, lineTotalOriginal - item.subtotal)
            val computedDiscPercent = if (lineTotalOriginal > 0) (calculatedDiscount / lineTotalOriginal) * 100.0 else 0.0
            
            val discStr = if (computedDiscPercent > 0.0) {
                String.format(Locale.US, "%.2f (%%)", computedDiscPercent)
            } else {
                "0.00"
            }
            canvas.drawText(discStr, tableColsX[6] + 4f, tableRowY, tableTextPaint)
            
            // CGST & SGST are split evenly from total tax rate
            val modelTaxRate = item.taxRate
            val cgstRate = modelTaxRate / 2.0
            val sgstRate = modelTaxRate / 2.0
            canvas.drawText(String.format(Locale.US, "%.2f", cgstRate), tableColsX[7] + 4f, tableRowY, tableTextPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", sgstRate), tableColsX[8] + 4f, tableRowY, tableTextPaint)
            canvas.drawText(String.format(Locale.US, "%,.2f", item.total), tableColsX[9] + 6f, tableRowY, tableTextPaint)

            tableRowY += 16f
        }

        // Draw table boundary vertical lines to make it look like a grids ledger
        val tableBottomY = 514f
        canvas.drawLine(leftBorder, tableBottomY, rightBorder, tableBottomY, borderPaint)
        for (colXValue in tableColsX) {
            canvas.drawLine(colXValue, topBorder + 240f, colXValue, tableBottomY, borderPaint)
        }
        canvas.drawLine(rightBorder, topBorder + 240f, rightBorder, tableBottomY, borderPaint) // Outer right boundary line

        // --- 6. TABLE FOOTER CALCULATIONS (y: 514 to 624) ---
        val totalDiscountValue = maxOf(0.0, totalAmountBeforeDisc - invoice.subtotal)
        val footerBgPaint = Paint().apply {
            color = Color.parseColor(themeBgHex)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val cgstTotal = items.sumOf { it.tax } / 2.0
        val sgstTotal = items.sumOf { it.tax } / 2.0

        // Row 1: Sub Total (Before Tax) (514f to 530f)
        canvas.drawRect(leftBorder + 0.5f, 514.5f, rightBorder - 0.5f, 530f, footerBgPaint)
        canvas.drawLine(leftBorder, 530f, rightBorder, 530f, borderPaint)
        canvas.drawText("Sub Total (Before Tax)", leftBorder + 10f, 525.5f, textPaint)
        canvas.drawText(String.format(Locale.US, "%,.2f", invoice.subtotal), tableColsX[9] + 6f, 525.5f, textPaint)

        // Row 2: CGST (530f to 546f)
        canvas.drawRect(leftBorder + 0.5f, 530.5f, rightBorder - 0.5f, 546f, footerBgPaint)
        canvas.drawLine(leftBorder, 546f, rightBorder, 546f, borderPaint)
        canvas.drawText("CGST Amount (Central Tax)", leftBorder + 10f, 541.5f, textPaint)
        canvas.drawText(String.format(Locale.US, "%,.2f", cgstTotal), tableColsX[9] + 6f, 541.5f, textPaint)

        // Row 3: SGST (546f to 562f)
        canvas.drawRect(leftBorder + 0.5f, 546.5f, rightBorder - 0.5f, 562f, footerBgPaint)
        canvas.drawLine(leftBorder, 562f, rightBorder, 562f, borderPaint)
        canvas.drawText("SGST Amount (State Tax)", leftBorder + 10f, 557.5f, textPaint)
        canvas.drawText(String.format(Locale.US, "%,.2f", sgstTotal), tableColsX[9] + 6f, 557.5f, textPaint)

        // Row 4: Discount (562f to 578f)
        canvas.drawRect(leftBorder + 0.5f, 562.5f, rightBorder - 0.5f, 578f, footerBgPaint)
        canvas.drawLine(leftBorder, 578f, rightBorder, 578f, borderPaint)
        canvas.drawText("Discount", leftBorder + 10f, 573.5f, textPaint)
        canvas.drawText(String.format(Locale.US, "- %,.2f", totalDiscountValue), tableColsX[9] + 6f, 573.5f, textPaint)

        // Row 5: Total Amount (Incl. Taxes) (578f to 598f)
        canvas.drawRect(leftBorder + 0.5f, 578.5f, rightBorder - 0.5f, 598f, footerBgPaint)
        canvas.drawLine(leftBorder, 598f, rightBorder, 598f, borderPaint)
        canvas.drawText("Total Amount (Incl. Taxes)", leftBorder + 10f, 592.5f, boldTextPaint.apply { color = primaryColor })
        canvas.drawText(String.format(Locale.US, "%,.2f", invoice.grandTotal), tableColsX[9] + 6f, 592.5f, boldTextPaint)
        boldTextPaint.color = textDarkColor // reset

        // Row 6: Amount in Words (598f to 611f)
        val amountInWords = convertAmountToWords(invoice.grandTotal)
        canvas.drawRect(leftBorder + 0.5f, 598.5f, rightBorder - 0.5f, 611f, footerBgPaint)
        canvas.drawLine(leftBorder, 611f, rightBorder, 611f, borderPaint)
        canvas.drawText("Rupees $amountInWords Only", leftBorder + 10f, 607.5f, boldTextPaint.apply { color = primaryColor; textSize = 11.0f })
        boldTextPaint.color = textDarkColor // reset

        // Row 7: Settlement/Balance Summary Row (611f to 624f)
        canvas.drawRect(leftBorder + 0.5f, 611.5f, rightBorder - 0.5f, 624f, footerBgPaint)
        canvas.drawLine(leftBorder, 624f, rightBorder, 624f, borderPaint)
        
        val isPaidOrClosed = invoice.status.equals("Paid", ignoreCase = true) || invoice.status.equals("Closed", ignoreCase = true)
        val outstandingVal = if (isPaidOrClosed) 0.0 else invoice.grandTotal
        val settledText = if (isPaidOrClosed) {
            "Settled completely by electronic transfer (Bank / UPI / Card) | Outstanding Balance: 0.00"
        } else {
            "Payment Pending for status: ${invoice.status} | Outstanding Balance: ${String.format(Locale.US, "%,.2f", outstandingVal)}"
        }
        canvas.drawText(settledText, leftBorder + 10f, 620.5f, boldTextPaint.apply { color = textMutedColor; textSize = 9.5f })
        boldTextPaint.color = textDarkColor // reset

        // --- 7. THREE-PANE LOWER FOOTER BLOCK (y: 624 to 827) ---
        val footerPaneWidth = 233.33f
        val pane1X = leftBorder // 15
        val pane2X = leftBorder + footerPaneWidth // 248.33
        val pane3X = pane2X + footerPaneWidth // 481.66
        
        canvas.drawLine(pane2X, 624f, pane2X, bottomBorder, borderPaint)
        canvas.drawLine(pane3X, 624f, pane3X, bottomBorder, borderPaint)

        // 7A: PANEL ONE - TERMS AND CONDITIONS (Left Column)
        val isTermsEnabledInSettings = prefs.getBoolean("show_terms_conditions", true)
        if (isTermsEnabledInSettings) {
            canvas.drawText("Terms and Conditions", pane1X + 8f, 642f, boldTextPaint.apply { textSize = 13.5f })
            
            canvas.drawText("E & O.E", pane1X + 8f, 658f, footerSmallPaint)
            canvas.drawText("1. Goods once sold will not be taken", pane1X + 8f, 676f, textPaint.apply { textSize = 12.0f })
            canvas.drawText("   back.", pane1X + 8f, 690f, textPaint)
            
            canvas.drawText("2. Interest @ 18% p.a. will be charged if", pane1X + 8f, 708f, textPaint)
            canvas.drawText("   payment is not made on time.", pane1X + 8f, 722f, textPaint)
            
            canvas.drawText("3. Subject to 'Delhi' Jurisdiction only.", pane1X + 8f, 740f, textPaint)
        }

        // Note details printed below terms & conditions
        if (invoice.notes.isNotBlank()) {
            val noteStartY = if (isTermsEnabledInSettings) 762f else 642f
            canvas.drawText("Note:", pane1X + 8f, noteStartY, boldTextPaint.apply { textSize = 12.0f })
            
            val notesText = invoice.notes
            val maxNotesWidth = footerPaneWidth - 16f
            val words = notesText.split("\\s+".toRegex())
            var currentLine = StringBuilder()
            var currentY = noteStartY + 14f
            val notePaint = Paint(textPaint).apply { textSize = 11.5f }
            
            for (word in words) {
                if (word.isBlank()) continue
                val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
                val measuredWidth = notePaint.measureText(testLine)
                if (measuredWidth <= maxNotesWidth) {
                    currentLine.append(if (currentLine.isEmpty()) word else " $word")
                } else {
                    if (currentY <= bottomBorder - 8f) {
                        canvas.drawText(currentLine.toString(), pane1X + 8f, currentY, notePaint)
                        currentY += 13f
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty() && currentY <= bottomBorder - 8f) {
                canvas.drawText(currentLine.toString(), pane1X + 8f, currentY, notePaint)
            }
        }

        // 7B: PANEL TWO - BANK INFORMATION & QR CODE (Middle Column)
        // Bank Details from Profile (Printed on top)
        val displayAccountName = if (!profile?.bankAccountName.isNullOrBlank()) profile!!.bankAccountName else bName
        val displayBank = if (!profile?.bankName.isNullOrBlank()) profile!!.bankName else "ICICI Bank"
        val displayAccountNo = if (!profile?.bankAccountNo.isNullOrBlank()) profile!!.bankAccountNo else if (!profile?.upiId.isNullOrBlank()) profile!!.upiId else "123456789"
        val displayBranch = if (!profile?.bankBranch.isNullOrBlank()) profile!!.bankBranch else "Noida"
        val displayIfsc = if (!profile?.bankIfsc.isNullOrBlank()) profile!!.bankIfsc else "ICICI1234"

        // Bank details set size 14.0f
        val bankTextPaint = Paint(textPaint).apply { textSize = 14.0f }
        val bankBoldPaint = Paint(boldTextPaint).apply { textSize = 14.0f }

        // Draw with Key bold and Value normal for ultra high quality output styling
        fun drawKeyValue(label: String, value: String, x: Float, y: Float) {
            canvas.drawText(label, x, y, bankBoldPaint)
            val labelWidth = bankBoldPaint.measureText(label)
            canvas.drawText(value, x + labelWidth, y, bankTextPaint)
        }

        drawKeyValue("Name: ", displayAccountName, pane2X + 8f, 642f)
        drawKeyValue("Bank: ", displayBank, pane2X + 8f, 660f)
        drawKeyValue("A/c No: ", displayAccountNo, pane2X + 8f, 678f)
        drawKeyValue("Branch Name: ", displayBranch, pane2X + 8f, 696f)
        drawKeyValue("IFSC Code: ", displayIfsc, pane2X + 8f, 714f)

        // Draw REAL dynamically sized Dynamic UPI QR Code containing exact bill total (Printed below bank details)
        val qrSizeInt = 98
        val qrX = pane2X + ((pane3X - pane2X) - qrSizeInt.toFloat()) / 2f
        val qrY = 724f

        val isQrEnabledInSettings = prefs.getBoolean("show_pdf_qr", true)
        val hasValidUpiId = profile != null && profile.upiId.trim().isNotBlank()

        if (isQrEnabledInSettings && hasValidUpiId) {
            try {
                val encodedPn = android.net.Uri.encode(profile!!.businessName)
                val upiUri = "upi://pay?pa=${profile.upiId.trim()}&pn=$encodedPn&am=${invoice.grandTotal}&cu=INR"
                val upiBitmap = generateQrCodeBitmap(upiUri, qrSizeInt)
                canvas.drawBitmap(upiBitmap, qrX, qrY, null)
            } catch (_: Exception) {
                // If bitmap generation fails, skip rendering QR frame
            }
        }

        // 7C: PANEL THREE - SIGNATURES & CLOSE (Right Column)
        val companySignatureLabel = "For $bName"
        boldTextPaint.textSize = 12.5f
        val centerSigX = pane3X + ((rightBorder - pane3X) - boldTextPaint.measureText(companySignatureLabel)) / 2f
        canvas.drawText(companySignatureLabel, centerSigX, 642f, boldTextPaint)

        // Draw optional digital signature if configured and enabled
        val isSigEnabled = prefs.getBoolean("authorized_signature_enabled", false)
        val sigPath = prefs.getString("authorized_signature_path", null)
        if (isSigEnabled && !sigPath.isNullOrBlank()) {
            val sigFile = File(sigPath)
            if (sigFile.exists()) {
                try {
                    val sigBitmap = android.graphics.BitmapFactory.decodeFile(sigPath)
                    if (sigBitmap != null) {
                        val sigWidthMax = (rightBorder - pane3X) - 30f // around 160f max width
                        val sigHeightMax = 70f // around 70f max height to fit perfectly
                        
                        val scaleX = sigWidthMax / sigBitmap.width.toFloat()
                        val scaleY = sigHeightMax / sigBitmap.height.toFloat()
                        val scale = minOf(scaleX, scaleY, 1.0f)
                        
                        val finalW = sigBitmap.width * scale
                        val finalH = sigBitmap.height * scale
                        
                        val drawX = pane3X + ((rightBorder - pane3X) - finalW) / 2f
                        val drawY = 780f - finalH
                        
                        val destRect = android.graphics.RectF(drawX, drawY, drawX + finalW, drawY + finalH)
                        
                        val sigPaint = android.graphics.Paint().apply {
                            isAntiAlias = true
                            isFilterBitmap = true
                        }
                        
                        canvas.drawBitmap(sigBitmap, null, destRect, sigPaint)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Signature baseline
        val signatureLineStartX = pane3X + 25f
        val signatureLineEndX = rightBorder - 25f
        val lineSignatureY = 785f
        canvas.drawLine(signatureLineStartX, lineSignatureY, signatureLineEndX, lineSignatureY, borderPaint)

        val sigText = "Authorized Signatory"
        boldTextPaint.textSize = 11.5f
        val centerSigLabelX = pane3X + ((rightBorder - pane3X) - boldTextPaint.measureText(sigText)) / 2f
        canvas.drawText(sigText, centerSigLabelX, 800f, boldTextPaint)
        boldTextPaint.textSize = 10.5f // reset

        // --- 8. CENTRED BRAND CREATION FOOTER REMOVED ---

        pdfDocument.finishPage(page)

        // Draw attached document on page 2 if it exists
        val attachmentPath = invoice.attachmentPath
        if (!attachmentPath.isNullOrEmpty() && attachmentPath != "null" && attachmentPath.isNotBlank()) {
            val file = File(attachmentPath)
            if (file.exists() && file.isFile) {
                try {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        val pageInfo2 = PdfDocument.PageInfo.Builder(730, 842, 2).create()
                        val page2 = pdfDocument.startPage(pageInfo2)
                        val canvas2 = page2.canvas
                        
                        val titlePaint = Paint().apply {
                            color = Color.parseColor(primaryColorHex)
                            typeface = getBoldTypeface()
                            textSize = 14f
                            isAntiAlias = true
                        }
                        canvas2.drawText("Attached Document - Invoice: ${invoice.invoiceNumber}", 30f, 40f, titlePaint)
                        
                        val maxWidth = 670f
                        val maxHeight = 740f
                        val srcWidth = bitmap.width.toFloat()
                        val srcHeight = bitmap.height.toFloat()
                        
                        val scale = Math.min(maxWidth / srcWidth, maxHeight / srcHeight)
                        val destWidth = srcWidth * scale
                        val destHeight = srcHeight * scale
                        
                        val left = 30f + (maxWidth - destWidth) / 2f
                        val top = 60f + (maxHeight - destHeight) / 2f
                        
                        val destRect = RectF(left, top, left + destWidth, top + destHeight)
                        canvas2.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
                        
                        pdfDocument.finishPage(page2)
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PdfGenerator", "Error appending attachment to PDF: ${e.message}", e)
                }
            }
        }

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
            "${context.packageName}.fileprovider",
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

    fun exportPdfToDownloads(context: Context, pdfFile: File, invoiceNumber: String): String? {
        try {
            val fileName = "Invoice_${invoiceNumber.replace("/", "_")}.pdf"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { out ->
                        pdfFile.inputStream().use { input ->
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
                pdfFile.inputStream().use { input ->
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

    fun shareViaWhatsApp(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
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
                "${context.packageName}.fileprovider",
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
                "${context.packageName}.fileprovider",
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

    private fun generateQrCodeBitmap(content: String, size: Int): android.graphics.Bitmap {
        val wm = com.google.zxing.MultiFormatWriter()
        val bitMatrix = wm.encode(content, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
        val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }

    fun previewPdf(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
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

    private fun drawBrandLogo(
        canvas: Canvas,
        logoLeft: Float,
        logoTop: Float,
        logoRight: Float,
        logoBottom: Float,
        profile: BusinessProfile?,
        primaryColor: Int,
        borderPaint: Paint
    ) {
        val centerX = logoLeft + (logoRight - logoLeft) / 2f
        val centerY = logoTop + (logoBottom - logoTop) / 2f

        val logoUrl = profile?.logoUrl ?: ""
        
        // Background card fill
        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#FFFFFF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(logoLeft, logoTop, logoRight, logoBottom, 6f, 6f, cardBgPaint)

        // Check if this is a custom local image path selected by user from file system
        if (logoUrl.isNotBlank() && !logoUrl.startsWith("preset_")) {
            try {
                val f = File(logoUrl)
                if (f.exists()) {
                    val bitmap = BitmapFactory.decodeFile(logoUrl)
                    if (bitmap != null) {
                        val destRect = RectF(logoLeft + 3f, logoTop + 3f, logoRight - 3f, logoBottom - 3f)
                        canvas.drawBitmap(bitmap, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG).apply { isAntiAlias = true })
                        
                        val borderOutlinePaint = Paint().apply {
                            color = Color.parseColor("#CBD5E1")
                            style = Paint.Style.STROKE
                            strokeWidth = 1f
                            isAntiAlias = true
                        }
                        canvas.drawRoundRect(logoLeft, logoTop, logoRight, logoBottom, 6f, 6f, borderOutlinePaint)
                        return
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val bName = if (profile != null && profile.businessName.isNotBlank()) profile.businessName else "My Business"
        val initials = bName.trim().split("\\s+".toRegex()).take(2).map { it.take(1) }.joinToString("").uppercase(java.util.Locale.US)
        val displayInitials = if (initials.isNotBlank()) initials else bName.take(2).uppercase(java.util.Locale.US)
        val iconBgPaint = Paint().apply {
            color = Color.parseColor("#EFF6FF")
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(logoLeft + 2f, logoTop + 2f, logoRight - 2f, logoBottom - 2f, 4f, 4f, iconBgPaint)
        
        val initialPaint = Paint().apply {
            color = primaryColor
            textSize = 12f
            typeface = getBoldTypeface()
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(displayInitials, centerX, centerY + 4f, initialPaint)
        
        val borderOutlinePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(logoLeft, logoTop, logoRight, logoBottom, 6f, 6f, borderOutlinePaint)
    }

    fun shareUpiQrAndLink(
        context: Context,
        upiUri: String,
        amount: Double,
        businessName: String,
        upiId: String
    ) {
        try {
            // 1. Generate QR Code Bitmap
            val size = 512
            val qrBitmap = generateQrCodeBitmap(upiUri, size)

            // 2. Save to cache directory
            val cacheFile = File(context.cacheDir, "UPI_Payment_QR.png")
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            java.io.FileOutputStream(cacheFile).use { out ->
                qrBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            }

            // 3. Get FileProvider URI
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                cacheFile
            )

            // 4. Create Share Intent
            val shareText = "Hello, please pay ₹${String.format(Locale.US, "%,.2f", amount)} to $businessName via UPI ID: $upiId\nPayment Link: $upiUri"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "UPI Payment QR Code")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Payment Link & QR Code")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text-only share
            try {
                val shareText = "Hello, please pay ₹${String.format(Locale.US, "%,.2f", amount)} to $businessName via UPI ID: $upiId\nPayment Link: $upiUri"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                val chooser = Intent.createChooser(shareIntent, "Share Payment Link")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (ex: Exception) {
                Toast.makeText(context, "Cannot share payment link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateModel2Pdf(
        context: Context,
        invoiceWithDetails: InvoiceWithDetails,
        profile: BusinessProfile?
    ): File {
        val invoice = invoiceWithDetails.invoice
        val customer = invoiceWithDetails.customer
        val items = invoiceWithDetails.lineItems

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(730, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val prefs = context.getSharedPreferences("invoice_generator_prefs", Context.MODE_PRIVATE)

        // Borders & Outer Dimensions
        val leftBorder = 15f
        val rightBorder = 715f
        val topBorder = 30f
        val bottomBorder = 827f

        val borderPaint = Paint().apply {
            color = Color.parseColor("#000000")
            style = Paint.Style.STROKE
            strokeWidth = 1.0f
            isAntiAlias = true
        }

        val fillBgPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = getNormalTypeface()
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 10.5f
            typeface = getBoldTypeface()
            isAntiAlias = true
        }

        val headerTitlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 13.0f
            typeface = getBoldTypeface()
            isAntiAlias = true
        }

        val bName = profile?.businessName?.takeIf { it.isNotBlank() } ?: "My Business"
        val bAddr = profile?.address?.takeIf { it.isNotBlank() } ?: "Business Address"
        val bPhone = profile?.phone?.takeIf { it.isNotBlank() } ?: ""
        val bEmail = profile?.email?.takeIf { it.isNotBlank() } ?: ""
        val bGstin = profile?.gstin?.takeIf { it.isNotBlank() } ?: "N.A."

        // 1. Draw Outer Boundary Box
        canvas.drawRect(leftBorder, topBorder, rightBorder, bottomBorder, borderPaint)

        // 2. Top Header Row: Page No (Left) | TAX INVOICE (Center) | Original Copy (Right)
        canvas.drawLine(leftBorder, topBorder + 18f, rightBorder, topBorder + 18f, borderPaint)
        canvas.drawText("Page No. 1 of 1", leftBorder + 6f, topBorder + 13f, textPaint)
        
        val docTitle = if (invoice.taxTotal > 0.0) "TAX INVOICE" else "BILL OF SUPPLY"
        val titleWidth = boldTextPaint.measureText(docTitle)
        canvas.drawText(docTitle, leftBorder + (700f - titleWidth) / 2f, topBorder + 13f, boldTextPaint)

        val copyLabel = if (invoice.downloadCount >= 3) "DUPLICATE COPY" else "Original Copy"
        val copyWidth = textPaint.measureText(copyLabel)
        canvas.drawText(copyLabel, rightBorder - 6f - copyWidth, topBorder + 13f, textPaint)

        // 3. Company Name & Contact Header (Centering Format)
        var curY = topBorder + 34f
        val bNameWidth = headerTitlePaint.measureText(bName)
        canvas.drawText(bName, leftBorder + (700f - bNameWidth) / 2f, curY, headerTitlePaint)
        
        curY += 15f
        val bAddrWidth = textPaint.measureText(bAddr)
        canvas.drawText(bAddr, leftBorder + (700f - bAddrWidth) / 2f, curY, textPaint)

        if (bPhone.isNotBlank() || bEmail.isNotBlank()) {
            curY += 15f
            val contactStr = listOfNotNull(
                if (bPhone.isNotBlank()) "Mobile: $bPhone" else null,
                if (bEmail.isNotBlank()) "Email: $bEmail" else null
            ).joinToString(" | ")
            val contactWidth = boldTextPaint.measureText(contactStr)
            canvas.drawText(contactStr, leftBorder + (700f - contactWidth) / 2f, curY, boldTextPaint)
        }

        curY += 15f
        val gstinStr = "GSTIN - $bGstin"
        val gstinWidth = boldTextPaint.measureText(gstinStr)
        canvas.drawText(gstinStr, leftBorder + (700f - gstinWidth) / 2f, curY, boldTextPaint)

        // Divider below Company Profile (y: 110f)
        val metaTopY = topBorder + 80f
        canvas.drawLine(leftBorder, metaTopY, rightBorder, metaTopY, borderPaint)

        // 4. Two-Column Metadata Box (y: 110f to 210f)
        val midX = 365f
        val metaBottomY = metaTopY + 100f
        canvas.drawLine(midX, metaTopY, midX, metaBottomY, borderPaint)
        canvas.drawLine(leftBorder, metaBottomY, rightBorder, metaBottomY, borderPaint)

        val sfd = SimpleDateFormat("dd-MMM-yyyy", Locale.US)
        val invDateStr = sfd.format(Date(invoice.dateTimestamp))
        val dueDateStr = if (invoice.dueDateTimestamp != 0L) sfd.format(Date(invoice.dueDateTimestamp)) else "N.A."

        // Left Column (Invoice Metadata)
        fun drawMetaPair(label: String, valStr: String, startY: Float) {
            canvas.drawText(label, leftBorder + 8f, startY, boldTextPaint)
            canvas.drawText(": $valStr", leftBorder + 130f, startY, textPaint)
        }

        drawMetaPair("Invoice Number", invoice.invoiceNumber, metaTopY + 15f)
        drawMetaPair("Invoice Date", invDateStr, metaTopY + 30f)
        drawMetaPair("Due date", dueDateStr, metaTopY + 45f)
        drawMetaPair("Place of Supply", if (invoice.placeOfSupply.isNotBlank()) invoice.placeOfSupply else "N.A.", metaTopY + 60f)
        drawMetaPair("Reverse Charge", "No", metaTopY + 75f)

        // Right Column (Transporter Details)
        fun drawTransPair(label: String, valStr: String, startY: Float) {
            canvas.drawText(label, midX + 8f, startY, boldTextPaint)
            canvas.drawText(": $valStr", midX + 135f, startY, textPaint)
        }

        drawTransPair("Transporter Details", "", metaTopY + 15f)
        drawTransPair("Transporter", if (invoice.brokerageBy.isNotBlank()) invoice.brokerageBy else "N.A.", metaTopY + 30f)
        drawTransPair("Vehicle No.", if (invoice.vehicleNumber.isNotBlank()) invoice.vehicleNumber else "N.A.", metaTopY + 45f)
        drawTransPair("Transporter Doc No.", "N.A.", metaTopY + 60f)
        drawTransPair("Transporter Doc Date", invDateStr, metaTopY + 75f)
        drawTransPair("E-Way Bill No.", "N.A.", metaTopY + 90f)

        // 5. Billing Details & Shipping Details Row (y: 210f to 280f)
        val partyBottomY = metaBottomY + 70f
        canvas.drawLine(midX, metaBottomY, midX, partyBottomY, borderPaint)
        canvas.drawLine(leftBorder, partyBottomY, rightBorder, partyBottomY, borderPaint)

        val cName = customer?.let { if (it.companyName.isNotBlank()) "${it.name} (${it.companyName})" else it.name } ?: "Guest Customer"
        val cGstin = customer?.gstin?.takeIf { it.isNotBlank() } ?: "N.A."
        val cPhone = customer?.phone?.takeIf { it.isNotBlank() } ?: "N.A."
        val cEmail = customer?.email?.takeIf { it.isNotBlank() } ?: "N.A."
        val cAddr = customer?.address?.takeIf { it.isNotBlank() } ?: "N.A."

        // Billing Details (Left Panel, Width ~350pt)
        canvas.drawText("Billing Details", leftBorder + 8f, metaBottomY + 15f, boldTextPaint)
        
        canvas.drawText("Name", leftBorder + 8f, metaBottomY + 30f, textPaint)
        val truncatedNameLeft = if (cName.length > 35) cName.take(32) + "..." else cName
        canvas.drawText(": $truncatedNameLeft", leftBorder + 45f, metaBottomY + 30f, boldTextPaint)
        
        val contactLineLeft = "GSTIN: $cGstin | Ph: $cPhone"
        canvas.drawText(contactLineLeft, leftBorder + 8f, metaBottomY + 45f, textPaint)
        
        val truncatedAddrLeft = if (cAddr.length > 55) cAddr.take(52) + "..." else cAddr
        canvas.drawText(truncatedAddrLeft, leftBorder + 8f, metaBottomY + 60f, textPaint)

        // Shipping Details (Right Panel, Width ~350pt)
        canvas.drawText("Shipping Details", midX + 8f, metaBottomY + 15f, boldTextPaint)
        
        canvas.drawText("Name", midX + 8f, metaBottomY + 30f, textPaint)
        val truncatedNameRight = if (cName.length > 35) cName.take(32) + "..." else cName
        canvas.drawText(": $truncatedNameRight", midX + 45f, metaBottomY + 30f, boldTextPaint)
        
        val contactLineRight = "GSTIN: $cGstin | Ph: $cPhone"
        canvas.drawText(contactLineRight, midX + 8f, metaBottomY + 45f, textPaint)
        
        val truncatedAddrRight = if (cAddr.length > 55) cAddr.take(52) + "..." else cAddr
        canvas.drawText(truncatedAddrRight, midX + 8f, metaBottomY + 60f, textPaint)

        // 6. Dynamic IRN / Ack Bar Row (y: 280f to 296f)
        val irnBottomY = partyBottomY + 16f
        canvas.drawLine(leftBorder, irnBottomY, rightBorder, irnBottomY, borderPaint)
        val irnText = "IRN: ${invoice.invoiceNumber.hashCode().toString().replace("-", "")} | Ack Date: $invDateStr"
        canvas.drawText(irnText, leftBorder + 6f, partyBottomY + 12f, boldTextPaint.apply { textSize = 9.0f })
        boldTextPaint.textSize = 10.5f

        // 7. Item Table Header & Grid (y: 296f to 590f)
        val tableTopY = irnBottomY
        val tableHeaderHeight = 22f
        val tableHeaderBottomY = tableTopY + tableHeaderHeight

        canvas.drawLine(leftBorder, tableHeaderBottomY, rightBorder, tableHeaderBottomY, borderPaint)

        val colX = floatArrayOf(
            leftBorder,        // Sr (30)
            leftBorder + 30f,  // Item Description (220)
            leftBorder + 250f, // HSN/SAC (75)
            leftBorder + 325f, // Qty (40)
            leftBorder + 365f, // Unit (40)
            leftBorder + 405f, // List Price (75)
            leftBorder + 480f, // Disc (50)
            leftBorder + 530f, // Tax % (55)
            leftBorder + 585f  // Amount (₹) (115) -> up to 715
        )

        // Draw Column Verticals down to table bottom
        val tableBottomY = 590f
        for (i in 1 until colX.size) {
            canvas.drawLine(colX[i], tableTopY, colX[i], tableBottomY, borderPaint)
        }

        // Draw Column Headers
        fun drawHeaderCell(text: String, startX: Float, endX: Float, alignRight: Boolean = false) {
            val w = boldTextPaint.measureText(text)
            val posX = if (alignRight) endX - w - 4f else startX + 4f
            canvas.drawText(text, posX, tableTopY + 15f, boldTextPaint)
        }

        drawHeaderCell("Sr.", colX[0], colX[1])
        drawHeaderCell("Item Description", colX[1], colX[2])
        drawHeaderCell("HSN/SAC", colX[2], colX[3])
        drawHeaderCell("Qty", colX[3], colX[4], alignRight = true)
        drawHeaderCell("Unit", colX[4], colX[5])
        drawHeaderCell("List Price", colX[5], colX[6], alignRight = true)
        drawHeaderCell("Disc.", colX[6], colX[7], alignRight = true)
        drawHeaderCell("Tax %", colX[7], colX[8], alignRight = true)
        drawHeaderCell("Amount (₹)", colX[8], rightBorder, alignRight = true)

        // Render Table Items
        var itemRowY = tableHeaderBottomY + 16f
        var totalAmountBeforeDisc = 0.0

        items.forEachIndexed { index, item ->
            if (itemRowY <= tableBottomY - 14f) {
                val srStr = (index + 1).toString()
                canvas.drawText(srStr, colX[0] + 6f, itemRowY, textPaint)
                canvas.drawText(item.productName, colX[1] + 4f, itemRowY, boldTextPaint)
                canvas.drawText(item.hsnSac.ifBlank { "N.A." }, colX[2] + 4f, itemRowY, textPaint)

                val qtyStr = String.format(Locale.US, "%.2f", item.quantity)
                val qtyW = textPaint.measureText(qtyStr)
                canvas.drawText(qtyStr, colX[4] - qtyW - 4f, itemRowY, textPaint)

                canvas.drawText(item.unit.ifBlank { "Pcs" }, colX[4] + 4f, itemRowY, textPaint)

                val priceStr = String.format(Locale.US, "%,.2f", item.price)
                val priceW = textPaint.measureText(priceStr)
                canvas.drawText(priceStr, colX[6] - priceW - 4f, itemRowY, textPaint)

                // Dynamic Line Item Discount Calculation
                val lineTotalOriginal = item.price * item.quantity
                totalAmountBeforeDisc += lineTotalOriginal
                val calculatedDiscount = maxOf(0.0, lineTotalOriginal - item.subtotal)
                val computedDiscPercent = if (lineTotalOriginal > 0) (calculatedDiscount / lineTotalOriginal) * 100.0 else 0.0

                val discStr = if (computedDiscPercent > 0.0) {
                    String.format(Locale.US, "%.2f%%", computedDiscPercent)
                } else {
                    "0.00"
                }
                val discW = textPaint.measureText(discStr)
                canvas.drawText(discStr, colX[7] - discW - 4f, itemRowY, textPaint)

                val taxRateStr = String.format(Locale.US, "%.2f", item.taxRate)
                val taxRateW = textPaint.measureText(taxRateStr)
                canvas.drawText(taxRateStr, colX[8] - taxRateW - 4f, itemRowY, textPaint)

                val itemTotal = item.subtotal + (item.subtotal * (item.taxRate / 100.0))
                val totalStr = String.format(Locale.US, "%,.2f", itemTotal)
                val totalW = boldTextPaint.measureText(totalStr)
                canvas.drawText(totalStr, rightBorder - totalW - 4f, itemRowY, boldTextPaint)

                itemRowY += 18f
            }
        }

        // 8. Discount & Total Summary Bar (y: 590f to 640f)
        canvas.drawLine(leftBorder, tableBottomY, rightBorder, tableBottomY, borderPaint)

        // Discount Row
        val discRowY = tableBottomY + 18f
        val totalDiscount = maxOf(0.0, totalAmountBeforeDisc - invoice.subtotal)
        canvas.drawText("Discount", colX[1] + 4f, discRowY - 4f, textPaint)
        val discValStr = String.format(Locale.US, "%,.2f", totalDiscount)
        val discValW = textPaint.measureText(discValStr)
        canvas.drawText(discValStr, rightBorder - discValW - 4f, discRowY - 4f, textPaint)

        canvas.drawLine(leftBorder, discRowY, rightBorder, discRowY, borderPaint)

        // Grand Total Row
        val totalRowY = discRowY + 22f
        canvas.drawText("Total", leftBorder + 80f, totalRowY - 6f, boldTextPaint.apply { textSize = 12.0f })
        val grandTotalStr = String.format(Locale.US, "%,.2f", invoice.grandTotal)
        val grandTotalW = boldTextPaint.measureText(grandTotalStr)
        canvas.drawText(grandTotalStr, rightBorder - grandTotalW - 4f, totalRowY - 6f, boldTextPaint.apply { textSize = 12.0f })
        boldTextPaint.textSize = 10.5f

        canvas.drawLine(leftBorder, totalRowY, rightBorder, totalRowY, borderPaint)

        // 9. Rupees in Words & Settlement Audit Bar (y: 640f to 680f)
        val amountInWords = convertAmountToWords(invoice.grandTotal)
        canvas.drawText("Rs. $amountInWords Only", leftBorder + 8f, totalRowY + 14f, boldTextPaint)

        val isPaid = invoice.status.equals("Paid", ignoreCase = true)
        val settledText = if (isPaid) {
            "Settled by - Bank : ${String.format(Locale.US, "%,.2f", invoice.grandTotal)} | Invoice Balance : 0.00"
        } else {
            "Settled by - Bank : 0.00 | Invoice Balance : ${String.format(Locale.US, "%,.2f", invoice.grandTotal)}"
        }
        canvas.drawText(settledText, leftBorder + 8f, totalRowY + 28f, boldTextPaint)

        val totalCgst = invoice.taxTotal / 2.0
        val totalSgst = invoice.taxTotal / 2.0
        val taxBreakdownStr = "Taxable Value: ${String.format(Locale.US, "%,.2f", invoice.subtotal)} | CGST: ${String.format(Locale.US, "%,.2f", totalCgst)} | SGST: ${String.format(Locale.US, "%,.2f", totalSgst)} | Total GST: ${String.format(Locale.US, "%,.2f", invoice.taxTotal)} | Grand Total: ${String.format(Locale.US, "%,.2f", invoice.grandTotal)}"
        canvas.drawText(taxBreakdownStr, leftBorder + 8f, totalRowY + 40f, boldTextPaint.apply { textSize = 8.5f })
        boldTextPaint.textSize = 10.5f
        textPaint.textSize = 10.5f

        val footerTopY = totalRowY + 46f
        canvas.drawLine(leftBorder, footerTopY, rightBorder, footerTopY, borderPaint)

        // 10. Three Panel Model 2 Footer: Terms | Bank & UPI QR | E-Invoice QR & Signature (y: 686f to 827f)
        val col1End = leftBorder + 160f
        val col2End = leftBorder + 320f
        val col3End = leftBorder + 470f

        canvas.drawLine(col1End, footerTopY, col1End, bottomBorder, borderPaint)
        canvas.drawLine(col3End, footerTopY, col3End, bottomBorder, borderPaint)

        // Panel 1: Terms and Conditions
        val isTermsEnabled = prefs.getBoolean("show_terms_conditions", true)
        if (isTermsEnabled) {
            canvas.drawText("Terms and Conditions", leftBorder + 6f, footerTopY + 14f, boldTextPaint)
            canvas.drawText("E & O.E", leftBorder + 6f, footerTopY + 26f, textPaint.apply { textSize = 9.0f })
            textPaint.textSize = 10.5f

            canvas.drawText("1. Goods once sold will not be", leftBorder + 6f, footerTopY + 40f, textPaint.apply { textSize = 9.0f })
            canvas.drawText("taken back.", leftBorder + 6f, footerTopY + 50f, textPaint)

            canvas.drawText("2. Interest @ 18% p.a. will be", leftBorder + 6f, footerTopY + 64f, textPaint)
            canvas.drawText("charged if payment for $bName", leftBorder + 6f, footerTopY + 74f, textPaint)
            canvas.drawText("is not made in time.", leftBorder + 6f, footerTopY + 84f, textPaint)

            canvas.drawText("3. Subject to jurisdiction", leftBorder + 6f, footerTopY + 108f, textPaint)
            canvas.drawText("only.", leftBorder + 6f, footerTopY + 118f, boldTextPaint.apply { textSize = 9.0f })
            boldTextPaint.textSize = 10.5f
            textPaint.textSize = 10.5f
        }

        // Panel 2: Dynamic Payment UPI QR & Bank Account Details (Center-Left)
        val isQrEnabled = prefs.getBoolean("show_pdf_qr", true)
        val upiId = profile?.upiId?.trim() ?: ""

        val hasQr = isQrEnabled && upiId.isNotBlank()
        if (hasQr) {
            try {
                val encodedPn = android.net.Uri.encode(bName)
                val upiUri = "upi://pay?pa=$upiId&pn=$encodedPn&am=${invoice.grandTotal}&cu=INR"
                val upiBitmap = generateQrCodeBitmap(upiUri, 65)
                canvas.drawBitmap(upiBitmap, col1End + 45f, footerTopY + 4f, null)
            } catch (_: Exception) {}
        }

        var bankY = if (hasQr) footerTopY + 72f else footerTopY + 14f
        val bankAcc = profile?.bankAccountNo?.takeIf { it.isNotBlank() } ?: "N.A."
        val bankN = profile?.bankName?.takeIf { it.isNotBlank() } ?: "N.A."
        val bankIfsc = profile?.bankIfsc?.takeIf { it.isNotBlank() } ?: "N.A."
        val bankBranch = profile?.bankBranch?.takeIf { it.isNotBlank() } ?: "N.A."
        val bankAccName = profile?.bankAccountName?.takeIf { it.isNotBlank() } ?: bName

        canvas.drawText("Account Number:", col1End + 6f, bankY, boldTextPaint.apply { textSize = 9.5f })
        canvas.drawText(bankAcc, col1End + 6f, bankY + 11f, textPaint.apply { textSize = 9.5f })
        canvas.drawText("Bank: $bankN", col1End + 6f, bankY + 22f, boldTextPaint.apply { textSize = 9.5f })
        canvas.drawText("IFSC: $bankIfsc", col1End + 6f, bankY + 33f, boldTextPaint.apply { textSize = 9.5f })
        canvas.drawText("Branch: $bankBranch", col1End + 6f, bankY + 44f, textPaint.apply { textSize = 9.5f })
        canvas.drawText("Name: $bankAccName", col1End + 6f, bankY + 55f, textPaint.apply { textSize = 9.5f })
        boldTextPaint.textSize = 10.5f
        textPaint.textSize = 10.5f

        // Panel 3: E-Invoice QR & Digital Signature (Center-Right to Right)
        canvas.drawText("E-Invoice QR", col3End + 65f, footerTopY + 14f, boldTextPaint)

        // Draw Dynamic E-Invoice Verification QR Code
        try {
            val eInvoiceQrPayload = "Invoice: ${invoice.invoiceNumber} | GSTIN: $bGstin | Total: ${invoice.grandTotal}"
            val eInvBitmap = generateQrCodeBitmap(eInvoiceQrPayload, 100)
            canvas.drawBitmap(eInvBitmap, col3End + 15f, footerTopY + 22f, null)
        } catch (_: Exception) {}

        val sigCompanyLabel = "For $bName"
        val sigCompanyW = boldTextPaint.measureText(sigCompanyLabel)
        canvas.drawText(sigCompanyLabel, rightBorder - sigCompanyW - 8f, footerTopY + 110f, boldTextPaint)

        val sigTextW = textPaint.measureText("Authorized Signatory")
        canvas.drawText("Authorized Signatory", rightBorder - sigTextW - 8f, bottomBorder - 8f, textPaint)

        pdfDocument.finishPage(page)

        val pdfFile = File(context.cacheDir, "Invoice_${invoice.invoiceNumber}.pdf")
        pdfDocument.writeTo(FileOutputStream(pdfFile))
        pdfDocument.close()

        return pdfFile
    }
}

