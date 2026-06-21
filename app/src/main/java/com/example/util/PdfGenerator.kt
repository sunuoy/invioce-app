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
        return Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
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
            textSize = 10.5f
            typeface = getNormalTypeface()
            isAntiAlias = true
        }

        val boldTextPaint = Paint().apply {
            color = textDarkColor
            textSize = 10.5f
            typeface = getBoldTypeface()
            isAntiAlias = true
        }

        val whiteTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10.5f
            typeface = getBoldTypeface()
            isAntiAlias = true
        }

        val footerSmallPaint = Paint().apply {
            color = textMutedColor
            textSize = 7.5f
            typeface = getItalicTypeface()
            isAntiAlias = true
        }

        // --- 1. OUTER DOCUMENT FRAME ---
        val leftBorder = 15f
        val rightBorder = 580f
        val topBorder = 15f
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
            canvas.rotate(-35f, 297f, 421f) // rotate 35 degrees under central A4 point (297, 421)
            canvas.drawText("DUPLICATE COPY", 297f, 421f, watermarkPaint)
            canvas.restore()
        }

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
        
        canvas.drawText("Page No. 1 of 1", 22f, 28f, headerTextPaint)
        
        // Centered document title
        val titleWidth = headerBoldTextPaint.measureText(docTitle)
        val titleCenter = leftBorder + ((rightBorder - leftBorder) - titleWidth) / 2f
        canvas.drawText(docTitle, titleCenter, 28f, headerBoldTextPaint)

        val rightHeaderLabel = if (invoice.downloadCount >= 3) "DUPLICATE COPY" else "ORIGINAL COPY"
        val rightHeaderWidth = headerTextPaint.measureText(rightHeaderLabel)
        val rightHeaderX = (rightBorder - 22f) - rightHeaderWidth
        canvas.drawText(rightHeaderLabel, rightHeaderX, 28f, headerTextPaint)

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
            textSize = 10.5f
            typeface = getBoldTypeface()
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

        val placeSupplyVal = invoice.placeOfSupply.takeIf { it.isNotBlank() } ?: "N.A."
        canvas.drawText("Place of Supply", midX + 10f, 209f, textPaint)
        canvas.drawText(" : $placeSupplyVal", midX + 115f, 209f, textPaint)

        val vehicleVal = invoice.vehicleNumber.takeIf { it.isNotBlank() } ?: "N.A."
        canvas.drawText("Vehicle Number", midX + 10f, 226f, textPaint)
        canvas.drawText(" : $vehicleVal", midX + 115f, 226f, textPaint)

        val brokerageVal = invoice.brokerageBy.takeIf { it.isNotBlank() } ?: "N.A."
        canvas.drawText("Brokerage By", midX + 10f, 241f, textPaint)
        canvas.drawText(" : $brokerageVal", midX + 115f, 241f, textPaint)

        // --- 5. PRODUCTS / SERVICES TABLE (y: 255 to 550) ---
        canvas.drawLine(leftBorder, 255f, rightBorder, 255f, borderPaint)

        // Columns definition with exact mathematical mappings to span right Border Box nicely
        val tableColsX = floatArrayOf(
            leftBorder,        // Sr (20)
            leftBorder + 20f,  // Item Description (180)
            leftBorder + 200f, // HSN/SAC (45)
            leftBorder + 245f, // Qty (30)
            leftBorder + 275f, // Unit (30)
            leftBorder + 305f, // List Price (50)
            leftBorder + 355f, // Disc (40)
            leftBorder + 395f, // CGST % (40)
            leftBorder + 435f, // SGST % (40)
            leftBorder + 475f  // Amount (90) -> up to right border
        )
        val tableColsWidths = floatArrayOf(20f, 180f, 45f, 30f, 30f, 50f, 40f, 40f, 40f, 90f)

        // Draw Table Header Fill block with Primary solid color
        canvas.drawRect(leftBorder + 0.5f, 255.5f, rightBorder - 0.5f, 275f, primaryFillPaint)
        canvas.drawLine(leftBorder, 275f, rightBorder, 275f, borderPaint)

        // Draw Header Titles inside Column Block
        canvas.drawText("Sr.", tableColsX[0] + 3f, 269f, whiteTextPaint)
        canvas.drawText("Item Description", tableColsX[1] + 4f, 269f, whiteTextPaint)
        canvas.drawText("HSN/SAC", tableColsX[2] + 4f, 269f, whiteTextPaint)
        canvas.drawText("Qty", tableColsX[3] + 4f, 269f, whiteTextPaint)
        canvas.drawText("Unit", tableColsX[4] + 4f, 269f, whiteTextPaint)
        canvas.drawText("List Price", tableColsX[5] + 4f, 269f, whiteTextPaint)
        canvas.drawText("Disc.", tableColsX[6] + 4f, 269f, whiteTextPaint)
        canvas.drawText("CGST %", tableColsX[7] + 4f, 269f, whiteTextPaint)
        canvas.drawText("SGST %", tableColsX[8] + 4f, 269f, whiteTextPaint)
        canvas.drawText("Amount (₹)", tableColsX[9] + 4f, 269f, whiteTextPaint)

        val rowShadingPaint = Paint().apply {
            color = Color.parseColor(themeBgHex) // Theme unified soft color tint
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        var tableRowY = 287f
        var totalAmountBeforeDisc = 0.0

        for (idx in items.indices) {
            val item = items[idx]
            if (tableRowY > 495f) {
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

            canvas.drawText("${idx + 1}", tableColsX[0] + 5f, tableRowY, textPaint)
            canvas.drawText(displayName, tableColsX[1] + 5f, tableRowY, textPaint)
            canvas.drawText(hsnSacVal, tableColsX[2] + 4f, tableRowY, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.quantity), tableColsX[3] + 4f, tableRowY, textPaint)
            canvas.drawText(item.unit.takeIf { it.isNotBlank() } ?: "N.A.", tableColsX[4] + 4f, tableRowY, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.price), tableColsX[5] + 4f, tableRowY, textPaint)
            
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
            
            // CGST & SGST are split evenly from total tax rate
            val modelTaxRate = item.taxRate
            val cgstRate = modelTaxRate / 2.0
            val sgstRate = modelTaxRate / 2.0
            canvas.drawText(String.format(Locale.US, "%.2f", cgstRate), tableColsX[7] + 4f, tableRowY, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", sgstRate), tableColsX[8] + 4f, tableRowY, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.total), tableColsX[9] + 6f, tableRowY, textPaint)

            tableRowY += 16f
        }

        // Draw table boundary vertical lines to make it look like a grids ledger
        val tableBottomY = 514f
        canvas.drawLine(leftBorder, tableBottomY, rightBorder, tableBottomY, borderPaint)
        for (colXValue in tableColsX) {
            canvas.drawLine(colXValue, 255f, colXValue, tableBottomY, borderPaint)
        }
        canvas.drawLine(rightBorder, 255f, rightBorder, tableBottomY, borderPaint) // Outer right boundary line

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
        canvas.drawText(String.format(Locale.US, "- %.2f", totalDiscountValue), tableColsX[9] + 6f, 573.5f, textPaint)

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
        canvas.drawText("Rupees $amountInWords Only", leftBorder + 10f, 607.5f, boldTextPaint.apply { color = primaryColor; textSize = 9.0f })
        boldTextPaint.color = textDarkColor // reset
        boldTextPaint.textSize = 10.5f // reset

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
        canvas.drawText(settledText, leftBorder + 10f, 620.5f, boldTextPaint.apply { color = textMutedColor; textSize = 7.5f })
        boldTextPaint.color = textDarkColor // reset
        boldTextPaint.textSize = 10.5f // reset

        // --- 7. THREE-PANE LOWER FOOTER BLOCK (y: 624 to 827) ---
        val footerPaneWidth = 188f
        val pane1X = leftBorder // 15
        val pane2X = leftBorder + footerPaneWidth // 203
        val pane3X = pane2X + footerPaneWidth // 391
        
        canvas.drawLine(pane2X, 624f, pane2X, bottomBorder, borderPaint)
        canvas.drawLine(pane3X, 624f, pane3X, bottomBorder, borderPaint)

        // 7A: PANEL ONE - TERMS AND CONDITIONS (Left Column)
        canvas.drawText("Terms and Conditions", pane1X + 8f, 642f, boldTextPaint.apply { textSize = 10.5f })
        boldTextPaint.textSize = 10.5f // reset
        
        canvas.drawText("E & O.E", pane1X + 8f, 656f, footerSmallPaint)
        canvas.drawText("1. Goods once sold will not be taken", pane1X + 8f, 672f, textPaint.apply { textSize = 7.5f })
        canvas.drawText("   back.", pane1X + 8f, 683f, textPaint)
        
        canvas.drawText("2. Interest @ 18% p.a. will be charged if", pane1X + 8f, 699f, textPaint)
        canvas.drawText("   payment is not made on time.", pane1X + 8f, 710f, textPaint)
        
        canvas.drawText("3. Subject to 'Delhi' Jurisdiction only.", pane1X + 8f, 726f, textPaint)
        textPaint.textSize = 10.5f // reset

        // Note details printed below terms & conditions
        if (invoice.notes.isNotBlank()) {
            canvas.drawText("Note:", pane1X + 8f, 745f, boldTextPaint.apply { textSize = 7.5f })
            boldTextPaint.textSize = 10.5f // reset
            
            val notesText = invoice.notes
            val maxNotesWidth = footerPaneWidth - 16f
            val words = notesText.split("\\s+".toRegex())
            var currentLine = StringBuilder()
            var currentY = 756f
            val notePaint = Paint(textPaint).apply { textSize = 7.0f }
            
            for (word in words) {
                if (word.isBlank()) continue
                val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
                val measuredWidth = notePaint.measureText(testLine)
                if (measuredWidth <= maxNotesWidth) {
                    currentLine.append(if (currentLine.isEmpty()) word else " $word")
                } else {
                    if (currentY <= bottomBorder - 8f) {
                        canvas.drawText(currentLine.toString(), pane1X + 8f, currentY, notePaint)
                        currentY += 9f
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty() && currentY <= bottomBorder - 8f) {
                canvas.drawText(currentLine.toString(), pane1X + 8f, currentY, notePaint)
            }
        }

        // 7B: PANEL TWO - QR CODE & BANK INFORMATION (Middle Column)
        // Draw REAL dynamically sized Dynamic UPI QR Code containing exact bill total
        val qrSizeInt = 128
        val qrX = pane2X + ((pane3X - pane2X) - qrSizeInt.toFloat()) / 2f
        val qrY = 628f

        val qrBlackPaint = Paint().apply { color = Color.BLACK; style = Paint.Style.FILL }

        if (profile != null && profile.upiId.isNotBlank()) {
            try {
                val encodedPn = android.net.Uri.encode(profile.businessName)
                val upiUri = "upi://pay?pa=${profile.upiId}&pn=$encodedPn&am=${invoice.grandTotal}&cu=INR"
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
        val displayAccountName = if (!profile?.bankAccountName.isNullOrBlank()) profile!!.bankAccountName else bName
        val displayBank = if (!profile?.bankName.isNullOrBlank()) profile!!.bankName else "ICICI Bank"
        val displayAccountNo = if (!profile?.bankAccountNo.isNullOrBlank()) profile!!.bankAccountNo else if (!profile?.upiId.isNullOrBlank()) profile!!.upiId else "123456789"
        val displayBranch = if (!profile?.bankBranch.isNullOrBlank()) profile!!.bankBranch else "Noida"
        val displayIfsc = if (!profile?.bankIfsc.isNullOrBlank()) profile!!.bankIfsc else "ICICI1234"

        textPaint.textSize = 7.5f
        boldTextPaint.textSize = 7.5f

        // Draw with Key bold and Value normal for ultra high quality output styling
        fun drawKeyValue(label: String, value: String, x: Float, y: Float) {
            canvas.drawText(label, x, y, boldTextPaint)
            val labelWidth = boldTextPaint.measureText(label)
            canvas.drawText(value, x + labelWidth, y, textPaint)
        }

        drawKeyValue("Name: ", displayAccountName, pane2X + 8f, 766f)
        drawKeyValue("Bank: ", displayBank, pane2X + 8f, 778f)
        drawKeyValue("A/c No: ", displayAccountNo, pane2X + 8f, 790f)
        drawKeyValue("Branch Name: ", displayBranch, pane2X + 8f, 802f)
        drawKeyValue("IFSC Code: ", displayIfsc, pane2X + 8f, 814f)

        textPaint.textSize = 10.5f // reset
        boldTextPaint.textSize = 10.5f // reset

        // 7C: PANEL THREE - SIGNATURES & CLOSE (Right Column)
        val companySignatureLabel = "For $bName"
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
        val centerSigLabelX = pane3X + ((rightBorder - pane3X) - boldTextPaint.measureText(sigText)) / 2f
        canvas.drawText(sigText, centerSigLabelX, 800f, boldTextPaint)

        // --- 8. CENTRED BRAND CREATION FOOTER REMOVED ---

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
}
