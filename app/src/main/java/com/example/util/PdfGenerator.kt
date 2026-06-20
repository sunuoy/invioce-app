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
        profile: BusinessProfile?
    ): File {
        val invoice = invoiceWithDetails.invoice
        val customer = invoiceWithDetails.customer
        val items = invoiceWithDetails.lineItems

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size: 595 x 842 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // --- ENHANCED COLOR PALETTE ---
        val prefs = context.getSharedPreferences("invoice_generator_prefs", Context.MODE_PRIVATE)
        val selectedTheme = prefs.getString("pdf_theme", "Classic Navy") ?: "Classic Navy"

        val primaryColorHex = when (selectedTheme) {
            "Forest Green" -> "#065F46"
            "Burgundy" -> "#881337"
            "Charcoal" -> "#1E293B"
            "Sunset Indigo" -> "#4338CA"
            else -> "#1E3A8A" // "Classic Navy"
        }
        val secondaryColorHex = when (selectedTheme) {
            "Forest Green" -> "#10B981"
            "Burgundy" -> "#E11D48"
            "Charcoal" -> "#64748B"
            "Sunset Indigo" -> "#6366F1"
            else -> "#2563EB" // "Classic Navy"
        }
        val themeBgHex = when (selectedTheme) {
            "Forest Green" -> "#F0FDF4"
            "Burgundy" -> "#FFF1F2"
            "Charcoal" -> "#F8FAFC"
            "Sunset Indigo" -> "#EEF2FF"
            else -> "#F1F5F9"
        }

        val primaryColor = Color.parseColor(primaryColorHex)
        val secondaryColor = Color.parseColor(secondaryColorHex)
        val dividerColor = Color.parseColor("#94A3B8") // Slate Border Frame Color (94A3B8)
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
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8.5f
            isAntiAlias = true
        }

        // Font Paints
        val titlePaint = Paint().apply {
            color = primaryColor
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerLabelPaint = Paint().apply {
            color = textMutedColor
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = textDarkColor
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = textDarkColor
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val whiteTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val footerSmallPaint = Paint().apply {
            color = textMutedColor
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            isAntiAlias = true
        }

        // --- 1. OUTER DOCUMENT FRAME ---
        val leftBorder = 15f
        val rightBorder = 580f
        val topBorder = 15f
        val bottomBorder = 827f

        // Draw outer box
        canvas.drawRect(leftBorder, topBorder, rightBorder, bottomBorder, borderPaint)

        // --- 2. HEADER TOP BAR (PREMIUM SOLID DESIGN) ---
        val headerBarPaint = Paint().apply {
            color = primaryColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(leftBorder + 0.5f, topBorder + 0.5f, rightBorder - 0.5f, 38f, headerBarPaint)
        canvas.drawLine(leftBorder, 38f, rightBorder, 38f, borderPaint)

        // Document Info Title Type
        val docTitle = if (invoice.taxTotal > 0.0) "TAX INVOICE" else "BILL OF SUPPLY"
        
        val headerTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }
        val headerBoldTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        
        canvas.drawText("Page No. 1 of 1", 22f, 28f, headerTextPaint)
        
        // Centered document title
        val titleWidth = headerBoldTextPaint.measureText(docTitle)
        val titleCenter = leftBorder + ((rightBorder - leftBorder) - titleWidth) / 2f
        canvas.drawText(docTitle, titleCenter, 28f, headerBoldTextPaint)

        canvas.drawText("ORIGINAL COPY", rightBorder - 85f, 28f, headerTextPaint)

        // --- 3. COMPANY / SELLER PROFILE BOX (y: 38 to 125) ---
        canvas.drawLine(leftBorder, 125f, rightBorder, 125f, borderPaint)

        // Logo image / short icon placeholder box
        val logoLeft = 22f
        val logoTop = 45f
        val logoRight = 82f
        val logoBottom = 115f

        // Draw stylized custom vector or monogram brand logo
        drawBrandLogo(canvas, logoLeft, logoTop, logoRight, logoBottom, profile, primaryColor, borderPaint)

        val bName = if (profile?.businessName.isNullOrBlank()) "Add Company Name" else profile.businessName
        val bAddr = if (profile?.address.isNullOrBlank()) "Add Address" else profile.address
        val bPhone = if (profile?.phone.isNullOrBlank()) "+91 9999999999" else profile.phone
        val bEmail = if (profile?.email.isNullOrBlank()) "company@gmail.com" else profile.email
        val bGstin = if (profile?.gstin.isNullOrBlank()) "29AAAAA1234F000" else profile.gstin
        val bUpi = if (profile?.upiId.isNullOrBlank()) "company@upi" else profile.upiId

        // Draw Business Details
        canvas.drawText(bName, 95f, 58f, titlePaint)
        canvas.drawText(bAddr, 95f, 74f, textPaint)
        canvas.drawText("Mobile: $bPhone | Email: $bEmail", 95f, 90f, textPaint)
        
        val gstinAndPan = "GSTIN - $bGstin | PAN - ${bGstin.take(10)}"
        canvas.drawText(gstinAndPan, 95f, 106f, boldTextPaint.apply { color = primaryColor })
        boldTextPaint.color = textDarkColor // reset

        // --- 4. BILLING DETAILS & INVOICE META BLOCK (y: 125 to 245) ---
        val midX = 297f
        canvas.drawLine(leftBorder, 245f, rightBorder, 245f, borderPaint)
        canvas.drawLine(midX, 125f, midX, 245f, borderPaint)

        val subHeadingFillPaint = Paint().apply {
            color = Color.parseColor(themeBgHex)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val subHeadingTextPaint = Paint().apply {
            color = primaryColor
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Shaded headers inside boxes
        canvas.drawRect(leftBorder + 0.5f, 125.5f, midX - 0.5f, 142f, subHeadingFillPaint)
        canvas.drawRect(midX + 0.5f, 125.5f, rightBorder - 0.5f, 142f, subHeadingFillPaint)
        canvas.drawLine(leftBorder, 142f, rightBorder, 142f, borderPaint)

        canvas.drawText("BILL TO (RECIPIENT)", leftBorder + 10f, 137.5f, subHeadingTextPaint)
        canvas.drawText("INVOICE METADATA", midX + 10f, 137.5f, subHeadingTextPaint)

        // Draw Recipient profile details
        val cName = customer?.name ?: "Walking Customer"
        val cGstin = customer?.gstin?.takeIf { it.isNotBlank() } ?: "N.A."
        val cPhone = customer?.phone?.takeIf { it.isNotBlank() } ?: "N.A."
        val cEmail = customer?.email?.takeIf { it.isNotBlank() } ?: "N.A."
        val cAddr = customer?.address?.takeIf { it.isNotBlank() } ?: "Add Address"

        canvas.drawText("Name", leftBorder + 10f, 158f, headerLabelPaint)
        canvas.drawText(": $cName", leftBorder + 65f, 158f, boldTextPaint)
        
        canvas.drawText("GSTIN", leftBorder + 10f, 175f, textPaint)
        canvas.drawText(": $cGstin", leftBorder + 65f, 175f, textPaint)
        
        canvas.drawText("Mobile", leftBorder + 10f, 192f, textPaint)
        canvas.drawText(": $cPhone", leftBorder + 65f, 192f, textPaint)

        canvas.drawText("Email", leftBorder + 10f, 209f, textPaint)
        canvas.drawText(": $cEmail", leftBorder + 65f, 209f, textPaint)

        canvas.drawText("Address", leftBorder + 10f, 226f, textPaint)
        canvas.drawText(": $cAddr", leftBorder + 65f, 226f, textPaint)

        // Draw Invoice Details
        val sfd = SimpleDateFormat("dd-MMM-yy", Locale.US)
        val invoiceDate = sfd.format(Date(invoice.dateTimestamp))
        val dueDate = sfd.format(Date(invoice.dateTimestamp + (5L * 24 * 60 * 60 * 1000))) // Simple 5 days fallback

        canvas.drawText("Invoice No.", midX + 10f, 158f, headerLabelPaint)
        canvas.drawText(" : ${invoice.invoiceNumber}", midX + 115f, 158f, boldTextPaint)

        canvas.drawText("Invoice Date", midX + 10f, 175f, textPaint)
        canvas.drawText(" : $invoiceDate", midX + 115f, 175f, textPaint)

        canvas.drawText("Due Date", midX + 10f, 192f, textPaint)
        canvas.drawText(" : $dueDate", midX + 115f, 192f, textPaint)

        val placeSupplyVal = invoice.placeOfSupply.takeIf { it.isNotBlank() } ?: "3004" // From image check-in demo
        canvas.drawText("Room No. Check In", midX + 10f, 209f, textPaint)
        canvas.drawText(" : $placeSupplyVal", midX + 115f, 209f, textPaint)

        val vehicleVal = invoice.vehicleNumber.takeIf { it.isNotBlank() } ?: "12:10 PM"
        canvas.drawText("Time Check Out", midX + 10f, 226f, textPaint)
        canvas.drawText(" : $vehicleVal", midX + 115f, 226f, textPaint)

        val brokerageVal = invoice.brokerageBy.takeIf { it.isNotBlank() } ?: "05:00 PM"
        canvas.drawText("Time", midX + 10f, 241f, textPaint)
        canvas.drawText(" : $brokerageVal", midX + 115f, 241f, textPaint)

        // --- 5. PRODUCTS / SERVICES TABLE (y: 255 to 550) ---
        canvas.drawLine(leftBorder, 255f, rightBorder, 255f, borderPaint)

        // Columns definition with exact mathematical mappings to span right Border Box nicely
        val tableColsX = floatArrayOf(
            leftBorder,       // Sr (25)
            leftBorder + 25f,  // Item Description (190)
            leftBorder + 215f, // HSN/SAC (50)
            leftBorder + 265f, // Qty (35)
            leftBorder + 300f, // Unit (35)
            leftBorder + 335f, // List Price (55)
            leftBorder + 390f, // Disc (50)
            leftBorder + 440f, // Tax % (45)
            leftBorder + 485f  // Amount (80) -> up to right border
        )
        val tableColsWidths = floatArrayOf(25f, 190f, 50f, 35f, 35f, 55f, 50f, 45f, 80f)

        // Draw Table Header Fill block with Primary solid color
        canvas.drawRect(leftBorder + 0.5f, 255.5f, rightBorder - 0.5f, 275f, primaryFillPaint)
        canvas.drawLine(leftBorder, 275f, rightBorder, 275f, borderPaint)

        // Draw Header Titles inside Column Block
        canvas.drawText("Sr.", tableColsX[0] + 5f, 269f, whiteTextPaint)
        canvas.drawText("Item Description", tableColsX[1] + 5f, 269f, whiteTextPaint)
        canvas.drawText("HSN/SAC", tableColsX[2] + 4f, 269f, whiteTextPaint)
        canvas.drawText("Qty", tableColsX[3] + 4f, 269f, whiteTextPaint)
        canvas.drawText("Unit", tableColsX[4] + 4f, 269f, whiteTextPaint)
        canvas.drawText("List Price", tableColsX[5] + 4f, 269f, whiteTextPaint)
        canvas.drawText("Disc.", tableColsX[6] + 4f, 269f, whiteTextPaint)
        canvas.drawText("Tax %", tableColsX[7] + 4f, 269f, whiteTextPaint)
        canvas.drawText("Amount (₹)", tableColsX[8] + 4f, 269f, whiteTextPaint)

        val rowShadingPaint = Paint().apply {
            color = Color.parseColor(themeBgHex) // Theme unified soft color tint
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        var tableRowY = 280f
        var totalAmountBeforeDisc = 0.0

        for (idx in items.indices) {
            val item = items[idx]
            if (tableRowY > 535f) {
                canvas.drawText("... Extra items content omitted (fits 1 page) ...", tableColsX[1] + 10f, tableRowY + 12f, boldTextPaint)
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

            canvas.drawText("${idx + 1}", tableColsX[0] + 8f, tableRowY, textPaint)
            canvas.drawText(displayName, tableColsX[1] + 6f, tableRowY, textPaint)
            canvas.drawText(hsnSacVal, tableColsX[2] + 6f, tableRowY, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.quantity), tableColsX[3] + 5f, tableRowY, textPaint)
            canvas.drawText(item.unit.takeIf { it.isNotBlank() } ?: "N.A.", tableColsX[4] + 5f, tableRowY, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.price), tableColsX[5] + 5f, tableRowY, textPaint)
            
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
            canvas.drawText(discStr, tableColsX[6] + 4f, tableRowY, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.taxRate), tableColsX[7] + 6f, tableRowY, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.total), tableColsX[8] + 6f, tableRowY, textPaint)

            tableRowY += 16f
        }

        // Draw table boundary vertical lines to make it look like a grids ledger
        val tableBottomY = 550f
        canvas.drawLine(leftBorder, tableBottomY, rightBorder, tableBottomY, borderPaint)
        for (colXValue in tableColsX) {
            canvas.drawLine(colXValue, 255f, colXValue, tableBottomY, borderPaint)
        }
        canvas.drawLine(rightBorder, 255f, rightBorder, tableBottomY, borderPaint) // Outer right boundary line

        // --- 6. TABLE FOOTER CALCULATIONS (y: 550 to 624) ---
        // Discount Row
        val totalDiscountValue = maxOf(0.0, totalAmountBeforeDisc - invoice.subtotal)
        val footerBgPaint = Paint().apply {
            color = Color.parseColor(themeBgHex)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRect(leftBorder + 0.5f, 550.5f, rightBorder - 0.5f, 568f, footerBgPaint)
        canvas.drawLine(leftBorder, 568f, rightBorder, 568f, borderPaint)
        canvas.drawText("Discount", leftBorder + 10f, 562f, textPaint)
        canvas.drawText(String.format(Locale.US, "- %.2f", totalDiscountValue), tableColsX[8] + 6f, 562f, textPaint)

        // Active Total Row
        canvas.drawRect(leftBorder + 0.5f, 568.5f, rightBorder - 0.5f, 588f, footerBgPaint)
        canvas.drawLine(leftBorder, 588f, rightBorder, 588f, borderPaint)
        canvas.drawText("Total Amount (Incl. Taxes)", leftBorder + 10f, 582f, boldTextPaint.apply { color = primaryColor })
        canvas.drawText(String.format(Locale.US, "%,.2f", invoice.grandTotal), tableColsX[8] + 6f, 582f, boldTextPaint)
        boldTextPaint.color = textDarkColor // reset

        // Amount in Words Row
        val amountInWords = convertAmountToWords(invoice.grandTotal)
        canvas.drawRect(leftBorder + 0.5f, 588.5f, rightBorder - 0.5f, 606f, footerBgPaint)
        canvas.drawLine(leftBorder, 606f, rightBorder, 606f, borderPaint)
        canvas.drawText("Rupees $amountInWords Only", leftBorder + 10f, 600f, boldTextPaint.apply { color = primaryColor })
        boldTextPaint.color = textDarkColor // reset

        // Settlement/Balance Summary Row
        canvas.drawRect(leftBorder + 0.5f, 606.5f, rightBorder - 0.5f, 624f, footerBgPaint)
        canvas.drawLine(leftBorder, 624f, rightBorder, 624f, borderPaint)
        
        val settledText = "Settled completely by electronic transfer (Bank / UPI / Card) | Outstanding Balance: 0.00"
        canvas.drawText(settledText, leftBorder + 10f, 618f, boldTextPaint.apply { color = textMutedColor })
        boldTextPaint.color = textDarkColor // reset

        // --- 7. THREE-PANE LOWER FOOTER BLOCK (y: 624 to 827) ---
        val footerPaneWidth = 188f
        val pane1X = leftBorder // 15
        val pane2X = leftBorder + footerPaneWidth // 203
        val pane3X = pane2X + footerPaneWidth // 391
        
        canvas.drawLine(pane2X, 624f, pane2X, bottomBorder, borderPaint)
        canvas.drawLine(pane3X, 624f, pane3X, bottomBorder, borderPaint)

        // 7A: PANEL ONE - TERMS AND CONDITIONS (Left Column)
        canvas.drawText("Terms and Conditions", pane1X + 8f, 642f, boldTextPaint.apply { textSize = 8.5f })
        boldTextPaint.textSize = 8.5f // reset
        
        canvas.drawText("E & O.E", pane1X + 8f, 656f, footerSmallPaint)
        canvas.drawText("1. Goods once sold will not be taken", pane1X + 8f, 672f, textPaint.apply { textSize = 7.5f })
        canvas.drawText("   back.", pane1X + 8f, 683f, textPaint)
        
        canvas.drawText("2. Interest @ 18% p.a. will be charged if", pane1X + 8f, 699f, textPaint)
        canvas.drawText("   payment is not made on time.", pane1X + 8f, 710f, textPaint)
        
        canvas.drawText("3. Subject to 'Delhi' Jurisdiction only.", pane1X + 8f, 726f, textPaint)
        textPaint.textSize = 8.5f // reset

        // 7B: PANEL TWO - QR CODE & BANK INFORMATION (Middle Column)
        // Draw REAL dynamically sized Dynamic UPI QR Code containing exact bill total
        val qrSizeInt = 52
        val qrX = pane2X + ((pane3X - pane2X) - qrSizeInt.toFloat()) / 2f
        val qrY = 635f

        val qrBlackPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }

        if (profile != null && profile.upiId.isNotBlank()) {
            try {
                val upiUri = "upi://pay?pa=${profile.upiId}&pn=${profile.businessName}&am=${invoice.grandTotal}&cu=INR"
                val upiBitmap = generateQrCodeBitmap(upiUri, qrSizeInt)
                canvas.drawBitmap(upiBitmap, qrX, qrY, null)
            } catch (e: Exception) {
                // Draw a fallback framing
                canvas.drawRect(qrX, qrY, qrX + qrSizeInt, qrY + qrSizeInt, borderPaint)
                canvas.drawRect(qrX + 4f, qrY + 4f, qrX + qrSizeInt - 4f, qrY + qrSizeInt - 4f, qrBlackPaint)
            }
        } else {
            // Draw fallback framing if upi id matches blank
            canvas.drawRect(qrX, qrY, qrX + qrSizeInt, qrY + qrSizeInt, borderPaint)
            canvas.drawRect(qrX + 4f, qrY + 4f, qrX + qrSizeInt - 4f, qrY + qrSizeInt - 4f, qrBlackPaint)
        }

        // Bank Details from Profile
        val bankNumberHeader = if (profile?.upiId.isNullOrBlank()) "123456789" else profile?.upiId
        canvas.drawText("Account/UPI ID: $bankNumberHeader", pane2X + 8f, 705f, boldTextPaint.apply { textSize = 7.5f })
        boldTextPaint.textSize = 8.5f // reset

        canvas.drawText("Bank: ICICI Bank", pane2X + 8f, 722f, textPaint.apply { textSize = 7.5f })
        canvas.drawText("IFSC: ICICI1234", pane2X + 8f, 737f, textPaint)
        canvas.drawText("Branch: Noida", pane2X + 8f, 752f, textPaint)
        canvas.drawText("Name: $bName", pane2X + 8f, 767f, textPaint)
        textPaint.textSize = 8.5f // reset

        // 7C: PANEL THREE - SIGNATURES & CLOSE (Right Column)
        val companySignatureLabel = "For $bName"
        val centerSigX = pane3X + ((rightBorder - pane3X) - boldTextPaint.measureText(companySignatureLabel)) / 2f
        canvas.drawText(companySignatureLabel, centerSigX, 642f, boldTextPaint)

        // Signature baseline
        val signatureLineStartX = pane3X + 25f
        val signatureLineEndX = rightBorder - 25f
        val lineSignatureY = 785f
        canvas.drawLine(signatureLineStartX, lineSignatureY, signatureLineEndX, lineSignatureY, borderPaint)

        val sigText = "Authorized Signatory"
        val centerSigLabelX = pane3X + ((rightBorder - pane3X) - boldTextPaint.measureText(sigText)) / 2f
        canvas.drawText(sigText, centerSigLabelX, 800f, boldTextPaint)

        // --- 8. CENTRED BRAND CREATION FOOTER ---
        val platformHeader = "Invoice Created by www.mazu.in"
        val createCenterPlatformLabelX = leftBorder + ((rightBorder - leftBorder) - platformHeaderWidthPaint().measureText(platformHeader)) / 2f
        canvas.drawText(platformHeader, createCenterPlatformLabelX, bottomBorder - 6f, platformHeaderWidthPaint().apply { color = secondaryColor })

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

    private fun platformHeaderWidthPaint(): Paint {
        return Paint().apply {
            textSize = 7.5f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
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

        val logoColorPaint = Paint().apply {
            color = primaryColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        when (logoUrl) {
            "preset_tech" -> {
                val p = android.graphics.Path()
                p.moveTo(centerX + 2f, logoTop + 6f)
                p.lineTo(centerX - 9f, centerY + 2f)
                p.lineTo(centerX + 1f, centerY + 2f)
                p.lineTo(centerX - 2f, logoBottom - 6f)
                p.lineTo(centerX + 9f, centerY - 2f)
                p.lineTo(centerX - 1f, centerY - 2f)
                p.close()
                canvas.drawPath(p, logoColorPaint)
            }
            "preset_crest" -> {
                val p = android.graphics.Path()
                p.moveTo(centerX, logoTop + 6f)
                p.lineTo(logoRight - 8f, logoTop + 10f)
                p.lineTo(logoRight - 8f, centerY + 4f)
                p.quadTo(logoRight - 8f, logoBottom - 8f, centerX, logoBottom - 5f)
                p.quadTo(logoLeft + 8f, logoBottom - 8f, logoLeft + 8f, centerY + 4f)
                p.lineTo(logoLeft + 8f, logoTop + 10f)
                p.close()
                canvas.drawPath(p, logoColorPaint)
                
                val innerPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 9f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText("★", centerX, centerY + 3f, innerPaint)
            }
            "preset_leaf" -> {
                val p = android.graphics.Path()
                p.moveTo(centerX - 6f, logoBottom - 6f)
                p.quadTo(centerX - 10f, centerY - 6f, centerX, logoTop + 6f)
                p.quadTo(centerX + 10f, centerY + 2f, centerX - 6f, logoBottom - 6f)
                p.close()
                canvas.drawPath(p, logoColorPaint)

                val stemPaint = Paint().apply {
                    color = Color.WHITE
                    strokeWidth = 1.2f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                canvas.drawLine(centerX - 5f, logoBottom - 9f, centerX - 1f, centerY, stemPaint)
            }
            "preset_star" -> {
                val p = android.graphics.Path()
                p.moveTo(centerX, logoTop + 5f)
                p.quadTo(centerX, centerY, logoRight - 5f, centerY)
                p.quadTo(centerX, centerY, centerX, logoBottom - 5f)
                p.quadTo(centerX, centerY, logoLeft + 5f, centerY)
                p.quadTo(centerX, centerY, centerX, logoTop + 5f)
                p.close()
                canvas.drawPath(p, logoColorPaint)
            }
            "preset_gear" -> {
                canvas.drawCircle(centerX, centerY, 6f, logoColorPaint)
                val gearOutlinePaint = Paint().apply {
                    color = primaryColor
                    strokeWidth = 2.5f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                canvas.drawCircle(centerX, centerY, 9f, gearOutlinePaint)
                val spokePaint = Paint().apply {
                    color = primaryColor
                    strokeWidth = 2f
                    isAntiAlias = true
                }
                for (i in 0 until 360 step 60) {
                    val angle = Math.toRadians(i.toDouble())
                    val x1 = (centerX + Math.cos(angle) * 6f).toFloat()
                    val y1 = (centerY + Math.sin(angle) * 6f).toFloat()
                    val x2 = (centerX + Math.cos(angle) * 12f).toFloat()
                    val y2 = (centerY + Math.sin(angle) * 12f).toFloat()
                    canvas.drawLine(x1, y1, x2, y2, spokePaint)
                }
            }
            "preset_cart" -> {
                canvas.drawCircle(centerX - 3f, logoBottom - 8f, 1.8f, logoColorPaint)
                canvas.drawCircle(centerX + 3f, logoBottom - 8f, 1.8f, logoColorPaint)
                val linePaint = Paint().apply {
                    color = primaryColor
                    strokeWidth = 1.8f
                    style = Paint.Style.STROKE
                    isAntiAlias = true
                }
                val p = android.graphics.Path()
                p.moveTo(logoLeft + 6f, logoTop + 8f)
                p.lineTo(logoLeft + 11f, logoTop + 8f)
                p.lineTo(centerX - 5f, centerY + 3f)
                p.lineTo(centerX + 7f, centerY + 3f)
                p.lineTo(logoRight - 6f, logoTop + 12f)
                p.lineTo(logoLeft + 11f, logoTop + 12f)
                canvas.drawPath(p, linePaint)
            }
            else -> {
                val bIcon = if (profile == null || profile.shortIcon.isBlank()) "💼" else profile.shortIcon
                val iconBgPaint = Paint().apply {
                    color = Color.parseColor("#EFF6FF")
                    style = Paint.Style.FILL
                    isAntiAlias = true
                }
                canvas.drawRoundRect(logoLeft + 2f, logoTop + 2f, logoRight - 2f, logoBottom - 2f, 4f, 4f, iconBgPaint)
                
                if (bIcon.length <= 2) {
                    val initialPaint = Paint().apply {
                        color = primaryColor
                        textSize = 12f
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(bIcon.uppercase(java.util.Locale.US), centerX, centerY + 4f, initialPaint)
                } else {
                    val emojiIconPaint = Paint().apply {
                        textSize = 14f
                        isAntiAlias = true
                        textAlign = Paint.Align.CENTER
                    }
                    canvas.drawText(bIcon, centerX, centerY + 5.5f, emojiIconPaint)
                }
            }
        }
        
        val borderOutlinePaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        canvas.drawRoundRect(logoLeft, logoTop, logoRight, logoBottom, 6f, 6f, borderOutlinePaint)
    }
}
