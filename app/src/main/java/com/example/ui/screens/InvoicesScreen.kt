package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.InvoiceWithDetails
import com.example.ui.InvoiceViewModel
import com.example.ui.screens.invoices.*
import com.example.util.ExcelExporter

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

    var activeInvoiceDetails by remember { mutableStateOf<InvoiceWithDetails?>(null) }
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

    val generatorPrefs = remember { context.getSharedPreferences("invoice_generator_prefs", Context.MODE_PRIVATE) }
    var pendingModelSelectionAction by remember { mutableStateOf<Triple<InvoiceWithDetails, String, (String) -> Unit>?>(null) }

    pendingModelSelectionAction?.let { (billing, actionName, onExecute) ->
        val currentModelPref = remember { generatorPrefs.getString("pdf_model", "Model 1") ?: "Model 1" }
        PdfModelSelectionDialog(
            invoiceNumber = billing.invoice.invoiceNumber,
            actionName = actionName,
            initialModel = currentModelPref,
            onDismiss = { pendingModelSelectionAction = null },
            onConfirm = { selectedModel, saveAsDefault ->
                if (saveAsDefault) {
                    generatorPrefs.edit().putString("pdf_model", selectedModel).apply()
                }
                pendingModelSelectionAction = null
                onExecute(selectedModel)
            }
        )
    }

    // Filter & Search
    val filteredInvoices = remember(invoices, selectedFilter, searchQuery) {
        invoices.filter {
            val matchesFilter = when (selectedFilter) {
                "All" -> true
                "Invoices" -> !it.invoice.documentType.equals("ESTIMATE", ignoreCase = true) && !it.invoice.documentType.equals("QUOTATION", ignoreCase = true)
                "Estimates" -> it.invoice.documentType.equals("ESTIMATE", ignoreCase = true) || it.invoice.documentType.equals("QUOTATION", ignoreCase = true)
                "Partial" -> it.invoice.status.equals("Partial", ignoreCase = true) || (it.invoice.paidAmount > 0.0 && it.invoice.paidAmount < it.invoice.grandTotal)
                else -> it.invoice.status.equals(selectedFilter, ignoreCase = true)
            }
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
                            IconButton(onClick = {
                                if (selectedInvoices.size == filteredInvoices.size) {
                                    selectedInvoices.clear()
                                } else {
                                    selectedInvoices.clear()
                                    selectedInvoices.addAll(filteredInvoices)
                                }
                            }) {
                                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                            }

                            IconButton(
                                onClick = {
                                    if (selectedInvoices.isNotEmpty()) {
                                        val csvFile = ExcelExporter.generateAccountingReportCsv(context, selectedInvoices.toList(), clients)
                                        if (csvFile != null) {
                                            ExcelExporter.shareCsvFile(context, csvFile)
                                            Toast.makeText(context, "Accounting spreadsheet generated!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Export error", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "No invoices selected", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.TableView, contentDescription = "Export Excel", tint = MaterialTheme.colorScheme.primary)
                            }

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
                                Icon(Icons.Default.Delete, contentDescription = "Bulk Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        )
                    )
                } else {
                    TopAppBar(
                        title = { Text("Documents & Billing", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            if (onMenuClick != null) {
                                IconButton(onClick = onMenuClick, modifier = Modifier.testTag("invoices_menu_btn")) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open menu")
                                }
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                isSelectionMode = true
                                selectedInvoices.clear()
                            }, modifier = Modifier.testTag("enter_bulk_select_invoices")) {
                                Icon(Icons.Default.List, contentDescription = "Select items")
                            }

                            IconButton(
                                onClick = {
                                    val csvFile = ExcelExporter.generateAccountingReportCsv(context, invoices, clients)
                                    if (csvFile != null) {
                                        ExcelExporter.shareCsvFile(context, csvFile)
                                        Toast.makeText(context, "Accounting spreadsheet generated", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Export error", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("invoices_catalog_excel_btn")
                            ) {
                                Icon(Icons.Default.TableView, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary)
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
                    ExtendedFloatingActionButton(
                        onClick = { isCreatingInvoice = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Create Document", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("create_invoice_fab")
                    )
                }
            },
            modifier = modifier
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by invoice # or client name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("invoice_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Filter Tabs (Horizontal Scrollable Chips)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filterOptions = listOf("All", "Invoices", "Estimates", "Draft", "Sent", "Partial", "Paid", "Closed")
                    filterOptions.forEach { filter ->
                        val isSelected = selectedFilter == filter
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }

                // Invoices List
                if (filteredInvoices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedFilter != "All") "No documents match your filter" else "No invoices or estimates yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (searchQuery.isEmpty() && selectedFilter == "All") {
                                Button(
                                    onClick = { isCreatingInvoice = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create First Invoice / Estimate")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                    ) {
                        items(filteredInvoices, key = { it.invoice.id }) { item ->
                            val isSelected = selectedInvoices.any { it.invoice.id == item.invoice.id }
                            CatalogInvoiceItemRow(
                                item = item,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onSelectedChange = { checked ->
                                    if (checked) {
                                        selectedInvoices.add(item)
                                    } else {
                                        selectedInvoices.removeAll { it.invoice.id == item.invoice.id }
                                    }
                                },
                                onViewClicked = {
                                    activeInvoiceDetails = item
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    activeInvoiceDetails?.let { detailItem ->
        InvoiceDetailSheet(
            invoiceWithDetails = detailItem,
            profile = profile,
            viewModel = viewModel,
            onDismiss = { activeInvoiceDetails = null },
            onEdit = {
                editingInvoice = detailItem
                activeInvoiceDetails = null
            },
            onDelete = {
                viewModel.deleteInvoice(detailItem.invoice.id)
                activeInvoiceDetails = null
            },
            onSelectPdfModel = { actionName, onExecute ->
                pendingModelSelectionAction = Triple(detailItem, actionName, onExecute)
            }
        )
    }

    // Bulk Delete Confirmation
    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete ${selectedInvoices.size} Documents?") },
            text = { Text("Are you sure you want to permanently delete these selected invoices/estimates? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteInvoicesBulk(selectedInvoices.map { it.invoice.id })
                        selectedInvoices.clear()
                        isSelectionMode = false
                        showBulkDeleteConfirm = false
                        Toast.makeText(context, "Selected documents deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
