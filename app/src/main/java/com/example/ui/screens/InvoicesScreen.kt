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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val profile by viewModel.businessProfile.collectAsStateWithLifecycle()

    var selectedFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    // Selected invoice for detail view sheet
    var activeInvoiceDetails by remember { mutableStateOf<InvoiceWithDetails?>(null) }
    // Navigation inside screener: List or Create Invoice
    var isCreatingInvoice by remember { mutableStateOf(false) }
    var editingInvoice by remember { mutableStateOf<InvoiceWithDetails?>(null) }

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
                TopAppBar(
                    title = { Text("Invoice Catalog", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { isCreatingInvoice = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add New Billings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { isCreatingInvoice = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_invoice_fab")
                ) {
                    Icon(Icons.Default.PostAdd, contentDescription = "New Invoice")
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
                            RecentInvoiceItemRow(
                                item = billing,
                                onViewClicked = { activeInvoiceDetails = billing }
                            )
                        }
                    }
                }
            }
        }
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
                onExportShare = {
                    try {
                        val pdfFile = PdfGenerator.generateInvoicePdf(context, billing, profile)
                        PdfGenerator.shareInvoicePdf(context, pdfFile)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Export Error: ${e.message}", Toast.LENGTH_LONG).show()
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
    onExportShare: () -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onExportShare,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1.5f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = "PDF Share", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export & Share", fontSize = 13.sp)
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

        // WhatsApp / Email Share & PDF Preview Row
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
                onClick = onShareEmail,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = "Email Share", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Email PDF", fontSize = 11.sp)
            }

            Button(
                onClick = onPreviewPdf,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                modifier = Modifier.weight(1.2f),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Preview", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("PDF Preview", fontSize = 11.sp)
            }
        }

        // UPI Pay Card if Business profile has UPI ID
        if (businessProfile != null && businessProfile.upiId.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Business UPI Payment Details:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(businessProfile.upiId, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Button(
                        onClick = {
                            try {
                                val upiUri = "upi://pay?pa=${businessProfile.upiId}&pn=${businessProfile.businessName}&am=${item.invoice.grandTotal}&cu=INR"
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "Hello, please pay ₹${String.format(Locale.US, "%.2f", item.invoice.grandTotal)} to ${businessProfile.businessName} via UPI ID: ${businessProfile.upiId}\nPayment Link: $upiUri")
                                }
                                val chooser = Intent.createChooser(shareIntent, "Share Payment Link")
                                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("CGST (Central GST - 50%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                Text(String.format(Locale.US, "₹%.2f", item.invoice.taxTotal / 2.0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SGST (State GST - 50%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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

    // List of active line items currently added
    val addedItems = remember { mutableStateListOf<InvoiceLineItem>() }

    // Prepopulate added items from editing item
    LaunchedEffect(editingInvoice) {
        editingInvoice?.let {
            addedItems.clear()
            addedItems.addAll(it.lineItems)
        }
    }

    // Dialog sheets
    var showAddItemDialog by remember { mutableStateOf(false) }
    var showAddClientDialog by remember { mutableStateOf(false) }

    // Computations
    val subtotal = addedItems.sumOf { it.subtotal }
    val taxTotal = addedItems.sumOf { it.tax }
    val grandTotal = addedItems.sumOf { it.total }

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
                                    placeOfSupply = placeOfSupply
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

                        if (customers.isEmpty()) {
                            Text(
                                "No registered clients yet. Please create one to generate bills.",
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        } else {
                            var clientSelectorExpanded by remember { mutableStateOf(false) }
                            OutlinedTextField(
                                value = selectedCustomer?.name ?: "Tap to choose recipient...",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Client Address Name") },
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Open dropdown") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { clientSelectorExpanded = true },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                enabled = true
                            )
                            DropdownMenu(
                                expanded = clientSelectorExpanded,
                                onDismissRequest = { clientSelectorExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                customers.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text("${c.name} (Ph: ${c.phone})") },
                                        onClick = {
                                            selectedCustomer = c
                                            clientSelectorExpanded = false
                                        }
                                    )
                                }
                            }
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
                                onValueChange = { vehicleNumber = it },
                                label = { Text("Vehicle Number") },
                                placeholder = { Text("e.g. GJ-01-XX-9999") },
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
                                Text(
                                    "${lineItem.quantity} ${lineItem.unit} @ ₹${lineItem.price} (GST Tax: ${lineItem.taxRate}%)",
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
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GST Tax total", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.US, "₹%.2f", taxTotal))
                        }
                        if (taxTotal > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("CGST (Central GST - 50%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                Text(String.format(Locale.US, "₹%.2f", taxTotal / 2.0), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("SGST (State GST - 50%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
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
                            placeOfSupply = placeOfSupply
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
                    // Dropdown selecting from Product list
                    OutlinedTextField(
                        value = chosenInventoryProd?.name ?: "Or pick from inventory Products...",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Inventory Product Stock (Optional)") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, "Open details") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { dropdownExpanded = true },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedBorderColor = MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        enabled = true
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

                    val sub = qty * rate
                    val computedTax = (sub * tax) / 100.0
                    val total = sub + computedTax

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
