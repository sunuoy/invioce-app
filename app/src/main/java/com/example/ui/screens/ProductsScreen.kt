package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Warning
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
import com.example.data.Product
import com.example.ui.InvoiceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var activeEditorProduct by remember { mutableStateOf<Product?>(null) }
    var showCreatorDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery) {
        products.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory (Stock & Tax)", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreatorDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_product_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product Stock")
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
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search product name...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Lookup") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("product_search_input"),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            if (filteredProducts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Inventory2,
                        contentDescription = "Search Empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No products found",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Register inventory stock using the Add + action",
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
                    items(filteredProducts, key = { it.id }) { item ->
                        ProductItemRow(
                            product = item,
                            onEditClicked = { activeEditorProduct = item },
                            onDeleteClicked = {
                                viewModel.deleteProduct(item)
                                Toast.makeText(context, "Product Deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Add Product Dialog
    if (showCreatorDialog) {
        ProductDialogEditor(
            onDismiss = { showCreatorDialog = false },
            onConfirm = { name, price, tax, unit, stock ->
                viewModel.saveProduct(0, name, price, tax, unit, stock)
                showCreatorDialog = false
            }
        )
    }

    // Modal Edit Product Dialog
    activeEditorProduct?.let { item ->
        ProductDialogEditor(
            product = item,
            onDismiss = { activeEditorProduct = null },
            onConfirm = { name, price, tax, unit, stock ->
                viewModel.saveProduct(item.id, name, price, tax, unit, stock)
                activeEditorProduct = null
            }
        )
    }
}

@Composable
fun ProductItemRow(
    product: Product,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1.5f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Rate: ₹${product.price} / ${product.unit}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Text(
                            text = "${product.taxRate}% GST",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Inventory stock visualization
                StockIndicatorTracker(stockValue = product.stock, unitStr = product.unit)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEditClicked) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit product properties",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDeleteClicked) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete product",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun StockIndicatorTracker(stockValue: Double, unitStr: String) {
    val stockColor = when {
        stockValue <= 0f -> Color(0xFFEF4444)     // Red / Out
        stockValue <= 10f -> Color(0xFFF59E0B)    // Amber / Warn
        else -> Color(0xFF10B981)                 // Green / Healthy
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(stockColor)
        )
        Text(
            text = when {
                stockValue <= 0f -> "Out of Stock"
                stockValue <= 10f -> "Low Stock: $stockValue $unitStr left"
                else -> "In Stock: $stockValue $unitStr available"
            },
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = if (stockValue <= 0f) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProductDialogEditor(
    product: Product? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, tax: Double, unit: String, stock: Double) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var priceStr by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var taxStr by remember { mutableStateOf(product?.taxRate?.toString() ?: "18") } // default GST is 18%
    var unit by remember { mutableStateOf(product?.unit ?: "pcs") }
    var stockStr by remember { mutableStateOf(product?.stock?.toString() ?: "50") }

    val isEdit = product != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Configure Stock Product" else "Register Inventory Stock") },
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
                    label = { Text("Product / Service Name*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("product_dialog_name")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Price (₹)*") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.2f).testTag("product_dialog_price")
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Billing Unit") },
                        placeholder = { Text("pcs, hrs, kg") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = taxStr,
                        onValueChange = { taxStr = it },
                        label = { Text("GST Tax (%)") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f).testTag("product_dialog_tax")
                    )
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Stock Quantity") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1.2f).testTag("product_dialog_stock")
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val price = priceStr.toDoubleOrNull() ?: 0.0
                val tax = taxStr.toDoubleOrNull() ?: 18.0
                val stock = stockStr.toDoubleOrNull() ?: 0.0

                onConfirm(name, price, tax, unit, stock)
            }) {
                Text(if (isEdit) "Save Edits" else "Confirm Register")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
