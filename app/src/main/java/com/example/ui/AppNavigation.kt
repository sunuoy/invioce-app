package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.data.UserAccount
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
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
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    var currentTab by remember { mutableStateOf(AppTab.DASHBOARD) }

    LaunchedEffect(currentUser) {
        if (currentUser?.role == "User" && currentTab == AppTab.DASHBOARD) {
            currentTab = AppTab.INVOICES
        }
    }

    if (currentUser == null) {
        LoginScreen(viewModel = viewModel)
        return
    }

    var showSeparateSettingsPage by remember { mutableStateOf(false) }
    var startInvoicesInCreateMode by remember { mutableStateOf(false) }
    var viewInvoiceId by remember { mutableStateOf<Int?>(null) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

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

                val tabsToDisplay = if (currentUser?.role == "User") {
                    AppTab.values().filter { it != AppTab.DASHBOARD }
                } else {
                    AppTab.values().toList()
                }

                tabsToDisplay.forEach { tab ->
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
                        val bottomTabs = if (currentUser?.role == "User") {
                            AppTab.values().filter { it != AppTab.DASHBOARD }
                        } else {
                            AppTab.values().toList()
                        }

                        bottomTabs.forEach { tab ->
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
fun LoginScreen(viewModel: InvoiceViewModel) {
    val users by viewModel.allUserAccounts.collectAsStateWithLifecycle()
    var selectedUserForLogin by remember { mutableStateOf<UserAccount?>(null) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF6366F1).copy(alpha = 0.15f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.2f),
                        radius = size.maxDimension * 0.5f
                    ),
                    radius = size.maxDimension * 0.5f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.1f, size.height * 0.2f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF8B5CF6).copy(alpha = 0.1f), Color.Transparent),
                        center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.8f),
                        radius = size.maxDimension * 0.4f
                    ),
                    radius = size.maxDimension * 0.4f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.8f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header Logo/Title
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "⚡ Invoice Easy Pro",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Access Control Portal",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }

            // Profile Selection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.8f)),
                elevation = CardDefaults.cardElevation(8.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select User Profile",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (users.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF6366F1))
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            users.forEach { user ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedUserForLogin = user }
                                        .background(
                                            Color(0xFF0F172A).copy(alpha = 0.6f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Avatar Circle
                                        val avatarChar = user.username.firstOrNull()?.uppercase() ?: "?"
                                        val avatarBgColor = if (user.role == "Admin") Color(0xFF6366F1) else Color(0xFF0D9488)
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(avatarBgColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = avatarChar,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = user.username,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Tap to enter passcode",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.White.copy(alpha = 0.5f)
                                            )
                                        }
                                    }

                                    // Role Badge
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (user.role == "Admin") Color(0xFF6366F1).copy(alpha = 0.2f) else Color(0xFF0D9488).copy(alpha = 0.2f),
                                        contentColor = if (user.role == "Admin") Color(0xFF818CF8) else Color(0xFF2DD4BF)
                                    ) {
                                        Text(
                                            text = user.role,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedUserForLogin?.let { user ->
        PasscodeEntryDialog(
            username = user.username,
            onDismiss = { selectedUserForLogin = null },
            onLogin = { passcode ->
                viewModel.loginUser(user.username, passcode) { success ->
                    if (success) {
                        selectedUserForLogin = null
                        Toast.makeText(context, "Logged in as ${user.username}", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Incorrect Passcode!", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
fun PasscodeEntryDialog(
    username: String,
    onDismiss: () -> Unit,
    onLogin: (String) -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Enter Passcode for $username",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Please enter the passcode assigned to this profile:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it },
                    placeholder = { Text("Enter Passcode") },
                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = null)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("passcode_dialog_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onLogin(passcode) },
                enabled = passcode.isNotEmpty(),
                modifier = Modifier.testTag("passcode_dialog_login_btn")
            ) {
                Text("Login")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
