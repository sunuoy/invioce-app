package com.example.ui.screens.invoices

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InvoiceWithDetails
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun InvoiceStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon) = when (status) {
        "Paid" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Icons.Default.CheckCircle)
        "Partial" -> Triple(Color(0xFFE0F2FE), Color(0xFF0369A1), Icons.Default.Payments)
        "Sent" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), Icons.Default.Send)
        "Draft" -> Triple(Color(0xFFF3E8FF), Color(0xFF7E22CE), Icons.Default.EditNote)
        "Closed" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Icons.Default.Cancel)
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF475569), Icons.Default.Info)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = if (status == "Partial") "Partially Paid" else status,
                color = textColor,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun DocumentTypeBadge(
    documentType: String,
    modifier: Modifier = Modifier
) {
    val isEstimate = documentType.equals("ESTIMATE", ignoreCase = true) || documentType.equals("QUOTATION", ignoreCase = true)
    if (isEstimate) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFFFEF3C7),
            modifier = modifier
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFFB45309),
                    modifier = Modifier.size(11.dp)
                )
                Text(
                    text = "ESTIMATE",
                    color = Color(0xFFB45309),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun CatalogInvoiceItemRow(
    item: InvoiceWithDetails,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onViewClicked: () -> Unit
) {
    val inv = item.invoice
    val cust = item.customer
    val sfd = SimpleDateFormat("dd MMM yyyy", Locale.US)
    val dateString = sfd.format(Date(inv.dateTimestamp))
    val isEstimate = inv.documentType.equals("ESTIMATE", ignoreCase = true) || inv.documentType.equals("QUOTATION", ignoreCase = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) {
                    onSelectedChange(!isSelected)
                } else {
                    onViewClicked()
                }
            }
            .testTag("invoice_row_${inv.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = onSelectedChange
                        )
                    }

                    // Avatar Icon
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isEstimate) Color(0xFFFEF3C7) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isEstimate) Icons.Default.Description else Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = if (isEstimate) Color(0xFFB45309) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = inv.invoiceNumber,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            DocumentTypeBadge(documentType = inv.documentType)
                        }
                        Text(
                            text = cust?.name?.takeIf { it.isNotBlank() } ?: "Walking Client",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Amount & Status
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = String.format(Locale.US, "₹%,.2f", inv.grandTotal),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isEstimate) Color(0xFFB45309) else MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    InvoiceStatusBadge(status = inv.status)
                }
            }

            // Partial Payment Progress Bar (if partially paid)
            if (inv.paidAmount > 0.0 && inv.paidAmount < inv.grandTotal) {
                Spacer(modifier = Modifier.height(10.dp))
                val progress = (inv.paidAmount / inv.grandTotal).toFloat().coerceIn(0f, 1f)
                val remaining = (inv.grandTotal - inv.paidAmount).coerceAtLeast(0.0)

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Paid: ₹${String.format(Locale.US, "%,.2f", inv.paidAmount)}",
                            fontSize = 11.sp,
                            color = Color(0xFF0369A1),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Due: ₹${String.format(Locale.US, "%,.2f", remaining)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF0284C7),
                        trackColor = Color(0xFFE2E8F0)
                    )
                }
            }

            // Footer meta: items count and date
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${item.lineItems.size} line ${if (item.lineItems.size == 1) "item" else "items"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
