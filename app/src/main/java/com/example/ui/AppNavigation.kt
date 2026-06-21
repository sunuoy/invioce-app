package com.example.ui

import androidx.compose.animation.*
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

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
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
                                icon = { Icon(tab.icon, contentDescription = tab.title) },
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
                        .padding(innerPadding)

                    val menuAction: () -> Unit = { scope.launch { drawerState.open() } }

                    when (targetTab) {
                        AppTab.DASHBOARD -> {
                            DashboardScreen(
                                viewModel = viewModel,
                                onCreateInvoiceClicked = { currentTab = AppTab.INVOICES },
                                onViewInvoiceDetails = { invoiceId ->
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
