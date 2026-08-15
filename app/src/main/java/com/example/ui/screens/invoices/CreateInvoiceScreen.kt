package com.example.ui.screens.invoices

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.util.InvoiceCalculator
import com.example.util.rememberInvoiceTotals
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

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

    var documentType by remember { mutableStateOf(editingInvoice?.invoice?.documentType ?: "INVOICE") }
    var invoiceNum by remember {
        mutableStateOf(
            editingInvoice?.invoice?.invoiceNumber
                ?: if (documentType == "INVOICE") viewModel.generateNextInvoiceNumber() else viewModel.generateNextEstimateNumber()
        )
    }

    var selectedCustomer by remember { mutableStateOf<Customer?>(editingInvoice?.customer) }
    var notes by remember { mutableStateOf(editingInvoice?.invoice?.notes ?: "") }
    var statusState by remember { mutableStateOf(editingInvoice?.invoice?.status ?: "Sent") }
    var vehicleNumber by remember { mutableStateOf(editingInvoice?.invoice?.vehicleNumber ?: "") }
    var brokerageBy by remember { mutableStateOf(editingInvoice?.invoice?.brokerageBy ?: "") }
    var placeOfSupply by remember { mutableStateOf(editingInvoice?.invoice?.placeOfSupply ?: "") }
    var transporterDocNo by remember { mutableStateOf(editingInvoice?.invoice?.transporterDocNo ?: "") }
    var eWayBillNo by remember { mutableStateOf(editingInvoice?.invoice?.eWayBillNo ?: "") }
    var dueDateTimestamp by remember { mutableStateOf(editingInvoice?.invoice?.dueDateTimestamp ?: 0L) }
    var attachmentPathState by remember { mutableStateOf(editingInvoice?.invoice?.attachmentPath ?: "") }

    var paymentMethodState by remember { mutableStateOf(editingInvoice?.invoice?.paymentMethod ?: "Cash") }
    var paymentNoteState by remember { mutableStateOf(editingInvoice?.invoice?.paymentNote ?: "") }
    var paymentAttachmentPathState by remember { mutableStateOf(editingInvoice?.invoice?.paymentAttachmentPath ?: "") }
    var closeReasonState by remember { mutableStateOf(editingInvoice?.invoice?.closeReason ?: "") }

    val addedItems = remember { mutableStateListOf<InvoiceLineItem>() }

    LaunchedEffect(editingInvoice) {
        editingInvoice?.let {
            addedItems.clear()
            addedItems.addAll(it.lineItems)
            dueDateTimestamp = it.invoice.dueDateTimestamp
            attachmentPathState = it.invoice.attachmentPath
            paymentMethodState = it.invoice.paymentMethod
            paymentNoteState = it.invoice.paymentNote
            paymentAttachmentPathState = it.invoice.paymentAttachmentPath
            closeReasonState = it.invoice.closeReason
            transporterDocNo = it.invoice.transporterDocNo
            eWayBillNo = it.invoice.eWayBillNo
            documentType = it.invoice.documentType
        }
    }

    var showAddItemDialog by remember { mutableStateOf(false) }
    var showAddClientDialog by remember { mutableStateOf(false) }
    var clientSelectorSheetOpen by remember { mutableStateOf(false) }

    val invoiceTotals by rememberInvoiceTotals(addedItems)
    val subtotal = invoiceTotals.subtotal
    val taxTotal = invoiceTotals.taxTotal
    val grandTotal = invoiceTotals.grandTotal

    val isEstimate = documentType == "ESTIMATE" || documentType == "QUOTATION"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (editingInvoice != null) "Edit ${if (isEstimate) "Estimate" else "Invoice"}" else "Create ${if (isEstimate) "Estimate" else "Invoice"}",
                        fontWeight = FontWeight.Bold
                    )
                },
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
                                Toast.makeText(context, "Document requires at least 1 item", Toast.LENGTH_SHORT).show()
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
                                    transporterDocNo = transporterDocNo,
                                    eWayBillNo = eWayBillNo,
                                    dueDateTimestamp = dueDateTimestamp,
                                    attachmentPath = attachmentPathState,
                                    paymentMethod = if (statusState == "Paid") paymentMethodState else "",
                                    paymentNote = if (statusState == "Paid") paymentNoteState else "",
                                    paymentAttachmentPath = if (statusState == "Paid") paymentAttachmentPathState else "",
                                    closeReason = if (statusState == "Closed") closeReasonState else "",
                                    documentType = documentType,
                                    paidAmount = editingInvoice?.invoice?.paidAmount ?: if (statusState == "Paid") grandTotal else 0.0
                                )
                                Toast.makeText(context, "${if (isEstimate) "Estimate" else "Invoice"} Saved Successfully!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        },
                        modifier = Modifier.testTag("save_invoice_top_bar_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = "Save Document")
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
            // Document Type Selector Card (Tax Invoice vs Estimate / Quotation)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEstimate) Color(0xFFFEF3C7).copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    ),
                    border = BorderStroke(1.dp, if (isEstimate) Color(0xFFF59E0B) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Document Type",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterChip(
                                selected = documentType == "INVOICE",
                                onClick = {
                                    if (documentType != "INVOICE") {
                                        documentType = "INVOICE"
                                        if (editingInvoice == null) {
                                            invoiceNum = viewModel.generateNextInvoiceNumber()
                                        }
                                    }
                                },
                                label = { Text("Tax Invoice", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    if (documentType == "INVOICE") {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    } else {
                                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = isEstimate,
                                onClick = {
                                    if (!isEstimate) {
                                        documentType = "ESTIMATE"
                                        if (editingInvoice == null) {
                                            invoiceNum = viewModel.generateNextEstimateNumber()
                                        }
                                    }
                                },
                                label = { Text("Estimate / Quote", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    if (isEstimate) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    } else {
                                        Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // General Details Card
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
                            label = { Text(if (isEstimate) "Estimate Number" else "Invoice Number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Client Selector Row
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Selected Client", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
                                label = { Text("Client Name") },
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
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { clientSelectorSheetOpen = true }
                            )
                        }

                        // Payment Status Chooser
                        Text("Status", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val statuses = if (isEstimate) listOf("Draft", "Sent", "Closed") else listOf("Paid", "Sent", "Draft", "Closed")
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

                        if (statusState == "Paid") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("Payment Method", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("Cash", "UPI", "Cheque").forEach { mode ->
                                            val selected = paymentMethodState == mode
                                            InputChip(
                                                selected = selected,
                                                onClick = { paymentMethodState = mode },
                                                label = { Text(mode) },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }

                                    OutlinedTextField(
                                        value = paymentNoteState,
                                        onValueChange = { paymentNoteState = it },
                                        label = { Text("Payment Note / Reference No.") },
                                        placeholder = { Text("e.g. UPI Transaction ID, Cheque number") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

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
                                    label = { Text(if (isEstimate) "Valid Until Date" else "Due Date") },
                                    trailingIcon = { Icon(Icons.Default.DateRange, "Select date") },
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
                                            DatePickerDialog(
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

            // Transport & Supply Details
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

            // Items List Heading
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Line Items (${addedItems.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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

            // Line items rows
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
                            text = "No line items added yet.\nPress 'Add Item' to add products or services.",
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
                                    text = "${String.format(Locale.US, "%.1f", lineItem.quantity)} ${lineItem.unit} @ ₹${String.format(Locale.US, "%,.2f", lineItem.price)} (Tax: ${lineItem.taxRate}%)",
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
                                    text = String.format(Locale.US, "₹%,.2f", lineItem.total),
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

            // Calculations Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Calculated Summary", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.US, "₹%,.2f", subtotal))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GST Tax Total", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(String.format(Locale.US, "₹%,.2f", taxTotal))
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GRAND TOTAL", fontWeight = FontWeight.Bold)
                            Text(String.format(Locale.US, "₹%,.2f", grandTotal), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Terms & Notes") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            placeholder = { Text("e.g. Valid for 30 days, UPI ID...") }
                        )
                    }
                }
            }

            // Save Document Button
            item {
                Button(
                    onClick = {
                        if (selectedCustomer == null) {
                            Toast.makeText(context, "Please select at least one Client", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (addedItems.isEmpty()) {
                            Toast.makeText(context, "Document requires at least 1 item", Toast.LENGTH_SHORT).show()
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
                            dueDateTimestamp = dueDateTimestamp,
                            attachmentPath = attachmentPathState,
                            documentType = documentType,
                            paidAmount = editingInvoice?.invoice?.paidAmount ?: if (statusState == "Paid") grandTotal else 0.0
                        )
                        Toast.makeText(context, "${if (isEstimate) "Estimate" else "Invoice"} Saved Successfully!", Toast.LENGTH_SHORT).show()
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
                    Text("Save & Dispatch ${if (isEstimate) "Estimate" else "Invoice"}", fontSize = 15.sp)
                }
            }
        }
    }

    // Client Selector Bottom Sheet
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
                val filteredSheetCustomers = remember(customers, clientSearchText) {
                    customers.filter { !it.isClosed }.filter {
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

                Button(
                    onClick = { showAddClientDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add Client", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add New Client Profile", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

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
                                Text("No clients found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
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
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
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
                                    }
                                    if (selectedCustomer?.id == clientItem.id) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Client Dialog
    if (showAddClientDialog) {
        var newCliName by remember { mutableStateOf("") }
        var newCliCompanyName by remember { mutableStateOf("") }
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
                        label = { Text("Client Name*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("new_client_name")
                    )
                    OutlinedTextField(
                        value = newCliCompanyName,
                        onValueChange = { newCliCompanyName = it },
                        label = { Text("Company Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCliPhone,
                        onValueChange = { newCliPhone = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCliEmail,
                        onValueChange = { newCliEmail = it },
                        label = { Text("Email Address") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
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
                        Toast.makeText(context, "Client name is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.saveCustomer(
                        id = 0,
                        name = newCliName,
                        companyName = newCliCompanyName,
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

    // Add Line Item Dialog
    if (showAddItemDialog) {
        var typedItemName by remember { mutableStateOf("") }
        var inputPrice by remember { mutableStateOf("") }
        var inputTaxRate by remember { mutableStateOf("") }
        var inputUnit by remember { mutableStateOf("pcs") }
        var inputQty by remember { mutableStateOf("1") }
        var inputHsnSac by remember { mutableStateOf("") }
        var inputDiscountVal by remember { mutableStateOf("") }
        var inputDiscountPercent by remember { mutableStateOf("") }

        var chosenInventoryProd by remember { mutableStateOf<Product?>(null) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddItemDialog = false },
            title = { Text("Add Item Entry") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = chosenInventoryProd?.name ?: "Pick from inventory...",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Inventory Product (Optional)") },
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
                                text = { Text("-- Custom Item --") },
                                onClick = {
                                    chosenInventoryProd = null
                                    dropdownExpanded = false
                                }
                            )
                            products.forEach { p ->
                                DropdownMenuItem(
                                    text = { Text("${p.name} (Stock: ${p.stock} ${p.unit})") },
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
                        onValueChange = {
                            typedItemName = it
                            if (it != chosenInventoryProd?.name) chosenInventoryProd = null
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
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f).testTag("added_item_qty_input")
                        )
                        OutlinedTextField(
                            value = inputUnit,
                            onValueChange = { inputUnit = it },
                            label = { Text("Unit") },
                            placeholder = { Text("pcs, kg, hrs") },
                            modifier = Modifier.weight(1.2f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputPrice,
                            onValueChange = { inputPrice = it },
                            label = { Text("Price (₹)*") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.2f).testTag("added_item_price_input")
                        )
                        OutlinedTextField(
                            value = inputTaxRate,
                            onValueChange = { inputTaxRate = it },
                            label = { Text("GST Tax (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = { Text("18") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = inputDiscountVal,
                            onValueChange = {
                                inputDiscountVal = it
                                if (it.isNotBlank() && it != "0") inputDiscountPercent = ""
                            },
                            label = { Text("Discount (₹)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = inputDiscountPercent,
                            onValueChange = {
                                inputDiscountPercent = it
                                if (it.isNotBlank() && it != "0") inputDiscountVal = ""
                            },
                            label = { Text("Discount (%)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = inputHsnSac,
                        onValueChange = { inputHsnSac = it },
                        label = { Text("HSN / SAC Code (Optional)") },
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
                        Toast.makeText(context, "Item name is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (rate < 0 || qty <= 0) {
                        Toast.makeText(context, "Please enter a valid price and quantity", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val amounts = InvoiceCalculator.calculateLineItem(
                        price = rate,
                        quantity = qty,
                        taxRate = tax,
                        discountAmount = discValue,
                        discountPercent = discPercent
                    )

                    addedItems.add(
                        InvoiceLineItem(
                            id = 0,
                            invoiceId = 0,
                            productId = chosenInventoryProd?.id ?: 0,
                            productName = finalName,
                            price = rate,
                            quantity = qty,
                            taxRate = tax,
                            unit = inputUnit.ifBlank { "pcs" },
                            subtotal = amounts.subtotal,
                            tax = amounts.tax,
                            total = amounts.total,
                            hsnSac = inputHsnSac.trim()
                        )
                    )
                    showAddItemDialog = false
                }) {
                    Text("Add Item")
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
