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
import com.example.ui.screens.*
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "dashboard_tab"),
    INVOICES("Invoices", Icons.Default.ReceiptLong, "invoices_tab"),
    PRODUCTS("Inventory", Icons.Default.Inventory2, "products_tab"),
    CUSTOMERS("Clients", Icons.Default.Group, "customers_tab"),
    PROFILE("Profile", Icons.Default.AccountCircle, "settings_tab")
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
            val surfaceColor = MaterialTheme.colorScheme.surface
            ModalDrawerSheet(
                modifier = Modifier
                    .width(300.dp)
                    .drawBehind {
                        // Draw background with 80% (top) to 95% (bottom) opacity (20% transparency)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    surfaceColor.copy(alpha = 0.82f),
                                    surfaceColor.copy(alpha = 0.95f)
                                )
                            ),
                            size = size
                        )

                        // Orb 1 (Top Left) - Brand Violet/Indigo
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF6366F1).copy(alpha = 0.12f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.15f),
                                radius = size.maxDimension * 0.5f
                            ),
                            radius = size.maxDimension * 0.5f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.15f)
                        )
                        // Orb 2 (Bottom Right) - Brand Blue
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.10f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.85f),
                                radius = size.maxDimension * 0.4f
                            ),
                            radius = size.maxDimension * 0.4f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.85f)
                        )
                        // Orb 3 (Center Left) - Coral Pink
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFFEC4899).copy(alpha = 0.08f), Color.Transparent),
                                center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.5f),
                                radius = size.maxDimension * 0.35f
                            ),
                            radius = size.maxDimension * 0.35f,
                            center = androidx.compose.ui.geometry.Offset(size.width * 0.2f, size.height * 0.5f)
                        )
                    },
                drawerContainerColor = Color.Transparent
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text(
                        text = "⚡ Invoice Easy Pro",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Professional Billing Hub",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))

                AppTab.values().forEach { tab ->
                    NavigationDrawerItem(
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(tab.title) },
                        selected = (!showSeparateSettingsPage && currentTab == tab),
                        onClick = {
                            currentTab = tab
                            showSeparateSettingsPage = false
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Share, contentDescription = "Invite Friends", tint = MaterialTheme.colorScheme.primary) },
                    label = { Text("Invite Friends / Share App", fontWeight = FontWeight.SemiBold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(
                                android.content.Intent.EXTRA_TEXT,
                                "Hey! Check out Invoice Generator App for instant invoice creation, inventory tracking & billing: https://github.com/sunuoy/invioce-app"
                            )
                            type = "text/plain"
                        }
                        val shareIntent = android.content.Intent.createChooser(sendIntent, "Invite via")
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_invite_item")
                )
                Spacer(modifier = Modifier.height(6.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Application preferences") },
                    label = { Text("Application Settings") },
                    selected = showSeparateSettingsPage,
                    onClick = {
                        showSeparateSettingsPage = true
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_settings_item")
                )
                Spacer(modifier = Modifier.height(6.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.error) },
                    label = { Text("Sign Out", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.logoutUser()
                    },
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .testTag("drawer_logout_item")
                )
                Spacer(modifier = Modifier.height(12.dp))
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
