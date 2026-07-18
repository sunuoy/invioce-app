package com.example.ui.screens

import android.widget.Toast
import android.content.Intent
import java.io.File
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Warning
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
import com.example.data.Product
import com.example.ui.InvoiceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsStateWithLifecycle()
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val lowStockThreshold by viewModel.lowStockThreshold.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var activeEditorProduct by remember { mutableStateOf<Product?>(null) }
    var showCreatorDialog by remember { mutableStateOf(false) }
    var isThresholdExpanded by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery) {
        products.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Inventory (Stock & Tax)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick, modifier = Modifier.testTag("products_menu_btn")) {
                            Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                        }
                    }
                },
                actions = {
                    // Export Stock movement ledger CSV
                    IconButton(onClick = {
                        if (products.isEmpty()) {
                            Toast.makeText(context, "No inventory items to export", Toast.LENGTH_SHORT).show()
                            return@IconButton
                        }
                        try {
                            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                            val csvHeader = "Date,Product Name,Transaction Type,Reference/Invoice,Qty In (Purchase),Qty Out (Sale),Stock Balance\n"
                            
                            val allRows = mutableListOf<String>()
                            
                            for (p in products) {
                                // Gather sales for this product
                                val sales = mutableListOf<Triple<Long, String, Double>>() // timestamp, invoiceNumber, qty
                                for (invDetails in invoices) {
                                    for (item in invDetails.lineItems) {
                                        if (item.productId == p.id) {
                                            sales.add(Triple(invDetails.invoice.dateTimestamp, invDetails.invoice.invoiceNumber, item.quantity))
                                        }
                                    }
                                }
                                
                                val totalSold = sales.sumOf { it.third }
                                val initialPurchaseQty = p.stock + totalSold
                                
                                // Define transactions: Inflow + Outflows
                                class StockTx(
                                    val timestamp: Long,
                                    val type: String,
                                    val ref: String,
                                    val qtyIn: Double,
                                    val qtyOut: Double
                                )
                                
                                val txList = mutableListOf<StockTx>()
                                // Add the initial inflow
                                txList.add(StockTx(p.dateTimestamp, "Purchase", "Initial Stock / Batch", initialPurchaseQty, 0.0))
                                // Add sales outflows
                                for (s in sales) {
                                    txList.add(StockTx(s.first, "Sale", s.second, 0.0, s.third))
                                }
                                
                                // Sort chronologically
                                txList.sortBy { it.timestamp }
                                
                                var runningBalance = 0.0
                                for (tx in txList) {
                                    runningBalance += (tx.qtyIn - tx.qtyOut)
                                    val dateStr = dateFormat.format(java.util.Date(tx.timestamp))
                                    val prodNameEscaped = "\"${p.name.replace("\"", "\"\"")}\""
                                    val refEscaped = "\"${tx.ref.replace("\"", "\"\"")}\""
                                    allRows.add("$dateStr,$prodNameEscaped,${tx.type},$refEscaped,${tx.qtyIn},${tx.qtyOut},$runningBalance")
                                }
                            }
                            
                            val csvContent = csvHeader + allRows.joinToString("\n")
                            val cacheDir = File(context.cacheDir, "exports").apply { mkdirs() }
                            val exportFile = File(cacheDir, "stock_movement_ledger_${System.currentTimeMillis()}.csv")
                            exportFile.writeText(csvContent, Charsets.UTF_8)
                            
                            val uri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                exportFile
                            )
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Stock Ledger Transaction History Export")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export Stock Ledger via"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Export Stock Ledger"
                        )
                    }
                },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF06B6D4).copy(alpha = 0.03f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .drawBehind {
                    // Orb 1 (Top Left) - Cyan
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF06B6D4).copy(alpha = 0.12f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.25f),
                            radius = size.maxDimension * 0.45f
                        ),
                        radius = size.maxDimension * 0.45f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.25f)
                    )
                    // Orb 2 (Bottom Right) - Amber
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFF59E0B).copy(alpha = 0.08f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.7f),
                            radius = size.maxDimension * 0.4f
                        ),
                        radius = size.maxDimension * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.7f)
                    )
                    // Orb 3 (Center Right) - Indigo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF6366F1).copy(alpha = 0.06f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.45f),
                            radius = size.maxDimension * 0.35f
                        ),
                        radius = size.maxDimension * 0.35f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.8f, size.height * 0.45f)
                    )
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
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

            // Dynamic Threshold Configuration Panel
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.clickable { isThresholdExpanded = !isThresholdExpanded }.testTag("threshold_settings_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Configure stock threshold",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Low Stock Threshold: ${lowStockThreshold.toInt()} units",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (isThresholdExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                val criticallyLowCount = products.count { it.stock <= lowStockThreshold }
                if (criticallyLowCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ) {
                        Text(
                            text = "$criticallyLowCount alerts",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isThresholdExpanded) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Configure the threshold value triggers low-stock alerts & highlights below critical balance quantities:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Slider(
                                value = lowStockThreshold,
                                onValueChange = { viewModel.updateLowStockThreshold(it) },
                                valueRange = 0f..50f,
                                steps = 50,
                                modifier = Modifier.weight(1f).testTag("threshold_slider")
                            )
                            Text(
                                text = "${lowStockThreshold.toInt()} units",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.width(60.dp)
                            )
                        }
                    }
                }
            }

            // Automated Notification Banner UI
            val lowStockProducts = products.filter { it.stock <= lowStockThreshold }
            if (lowStockProducts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .testTag("low_stock_notification_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Automated Inventory Alert",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "${lowStockProducts.size} items are critically below threshold of ${lowStockThreshold.toInt()} units.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val primaryColor = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.size(96.dp)) {
                        val cx = size.width / 2
                        val cy = size.height / 2
                        val depth = 16.dp.toPx()
                        
                        val topPath = Path().apply {
                            moveTo(cx, cy - 24.dp.toPx())
                            lineTo(cx + 36.dp.toPx(), cy - 12.dp.toPx() - depth / 2)
                            lineTo(cx, cy - depth)
                            lineTo(cx - 36.dp.toPx(), cy - 12.dp.toPx() - depth / 2)
                            close()
                        }
                        val leftPath = Path().apply {
                            moveTo(cx - 36.dp.toPx(), cy - 12.dp.toPx() - depth / 2)
                            lineTo(cx, cy - depth)
                            lineTo(cx, cy + 24.dp.toPx() - depth)
                            lineTo(cx - 36.dp.toPx(), cy + 12.dp.toPx() - depth)
                            close()
                        }
                        val rightPath = Path().apply {
                            moveTo(cx, cy - depth)
                            lineTo(cx + 36.dp.toPx(), cy - 12.dp.toPx() - depth / 2)
                            lineTo(cx + 36.dp.toPx(), cy + 12.dp.toPx() - depth)
                            lineTo(cx, cy + 24.dp.toPx() - depth)
                            close()
                        }

                        drawPath(leftPath, color = primaryColor.copy(alpha = 0.04f))
                        drawPath(rightPath, color = primaryColor.copy(alpha = 0.02f))
                        drawPath(topPath, color = primaryColor.copy(alpha = 0.08f))

                        drawPath(leftPath, color = primaryColor.copy(alpha = 0.35f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(rightPath, color = primaryColor.copy(alpha = 0.35f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(topPath, color = primaryColor.copy(alpha = 0.45f), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
                            lowStockThreshold = lowStockThreshold,
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
    }

    // Modal Add Product Dialog
    if (showCreatorDialog) {
        ProductDialogEditor(
            onDismiss = { showCreatorDialog = false },
            onConfirm = { name, price, tax, unit, stock, hsnSac, attachmentPath ->
                viewModel.saveProduct(0, name, price, tax, unit, stock, hsnSac, attachmentPath)
                showCreatorDialog = false
            }
        )
    }

    // Modal Edit Product Dialog
    activeEditorProduct?.let { item ->
        ProductDialogEditor(
            product = item,
            onDismiss = { activeEditorProduct = null },
            onConfirm = { name, price, tax, unit, stock, hsnSac, attachmentPath ->
                viewModel.saveProduct(item.id, name, price, tax, unit, stock, hsnSac, attachmentPath)
                activeEditorProduct = null
            }
        )
    }
}

@Composable
fun ProductItemRow(
    product: Product,
    lowStockThreshold: Float,
    onEditClicked: () -> Unit,
    onDeleteClicked: () -> Unit
) {
    val isCritical = product.stock <= lowStockThreshold
    val cardBg = if (isCritical) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("product_item_${product.id}")
            .graphicsLayer {
                rotationX = 2.5f
                rotationY = -3f
                cameraDistance = 14f * density
            }
            .drawBehind {
                val circleColor = if (isCritical) Color(0xFFEF4444) else Color(0xFF10B981)
                drawCircle(
                    color = circleColor.copy(alpha = 0.05f),
                    radius = size.maxDimension * 0.35f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.8f)
                )
                drawCircle(
                    color = circleColor.copy(alpha = 0.02f),
                    radius = size.maxDimension * 0.5f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.8f)
                )
            },
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(4.dp),
        border = if (isCritical) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        } else {
            BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.05f),
                        Color.Black.copy(alpha = 0.1f)
                    )
                )
            )
        }
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
                if (product.hsnSac.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Text(
                            text = "HSN/SAC: ${product.hsnSac}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                // Inventory stock visualization
                StockIndicatorTracker(stockValue = product.stock, unitStr = product.unit, lowStockThreshold = lowStockThreshold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (product.attachmentPath.isNotBlank()) {
                    val context = LocalContext.current
                    IconButton(onClick = {
                        try {
                            val file = File(product.attachmentPath)
                            if (file.exists()) {
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val extension = file.extension.lowercase()
                                val mimeType = when (extension) {
                                    "pdf" -> "application/pdf"
                                    "jpg", "jpeg" -> "image/jpeg"
                                    "png" -> "image/png"
                                    "gif" -> "image/gif"
                                    "txt" -> "text/plain"
                                    else -> "*/*"
                                }
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, mimeType)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } else {
                                Toast.makeText(context, "Document file does not exist locally.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Unable to open document: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "View Stock Purchase Document",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
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
fun StockIndicatorTracker(stockValue: Double, unitStr: String, lowStockThreshold: Float) {
    val isCritical = stockValue <= lowStockThreshold
    val stockColor = when {
        stockValue <= 0f -> Color(0xFFEF4444)     // Red / Out
        isCritical -> Color(0xFFF59E0B)           // Amber / Warn
        else -> Color(0xFF10B981)                 // Green / Healthy
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    isCritical -> "Low stock: $stockValue $unitStr"
                    else -> "In Stock: $stockValue $unitStr available"
                },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = if (isCritical) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        val progress = (stockValue / 100.0).coerceIn(0.0, 1.0).toFloat()
        LinearProgressIndicator(
            progress = { progress },
            color = stockColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .width(160.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun ProductDialogEditor(
    product: Product? = null,
    onDismiss: () -> Unit,
    onConfirm: (name: String, price: Double, tax: Double, unit: String, stock: Double, hsnSac: String, attachmentPath: String) -> Unit
) {
    var name by remember { mutableStateOf(product?.name ?: "") }
    var priceStr by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var taxStr by remember { mutableStateOf(product?.taxRate?.toString() ?: "18") } // default GST is 18%
    var unit by remember { mutableStateOf(product?.unit ?: "kg") }
    var stockStr by remember { mutableStateOf(product?.stock?.toString() ?: "50") }
    var hsnSac by remember { mutableStateOf(product?.hsnSac ?: "") }
    var attachmentPathState by remember { mutableStateOf(product?.attachmentPath ?: "") }

    val isEdit = product != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Configure Stock Product" else "Register Inventory Stock") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
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
                        placeholder = { Text("kg, bags, QT, box") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("kg", "bags", "QT", "box").forEach { suggestion ->
                        val isSelected = unit.lowercase(Locale.ROOT) == suggestion.lowercase(Locale.ROOT)
                        SuggestionChip(
                            onClick = { unit = suggestion },
                            label = { Text(suggestion, fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }

                OutlinedTextField(
                    value = hsnSac,
                    onValueChange = { hsnSac = it },
                    label = { Text("HSN / SAC Code") },
                    placeholder = { Text("e.g. 998311, 8471") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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

                Spacer(modifier = Modifier.height(4.dp))
                Text("Purchase Invoice / Document", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (attachmentPathState.isEmpty()) {
                    val context = LocalContext.current
                    val launcher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.GetContent()
                    ) { uri ->
                        uri?.let {
                            try {
                                val attachmentsDir = File(context.filesDir, "attachments").apply { mkdirs() }
                                val extension = context.contentResolver.getType(it)?.split("/")?.lastOrNull() ?: "bin"
                                val destFile = File(attachmentsDir, "purchase_${System.currentTimeMillis()}.$extension")
                                context.contentResolver.openInputStream(it)?.use { input ->
                                    destFile.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                attachmentPathState = destFile.absolutePath
                                Toast.makeText(context, "Purchase document attached!", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Failed to copy file: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    Button(
                        onClick = { launcher.launch("*/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "Attach Document", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Attach Stock Invoice", fontSize = 12.sp)
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val file = File(attachmentPathState)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.InsertDriveFile,
                                contentDescription = "Attachment present",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Column {
                                Text(
                                    text = file.name,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        IconButton(onClick = { attachmentPathState = "" }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove attachment",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val price = priceStr.toDoubleOrNull() ?: 0.0
                val tax = taxStr.toDoubleOrNull() ?: 18.0
                val stock = stockStr.toDoubleOrNull() ?: 0.0

                onConfirm(name, price, tax, unit, stock, hsnSac, attachmentPathState)
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
