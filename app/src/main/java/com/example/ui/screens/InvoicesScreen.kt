package com.example.ui.screens

import android.widget.Toast
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.InvoiceViewModel
import com.example.util.PdfGenerator
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoicesScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null,
    startInCreateMode: Boolean = false,
    onClearCreateMode: (() -> Unit)? = null,
    viewInvoiceId: Int? = null,
    onClearViewInvoiceId: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()
    val clients by viewModel.customers.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    // Selected invoice for detail view sheet
    var activeInvoiceDetails by remember { mutableStateOf<InvoiceWithDetails?>(null) }
    // Navigation inside screener: List or Create Invoice
    var isCreatingInvoice by remember { mutableStateOf(false) }
    var editingInvoice by remember { mutableStateOf<InvoiceWithDetails?>(null) }

    LaunchedEffect(startInCreateMode) {
        if (startInCreateMode) {
            isCreatingInvoice = true
            onClearCreateMode?.invoke()
        }
    }

    LaunchedEffect(viewInvoiceId, invoices) {
        if (viewInvoiceId != null) {
            val matchingInvoice = invoices.find { it.invoice.id == viewInvoiceId }
            if (matchingInvoice != null) {
                activeInvoiceDetails = matchingInvoice
                onClearViewInvoiceId?.invoke()
            }
        }
    }

    // Multi-selection states
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedInvoices = remember { mutableStateListOf<InvoiceWithDetails>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    // Filter invoices
    val filteredInvoices = remember(invoices, selectedFilter, searchQuery) {
        invoices.filter {
            val matchesFilter = selectedFilter == "All" || it.invoice.status == selectedFilter
            val matchesSearch = it.invoice.invoiceNumber.contains(searchQuery, ignoreCase = true) ||
                    (it.customer?.name?.contains(searchQuery, ignoreCase = true) ?: false)
            matchesFilter && matchesSearch
        }
    }

    if (isCreatingInvoice || editingInvoice != null) {
        CreateInvoiceScreen(
            viewModel = viewModel,
            editingInvoice = editingInvoice,
            onBack = {
                isCreatingInvoice = false
                editingInvoice = null
            }
        )
    } else {
        Scaffold(
            topBar = {
                if (isSelectionMode) {
                    TopAppBar(
                        title = { Text("${selectedInvoices.size} Selected", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = {
                                isSelectionMode = false
                                selectedInvoices.clear()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel bulk selection")
                            }
                        },
                        actions = {
                            // Select All / Deselect All trigger
                            IconButton(onClick = {
                                if (selectedInvoices.size == filteredInvoices.size) {
                                    selectedInvoices.clear()
                                } else {
                                    selectedInvoices.clear()
                                    selectedInvoices.addAll(filteredInvoices)
                                }
                            }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Toggle Select All Invoices")
                            }

                            // Excel spreading export
                            IconButton(
                                onClick = {
                                    if (selectedInvoices.isNotEmpty()) {
                                        val csvFile = com.example.util.ExcelExporter.generateAccountingReportCsv(context, selectedInvoices.toList(), clients)
                                        if (csvFile != null) {
                                            com.example.util.ExcelExporter.shareCsvFile(context, csvFile)
                                            Toast.makeText(context, "Accounting spreadsheet generated for selected items!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Export error", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "No invoices selected to export", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.TableView, contentDescription = "Spreadsheet export chosen bills", tint = MaterialTheme.colorScheme.primary)
                            }
                            
                            // Bulk remove action
                            IconButton(
                                onClick = {
                                    if (selectedInvoices.isNotEmpty()) {
                                        showBulkDeleteConfirm = true
                                    } else {
                                        Toast.makeText(context, "No invoices selected", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("bulk_delete_invoices_btn")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Bulk Delete selected invoices", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    )
                } else {
                    TopAppBar(
                        title = { Text("Invoice Catalog", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            if (onMenuClick != null) {
                                IconButton(onClick = onMenuClick, modifier = Modifier.testTag("invoices_menu_btn")) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                                }
                            }
                        },
                        actions = {
                            // Enable bulk deletion modes
                            IconButton(onClick = {
                                isSelectionMode = true
                                selectedInvoices.clear()
                            }, modifier = Modifier.testTag("enter_bulk_select_invoices")) {
                                Icon(Icons.Default.List, contentDescription = "Enable Bulk Delete selection")
                            }

                             IconButton(
                                onClick = {
                                    val csvFile = com.example.util.ExcelExporter.generateAccountingReportCsv(context, invoices, clients)
                                    if (csvFile != null) {
                                        com.example.util.ExcelExporter.shareCsvFile(context, csvFile)
                                        Toast.makeText(context, "Accounting sheet loaded", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Share failure", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("invoices_catalog_excel_btn")
                            ) {
                                Icon(Icons.Default.TableView, contentDescription = "Excel spreadsheet accounting export", tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            floatingActionButton = {
                if (!isSelectionMode) {
                    FloatingActionButton(
                        onClick = { isCreatingInvoice = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.testTag("add_invoice_fab")
                    ) {
                        Icon(Icons.Default.PostAdd, contentDescription = "New Invoice")
                    }
                }
            },
            modifier = modifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search number, client name...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Lookup") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag("invoice_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Filter Buttons Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val statusFilters = listOf("All", "Draft", "Sent", "Paid", "Closed")
                    statusFilters.forEach { item ->
                        val isSelected = selectedFilter == item
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = item },
                            label = { Text(item, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Invoices List
                if (filteredInvoices.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = "Search Empty",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No invoices found",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Add entries using the button on the top right",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredInvoices, key = { it.invoice.id }) { billing ->
                            val isSelected = selectedInvoices.contains(billing)
                            CatalogInvoiceItemRow(
                                item = billing,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onSelectedChange = { selected ->
                                    if (selected) {
                                        if (!selectedInvoices.contains(billing)) selectedInvoices.add(billing)
                                    } else {
                                        selectedInvoices.remove(billing)
                                    }
                                },
                                onViewClicked = { activeInvoiceDetails = billing }
                            )
                        }
                    }
                }
            }
        }
    }

    // Modal dialogue confirming bulk deletion
    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Bulk Deletion") },
            text = { Text("Are you sure you want to permanently delete these ${selectedInvoices.size} selected invoices? Product stock values subtracted during save will NOT be restored automatically unless products are separately updated.") },
            confirmButton = {
                Button(
                    onClick = {
                        val idsToDelete = selectedInvoices.map { it.invoice.id }
                        viewModel.deleteInvoicesBulk(idsToDelete)
                        isSelectionMode = false
                        selectedInvoices.clear()
                        showBulkDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete ${selectedInvoices.size} Invoices")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Invoice Detail Sheet Dialog
    activeInvoiceDetails?.let { billing ->
        ModalBottomSheet(
            onDismissRequest = { activeInvoiceDetails = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            InvoiceDetailLayout(
                item = billing,
                businessProfile = profile,
                onUpdateStatus = { st ->
                    viewModel.updateInvoiceStatus(billing.invoice.id, st)
                    // Update active view
                    activeInvoiceDetails = billing.copy(invoice = billing.invoice.copy(status = st))
                },
                onEdit = {
                    editingInvoice = billing
                    activeInvoiceDetails = null
                },
                onDelete = {
                    viewModel.deleteInvoice(billing.invoice.id)
                    activeInvoiceDetails = null
                    Toast.makeText(context, "Invoice Deleted", Toast.LENGTH_SHORT).show()
                },
                onExportPdf = {
                    try {
                        val pdfFile = PdfGenerator.generateInvoicePdf(context, billing, profile)
                        val exportedMessage = PdfGenerator.exportPdfToDownloads(context, pdfFile, billing.invoice.invoiceNumber)
                        if (exportedMessage != null) {
                            // Increment download count since they exported the copy
                            viewModel.incrementDownloadCount(billing.invoice.id)
                            // Update local sheet details state statically to reflect the increment
                            activeInvoiceDetails = billing.copy(
                                invoice = billing.invoice.copy(downloadCount = billing.invoice.downloadCount + 1)
                            )
                            Toast.makeText(context, "$exportedMessage", Toast.LENGTH_LONG).show()
                            
                            // Automatically open the exported PDF file
                            try {
                                PdfGenerator.previewPdf(context, pdfFile)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to auto-open PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Failed to export PDF locally.", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Export Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onSharePdf = {
                    try {
                        val pdfFile = PdfGenerator.generateInvoicePdf(context, billing, profile)
                        PdfGenerator.shareInvoicePdf(context, pdfFile)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Share Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onPreviewPdf = {
                    try {
                        val pdfFile = PdfGenerator.generateInvoicePdf(context, billing, profile)
                        PdfGenerator.previewPdf(context, pdfFile)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Preview Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onShareWhatsApp = {
                    try {
                        val pdfFile = PdfGenerator.generateInvoicePdf(context, billing, profile)
                        PdfGenerator.shareViaWhatsApp(context, pdfFile)
                    } catch (e: Exception) {
                        Toast.makeText(context, "WhatsApp Share Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onShareEmail = {
                    try {
                        val pdfFile = PdfGenerator.generateInvoicePdf(context, billing, profile)
                        PdfGenerator.shareViaEmail(context, pdfFile)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Email Share Error: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                },
                onDismiss = { activeInvoiceDetails = null }
            )
        }
    }
}

@Composable
fun InvoiceDetailLayout(
    item: InvoiceWithDetails,
    businessProfile: BusinessProfile?,
    onUpdateStatus: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExportPdf: () -> Unit,
    onSharePdf: () -> Unit,
    onPreviewPdf: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onShareEmail: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dateStr = remember(item.invoice.dateTimestamp) {
        val format = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        format.format(Date(item.invoice.dateTimestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 24.dp)
    ) {
        // Sheet Title / Top Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = item.invoice.invoiceNumber,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "Billed on: $dateStr", style = MaterialTheme.typography.bodySmall)
                
                Spacer(modifier = Modifier.height(4.dp))
                val downloads = item.invoice.downloadCount
                val remaining = (3 - downloads).coerceAtLeast(0)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(
                            color = if (downloads >= 3) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (downloads >= 3) Icons.Default.Warning else Icons.Default.DownloadDone,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = if (downloads >= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = if (downloads >= 3) {
                            "Duplicate Copy Mode (Downloaded $downloads times)"
                        } else {
                            "Original COPY ($remaining/3 left before Duplicate PDF)"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (downloads >= 3) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }

        Divider(modifier = Modifier.padding(vertical = 12.dp))

        // Change Status Chips Selector Header
        Text("Change Status:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Draft", "Sent", "Paid", "Closed").forEach { st ->
                val active = item.invoice.status == st
                SuggestionChip(
                    onClick = { onUpdateStatus(st) },
                    label = { Text(st, fontSize = 11.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Actions Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onExportPdf,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Download, contentDescription = "PDF Export", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export", fontSize = 12.sp)
            }

            Button(
                onClick = onSharePdf,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "PDF Share", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share", fontSize = 12.sp)
            }

            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit invoice details",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.errorContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // WhatsApp Share & PDF Preview Row
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onShareWhatsApp,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "WhatsApp", modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("WhatsApp", fontSize = 11.sp, color = Color.White)
            }

            Button(
                onClick = onPreviewPdf,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Preview", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Spacer(modifier = Modifier.width(4.dp))
                Text("PDF Preview", fontSize = 11.sp, color = MaterialTheme.colorScheme.onTertiaryContainer)
            }
        }

        // UPI Pay Card if Business profile has UPI ID
        if (businessProfile != null && businessProfile.upiId.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Business UPI Payment Details:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(businessProfile.upiId, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Total Bill: ₹${String.format(Locale.US, "%.2f", item.invoice.grandTotal)}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = {
                                try {
                                    val encodedPn = android.net.Uri.encode(businessProfile.businessName)
                                    val upiUri = "upi://pay?pa=${businessProfile.upiId}&pn=$encodedPn&am=${item.invoice.grandTotal}&cu=INR"
                                    PdfGenerator.shareUpiQrAndLink(
                                        context = context,
                                        upiUri = upiUri,
                                        amount = item.invoice.grandTotal,
                                        businessName = businessProfile.businessName,
                                        upiId = businessProfile.upiId
                                    )
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot share UPI link", Toast.LENGTH_SHORT).show()
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = "Pay UPI link", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share Pay Link", fontSize = 10.sp)
                        }
                    }

                    // REAL UPI QR Code scan & pay visualization
                    Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    Text("Scan to Pay Instantly", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    UpiQrImage(
                        upiId = businessProfile.upiId,
                        businessName = businessProfile.businessName,
                        amount = item.invoice.grandTotal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Client info & transport details card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Client/Customer Details:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(item.customer?.name ?: "Walk-in Guest", fontWeight = FontWeight.Bold)
                item.customer?.let { client ->
                    if (client.phone.isNotBlank()) Text("Phone: ${client.phone}", style = MaterialTheme.typography.bodySmall)
                    if (client.gstin.isNotBlank()) Text("GSTIN: ${client.gstin}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    if (client.placeOfSupply.isNotBlank()) Text("Place of Supply: ${client.placeOfSupply}", style = MaterialTheme.typography.bodySmall)
                    if (client.address.isNotBlank()) Text("Address: ${client.address}", style = MaterialTheme.typography.bodySmall)
                }

                if (item.invoice.placeOfSupply.isNotBlank() || item.invoice.vehicleNumber.isNotBlank() || item.invoice.brokerageBy.isNotBlank()) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Transportation Details:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (item.invoice.placeOfSupply.isNotBlank()) {
                        Text("Sourcing State: ${item.invoice.placeOfSupply}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (item.invoice.vehicleNumber.isNotBlank()) {
                        Text("Vehicle Number: ${item.invoice.vehicleNumber}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    }
                    if (item.invoice.brokerageBy.isNotBlank()) {
                        Text("Brokerage By: ${item.invoice.brokerageBy}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Line items summary headers
        Text("Billing Items (${item.lineItems.size}):", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 180.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(item.lineItems) { line ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(line.productName, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val hsnText = if (line.hsnSac.isNotBlank()) " | HSN: ${line.hsnSac}" else ""
                        Text(
                            text = "${line.quantity} ${line.unit} @ ₹${line.price} + ${line.taxRate}% tax$hsnText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "₹%.2f", line.total),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Divider(modifier = Modifier.padding(vertical = 10.dp))

        // Receipt Summary Calculations
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(String.format(Locale.US, "₹%.2f", item.invoice.subtotal))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("GST Tax total", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(String.format(Locale.US, "₹%.2f", item.invoice.taxTotal))
        }
        if (item.invoice.taxTotal > 0) {
            val sub = item.invoice.subtotal
            val totalTax = item.invoice.taxTotal
            val baseGstPercent = if (sub > 0) (totalTax / sub) * 100.0 else 0.0
            val halfGstPercent = baseGstPercent / 2.0
            val percentStr = if (halfGstPercent % 1.0 == 0.0) {
                String.format(Locale.US, "%.0f%%", halfGstPercent)
            } else {
                String.format(Locale.US, "%.2f%%", halfGstPercent)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CGST (Central GST - $percentStr)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text(String.format(Locale.US, "₹%.2f", item.invoice.taxTotal / 2.0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SGST (State GST - $percentStr)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text(String.format(Locale.US, "₹%.2f", item.invoice.taxTotal / 2.0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Grand Total", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text(
                text = String.format(Locale.US, "₹%.2f", item.invoice.grandTotal),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


// ------------------ INVOICE DESIGN CREATOR ------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateInvoiceScreen(
    viewModel: InvoiceViewModel,
    editingInvoice: InvoiceWithDetails? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()

    var invoiceNum by remember { mutableStateOf(editingInvoice?.invoice?.invoiceNumber ?: viewModel.generateNextInvoiceNumber()) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(editingInvoice?.customer) }
    var notes by remember { mutableStateOf(editingInvoice?.invoice?.notes ?: "") }
    var statusState by remember { mutableStateOf(editingInvoice?.invoice?.status ?: "Paid") } // default status is Paid
    var vehicleNumber by remember { mutableStateOf(editingInvoice?.invoice?.vehicleNumber ?: "") }
    var brokerageBy by remember { mutableStateOf(editingInvoice?.invoice?.brokerageBy ?: "") }
    var placeOfSupply by remember { mutableStateOf(editingInvoice?.invoice?.placeOfSupply ?: "") }
    var dueDateTimestamp by remember { mutableStateOf(editingInvoice?.invoice?.dueDateTimestamp ?: 0L) }

    // List of active line items currently added
    val addedItems = remember { mutableStateListOf<InvoiceLineItem>() }

    // Prepopulate added items from editing item
    LaunchedEffect(editingInvoice) {
        editingInvoice?.let {
            addedItems.clear()
            addedItems.addAll(it.lineItems)
            dueDateTimestamp = it.invoice.dueDateTimestamp
        }
    }

    // Dialog sheets
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showAddClientDialog by remember { mutableStateOf(false) }
    var clientSelectorSheetOpen by remember { mutableStateOf(false) }

    // Computations via custom high-performance hook
    val invoiceTotals by com.example.util.rememberInvoiceTotals(addedItems)
    val subtotal = invoiceTotals.subtotal
    val taxTotal = invoiceTotals.taxTotal
    val grandTotal = invoiceTotals.grandTotal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editingInvoice != null) "Edit Invoice" else "Design Invoice", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Return")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (selectedCustomer == null) {
                                Toast.makeText(context, "Please select at least one Client", Toast.LENGTH_SHORT).show()
                            } else if (addedItems.isEmpty()) {
                                Toast.makeText(context, "Billing needs at least 1 item", Toast.LENGTH_SHORT).show()
                            } else {
                                viewModel.saveInvoice(
                                    id = editingInvoice?.invoice?.id ?: 0,
                                    invoiceNumber = invoiceNum,
                                    customerId = selectedCustomer!!.id,
                                    status = statusState,
                                    items = addedItems.toList(),
                                    notes = notes,
                                    vehicleNumber = vehicleNumber,
                                    brokerageBy = brokerageBy,
                                    placeOfSupply = placeOfSupply,
                                    dueDateTimestamp = dueDateTimestamp
                                )
                                Toast.makeText(context, "Invoice Saved Successfully!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("save_invoice_top_bar_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = "Register Invoice")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Info Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("General Details", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                        OutlinedTextField(
                            value = invoiceNum,
                            onValueChange = { invoiceNum = it },
                            label = { Text("Invoice Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Customer Selection Header Block
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Selected Client / Customer", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            TextButton(onClick = { showAddClientDialog = true }) {
                                Icon(Icons.Default.PersonAdd, contentDescription = "Quick client add", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Client", fontSize = 12.sp)
                            }
                        }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedCustomer?.name ?: "Tap to choose recipient...",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Client Address Name") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Open sheet") },
                                modifier = Modifier.fillMaxWidth().testTag("client_picker_trigger"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = MaterialTheme.colorScheme.outline,
                                    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                enabled = false
                            )
                            // Clear overlay box that intercepts the click perfectly
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { clientSelectorSheetOpen = true }
                            )
                        }

                        // Status Chooser
                        Text("Invoice Payment Status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val statuses = listOf("Paid", "Sent", "Draft")
                            statuses.forEach { item ->
                                val active = statusState == item
                                InputChip(
                                    selected = active,
                                    onClick = { statusState = item },
                                    label = { Text(item) },
                                    avatar = {
                                        if (active) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Due Date Picker
                        val dFormatter = remember { SimpleDateFormat("dd-MM-yyyy", Locale.US) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = if (dueDateTimestamp != 0L) dFormatter.format(Date(dueDateTimestamp)) else "Not set (Default 5 days)",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Due Date") },
                                    trailingIcon = { Icon(Icons.Default.DateRange, "Select due date") },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    enabled = false
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable {
                                            val cal = Calendar.getInstance().apply {
                                                if (dueDateTimestamp != 0L) {
                                                    timeInMillis = dueDateTimestamp
                                                } else {
                                                    timeInMillis = System.currentTimeMillis() + (5L * 24 * 60 * 60 * 1000)
                                                }
                                            }
                                            android.app.DatePickerDialog(
                                                context,
                                                { _, y, m, d ->
                                                    val resCal = Calendar.getInstance().apply {
                                                        set(Calendar.YEAR, y)
                                                        set(Calendar.MONTH, m)
                                                        set(Calendar.DAY_OF_MONTH, d)
                                                        set(Calendar.HOUR_OF_DAY, 23)
                                                        set(Calendar.MINUTE, 59)
                                                        set(Calendar.SECOND, 59)
                                                    }
                                                    dueDateTimestamp = resCal.timeInMillis
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        }
                                )
                            }
                            if (dueDateTimestamp != 0L) {
                                IconButton(
                                    onClick = { dueDateTimestamp = 0L },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear Due Date",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Sourcing, Transport and Brokerage Parameters
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Transport & Supply Details (Optional)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedTextField(
                            value = placeOfSupply,
                            onValueChange = { placeOfSupply = it },
                            label = { Text("Place of Supply / Sourcing State") },
                            placeholder = { Text("e.g. Maharashtra, Haryana, Delhi") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = vehicleNumber,
                                onValueChange = { vehicleNumber = it.uppercase() },
                                label = { Text("Vehicle Number") },
                                placeholder = { Text("e.g. DL-67-AB-3672") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = brokerageBy,
                                onValueChange = { brokerageBy = it },
                                label = { Text("Brokerage By") },
                                placeholder = { Text("e.g. Transit Logix") },
                                modifier = Modifier.weight(1.2f),
                                singleLine = true
                            )
                        }
                    }
                }
            }

            // Products list creation controller heading
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Itemized Billings", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    Button(
                        onClick = { showAddItemDialog = true },
                        modifier = Modifier.testTag("add_item_button"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = "Add Item Entry")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Item", fontSize = 12.sp)
                    }
                }
            }

            // Added Items lists entries
            if (addedItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No line items added yet.\nPress 'Add Item' to bill client.",
                            color = MaterialTheme.colorScheme.outline,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                items(addedItems) { lineItem ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(1.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.5f)) {
                                Text(lineItem.productName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                val origSub = lineItem.price * lineItem.quantity
                                val discountVal = maxOf(0.0, origSub - lineItem.subtotal)
                                Text(
                                    buildString {
                                        append("${lineItem.quantity} ${lineItem.unit} @ ₹${lineItem.price}")
                                        if (discountVal > 0.0) {
                                            append(" (-₹${String.format(Locale.US, "%.2f", discountVal)})")
                                        }
                                        append(" (GST: ${lineItem.taxRate}%)")
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    String.format(Locale.US, "₹%.2f", lineItem.total),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                IconButton(onClick = { addedItems.remove(lineItem) }) {
                                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            // Invoices totals / notes card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Calculated Receipt Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Divider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.US, "₹%.2f", subtotal))
                        }
                        val totalAmountBeforeDisc = addedItems.sumOf { it.price * it.quantity }
                        val totalDiscountValue = maxOf(0.0, totalAmountBeforeDisc - subtotal)
                        if (totalDiscountValue > 0.0) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total Discount", color = MaterialTheme.colorScheme.error)
                                Text(
                                    String.format(Locale.US, "-₹%.2f", totalDiscountValue),
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GST Tax total", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.US, "₹%.2f", taxTotal))
                        }
                        if (taxTotal > 0) {
                            val baseGstPercent = if (subtotal > 0) (taxTotal / subtotal) * 100.0 else 0.0
                            val halfGstPercent = baseGstPercent / 2.0
                            val percentStr = if (halfGstPercent % 1.0 == 0.0) {
                                String.format(Locale.US, "%.0f%%", halfGstPercent)
                            } else {
                                String.format(Locale.US, "%.2f%%", halfGstPercent)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("CGST (Central GST - $percentStr)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(String.format(Locale.US, "₹%.2f", taxTotal / 2.0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("SGST (State GST - $percentStr)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(String.format(Locale.US, "₹%.2f", taxTotal / 2.0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GRAND TOTAL", fontWeight = FontWeight.Bold)
                            Text(String.format(Locale.US, "₹%.2f", grandTotal), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Terms, Notes or payment details") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            placeholder = { Text("e.g. UPI ID, Bank Transfer detials") }
                        )
                    }
                }
            }

            // Main Save trigger button
            item {
                Button(
                    onClick = {
                        if (selectedCustomer == null) {
                            Toast.makeText(context, "Please select at least one Client", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (addedItems.isEmpty()) {
                            Toast.makeText(context, "Billing needs at least 1 item", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.saveInvoice(
                            id = editingInvoice?.invoice?.id ?: 0,
                            invoiceNumber = invoiceNum,
                            customerId = selectedCustomer!!.id,
                            status = statusState,
                            items = addedItems.toList(),
                            notes = notes,
                            vehicleNumber = vehicleNumber,
                            brokerageBy = brokerageBy,
                            placeOfSupply = placeOfSupply,
                            dueDateTimestamp = dueDateTimestamp
                        )
                        Toast.makeText(context, "Invoice Saved Successfully!", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("save_invoice_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Submit record")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save & Dispatch Invoice", fontSize = 15.sp)
                }
            }
        }
    }

    // ModalBottomSheet for selecting/searching and inline editing/adding clients
    if (clientSelectorSheetOpen) {
        ModalBottomSheet(
            onDismissRequest = { clientSelectorSheetOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Client Recipient", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { clientSelectorSheetOpen = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close sheet")
                    }
                }

                var clientSearchText by remember { mutableStateOf("") }
                var showAddClientDialogFlow by remember { mutableStateOf(false) }
                var activeEditClientFlow by remember { mutableStateOf<Customer?>(null) }

                val filteredSheetCustomers = remember(customers, clientSearchText) {
                    customers.filter {
                        it.name.contains(clientSearchText, ignoreCase = true) ||
                                it.phone.contains(clientSearchText) ||
                                it.email.contains(clientSearchText, ignoreCase = true)
                    }
                }

                OutlinedTextField(
                    value = clientSearchText,
                    onValueChange = { clientSearchText = it },
                    placeholder = { Text("Search client name, phone or email...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )

                // Quick client add action
                Button(
                    onClick = { showAddClientDialogFlow = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Client", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add New Client Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                Text("Saved Profiles (${filteredSheetCustomers.size})", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (filteredSheetCustomers.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No clients match. Add one above!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    } else {
                        items(filteredSheetCustomers) { clientItem ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCustomer = clientItem
                                        clientSelectorSheetOpen = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedCustomer?.id == clientItem.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(clientItem.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (clientItem.phone.isNotBlank()) {
                                            Text("Phone: ${clientItem.phone}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (clientItem.email.isNotBlank()) {
                                            Text("Email: ${clientItem.email}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    IconButton(onClick = { activeEditClientFlow = clientItem }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile inline", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                // Inner dialog launchers
                if (showAddClientDialogFlow) {
                    var newCliName by remember { mutableStateOf("") }
                    var newCliPhone by remember { mutableStateOf("") }
                    var newCliEmail by remember { mutableStateOf("") }
                    var newCliAddr by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = { showAddClientDialogFlow = false },
                        title = { Text("Register New Client") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = newCliName,
                                    onValueChange = { newCliName = it },
                                    label = { Text("Client/Business Name*") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newCliPhone,
                                    onValueChange = { newCliPhone = it },
                                    label = { Text("Phone Number") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newCliEmail,
                                    onValueChange = { newCliEmail = it },
                                    label = { Text("Email Address") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newCliAddr,
                                    onValueChange = { newCliAddr = it },
                                    label = { Text("Billing Address") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (newCliName.isBlank()) {
                                    Toast.makeText(context, "Name cannot be blank", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.saveCustomer(
                                    id = 0,
                                    name = newCliName,
                                    phone = newCliPhone,
                                    email = newCliEmail,
                                    address = newCliAddr
                                )
                                showAddClientDialogFlow = false
                            }) {
                                Text("Save Client")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAddClientDialogFlow = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                activeEditClientFlow?.let { origin ->
                    var newCliName by remember { mutableStateOf(origin.name) }
                    var newCliPhone by remember { mutableStateOf(origin.phone) }
                    var newCliEmail by remember { mutableStateOf(origin.email) }
                    var newCliAddr by remember { mutableStateOf(origin.address) }

                    AlertDialog(
                        onDismissRequest = { activeEditClientFlow = null },
                        title = { Text("Edit Client Details") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = newCliName,
                                    onValueChange = { newCliName = it },
                                    label = { Text("Client/Business Name*") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newCliPhone,
                                    onValueChange = { newCliPhone = it },
                                    label = { Text("Phone Number") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newCliEmail,
                                    onValueChange = { newCliEmail = it },
                                    label = { Text("Email Address") },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = newCliAddr,
                                    onValueChange = { newCliAddr = it },
                                    label = { Text("Billing Address") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        },
                        confirmButton = {
                            Button(onClick = {
                                if (newCliName.isBlank()) {
                                    Toast.makeText(context, "Name cannot be blank", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.saveCustomer(
                                    id = origin.id,
                                    name = newCliName,
                                    phone = newCliPhone,
                                    email = newCliEmail,
                                    address = newCliAddr
                                )
                                activeEditClientFlow = null
                            }) {
                                Text("Save Changes")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { activeEditClientFlow = null }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal adding new client inline dialog
    if (showAddClientDialog) {
        var newCliName by remember { mutableStateOf("") }
        var newCliPhone by remember { mutableStateOf("") }
        var newCliEmail by remember { mutableStateOf("") }
        var newCliAddr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddClientDialog = false },
            title = { Text("Register New Client") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCliName,
                        onValueChange = { newCliName = it },
                        label = { Text("Client/Business Name*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_client_name")
                    )
                    OutlinedTextField(
                        value = newCliPhone,
                        onValueChange = { newCliPhone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCliEmail,
                        onValueChange = { newCliEmail = it },
                        label = { Text("Email Address") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCliAddr,
                        onValueChange = { newCliAddr = it },
                        label = { Text("Billing Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newCliName.isBlank()) {
                        Toast.makeText(context, "Name matches a required field", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.saveCustomer(
                        id = 0,
                        name = newCliName,
                        phone = newCliPhone,
                        email = newCliEmail,
                        address = newCliAddr
                    )
                    showAddClientDialog = false
                }) {
                    Text("Save Client")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddClientDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Modal adding dialog for line items
    if (showAddItemDialog) {
        var typedItemName by remember { mutableStateOf("") }
        var inputPrice by remember { mutableStateOf("") }
        var inputTaxRate by remember { mutableStateOf("") }
        var inputUnit by remember { mutableStateOf("pcs") }
        var inputQty by remember { mutableStateOf("1") }
        var inputHsnSac by remember { mutableStateOf("") }
        var inputDiscountVal by remember { mutableStateOf("") }
        var inputDiscountPercent by remember { mutableStateOf("") }

        // Autocomplete helper from products inventory lists
        var chosenInventoryProd by remember { mutableStateOf<Product?>(null) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add Billing Item") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Dropdown selecting from Product list with reliable click overlay
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = chosenInventoryProd?.name ?: "Or pick from inventory Products...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Inventory Product Stock (Optional)") },
                            trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Open details") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = MaterialTheme.colorScheme.outline,
                                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            enabled = false
                        )
                        // Clear overlay box to capture tap event
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { dropdownExpanded = true }
                        )
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            DropdownMenuItem(
                                text = { Text("-- Clear Inventory Selection --") },
                                onClick = {
                                    chosenInventoryProd = null
                                    dropdownExpanded = false
                                }
                            )
                            products.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (Qty Left: ${p.stock} ${p.unit})") },
                                    onClick = {
                                        chosenInventoryProd = p
                                        typedItemName = p.name
                                        inputPrice = p.price.toString()
                                        inputTaxRate = p.taxRate.toString()
                                        inputUnit = p.unit
                                        inputHsnSac = p.hsnSac
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = typedItemName,
                        onValueChange = { itValue ->
                            typedItemName = itValue
                            // clear choose inventory if name changes away
                            if (itValue != chosenInventoryProd?.name) {
                                chosenInventoryProd = null
                            }
                        },
                        label = { Text("Product / Service Name*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("added_item_name_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputQty,
                            onValueChange = { inputQty = it },
                            label = { Text("Qty") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("added_item_qty_input")
                        )
                        OutlinedTextField(
                            value = inputUnit,
                            onValueChange = { inputUnit = it },
                            label = { Text("Unit") },
                            placeholder = { Text("pcs, hrs, kg") },
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    // Quick unit helper chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("kg", "bags", "pcs", "box").forEach { suggestion ->
                            val isSelected = inputUnit.lowercase(Locale.ROOT) == suggestion
                            SuggestionChip(
                                onClick = { inputUnit = suggestion },
                                label = { Text(suggestion, fontSize = 10.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputPrice,
                            onValueChange = { inputPrice = it },
                            label = { Text("Price (₹)*") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.2f).testTag("added_item_price_input")
                        )
                        OutlinedTextField(
                            value = inputTaxRate,
                            onValueChange = { inputTaxRate = it },
                            label = { Text("GST Tax (%)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("18") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputDiscountVal,
                            onValueChange = {
                                inputDiscountVal = it
                                if (it.isNotBlank() && it != "0") {
                                    inputDiscountPercent = ""
                                }
                            },
                            label = { Text("Discount (₹)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(1f).testTag("discount_rupees_input")
                        )
                        OutlinedTextField(
                            value = inputDiscountPercent,
                            onValueChange = {
                                inputDiscountPercent = it
                                if (it.isNotBlank() && it != "0") {
                                    inputDiscountVal = ""
                                }
                            },
                            label = { Text("Discount (%)") },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("0") },
                            modifier = Modifier.weight(1f).testTag("discount_percent_input")
                        )
                    }

                    OutlinedTextField(
                        value = inputHsnSac,
                        onValueChange = { inputHsnSac = it },
                        label = { Text("HSN/SAC Code (Optional)") },
                        placeholder = { Text("e.g. 998311, 4802") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val finalName = typedItemName.trim()
                    val qty = inputQty.toDoubleOrNull() ?: 1.0
                    val rate = inputPrice.toDoubleOrNull() ?: 0.0
                    val tax = inputTaxRate.toDoubleOrNull() ?: 0.0
                    val discValue = inputDiscountVal.toDoubleOrNull() ?: 0.0
                    val discPercent = inputDiscountPercent.toDoubleOrNull() ?: 0.0

                    if (finalName.isBlank()) {
                        Toast.makeText(context, "Product name cannot be blank", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (rate < 0) {
                        Toast.makeText(context, "Rate cannot be negative", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (qty <= 0) {
                        Toast.makeText(context, "Quantity must be greater than 0", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (discValue < 0 || discPercent < 0) {
                        Toast.makeText(context, "Discount cannot be negative", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (discPercent > 100) {
                        Toast.makeText(context, "Discount percentage cannot exceed 100%", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val amounts = com.example.util.InvoiceCalculator.calculateLineItem(
                        price = rate,
                        quantity = qty,
                        taxRate = tax,
                        discountAmount = discValue,
                        discountPercent = discPercent
                    )
                    val sub = amounts.subtotal
                    val computedTax = amounts.tax
                    val total = amounts.total

                    val item = InvoiceLineItem(
                        invoiceId = 0,
                        productId = chosenInventoryProd?.id ?: 0,
                        productName = finalName,
                        price = rate,
                        quantity = qty,
                        taxRate = tax,
                        unit = inputUnit.trim().lowercase(Locale.ROOT),
                        subtotal = sub,
                        tax = computedTax,
                        total = total,
                        hsnSac = inputHsnSac.trim()
                    )

                    addedItems.add(item)
                    showAddItemDialog = false
                }) {
                    Text("Confirm Item")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun UpiQrImage(upiId: String, businessName: String, amount: Double) {
    val upiUri = remember(upiId, businessName, amount) {
        val encodedPn = android.net.Uri.encode(businessName)
        "upi://pay?pa=$upiId&pn=$encodedPn&am=$amount&cu=INR"
    }
    val qrBitmap = remember(upiUri) {
        try {
            val size = 220
            val wm = com.google.zxing.MultiFormatWriter()
            val bitMatrix = wm.encode(upiUri, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
            val bitmap = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    qrBitmap?.let {
        androidx.compose.foundation.Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Scan to Pay UPI QR Code",
            modifier = Modifier
                .size(150.dp)
                .background(androidx.compose.ui.graphics.Color.White, shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        )
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
    val dFormatter = remember { SimpleDateFormat("dd-MM-yyyy", Locale.US) }
    val statusColor = when (item.invoice.status) {
        "Paid" -> Color(0xFF10B981)
        "Sent" -> Color(0xFF3B82F6)
        "Draft" -> Color(0xFF6B7280)
        else -> Color(0xFFF59E0B)
    }

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
            .testTag("invoice_card_${item.invoice.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectedChange,
                    modifier = Modifier.padding(end = 12.dp).testTag("invoice_checkbox_${item.invoice.id}")
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Invoice #${item.invoice.invoiceNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.15f),
                        contentColor = statusColor
                    ) {
                        Text(
                            text = item.invoice.status,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = item.customer?.name ?: "No Client Information",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Billed Date: ${dFormatter.format(Date(item.invoice.dateTimestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Text(
                        text = String.format(Locale.US, "₹%.2f", item.invoice.grandTotal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (item.invoice.downloadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Downloaded count logo",
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Exported/Shared ${item.invoice.downloadCount} times",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}


