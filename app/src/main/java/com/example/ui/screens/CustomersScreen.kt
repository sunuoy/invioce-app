package com.example.ui.screens

import android.widget.Toast
import java.io.File
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Customer
import com.example.ui.InvoiceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var activeEditingCustomer by remember { mutableStateOf<Customer?>(null) }

    // Multi-selection status variables
    var isSelectionMode by remember { mutableStateOf(false) }
    val selectedCustomers = remember { mutableStateListOf<Customer>() }
    var showBulkDeleteConfirm by remember { mutableStateOf(false) }

    val filteredCustomers = remember(customers, searchQuery) {
        customers.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.phone.contains(searchQuery) ||
                    it.email.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedCustomers.size} selected", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSelectionMode = false
                            selectedCustomers.clear()
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel bulk selection")
                        }
                    },
                    actions = {
                        // Select All / Deselect All trigger
                        IconButton(onClick = {
                            if (selectedCustomers.size == filteredCustomers.size) {
                                selectedCustomers.clear()
                            } else {
                                selectedCustomers.clear()
                                selectedCustomers.addAll(filteredCustomers)
                            }
                        }) {
                            Icon(
                                imageVector = if (selectedCustomers.size == filteredCustomers.size) Icons.Default.SelectAll else Icons.Default.SelectAll,
                                contentDescription = "Toggle Select All"
                            )
                        }
                        
                        // Bulk remove action
                        IconButton(
                            onClick = {
                                if (selectedCustomers.isNotEmpty()) {
                                    showBulkDeleteConfirm = true
                                } else {
                                    Toast.makeText(context, "No clients selected", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.testTag("bulk_delete_clients_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Bulk Delete selected clients", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                )
            } else {
                TopAppBar(
                    title = { Text("Clients Directory", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        if (onMenuClick != null) {
                            IconButton(onClick = onMenuClick, modifier = Modifier.testTag("customers_menu_btn")) {
                                Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                            }
                        }
                    },
                    actions = {
                        // Export Clients to CSV
                        IconButton(onClick = {
                            if (customers.isEmpty()) {
                                Toast.makeText(context, "No clients to export", Toast.LENGTH_SHORT).show()
                                return@IconButton
                            }
                            try {
                                val csvHeader = "ID,Name,Company Name,Phone,Email,Address,GSTIN,Place of Supply\n"
                                val csvBody = customers.joinToString("\n") { c ->
                                    val id = c.id
                                    val name = "\"${c.name.replace("\"", "\"\"")}\""
                                    val company = "\"${c.companyName.replace("\"", "\"\"")}\""
                                    val phone = "\"${c.phone.replace("\"", "\"\"")}\""
                                    val email = "\"${c.email.replace("\"", "\"\"")}\""
                                    val address = "\"${c.address.replace("\"", "\"\"")}\""
                                    val gstin = "\"${c.gstin.replace("\"", "\"\"")}\""
                                    val placeOfSupply = "\"${c.placeOfSupply.replace("\"", "\"\"")}\""
                                    "$id,$name,$company,$phone,$email,$address,$gstin,$placeOfSupply"
                                }
                                val csvContent = csvHeader + csvBody
                                val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
                                val exportFile = File(cacheDir, "clients_list_${System.currentTimeMillis()}.csv")
                                exportFile.writeText(csvContent, Charsets.UTF_8)

                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    exportFile
                                )
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Clients List Directory Export")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export Clients List via"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export Clients Directory"
                            )
                        }

                        // Enter bulk select manually
                        IconButton(onClick = {
                            isSelectionMode = true
                            selectedCustomers.clear()
                        }, modifier = Modifier.testTag("enter_bulk_select_clients")) {
                            Icon(Icons.Default.Checklist, contentDescription = "Enable Bulk Delete")
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
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_customer_fab")
                ) {
                    Icon(Icons.Default.PersonAddAlt, contentDescription = "New Client Account")
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEC4899).copy(alpha = 0.03f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .drawBehind {
                    // Orb 1 (Top Left) - Pink
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFEC4899).copy(alpha = 0.12f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.3f),
                            radius = size.maxDimension * 0.45f
                        ),
                        radius = size.maxDimension * 0.45f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.3f)
                    )
                    // Orb 2 (Bottom Right) - Purple
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.08f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.75f),
                            radius = size.maxDimension * 0.4f
                        ),
                        radius = size.maxDimension * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.75f)
                    )
                    // Orb 3 (Center Left) - Emerald Green
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF10B981).copy(alpha = 0.06f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.55f),
                            radius = size.maxDimension * 0.35f
                        ),
                        radius = size.maxDimension * 0.35f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.55f)
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Lookup client by name, phone or email...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = "Search Filter") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("client_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredCustomers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Contacts,
                        contentDescription = "Search Empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No clients registered",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add client accounts using the Person button below",
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
                    items(filteredCustomers, key = { it.id }) { item ->
                        val isSelected = selectedCustomers.contains(item)
                        CustomerItemCard(
                            customer = item,
                            isSelectionMode = isSelectionMode,
                            isSelected = isSelected,
                            onSelectedChange = { selected ->
                                if (selected) {
                                    if (!selectedCustomers.contains(item)) selectedCustomers.add(item)
                                } else {
                                    selectedCustomers.remove(item)
                                }
                            },
                            onEditClicked = { activeEditingCustomer = item },
                            onDeleteClicked = {
                                viewModel.deleteCustomer(item)
                                Toast.makeText(context, "Client account deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
        }
    }

    // Modal adding inline
    if (showAddDialog) {
        CustomerEditorDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, companyName, phone, email, addr, gstin, supply ->
                viewModel.saveCustomer(0, name, companyName, phone, email, addr, gstin, supply)
                showAddDialog = false
            }
        )
    }

    // Modal edit inline
    activeEditingCustomer?.let { original ->
        CustomerEditorDialog(
            customer = original,
            onDismiss = { activeEditingCustomer = null },
            onConfirm = { name, companyName, phone, email, addr, gstin, supply ->
                viewModel.saveCustomer(original.id, name, companyName, phone, email, addr, gstin, supply)
                activeEditingCustomer = null
            }
        )
    }

    // Bulk Delete Confirmation Dialogue
    if (showBulkDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteConfirm = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Confirm Bulk Deletion") },
            text = { Text("Are you sure you want to permanently delete these ${selectedCustomers.size} selected client accounts? This operation is irreversible and historic invoices linked with them will refer to blank client names.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomersBulk(selectedCustomers.toList())
                        isSelectionMode = false
                        selectedCustomers.clear()
                        showBulkDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete ${selectedCustomers.size} Clients")
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

@Composable
fun CustomerItemCard(
    customer: Customer,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isSelectionMode) {
                    onSelectedChange(!isSelected)
                }
            }
            .testTag("client_card_${customer.id}")
            .graphicsLayer {
                rotationX = 2.5f
                rotationY = -3.5f
                cameraDistance = 14f * density
            }
            .drawBehind {
                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = 0.04f),
                    radius = size.maxDimension * 0.35f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.8f)
                )
                drawCircle(
                    color = Color(0xFF3B82F6).copy(alpha = 0.02f),
                    radius = size.maxDimension * 0.5f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.8f)
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) {
                Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary))
            } else {
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Black.copy(alpha = 0.1f)
                    )
                )
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Checkbox on the left
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = onSelectedChange,
                    modifier = Modifier.padding(end = 12.dp).testTag("client_checkbox_${customer.id}")
                )
            }

            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = if (customer.companyName.isNotEmpty()) "${customer.name} (${customer.companyName})" else customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (customer.phone.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Contact phone",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = customer.phone,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (customer.email.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Contact email",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = customer.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (customer.address.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Billing location address",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = customer.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (customer.gstin.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AssignmentInd,
                            contentDescription = "Client Tax Identification",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "GSTIN: ${customer.gstin}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (customer.placeOfSupply.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Place of Supply",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "Supply State: ${customer.placeOfSupply}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (!isSelectionMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditClicked) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Client properties",
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = onDeleteClicked) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete client",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerEditorDialog(
    customer: Customer? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, companyName: String, phone: String, email: String, address: String, gstin: String, placeOfSupply: String) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var companyName by remember { mutableStateOf(customer?.companyName ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var gstin by remember { mutableStateOf(customer?.gstin ?: "") }
    var placeOfSupply by remember { mutableStateOf(customer?.placeOfSupply ?: "") }

    val isEdit = customer != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Configure Client Account" else "Register Buyer Account") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Person Name*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("client_dialog_name")
                )
                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name / Business") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("client_dialog_phone")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = gstin,
                    onValueChange = { gstin = it },
                    label = { Text("Client GSTIN/Tax ID") },
                    placeholder = { Text("e.g. 07AAACH1234F1Z1") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = placeOfSupply,
                    onValueChange = { placeOfSupply = it },
                    label = { Text("Place of Supply (State)") },
                    placeholder = { Text("e.g. Maharashtra, Delhi") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Billing Address") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(name, companyName, phone, email, address, gstin, placeOfSupply)
            }) {
                Text(if (isEdit) "Save Details" else "Add Directory")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
