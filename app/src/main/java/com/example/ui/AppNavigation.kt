package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import com.example.ui.screens.*
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "dashboard_tab"),
    INVOICES("Invoices", Icons.Default.ReceiptLong, "invoices_tab"),
    PRODUCTS("Inventory", Icons.Default.Inventory2, "products_tab"),
    CUSTOMERS("Clients", Icons.Default.Group, "customers_tab"),
    PROFILE("Profile", Icons.Default.AccountCircle, "settings_tab")
}

fun android.content.Context.shareApp() {
    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(
            android.content.Intent.EXTRA_TEXT,
            "Hey! Check out Invoice Generator App for instant invoice creation, inventory tracking & billing: https://github.com/sunuoy/invioce-app"
        )
        type = "text/plain"
    }
    startActivity(android.content.Intent.createChooser(sendIntent, "Invite via"))
}

@Composable
fun MainAppNavigation(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }
    var showSeparateSettingsPage by remember { mutableStateOf(false) }
    var startInvoicesInCreateMode by remember { mutableStateOf(false) }
    var viewInvoiceId by remember { mutableStateOf<Int?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            val businessProfile by viewModel.businessProfile.collectAsState()
            val surfaceColor = MaterialTheme.colorScheme.surface
            ModalDrawerSheet(
                modifier = Modifier
                    .width(310.dp)
                    .drawBehind {
                        // Draw background with 88% (top) to 98% (bottom) opacity
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    surfaceColor.copy(alpha = 0.88f),
                                    surfaceColor.copy(alpha = 0.98f)
                                )
                            ),
                            size = size
                        )

                        // Orb 1 (Top Left) - Brand Violet/Indigo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF6366F1).copy(alpha = 0.15f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.15f),
                                radius = size.maxDimension * 0.5f
                            ),
                            radius = size.maxDimension * 0.5f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.15f)
                        )
                        // Orb 2 (Bottom Right) - Brand Blue
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.12f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.85f),
                                radius = size.maxDimension * 0.4f
                            ),
                            radius = size.maxDimension * 0.4f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.85f)
                        )
                    },
                drawerContainerColor = Color.Transparent
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // GORGEOUS GLASS BRAND CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(
                            1.dp, 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), 
                            RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Glowing Brand Logo circle
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(8.dp, RoundedCornerShape(14.dp), ambientColor = MaterialTheme.colorScheme.primary)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.secondary
                                        )
                                    ),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = businessProfile?.shortIcon?.takeIf { it.isNotEmpty() } ?: "⚡",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(14.dp))
                        
                        Column {
                            Text(
                                text = businessProfile?.businessName ?: "Invoice Easy Pro",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Professional Billing Hub",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                
                // MENU SECTION HEADER
                Text(
                    text = "MAIN HUB",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    letterSpacing = 1.2.sp
                )

                // Render main navigation menu items
                AppTab.values().forEach { tab ->
                    DrawerMenuItem(
                        icon = tab.icon,
                        title = tab.title,
                        selected = (!showSeparateSettingsPage && currentTab == tab),
                        onClick = {
                            currentTab = tab
                            showSeparateSettingsPage = false
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.testTag(tab.tag)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                
                // USER PROFILE / BUSINESS SETTINGS FOOTER CARD
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(
                            1.dp, 
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), 
                            RoundedCornerShape(24.dp)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        // Company Info Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        MaterialTheme.colorScheme.secondaryContainer,
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = businessProfile?.businessName ?: "Apex Tech Solutions",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = businessProfile?.email?.takeIf { it.isNotEmpty() } ?: "admin@invoiceeasy.com",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        
                        // Action buttons row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Invite/Share App Button
                            IconButton(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    context.shareApp()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("drawer_invite_item")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Invite Friends / Share App",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // App Preferences/Settings Button
                            IconButton(
                                onClick = {
                                    showSeparateSettingsPage = true
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("drawer_settings_item")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Application Preferences",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // Sign Out Button
                            IconButton(
                                onClick = {
                                    scope.launch { drawerState.close() }
                                    viewModel.logoutUser()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("drawer_logout_item")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Sign Out",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        gesturesEnabled = true
    ) {
        if (showSeparateSettingsPage) {
            AppSettingsScreen(
                viewModel = viewModel,
                onMenuClick = { scope.launch { drawerState.open() } },
                onBackToApp = { showSeparateSettingsPage = false },
                modifier = modifier
            )
        } else {
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 8.dp
                    ) {
                        AppTab.values().forEach { tab ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { currentTab = tab },
                                icon = {
                                    val iconScale by animateFloatAsState(
                                        targetValue = if (currentTab == tab) 1.25f else 1.0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioHighBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        label = "nav_icon_scale"
                                    )
                                    val iconTranslationY by animateFloatAsState(
                                        targetValue = if (currentTab == tab) -6f else 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioHighBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        label = "nav_icon_translation_y"
                                    )
                                    val iconRotation by animateFloatAsState(
                                        targetValue = if (currentTab == tab) 360f else 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        ),
                                        label = "nav_icon_rotation"
                                    )
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        modifier = Modifier.graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                            translationY = iconTranslationY * density
                                            rotationZ = iconRotation
                                        }
                                    )
                                },
                                label = { Text(tab.title) },
                                modifier = Modifier.testTag(tab.tag)
                            )
                        }
                    }
                },
                modifier = modifier
            ) { innerPadding ->
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.98f)) togetherWith (fadeOut() + scaleOut(targetScale = 0.98f))
                    },
                    label = "tab_transitions"
                ) { targetTab ->
                    val screenModifier = Modifier
                        .fillMaxSize()
                        .padding(
                            top = 0.dp,
                            bottom = innerPadding.calculateBottomPadding()
                        )

                    val menuAction: () -> Unit = { scope.launch { drawerState.open() } }

                    when (targetTab) {
                        AppTab.DASHBOARD -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onCreateInvoiceClicked = {
                                    startInvoicesInCreateMode = true
                                    currentTab = AppTab.INVOICES
                                },
                                onViewInvoiceDetails = { invoiceId ->
                                    viewInvoiceId = invoiceId
                                    currentTab = AppTab.INVOICES
                                },
                                onMenuClick = menuAction,
                                modifier = screenModifier
                            )
                        }
                        AppTab.INVOICES -> {
                            InvoicesScreen(
                                viewModel = viewModel,
                                onMenuClick = menuAction,
                                startInCreateMode = startInvoicesInCreateMode,
                                onClearCreateMode = { startInvoicesInCreateMode = false },
                                viewInvoiceId = viewInvoiceId,
                                onClearViewInvoiceId = { viewInvoiceId = null },
                                modifier = screenModifier
                            )
                        }
                        AppTab.PRODUCTS -> {
                            ProductsScreen(
                                viewModel = viewModel,
                                onMenuClick = menuAction,
                                modifier = screenModifier
                            )
                        }
                        AppTab.CUSTOMERS -> {
                            CustomersScreen(
                                viewModel = viewModel,
                                onMenuClick = menuAction,
                                modifier = screenModifier
                            )
                        }
                        AppTab.PROFILE -> {
                            SettingsScreen(
                                viewModel = viewModel,
                                onMenuClick = menuAction,
                                modifier = screenModifier
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "drawer_item_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(durationMillis = 200),
        label = "drawer_item_content"
    )
    val indicatorScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "drawer_item_indicator"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .graphicsLayer {
                scaleX = if (selected) 1.01f else 1f
                scaleY = if (selected) 1.01f else 1f
            }
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical indicator pill on the left
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(16.dp)
                .graphicsLayer { scaleY = indicatorScale }
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        
        Spacer(modifier = Modifier.width(10.dp))
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(14.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
