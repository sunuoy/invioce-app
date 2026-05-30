package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.screens.*

enum class AppTab(val title: String, val icon: ImageVector, val tag: String) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard, "dashboard_tab"),
    INVOICES("Invoices", Icons.Default.ReceiptLong, "invoices_tab"),
    PRODUCTS("Inventory", Icons.Default.Inventory2, "products_tab"),
    CUSTOMERS("Clients", Icons.Default.Group, "customers_tab"),
    SETTINGS("Settings", Icons.Default.Settings, "settings_tab")
}

@Composable
fun MainAppNavigation(
    viewModel: InvoiceViewModel,
    modifier: Modifier = Modifier
) {
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }

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

            when (targetTab) {
                AppTab.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onCreateInvoiceClicked = { currentTab = AppTab.INVOICES },
                        onViewInvoiceDetails = { invoiceId ->
                            currentTab = AppTab.INVOICES
                            // Navigation within Invoices screen is reactive; we route there
                        },
                        modifier = screenModifier
                    )
                }
                AppTab.INVOICES -> {
                    InvoicesScreen(
                        viewModel = viewModel,
                        modifier = screenModifier
                    )
                }
                AppTab.PRODUCTS -> {
                    ProductsScreen(
                        viewModel = viewModel,
                        modifier = screenModifier
                    )
                }
                AppTab.CUSTOMERS -> {
                    CustomersScreen(
                        viewModel = viewModel,
                        modifier = screenModifier
                    )
                }
                AppTab.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel,
                        modifier = screenModifier
                    )
                }
            }
        }
    }
}
