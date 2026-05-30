package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.InvoiceWithDetails
import com.example.ui.InvoiceViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: InvoiceViewModel,
    onCreateInvoiceClicked: () -> Unit,
    onViewInvoiceDetails: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val totalSales by viewModel.totalSales.collectAsStateWithLifecycle()
    val outstandingAmount by viewModel.outstandingAmount.collectAsStateWithLifecycle()
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()

    // Filter recent ones (last 5)
    val recentInvoices = remember(invoices) {
        invoices.take(5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = profile?.shortIcon?.takeIf { it.isNotBlank() } ?: "💼",
                                    fontSize = 20.sp
                                )
                            }
                        }
                        Column {
                            Text(
                                text = profile?.businessName?.takeIf { it.isNotBlank() } ?: "My Business",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Revenue Dashboard",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (profile?.gstin?.isNotBlank() == true) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.padding(end = 12.dp)
                        ) {
                            Text(
                                text = "GST Registered",
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                ),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onCreateInvoiceClicked,
                icon = { Icon(Icons.Default.Add, contentDescription = "New Invoice") },
                text = { Text("Generate Bill") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_invoice_fab")
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
          ) {
            // Analytics Cards Block
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Paid Invoices Card (Total Sales)
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("total_sales_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.TrendingUp,
                                        contentDescription = "Sales Tracker",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Total Sales",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = String.format(Locale.US, "₹%.2f", totalSales ?: 0.0),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "Paid invoices only",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // Outstanding (Draft, Sent) Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("outstanding_sales_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AccountBalanceWallet,
                                        contentDescription = "Pending Payments",
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Outstanding",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = String.format(Locale.US, "₹%.2f", outstandingAmount ?: 0.0),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Draft / Sent statuses",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // Canvas Revenue Trend mini-chart (Only render if we have invoices)
            if (invoices.isNotEmpty()) {
                item {
                    Text(
                        text = "Sales Trend Projection",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            InvoiceTrendGraph(
                                invoices = invoices,
                                primaryColor = MaterialTheme.colorScheme.primary,
                                secondaryColor = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // Recent Invoices Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Invoices",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total: ${invoices.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // List of Invoices
            if (recentInvoices.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Description,
                                contentDescription = "None",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Welcome to Invoice Generator!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Generate professional tax invoices instantly. Get started by clicking 'Generate Bill' at the bottom right.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(recentInvoices, key = { it.invoice.id }) { item ->
                    RecentInvoiceItemRow(
                        item = item,
                        onViewClicked = { onViewInvoiceDetails(item.invoice.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecentInvoiceItemRow(
    item: InvoiceWithDetails,
    onViewClicked: () -> Unit
) {
    val dateString = remember(item.invoice.dateTimestamp) {
        val sfd = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault())
        sfd.format(Date(item.invoice.dateTimestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewClicked() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.invoice.invoiceNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    InvoiceStatusBadge(item.invoice.status)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.customer?.name ?: "Walk-in Client",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = dateString,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = String.format(Locale.US, "₹%.2f", item.invoice.grandTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${item.lineItems.size} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun InvoiceStatusBadge(status: String) {
    val color = when (status) {
        "Paid" -> Color(0xFF10B981) // Green
        "Sent" -> Color(0xFF3B82F6) // Blue
        "Closed" -> Color(0xFF6B7280) // Gray for closed status
        else -> Color(0xFFF59E0B)   // Amber
    }

    Surface(
        color = color.copy(alpha = 0.12f),
        contentColor = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun InvoiceTrendGraph(
    invoices: List<InvoiceWithDetails>,
    primaryColor: Color,
    secondaryColor: Color
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (invoices.isEmpty()) return@Canvas

        // Group invoice sales by day (or simply order chronologically up to 10 points)
        val sortedList = invoices.sortedBy { it.invoice.dateTimestamp }.takeLast(10)
        val maxVal = (sortedList.maxOfOrNull { it.invoice.grandTotal } ?: 100.0).coerceAtLeast(100.0)

        val strokePath = Path()
        val fillPath = Path()

        val width = size.width
        val height = size.height

        val stepX = width / (sortedList.size - 1).coerceAtLeast(1)

        sortedList.forEachIndexed { idx, item ->
            // Normalise coordinate
            val fraction = item.invoice.grandTotal / maxVal
            val x = idx * stepX
            val y = height - (fraction.toFloat() * height)

            if (idx == 0) {
                strokePath.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                strokePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (idx == sortedList.size - 1) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Draw background gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw trend stroke line
        drawPath(
            path = strokePath,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx())
        )
    }
}
