package com.example.util

import androidx.compose.runtime.*
import com.example.data.InvoiceLineItem

data class InvoiceTotals(
    val subtotal: Double,
    val taxTotal: Double,
    val grandTotal: Double
)

object InvoiceCalculator {
    /**
     * Pure helper function to calculate invoice totals for any list of line items.
     */
    fun calculateTotals(items: List<InvoiceLineItem>): InvoiceTotals {
        val subtotal = items.sumOf { it.subtotal }
        val taxTotal = items.sumOf { it.tax }
        val grandTotal = items.sumOf { it.total }
        return InvoiceTotals(subtotal, taxTotal, grandTotal)
    }

    /**
     * Helper to compute single line item subtotal, tax, and total safely.
     */
    fun calculateLineItem(price: Double, quantity: Double, taxRate: Double): LineAmounts {
        val subtotal = price * quantity
        val tax = (subtotal * taxRate) / 100.0
        val total = subtotal + tax
        return LineAmounts(subtotal, tax, total)
    }
}

data class LineAmounts(
    val subtotal: Double,
    val tax: Double,
    val total: Double
)

/**
 * Custom Compose state hook that auto-recalculates subtotal, tax, and grand total of selected items.
 * Uses 'derivedStateOf' to ensure elite performance, skipping unnecessary recomposition passes when
 * non-essential parts of the screen redraw.
 */
@Composable
fun rememberInvoiceTotals(addedItems: List<InvoiceLineItem>): State<InvoiceTotals> {
    return remember(addedItems) {
        derivedStateOf {
            InvoiceCalculator.calculateTotals(addedItems)
        }
    }
}
