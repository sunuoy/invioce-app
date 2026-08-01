package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
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
    modifier: Modifier = Modifier,
    onMenuClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "iconPulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    val arrowOffset by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "arrowOffset"
    )

    val prefs = remember(context) { context.getSharedPreferences("invoice_generator_prefs", android.content.Context.MODE_PRIVATE) }
    var showTaxSummary by remember(prefs) {
        mutableStateOf(prefs.getBoolean("show_tax_summary", true))
    }
    var showSalesTrend by remember(prefs) {
        mutableStateOf(prefs.getBoolean("show_sales_trend", true))
    }

    var taxStartDate by remember { mutableStateOf(System.currentTimeMillis() - 30L * 24L * 60L * 60L * 1000L) } // 30 days ago
    var taxEndDate by remember { mutableStateOf(System.currentTimeMillis()) } // today


    DisposableEffect(prefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "show_tax_summary") {
                showTaxSummary = prefs.getBoolean("show_tax_summary", true)
            } else if (key == "show_sales_trend") {
                showSalesTrend = prefs.getBoolean("show_sales_trend", true)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose {
            prefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

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
                navigationIcon = {
                    if (onMenuClick != null) {
                        IconButton(onClick = onMenuClick, modifier = Modifier.testTag("dashboard_menu_btn")) {
                            Icon(Icons.Default.Menu, contentDescription = "Open navigation menu")
                        }
                    }
                },
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
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = "Business Logo",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF3B82F6).copy(alpha = 0.03f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
                .drawBehind {
                    // Orb 1 (Top Left) - Brand Blue
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.10f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.2f),
                            radius = size.maxDimension * 0.45f
                        ),
                        radius = size.maxDimension * 0.45f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.15f, size.height * 0.2f)
                    )
                    // Orb 2 (Bottom Right) - Emerald Green
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF10B981).copy(alpha = 0.08f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.75f),
                            radius = size.maxDimension * 0.4f
                        ),
                        radius = size.maxDimension * 0.4f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.85f, size.height * 0.75f)
                    )
                    // Orb 3 (Center Right) - Violet Indigo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFF6366F1).copy(alpha = 0.06f), Color.Transparent),
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.80f, size.height * 0.4f),
                            radius = size.maxDimension * 0.35f
                        ),
                        radius = size.maxDimension * 0.35f,
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.80f, size.height * 0.4f)
                    )
                }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
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
                            .testTag("total_sales_card")
                            .floating3D(rotationX = 5f, rotationY = -8f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Black.copy(alpha = 0.15f)
                                )
                            )
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF10B981), // Emerald Green
                                            Color(0xFF047857)  // Deep Emerald/Forest Green
                                        )
                                    )
                                )
                                .drawBehind {
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.08f),
                                        radius = size.maxDimension * 0.4f,
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f)
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.04f),
                                        radius = size.maxDimension * 0.6f,
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.1f)
                                    )
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.06f),
                                        start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.8f),
                                        end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.4f),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f))
                                            .graphicsLayer {
                                                scaleX = iconScale
                                                scaleY = iconScale
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.TrendingUp,
                                            contentDescription = "Sales Tracker",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .graphicsLayer {
                                                    translationY = arrowOffset * density
                                                }
                                        )
                                    }
                                    Text(
                                        text = "Total Sales",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = String.format(Locale.US, "₹%,.2f", totalSales ?: 0.0),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Paid invoices only",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Outstanding (Draft, Sent) Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("outstanding_sales_card")
                            .floating3D(rotationX = 5f, rotationY = -8f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Black.copy(alpha = 0.15f)
                                )
                            )
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFEF4444), // Coral Red
                                            Color(0xFFB91C1C)  // Ruby/Crimson Red
                                        )
                                    )
                                )
                                .drawBehind {
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.08f),
                                        radius = size.maxDimension * 0.4f,
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.9f)
                                    )
                                    drawCircle(
                                        color = Color.White.copy(alpha = 0.04f),
                                        radius = size.maxDimension * 0.6f,
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.9f)
                                    )
                                    drawLine(
                                        color = Color.White.copy(alpha = 0.06f),
                                        start = androidx.compose.ui.geometry.Offset(0f, size.height * 0.2f),
                                        end = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.6f),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                }
                                .padding(16.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f))
                                            .graphicsLayer {
                                                scaleX = iconScale
                                                scaleY = iconScale
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AccountBalanceWallet,
                                            contentDescription = "Pending Payments",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Text(
                                        text = "Outstanding",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = String.format(Locale.US, "₹%,.2f", outstandingAmount ?: 0.0),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Draft / Sent statuses",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Canvas Revenue Trend mini-chart (Always render to maintain layout structure)
            if (showSalesTrend) {
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
                            .height(130.dp)
                            .floating3D(rotationX = 4f, rotationY = -4f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.2f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Black.copy(alpha = 0.1f)
                                )
                            )
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.15f)
                                        )
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            InvoiceTrendGraph(
                                invoices = invoices,
                                primaryColor = MaterialTheme.colorScheme.primary,
                                secondaryColor = MaterialTheme.colorScheme.secondary
                            )

                            if (invoices.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No sales data found. Add invoices to project trend.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2.2. Interactive Tax / GST summary dashboard
            if (showTaxSummary) {
                item {
                val context = LocalContext.current
                
                // Format dates for display
                val dFormatter = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                
                // Aggregate figures on the fly
                val filteredInvoicesForTax = remember(invoices, taxStartDate, taxEndDate) {
                    invoices.filter {
                        val timestamp = it.invoice.dateTimestamp
                        timestamp in taxStartDate..taxEndDate
                    }
                }
                
                val totalTaxableTurnover = remember(filteredInvoicesForTax) {
                    filteredInvoicesForTax.sumOf { it.invoice.subtotal }
                }
                val totalGstCollected = remember(filteredInvoicesForTax) {
                    filteredInvoicesForTax.sumOf { it.invoice.taxTotal }
                }
                val totalGrossSales = remember(filteredInvoicesForTax) {
                    filteredInvoicesForTax.sumOf { it.invoice.grandTotal }
                }
                
                // GST Rates Breakdown
                val gstBreakdown = remember(filteredInvoicesForTax) {
                    val map = mutableMapOf<Double, Double>()
                    for (inf in filteredInvoicesForTax) {
                        for (item in inf.lineItems) {
                            val rate = item.taxRate
                            map[rate] = map.getOrDefault(rate, 0.0) + item.tax
                        }
                    }
                    map.toList().sortedBy { it.first }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().testTag("tax_summary_dashboard_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Assessment,
                                contentDescription = "GST Tax Filing Report logo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "GST Fiscal Tax Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }

                        Text(
                            text = "Aggregates tax values (CGST/SGST/IGST components) for easy bookkeeping and corporate/individual GSTR returns.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Date Selectors
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Start Date
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val cal = Calendar.getInstance().apply { timeInMillis = taxStartDate }
                                        android.app.DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                val resCal = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, y)
                                                    set(Calendar.MONTH, m)
                                                    set(Calendar.DAY_OF_MONTH, d)
                                                    set(Calendar.HOUR_OF_DAY, 0)
                                                    set(Calendar.MINUTE, 0)
                                                    set(Calendar.SECOND, 0)
                                                }
                                                taxStartDate = resCal.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("From date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = dFormatter.format(Date(taxStartDate)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // End Date
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        val cal = Calendar.getInstance().apply { timeInMillis = taxEndDate }
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
                                                taxEndDate = resCal.timeInMillis
                                            },
                                            cal.get(Calendar.YEAR),
                                            cal.get(Calendar.MONTH),
                                            cal.get(Calendar.DAY_OF_MONTH)
                                        ).show()
                                    }
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("To date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = dFormatter.format(Date(taxEndDate)),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Summary Statistics
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Invoices matched in range", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("${filteredInvoicesForTax.size} bills", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Taxable turnover (Subtotal)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(String.format(Locale.US, "₹%,.2f", totalTaxableTurnover), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Aggregated GST Collected", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                    Text(String.format(Locale.US, "₹%,.2f", totalGstCollected), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total gross business sales", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(String.format(Locale.US, "₹%,.2f", totalGrossSales), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // GST Rate Categories Breakdown
                        if (gstBreakdown.isNotEmpty()) {
                            Text(
                                text = "Taxes Breakdown by GST rate tier:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for ((rate, taxVal) in gstBreakdown) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "@ $rate% GST rate",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = String.format(Locale.US, "₹%,.2f", taxVal),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "No item-wise taxes were collected for the selected period range.",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
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
                    text = String.format(Locale.US, "₹%,.2f", item.invoice.grandTotal),
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
    val barGradients = remember {
        listOf(
            listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8)), // Blue
            listOf(Color(0xFF10B981), Color(0xFF047857)), // Emerald Green
            listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9)), // Purple
            listOf(Color(0xFFEC4899), Color(0xFFBE185D)), // Pink
            listOf(Color(0xFFF59E0B), Color(0xFFB45309)), // Amber
            listOf(Color(0xFFEF4444), Color(0xFFB91C1C)), // Red
            listOf(Color(0xFF06B6D4), Color(0xFF0891B2)), // Cyan
            listOf(Color(0xFF6366F1), Color(0xFF4338CA))  // Indigo
        )
    }

    val lastEightMonths = remember(invoices) {
        val keySdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val labelSdf = SimpleDateFormat("MMM", Locale.US)
        
        // Group invoices once into a map by yyyy-MM
        val monthTotalsMap = invoices.groupBy {
            keySdf.format(Date(it.invoice.dateTimestamp))
        }.mapValues { entry ->
            entry.value.sumOf { it.invoice.grandTotal }
        }
        
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -7)
        val list = ArrayList<Pair<String, Double>>(8)
        
        for (i in 0 until 8) {
            val label = labelSdf.format(calendar.time)
            val key = keySdf.format(calendar.time)
            val totalForMonth = monthTotalsMap[key] ?: 0.0
            list.add(Pair(label, totalForMonth))
            calendar.add(Calendar.MONTH, 1)
        }
        list
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (lastEightMonths.isEmpty()) return@Canvas

            // Draw grid lines for a technical look
            val gridLinesCount = 3
            val stepYGrid = size.height / (gridLinesCount + 1)
            for (i in 1..gridLinesCount) {
                val yGrid = i * stepYGrid
                drawLine(
                    color = primaryColor.copy(alpha = 0.08f),
                    start = androidx.compose.ui.geometry.Offset(0f, yGrid),
                    end = androidx.compose.ui.geometry.Offset(size.width, yGrid),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val maxVal = (lastEightMonths.maxOfOrNull { it.second } ?: 100.0).coerceAtLeast(100.0)
            val width = size.width
            val height = size.height

            val stepX = width / lastEightMonths.size
            val barWidth = stepX * 0.45f
            val depth = 6.dp.toPx() // 3D depth perspective skew
            val usableHeight = height - depth - 8.dp.toPx()

            lastEightMonths.forEachIndexed { idx, pair ->
                val gradient = barGradients[idx % barGradients.size]
                val startColor = gradient[0]
                val endColor = gradient[1]

                val fraction = pair.second / maxVal
                val x = idx * stepX + stepX * 0.275f
                val barHeight = (fraction.toFloat() * usableHeight).coerceAtLeast(4.dp.toPx())
                val yFront = height - barHeight

                // Front Face
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(startColor, endColor)
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(x, yFront),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
                )

                // Top Face (3D perspective slant)
                val topPath = Path().apply {
                    moveTo(x, yFront)
                    lineTo(x + depth, yFront - depth)
                    lineTo(x + barWidth + depth, yFront - depth)
                    lineTo(x + barWidth, yFront)
                    close()
                }
                drawPath(
                    path = topPath,
                    color = startColor.copy(alpha = 0.9f)
                )

                // Side Face (3D perspective slant)
                val sidePath = Path().apply {
                    moveTo(x + barWidth, yFront)
                    lineTo(x + barWidth + depth, yFront - depth)
                    lineTo(x + barWidth + depth, height - depth)
                    lineTo(x + barWidth, height)
                    close()
                }
                drawPath(
                    path = sidePath,
                    color = endColor.copy(alpha = 0.65f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Month Labels Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            lastEightMonths.forEach { pair ->
                Text(
                    text = pair.first,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

fun Modifier.floating3D(
    rotationX: Float = 5f,
    rotationY: Float = -8f,
    cameraDistance: Float = 14f
): Modifier = this.composed {
    var rawTiltX by remember { mutableStateOf(0f) }
    var rawTiltY by remember { mutableStateOf(0f) }

    val animatedTiltX by animateFloatAsState(
        targetValue = rawTiltX,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tiltX"
    )
    val animatedTiltY by animateFloatAsState(
        targetValue = rawTiltY,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "tiltY"
    )

    this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent()
                    val changes = event.changes
                    if (changes.any { it.pressed }) {
                        val position = changes.first().position
                        val w = size.width
                        val h = size.height
                        if (w > 0 && h > 0) {
                            val normX = ((position.x - w / 2f) / (w / 2f)).coerceIn(-1f, 1f)
                            val normY = ((position.y - h / 2f) / (h / 2f)).coerceIn(-1f, 1f)
                            rawTiltX = -normY * 12f
                            rawTiltY = normX * 12f
                        }
                    } else {
                        rawTiltX = 0f
                        rawTiltY = 0f
                    }
                }
            }
        }
        .graphicsLayer {
            this.rotationX = rotationX + animatedTiltX
            this.rotationY = rotationY + animatedTiltY
            this.cameraDistance = cameraDistance * density
        }
}
